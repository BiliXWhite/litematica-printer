pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net") { name = "Fabric" }
        maven("https://jitpack.io") { name = "Jitpack" }
    }
    resolutionStrategy {
        eachPlugin {
            when (requested.id.id) {
                "com.replaymod.preprocess" -> {
                    useModule("com.github.Fallen-Breath:preprocessor:${requested.version}")
                }
            }
        }
    }
}

val versions = listOf(
    "1.18.2",
    "1.19.4",
    "1.20.1",
    "1.20.2",
    "1.20.4",
    "1.20.6",
    "1.21.1",
    "1.21.3",
    "1.21.4",
    "1.21.5",
    "1.21.6",
    "1.21.9",
    "1.21.11"
)

for (version in versions) {
    include(":$version")
    project(":$version").apply {
        projectDir = file("versions/$version")
        buildFileName = "../../build.fabric.gradle.kts"
    }
}

include(":fabricWrapper")