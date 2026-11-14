pluginManagement {
    includeBuild("plugin")

    repositories {
        gradlePluginPortal()
        maven("https://eldonexus.de/repository/maven-public/")
    }
}

rootProject.name = "minestom-events"

include("core")
include("example")
