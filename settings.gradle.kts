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
            if (requested.id.id == "com.replaymod.preprocess") {
                useModule("com.github.Fallen-Breath:preprocessor:${requested.version}")
            }
        }
    }
}


val versions = listOf(
    "1.18.2",
    "1.19.4",
    "1.20.1", "1.20.2", "1.20.4", "1.20.6",
    "1.21.1", "1.21.3", "1.21.4", "1.21.5", "1.21.6", "1.21.9", "1.21.11",
    "26.1"
)

for (version in versions) {
    include(":$version")
    project(":$version").apply {
        projectDir = file("versions/$version")
        buildFileName = if (parseMcVersionToNumber(version) > 260000) {
            "../../build.fabric.gradle.kts"
        } else {
            "../../build.fabric.remap.gradle.kts"
        }
    }
}

include(":fabricWrapper")

fun parseMcVersionToNumber(mcVersionStr: String): Int {
    if (mcVersionStr.isBlank()) return 0
    return try {
        val cleanVersion = mcVersionStr.split("-")[0].replace(Regex("[^0-9.]"), "")
        val versionParts = cleanVersion.split(".").filter { it.isNotEmpty() }
        val major = versionParts.getOrNull(0)?.toIntOrNull() ?: 0
        val minor = versionParts.getOrNull(1)?.toIntOrNull() ?: 0
        val patch = versionParts.getOrNull(2)?.toIntOrNull() ?: 0
        major * 10000 + minor * 100 + patch
    } catch (e: Exception) {
        0
    }
}