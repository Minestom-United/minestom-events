plugins {
    id("java-gradle-plugin")
    id("net.kyori.blossom") version "2.1.0"
}

group = "dev.minestomunited"
version = "1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
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
    plugins {
        create("minestomEvents") {
            id = "dev.minestomunited.minestom-events"
            implementationClass = "dev.minestomunited.minestomevents.plugin.MinestomEventsPlugin"
            displayName = "Minestom Events"
        }
    }
}
