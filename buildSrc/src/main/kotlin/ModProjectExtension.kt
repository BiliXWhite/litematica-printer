import org.gradle.api.Project
import org.gradle.api.GradleException
import org.gradle.api.JavaVersion
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

fun Project.propOrNull(key: String) = findProperty(key)
fun Project.prop(key: String) = propOrNull(key) ?: throw GradleException("属性 $key 未配置/值为空")

fun Project.propStrOrNull(key: String): String? = propOrNull(key)?.toString()
fun Project.propStr(key: String): String = propStrOrNull(key)
    ?: throw GradleException("属性 $key 未配置/值为空，或无法转换为字符串")

fun Project.downloadDependencyMod(downloadUrl: String, fileName: String? = null): File? {
    return rootProject.downloadFile(
        downloadUrl = downloadUrl,
        outputDirPath = "${rootProject.projectDir}/libs",
        fileName = fileName
    )
}

val Project.modId get() = propStr("mod_id")
val Project.wrapperModId get() = "$modId-wrapper"
val Project.modName get() = propStr("mod_name")
val Project.modVersion get() = propStr("mod_version")
val Project.modMavenGroup get() = propStr("mod_maven_group")
val Project.modArchivesBaseName get() = propStr("mod_archives_base_name")

val Project.modDescription get() = propStrOrNull("mod_description")
val Project.modHomepage get() = propStrOrNull("mod_homepage")
val Project.modLicense get() = propStrOrNull("mod_license")
val Project.modSources get() = propStrOrNull("mod_sources")

val Project.mcDependency get() = propStrOrNull("minecraft_dependency")
val Project.mcVersion get() = propStrOrNull("minecraft_version")
val Project.mcVersionInt get() = propStrOrNull("mcVersion")?.toIntOrNull() ?: -1
val Project.fabricLoaderVersion get() = propStrOrNull("loader_version")
val Project.fabricApiVersion get() = propStrOrNull("fabric_version")

val Project.lombokVersion get() = propStr("lombok_version")

val Project.javaVersion
    get() = when {
        mcVersionInt >= 260000 -> JavaVersion.VERSION_25
        mcVersionInt >= 12005 -> JavaVersion.VERSION_21
        mcVersionInt >= 11800 -> JavaVersion.VERSION_17
        mcVersionInt >= 11700 -> JavaVersion.VERSION_16
        else -> JavaVersion.VERSION_1_8
    }
val Project.mixinJavaVersion get() = "JAVA_${javaVersion}"


val Project.fullProjectVersion: String get() = getFullProjectVersion(modVersion)

private fun getFullProjectVersion(modVersion: String): String {
    val time = SimpleDateFormat("yyMMdd")
        .apply { timeZone = TimeZone.getTimeZone("GMT+08:00") }
        .format(Date())
        .toString()
    var version = "$modVersion+$time"
    if (System.getenv("IS_THIS_RELEASE") == "false") {
        val buildNumber: String? = System.getenv("GITHUB_RUN_NUMBER")
        if (buildNumber != null) {
            version += "+build.$buildNumber"
        }
    }
    return version;
}

val Project.placeholderProps: Map<String, Any?>
    get() = mapOf(
        "mod_id" to modId,
        "mod_wrapper_id" to wrapperModId,
        "mod_name" to modName,
        "mod_version" to fullProjectVersion,
        "mod_description" to modDescription,
        "mod_homepage" to modHomepage,
        "mod_license" to modLicense,
        "mod_sources" to modSources,
        "loader_version" to fabricLoaderVersion,
        "fabric_api_version" to fabricApiVersion,
        "minecraft_dependency" to mcDependency,
        "compatibility_level" to mixinJavaVersion
    ).filterValues { it != null }.mapValues { it.value!! }