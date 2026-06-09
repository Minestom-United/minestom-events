plugins {
    id("java-gradle-plugin")
    id("net.kyori.blossom") version "2.1.0"
    id("com.gradle.plugin-publish") version "2.1.1"
}

group = "dev.minestom-united"
version = "0.0.2"

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
    withSourcesJar()
    withJavadocJar()
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.github.classgraph:classgraph:4.8.179")
}

sourceSets {
    main {
        blossom {
            javaSources {
                property("version", project.version.toString())
            }
        }
    }
}

gradlePlugin {
    website = "https://github.com/Minestom-United/minestom-events"
    vcsUrl = "https://github.com/Minestom-United/minestom-events"
    plugins {
        create("minestomEvents") {
            id = "dev.minestom-united.minestom-events"
            implementationClass = "dev.minestomunited.minestomevents.plugin.MinestomEventsPlugin"
            displayName = "Minestom Events"
            description =
                "Gradle plugin that generates a typed event API for Minestom by scanning event classes at build time"
            tags = listOf("minestom", "generate", "events")
        }
    }
}
