plugins {
    java
    id("dev.minestom-united.minestom-events")
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
    implementation("net.minestom:minestom:2026.08.28-26.2")
    implementation(project(":core"))
}
