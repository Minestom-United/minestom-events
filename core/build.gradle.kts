plugins {
    id("java-library")
    `maven-publish`
    id("com.vanniktech.maven.publish") version "0.36.0"
}

group = "dev.minestom-united.minestom-events-core"
version = "0.0.1"

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
    withSourcesJar()
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("net.minestom:minestom:2026.06.05-26.1.2")
}

mavenPublishing {
    coordinates("dev.minestom-united", "minestom-events-core", version as String?)

    publishToMavenCentral()
    signAllPublications()

    pom {
        name = project.name
        description = "Runtime library for the minestom-events typed event API for Minestom"
        url = "https://github.com/Minestom-United/minestom-events"

        licenses {
            license {
                name = "MIT"
                url = "https://github.com/Minestom-United/minestom-events/blob/master/LICENSE"
            }
        }

        developers {
            developer {
                id = "Webhead1104"
                url = "https://github.com/Webhead1104"
            }
        }

        issueManagement {
            system = "Github"
            url = "https://github.com/Minestom-United/minestom-events/issues"
        }

        scm {
            url.set("https://github.com/Minestom-United/minestom-events")
            connection.set("scm:git:git://github.com/Minestom-United/minestom-events.git")
            developerConnection.set("scm:git:git@github.com:Minestom-United/minestom-events.git")
        }
    }
}
