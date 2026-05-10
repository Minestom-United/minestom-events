package dev.minestomunited.minestomevents.plugin;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.jvm.tasks.Jar;

public class MinestomEventsPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        project.getPlugins().apply(JavaPlugin.class);

        MinestomEventsExtension ext = project.getExtensions().create(
            "minestomEvents", MinestomEventsExtension.class
        );
        ext.getCompileOnly().convention(false);
        ext.getScanPackages().convention(java.util.List.of());

        var generatedDir = project.getLayout().getBuildDirectory()
            .dir("generated/minestom-events/java");

        project.getRepositories().maven(repo ->
            repo.setUrl("https://repo.minestom-united.dev/"));

        Project coreProject = project.getRootProject().getAllprojects().stream()
            .filter(p -> "dev.minestomunited".equals(p.getGroup().toString())
                && ":core".equals(p.getPath()))
            .findFirst().orElse(null);
        if (coreProject != null) {
            project.getDependencies().add(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME, coreProject);
        } else {
            project.getRepositories().maven(repo -> {
                boolean isSnapshot = BuildConstants.VERSION.endsWith("-SNAPSHOT");
                repo.setUrl(isSnapshot
                    ? "https://repo.minestom-united.dev/snapshots"
                    : "https://repo.minestom-united.dev/releases");
            });
            project.getDependencies().add(
                JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME,
                "dev.minestomunited:minestom-events:" + BuildConstants.VERSION
            );
        }

        JavaPluginExtension javaExt = project.getExtensions().getByType(JavaPluginExtension.class);
        SourceSet main = javaExt.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);

        TaskProvider<EventsGeneratorTask> generateTask = project.getTasks()
            .register("generateEvents", EventsGeneratorTask.class, task -> {
                task.setDescription("Generates Events facade from Minestom event classes.");
                task.setGroup("minestom-events");
                task.getClasspath().from(
                    project.getConfigurations().named(JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME)
                );
                task.getSourceDirs().from(main.getJava().getSrcDirs());
                task.getOutputPackage().set(ext.getOutputPackage());
                task.getScanPackages().set(ext.getScanPackages());
                task.getOutputDir().set(generatedDir);
            });

        main.getJava().srcDir(generatedDir);

        project.getTasks().named(main.getCompileJavaTaskName())
            .configure(t -> t.dependsOn(generateTask));

        project.getTasks().named("jar", Jar.class).configure(jar ->
            jar.exclude(e -> {
                if (!ext.getCompileOnly().get()) return false;
                String path = e.getRelativePath().getPathString();
                String genPath = ext.getOutputPackage().get().replace('.', '/');
                return path.startsWith(genPath);
            })
        );
    }
}
