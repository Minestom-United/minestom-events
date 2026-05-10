package dev.minestomunited.minestomevents.plugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

@CacheableTask
public abstract class EventsGeneratorTask extends DefaultTask {

    private static final String MINESTOM_EVENT = "net.minestom.server.event.Event";
    private static final String MINESTOM_EVENT_ANNO = "dev.minestomunited.minestomevents.MinestomEvent";

    private static final Pattern RE_PACKAGE = Pattern.compile(
        "^\\s*package\\s+([\\w.]+)\\s*;", Pattern.MULTILINE);
    private static final Pattern RE_CLASS = Pattern.compile(
        "(?:^|\\s)public\\s+(?:final\\s+)?(?:class|record)\\s+(\\w+)", Pattern.MULTILINE);
    private static final Pattern RE_ANNO = Pattern.compile(
        "@MinestomEvent(?:\\s|\\(|$)");
    private static final Pattern RE_IMPLEMENTS_EVENT = Pattern.compile(
        "\\bimplements\\b[^{]*\\bEvent\\b");
    private static final Pattern RE_DEPRECATED_FOR_REMOVAL = Pattern.compile(
        "@Deprecated\\s*\\(\\s*forRemoval\\s*=\\s*true");
    private static final Pattern RE_EXTENDS_EVENT = Pattern.compile(
        "\\bclass\\s+\\w+(?:<[^>]*>)?\\s+extends\\s+\\w*Event\\b");

    @InputFiles
    @PathSensitive(PathSensitivity.NONE)
    public abstract ConfigurableFileCollection getClasspath();

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getSourceDirs();

    @Input
    public abstract Property<String> getOutputPackage();

    @Input
    public abstract ListProperty<String> getScanPackages();

    @Input
    public abstract Property<Boolean> getExcludeDeprecatedForRemoval();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDir();

    @TaskAction
    public void generate() throws Exception {
        String pkg = getOutputPackage().get();
        File pkgDir = new File(getOutputDir().get().getAsFile(), pkg.replace('.', '/'));
        pkgDir.mkdirs();

        Set<String> eventClasses = new LinkedHashSet<>();

        scanSourceFiles(eventClasses);
        scanClasspath(eventClasses);

        List<String> sorted = new ArrayList<>(eventClasses);
        sorted.sort(Comparator.naturalOrder());

        getLogger().lifecycle("minestom-events: generated Events.java with {} event methods", sorted.size());
        Files.writeString(
            new File(pkgDir, "Events.java").toPath(),
            buildSource(pkg, sorted)
        );
    }

    private void scanSourceFiles(Set<String> eventClasses) {
        for (File srcDir : getSourceDirs().getFiles()) {
            if (!srcDir.isDirectory()) continue;
            scanDir(srcDir, eventClasses);
        }
    }

    private void scanDir(File dir, Set<String> eventClasses) {
        File[] entries = dir.listFiles();
        if (entries == null) return;
        for (File f : entries) {
            if (f.isDirectory()) {
                scanDir(f, eventClasses);
            } else if (f.getName().endsWith(".java")) {
                parseSourceFile(f, eventClasses);
            }
        }
    }

    private void parseSourceFile(File file, Set<String> eventClasses) {
        String src;
        try {
            src = Files.readString(file.toPath());
        } catch (IOException e) {
            return;
        }

        if (!RE_ANNO.matcher(src).find()
            && !RE_IMPLEMENTS_EVENT.matcher(src).find()
            && !RE_EXTENDS_EVENT.matcher(src).find()) return;
        if (getExcludeDeprecatedForRemoval().get() && RE_DEPRECATED_FOR_REMOVAL.matcher(src).find()) return;

        Matcher pkgMatcher = RE_PACKAGE.matcher(src);
        if (!pkgMatcher.find()) return;
        String filePkg = pkgMatcher.group(1);

        Matcher classMatcher = RE_CLASS.matcher(src);
        if (!classMatcher.find()) return;
        String className = classMatcher.group(1);

        eventClasses.add(filePkg + "." + className);
    }

    private void scanClasspath(Set<String> eventClasses) {
        List<File> classpathFiles = getClasspath().getFiles().stream()
            .filter(File::exists)
            .toList();

        List<String> packages = getScanPackages().get();

        ClassGraph cg = new ClassGraph()
            .enableClassInfo()
            .enableAnnotationInfo()
            .overrideClasspath(classpathFiles);

        if (!packages.isEmpty()) {
            cg.acceptPackages(packages.toArray(new String[0]));
        }

        try (ScanResult result = cg.scan()) {
            ClassInfoList annotated = result.getClassesWithAnnotation(MINESTOM_EVENT_ANNO);
            ClassInfoList subtypes = result.getClassesImplementing(MINESTOM_EVENT);

            annotated.stream()
                .filter(this::isConcretePublic)
                .filter(ci -> !isDeprecatedForRemoval(ci))
                .map(ClassInfo::getName)
                .forEach(eventClasses::add);

            subtypes.stream()
                .filter(this::isConcretePublic)
                .filter(ci -> !isDeprecatedForRemoval(ci))
                .map(ClassInfo::getName)
                .forEach(eventClasses::add);
        }
    }

    private boolean isDeprecatedForRemoval(ClassInfo ci) {
        if (!getExcludeDeprecatedForRemoval().get()) return false;
        var anno = ci.getAnnotationInfo("java.lang.Deprecated");
        if (anno == null) return false;
        var param = anno.getParameterValues().getValue("forRemoval");
        return Boolean.TRUE.equals(param);
    }

    private boolean isConcretePublic(ClassInfo ci) {
        return ci.isPublic()
            && !ci.isAbstract()
            && !ci.isInterface()
            && !ci.isInnerClass()
            && !ci.isAnonymousInnerClass();
    }

    private String buildSource(String pkg, List<String> eventClasses) {
        Map<String, String> fqnToMethod = resolveMethodNames(eventClasses);

        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(pkg).append(";\n\n");
        sb.append("import dev.minestomunited.minestomevents.EventFilter;\n");
        sb.append("import dev.minestomunited.minestomevents.EventRegistrar;\n");
        sb.append("import java.util.function.Consumer;\n\n");
        sb.append("public final class Events {\n");
        sb.append("    private Events() {}\n\n");

        sb.append("    public static <E extends net.minestom.server.event.Event> void on(\n");
        sb.append("            Class<E> eventClass, Consumer<E> handler) {\n");
        sb.append("        EventRegistrar.register(eventClass, handler);\n");
        sb.append("    }\n\n");
        sb.append("    public static <E extends net.minestom.server.event.Event> void on(\n");
        sb.append("            Class<E> eventClass, EventFilter filter, Consumer<E> handler) {\n");
        sb.append("        EventRegistrar.register(eventClass, filter, handler);\n");
        sb.append("    }\n\n");

        for (String fqn : eventClasses) {
            String method = fqnToMethod.get(fqn);

            sb.append("    public static void ").append(method)
                .append("(Consumer<").append(fqn).append("> handler) {\n");
            sb.append("        EventRegistrar.register(").append(fqn).append(".class, handler);\n");
            sb.append("    }\n\n");

            sb.append("    public static void ").append(method)
                .append("(EventFilter filter, Consumer<").append(fqn).append("> handler) {\n");
            sb.append("        EventRegistrar.register(").append(fqn).append(".class, filter, handler);\n");
            sb.append("    }\n\n");
        }

        sb.append("}\n");
        return sb.toString();
    }

    private Map<String, String> resolveMethodNames(List<String> eventClasses) {
        Map<String, List<String>> byMethod = new LinkedHashMap<>();
        for (String fqn : eventClasses) {
            String simple = fqn.substring(fqn.lastIndexOf('.') + 1);
            byMethod.computeIfAbsent(toMethodName(simple), _ -> new ArrayList<>()).add(fqn);
        }

        Map<String, String> fqnToMethod = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : byMethod.entrySet()) {
            if (entry.getValue().size() == 1) {
                fqnToMethod.put(entry.getValue().getFirst(), entry.getKey());
            } else {
                for (String fqn : entry.getValue()) {
                    fqnToMethod.put(fqn, toQualifiedMethodName(fqn));
                }
            }
        }
        return fqnToMethod;
    }

    private static String toMethodName(String simpleName) {
        String stripped = simpleName.endsWith("Event")
            ? simpleName.substring(0, simpleName.length() - 5)
            : simpleName;
        if (stripped.isEmpty()) return "on";
        return "on" + Character.toUpperCase(stripped.charAt(0)) + stripped.substring(1);
    }

    private static final Set<String> SKIP_PKG_SEGMENTS = Set.of(
        "net", "com", "io", "org", "dev",
        "github", "gitlab",
        "server", "event", "events", "api", "lib"
    );

    private static String toQualifiedMethodName(String fqn) {
        String[] parts = fqn.split("\\.");
        String className = parts[parts.length - 1];
        String stripped = className.endsWith("Event")
            ? className.substring(0, className.length() - 5)
            : className;

        // Find first "event"/"events" package segment, then walk backward for library name
        int eventIdx = -1;
        for (int i = 0; i < parts.length - 1; i++) {
            if (parts[i].equalsIgnoreCase("event") || parts[i].equalsIgnoreCase("events")) {
                eventIdx = i;
                break;
            }
        }

        String qualifier = "";
        int searchFrom = eventIdx >= 0 ? eventIdx - 1 : parts.length - 2;
        for (int i = searchFrom; i >= 0; i--) {
            String seg = parts[i];
            if (SKIP_PKG_SEGMENTS.contains(seg.toLowerCase())) continue;
            if (seg.chars().anyMatch(Character::isDigit)) continue;
            qualifier = Character.toUpperCase(seg.charAt(0)) + seg.substring(1);
            break;
        }
        return "on" + qualifier + (stripped.isEmpty() ? ""
            : Character.toUpperCase(stripped.charAt(0)) + stripped.substring(1));
    }
}
