plugins {
    id("java-gradle-plugin")
    id("net.kyori.blossom") version "2.1.0"
    `maven-publish`
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

publishing {
    publications {
        withType<MavenPublication> {
            if (name == "pluginMaven") {
                artifactId = "minestom-events"
            }
        }
    }

    repositories {
        maven {
            name = "MinestomUnitedRepository"
            val isSnapshot = version.toString().endsWith("-SNAPSHOT")
            url = uri(
                if (isSnapshot)
                    "https://repo.minestom-united.dev/snapshots"
                else "https://repo.minestom-united.dev/releases"
            )

            var u = System.getenv("REPO_USERNAME")
            var p = System.getenv("REPO_PASSWORD")

            if (u == null || u.isEmpty()) u = "no-value-provided"
            if (p == null || p.isEmpty()) p = "no-value-provided"

            val user = providers.gradleProperty("MinestomUnitedRepositoryUsername").orElse(u).get()
            val pass = providers.gradleProperty("MinestomUnitedRepositoryPassword").orElse(p).get()

            credentials {
                username = user
                password = pass
            }
            authentication {
                create<BasicAuthentication>("basic")
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
