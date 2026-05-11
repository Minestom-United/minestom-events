plugins {
    java
    id("dev.minestomunited.minestom-events")
}

group = "dev.minestomunited"
version = "1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

minestomEvents {
    outputPackage.set("dev.minestomunited.example.generated")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("net.minestom:minestom:2026.05.11-1.21.11")
}
