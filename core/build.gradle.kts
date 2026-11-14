plugins {
    `java-library`
    alias(libs.plugins.publish)
}

group = "dev.minestom-united"
version = "0.0.4"
description = "Runtime library for the minestom-events typed event API for Minestom"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(libs.minestom)
}

java {
    withSourcesJar()
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

val isSnapshot = version.toString().endsWith("-SNAPSHOT")

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()

    pom {
        name = "minestom-events-core"
        description = project.description
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
            url = "https://github.com/Minestom-United/minestom-events"
            connection = "scm:git:git://github.com/Minestom-United/minestom-events.git"
            developerConnection = "scm:git:git@github.com:Minestom-United/minestom-events.git"
        }
    }
}
