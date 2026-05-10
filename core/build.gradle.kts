plugins {
    id("java-library")
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
    compileOnly("net.minestom:minestom:2026.04.13-1.21.11")
}
