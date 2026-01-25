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
    includeBuild("build-logic")
}


val versions = listOf(
    "1.18.2",
    "1.19.4",
    "1.20.1", "1.20.2", "1.20.4", "1.20.6",
    "1.21.1", "1.21.3", "1.21.4", "1.21.5", "1.21.6", "1.21.9", "1.21.11"
//    , "26.1"
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

// 暂时不了解怎么写到插件里, 先丢在这里吧
fun parseMcVersionToNumber(mcVersionStr: String): Int {
    val cleanVersion = mcVersionStr.split("-")[0] // 去掉 -fabric/-pre/-rc 等后缀
        .replace(Regex("[^0-9.]"), "") // 移除所有非数字、非点的字符
    val versionParts = cleanVersion.split(".")
        .filter { it.isNotEmpty() } // 过滤空字符串（避免异常分割）
    val major = versionParts.getOrNull(0)?.toIntOrNull() ?: 0
    val minor = versionParts.getOrNull(1)?.toIntOrNull() ?: 0
    val patch = versionParts.getOrNull(2)?.toIntOrNull() ?: 0
    return major * 10000 + minor * 100 + patch
}