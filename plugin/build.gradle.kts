plugins {
    id("java-gradle-plugin")
    id("com.gradle.plugin-publish") version "2.2.1"
    id("com.gradleup.shadow") version "9.6.1"
}

group = "dev.minestom-united"
version = "0.0.4"

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
    withSourcesJar()
    withJavadocJar()
}

repositories {
    mavenCentral()
    maven("https://eldonexus.de/repository/maven-public/")
}

dependencies {
    implementation("io.github.classgraph:classgraph:6.0.2")
    implementation("net.strokkur.japutil:code-gen:0.1.4")
}

tasks.shadowJar {
    archiveClassifier = ""
    relocate("net.strokkur.japutil", "dev.minestomunited.minestomevents.plugin.shaded.japutil")
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
            tags = listOf("minestom", "minecraft", "generate", "events")
        }
    }
}
