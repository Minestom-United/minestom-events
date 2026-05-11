plugins {
    id("java-library")
    `maven-publish`
}

group = "dev.minestomunited"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("net.minestom:minestom:2026.05.11-1.21.11")
}

publishing {
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

    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = "minestom-events-core"
        }
    }
}
