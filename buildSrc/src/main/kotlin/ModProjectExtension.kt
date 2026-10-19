import org.gradle.api.GradleException
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import java.io.File

fun Project.propOrNull(key: String) = findProperty(key)
fun Project.prop(key: String) = propOrNull(key) ?: throw GradleException("buildSrc: 属性 $key 未配置/值为空")

fun Project.propStrOrNull(key: String): String? = propOrNull(key)?.toString()
fun Project.propStr(key: String): String = propStrOrNull(key)
    ?: throw GradleException("buildSrc: 属性 $key 未配置/值为空，或无法转换为字符串")

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

val Project.malilib get() = propStrOrNull("malilib")
val Project.litematica get() = propStrOrNull("litematica")

val Project.lombokVersion get() = propStr("lombok_version")
val Project.githubRunNumber get() = System.getenv("GITHUB_RUN_NUMBER")?.takeIf { it.isNotBlank() }
val Project.isReleaseWorkflow get() = System.getenv("IS_THIS_RELEASE")?.equals("true", ignoreCase = true) == true

private val Project.ciBuildVersion: String?
    get() = githubRunNumber?.let { buildNumber ->
        if (isReleaseWorkflow) "beta$buildNumber" else "dev$buildNumber"
    }

val Project.javaVersion
    get() = when {
        mcVersionInt >= 260000 -> JavaVersion.VERSION_25
        mcVersionInt >= 12005 -> JavaVersion.VERSION_21
        mcVersionInt >= 11800 -> JavaVersion.VERSION_17
        mcVersionInt >= 11700 -> JavaVersion.VERSION_16
        else -> JavaVersion.VERSION_1_8
    }
val Project.mixinJavaVersion get() = "JAVA_${javaVersion}"

val Project.fullProjectVersion: String get() {
    return ciBuildVersion ?: "$modVersion-local"
}

private val Project.modVersionFlavorSuffix: String
    get() = modVersion.substringAfter('-', "").let { if (it.isEmpty()) "" else "-$it" }

val Project.artifactVersion: String get() {
    val buildNumber = githubRunNumber
    val baseVersion = mcVersion ?: modVersion.substringBefore('-')
    val flavorSuffix = modVersionFlavorSuffix
    return when {
        buildNumber != null && isReleaseWorkflow -> "$baseVersion$flavorSuffix-beta$buildNumber"
        buildNumber != null -> "$baseVersion$flavorSuffix-dev$buildNumber"
        else -> "$baseVersion$flavorSuffix-local"
    }
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
        "compatibility_level" to mixinJavaVersion,
        "beacon_client_mixins" to if (mcVersionInt < 260200) {
            "\"printer.mc.BeaconBlockEntityAccessor\",\n" +
                    "    \"printer.mc.BeaconButtonAccessor\",\n" +
                    "    \"printer.mc.BeaconScreenMixin\","
        } else {
            ""
        },
        "chesttracker_client_mixins" to if (mcVersionInt >= 12104) {
            "\"printer.chesttracker.ItemListWidgetAccessor\",\n" +
                    "    \"printer.chesttracker.ItemListWidgetMixin\","
        } else {
            ""
        },
        // 虚空仓库的库存查询只在 1.21.11+ 存在（服务端 carpet-Neko-Addition 也只支持这几个版本）。
        // 老版本上这些混入类整个被 //#if 剔掉，这里必须同步留空 ——
        // mixins.json 不做预处理，写死一个不存在的类名会让 Mixin 在启动阶段直接报错。
        //
        // 保留 missing 的施工需求语义，只在 GUI/HUD 显示收集缺口。
        "void_warehouse_litematica_mixins" to if (mcVersionInt >= 12111) {
            "\"printer.litematica.MixinMaterialListEntry\",\n" +
                    "    \"printer.malilib.GuiListBaseAccessor\",\n" +
                    "    \"printer.litematica.MixinMaterialListSorter\",\n" +
                    "    \"printer.litematica.MixinMaterialListHudRenderer\",\n" +
                    "    \"printer.litematica.MixinWidgetMaterialListEntry\","
        } else {
            ""
        },
        "malilib" to malilib,
        "litematica" to litematica
    ).filterValues { it != null }.mapValues { it.value!! }
