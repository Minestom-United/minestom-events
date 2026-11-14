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
import net.strokkur.jap.code.CodeGenUtil;
import net.strokkur.jap.code.annotations.CodeAnnotation;
import net.strokkur.jap.code.classmodel.CodeClass;
import net.strokkur.jap.code.classmodel.CodeMethod;
import net.strokkur.jap.code.classmodel.CodeParameterDefinition;
import net.strokkur.jap.code.classmodel.builder.ClassBuilder;
import net.strokkur.jap.code.expression.Expressions;
import net.strokkur.jap.code.expression.simple.CodeStringExpression;
import net.strokkur.jap.code.statement.Statements;
import net.strokkur.jap.code.type.CodeClassType;
import net.strokkur.jap.code.type.CodeTypes;
import net.strokkur.jap.code.type.generic.CodeGenericTypeDefinition;
import net.strokkur.jap.code.type.generic.GenericEnclosure;
import net.strokkur.jap.code.util.Modifiers;
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

import static net.strokkur.jap.code.type.CodeTypes.generic;

@CacheableTask
public abstract class EventsGeneratorTask extends DefaultTask {

    private static final String MINESTOM_EVENT = "net.minestom.server.event.Event";

    private static final Pattern RE_PACKAGE = Pattern.compile(
        "^\\s*package\\s+([\\w.]+)\\s*;", Pattern.MULTILINE);
    private static final Pattern RE_CLASS = Pattern.compile(
        "(?:^|\\s)public\\s+(?:final\\s+)?(?:class|record)\\s+(\\w+)", Pattern.MULTILINE);
    private static final Pattern RE_IMPLEMENTS_EVENT = Pattern.compile(
        "\\bimplements\\b[^{]*\\bEvent\\b");
    private static final Pattern RE_EXTENDS_EVENT = Pattern.compile(
        "\\bclass\\s+\\w+(?:<[^>]*>)?\\s+extends\\s+\\w*Event\\b");
    private static final CodeClassType MINESTOM_EVENT_CODE_TYPE = CodeTypes.of("net.minestom.server.event.Event");
    private static final CodeClassType JAVA_CLASS_CODE_TYPE = CodeTypes.of("java.lang.Class");
    private static final CodeClassType JAVA_CONSUMER_CODE_TYPE = CodeTypes.of("java.util.function.Consumer");
    private static final CodeClassType EVENT_REGISTRAR = CodeTypes.of(
        "dev.minestomunited.minestomevents.EventRegistrar");
    private static final CodeClassType EVENT_HANDLE = CodeTypes.of(
        "dev.minestomunited.minestomevents.EventHandle");
    private static final CodeClassType EVENT_FILTER = CodeTypes.of(
        "dev.minestomunited.minestomevents.EventFilter");

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

    @OutputDirectory
    public abstract DirectoryProperty getOutputDir();

    @TaskAction
    public void generate() throws Exception {
        String pkg = getOutputPackage().get();
        File pkgDir = new File(getOutputDir().get().getAsFile(), pkg.replace('.', '/'));
        if (!pkgDir.exists() && !pkgDir.mkdirs()) {
            getLogger().error("Could not create directory: {}", pkgDir);
            return;
        }

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

        if (!RE_IMPLEMENTS_EVENT.matcher(src).find()
            && !RE_EXTENDS_EVENT.matcher(src).find()) return;

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
            .enableClasspathEntries(classpathFiles);

        if (!packages.isEmpty()) {
            cg.acceptPackages(packages.toArray(new String[0]));
        }

        try (ScanResult result = cg.scan()) {
            ClassInfoList subtypes = result.getAllClassesImplementing(MINESTOM_EVENT);

            subtypes.stream()
                .filter(this::isConcretePublic)
                .map(ClassInfo::getName)
                .forEach(eventClasses::add);
        }
    }

    private boolean isConcretePublic(ClassInfo ci) {
        return ci.isPublic()
            && !ci.isAbstract()
            && !ci.isInterface()
            && !ci.isNestedClass()
            && !ci.isAnonymousClass();
    }

    private String buildSource(String pkg, List<String> eventClasses) {
        ClassBuilder builder = CodeClass.builder(pkg + ".Events")
            .addModifiers(Modifiers.PUBLIC, Modifiers.FINAL)
            .addAnnotations(
                CodeAnnotation.of(CodeTypes.of(SuppressWarnings.class), new CodeStringExpression("all")))
            .addConstructor(ctor -> ctor.addModifiers(Modifiers.PRIVATE))
            .addMethods(
                CodeMethod.builder("on")
                    .setReturnType(EVENT_HANDLE)
                    .addModifiers(Modifiers.PUBLIC, Modifiers.STATIC)
                    .addGenerics(
                        CodeGenericTypeDefinition.of("E", GenericEnclosure.withExtends(MINESTOM_EVENT_CODE_TYPE)))
                    .addParameters(
                        CodeParameterDefinition.of(JAVA_CLASS_CODE_TYPE.typed(generic("E")), "eventClass"),
                        CodeParameterDefinition.of(JAVA_CONSUMER_CODE_TYPE.typed(generic("E")), "handler")
                    )
                    .addCode(
                        Statements.returnStmt(EVENT_REGISTRAR.chainMethod("register")
                            .addParameters(Expressions.variable("eventClass"), Expressions.variable("handler")))
                    ),
                CodeMethod.builder("on")
                    .setReturnType(EVENT_HANDLE)
                    .addModifiers(Modifiers.PUBLIC, Modifiers.STATIC)
                    .addGenerics(
                        CodeGenericTypeDefinition.of("E", GenericEnclosure.withExtends(MINESTOM_EVENT_CODE_TYPE)))
                    .addParameters(
                        CodeParameterDefinition.of(JAVA_CLASS_CODE_TYPE.typed(generic("E")), "eventClass"),
                        CodeParameterDefinition.of(EVENT_FILTER, "filter"),
                        CodeParameterDefinition.of(JAVA_CONSUMER_CODE_TYPE.typed(generic("E")), "handler")
                    )
                    .addCode(
                        Statements.returnStmt(
                            EVENT_REGISTRAR.chainMethod("register")
                                .addParameters(
                                    Expressions.variable("eventClass"),
                                    Expressions.variable("filter"),
                                    Expressions.variable("handler")
                                )
                        )
                    )
            );

        Map<String, String> fqnToMethod = resolveMethodNames(eventClasses);

        for (String eventClass : eventClasses) {
            CodeClassType type = CodeTypes.of(eventClass);
            String methodName = fqnToMethod.get(eventClass);
            builder.addMethods(
                CodeMethod.builder(methodName)
                    .setReturnType(EVENT_HANDLE)
                    .addModifiers(Modifiers.PUBLIC, Modifiers.STATIC)
                    .addParameters(CodeParameterDefinition.of(JAVA_CONSUMER_CODE_TYPE.typed(type), "handler"))
                    .addCode(
                        Statements.returnStmt(
                            EVENT_REGISTRAR.chainMethod("register")
                                .addParameters(
                                    type.chainField("class"),
                                    Expressions.variable("handler")
                                )
                        )
                    ),
                CodeMethod.builder(methodName)
                    .setReturnType(EVENT_HANDLE)
                    .addModifiers(Modifiers.PUBLIC, Modifiers.STATIC)
                    .addParameters(
                        CodeParameterDefinition.of(EVENT_FILTER, "filter"),
                        CodeParameterDefinition.of(JAVA_CONSUMER_CODE_TYPE.typed(type), "handler")
                    )
                    .addCode(
                        Statements.returnStmt(
                            EVENT_REGISTRAR.chainMethod("register")
                                .addParameters(
                                    type.chainField("class"),
                                    Expressions.variable("filter"),
                                    Expressions.variable("handler")
                                )
                        )
                    )
            );
        }

        return CodeGenUtil.createJavaFile(builder.toClass());
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
