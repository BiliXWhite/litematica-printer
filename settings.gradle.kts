import groovy.json.JsonSlurper

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


val jsonFile = file("settings.json")
val jsonText = jsonFile.readText()
@Suppress("UNCHECKED_CAST")
val settings = JsonSlurper().parseText(jsonText) as Map<String, Any>
@Suppress("UNCHECKED_CAST")
val versions = settings["versions"] as List<String>

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
    } catch (_: Exception) {
        0
    }
}