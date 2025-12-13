@file:Suppress("UnstableApiUsage")

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.text.SimpleDateFormat
import java.util.*

plugins {
    id("maven-publish")
    id("fabric-loom")
    id("com.replaymod.preprocess")
    id("me.fallenbreath.yamlang")
}

// ========================== 基础属性配置 ==========================
val props = project.properties
val mcVersion = props["mcVersion"] as Int
val modId = props["mod_id"] as String
val modWrapperId = props["mod_wrapper_id"] as String
val modName = props["mod_name"] as String
val modMavenGroup = props["mod_maven_group"] as String
val modVersion = props["mod_version"] as String
val modArchivesBaseName = props["mod_archives_base_name"] as String
val modDescription = props["mod_description"] as String
val modHomepage = props["mod_homepage"] as String
val modLicense = props["mod_license"] as String
val modSources = props["mod_sources"] as String
val loaderVersion = props["loader_version"] as String

val minecraftDependency = props["minecraft_dependency"] as String
val minecraftVersion = props["minecraft_version"] as String
val fabricApiVersion = props["fabric_version"] as String

// Java 兼容性配置
val javaCompatibility = when {
    mcVersion >= 12005 -> JavaVersion.VERSION_21    // 1.20.5+      需要 Java 21
    mcVersion >= 11800 -> JavaVersion.VERSION_17    // 1.18-1.20.4  需要 Java 17
    mcVersion >= 11700 -> JavaVersion.VERSION_16    // 1.17.x       需要 Java 16
    else -> JavaVersion.VERSION_1_8                 // 1.16.x 及以下使用 Java 8
}
val mixinCompatibilityLevel = javaCompatibility

// 版本号（添加构建时间戳）
val buildTimestamp = SimpleDateFormat("yyMMdd").apply {
    timeZone = TimeZone.getTimeZone("GMT+08:00")
}.format(Date())

version = "$modVersion+$buildTimestamp"
group = modMavenGroup

// ========================== 仓库配置 ==========================
fun RepositoryHandler.addMavenRepo(name: String, url: String, vararg groups: String) {
    maven {
        this.name = name
        this.url = uri(url)
        content {
            groups.forEach { group ->
                if (group.contains('*')) {
                    includeGroup(group)
                } else {
                    includeGroupAndSubgroups(group)
                }
            }
        }
    }
}

repositories {
    // 官方核心仓库
    mavenCentral()

    // 基础仓库
    addMavenRepo("FabricMC", "https://maven.fabricmc.net")
    addMavenRepo("Fallen-Breath", "https://maven.fallenbreath.me/releases")

    // 主流模组仓库
    addMavenRepo("Modrinth", "https://api.modrinth.com/maven", "maven.modrinth")
    addMavenRepo("CurseMaven", "https://www.cursemaven.com", "curse.maven")

    // ModMenu 官方源
    addMavenRepo("TerraformersMC", "https://maven.terraformersmc.com/releases")
    addMavenRepo("Nucleoid", "https://maven.nucleoid.xyz/")   // ModMenu依赖 Text Placeholder API

    addMavenRepo("Masa", "https://masa.dy.fi/maven")
    addMavenRepo("Shedaniel", "https://maven.shedaniel.me/") // Cloth API/Config 官方源
    addMavenRepo("XanderReleases", "https://maven.isxander.dev/releases")
    addMavenRepo("Jackfred", "https://maven.jackf.red/releases") // JackFredLib 依赖
    addMavenRepo("BlameJared", "https://maven.blamejared.com") // Searchables 配置库
    addMavenRepo("Kyrptonaught", "https://maven.kyrptonaught.dev") // KyrptConfig 依赖
    addMavenRepo("CottonMC", "https://server.bbkr.space/artifactory/libs-release") // LibGui 依赖

    addMavenRepo("Jitpack", "https://jitpack.io")
    addMavenRepo("Pinyin4j", "https://mvnrepository.com/artifact/com.belerweb/pinyin4j", "com.belerweb")// 拼音库
}

// ========================== Gradle 扩展函数（方便调用） ==========================
fun downloadExternalMod(downloadUrl: String, fileName: String? = null): File? {
    return rootProject.downloadFile(
        downloadUrl = downloadUrl,
        outputDirPath = "${rootProject.projectDir}/libs",
        fileName = fileName
    )
}

// ========================== 依赖配置 ==========================
dependencies {
    // 核心依赖
    minecraft("com.mojang:minecraft:$minecraftVersion")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:$loaderVersion")
    modImplementation("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")

    // 拼音库（内嵌）
    modImplementation("com.belerweb:pinyin4j:${props["pinyin_version"]}")?.let { include(it) }

    // ModMenu
    modImplementation("com.terraformersmc:modmenu:${props["modmenu"]}")


//    // bunnyi116/fabric-bedrock-miner 测试时去掉注释可以不用自己下载模组
//    // 模组版本号是写死了, 因为现在是用的预处理器, 所以使用mc变量就行(其实是我懒得在版本配置中去添加)
//    if (mcVersion >= 11900) {
//        modImplementation("maven.modrinth:next-fabric-bedrock-miner:v1.4.8-mc$minecraftVersion")
//    }

    // Masa 系列模组（根据 MC 版本选择）
    if (mcVersion > 12006) {
        modImplementation("com.github.sakura-ryoko:malilib:${props["malilib"]}")
        modImplementation("com.github.sakura-ryoko:litematica:${props["litematica"]}")
        modImplementation("com.github.sakura-ryoko:tweakeroo:${props["tweakeroo"]}")
    } else {
//        modImplementation("fi.dy.masa.malilib:${props["malilib"]}")
//        modImplementation("curse.maven:litematica-308892:${props["litematica"]}")
//        modImplementation("curse.maven:tweakeroo-297344:${props["tweakeroo"]}")
        modImplementation("maven.modrinth:malilib:${props["malilib"]}")
        modImplementation("maven.modrinth:litematica:${props["litematica"]}")
        modImplementation("maven.modrinth:tweakeroo:${props["tweakeroo"]}")

    }

    // 箱子追踪相关（1.21.5 以下）
    if (mcVersion < 12105) {
        modImplementation("maven.modrinth:chest-tracker:${props["chesttracker"]}")
        modImplementation("maven.modrinth:where-is-it:${props["whereisit"]}")
    }

    // 快捷潜影盒
    if (mcVersion >= 12006) {
        val quickshulkerUrl = props["quickshulker"].toString()
        if (quickshulkerUrl.isNotEmpty()) {
            val quickshulkerFile = downloadExternalMod(quickshulkerUrl)
            if (quickshulkerFile != null && quickshulkerFile.exists()) {
                modImplementation(files(quickshulkerFile))
            }
        }
        // 快捷潜影盒依赖(运行时)
        if (mcVersion==12006){  // 1.20.6 是 Haocen2004/quickshulker 分支, 所以还是使用之前老版本的依赖
            modImplementation ("net.kyrptonaught:kyrptconfig:${props["kyrptconfig"]}") // 快捷潜影盒依赖(运行时)
        }else{
            modImplementation ("me.fallenbreath:conditional-mixin-fabric:0.6.4")
        }
    } else {
        modImplementation("curse.maven:quick-shulker-362669:${props["quick_shulker"]}")
        modImplementation ("net.kyrptonaught:kyrptconfig:${props["kyrptconfig"]}") // 快捷潜影盒依赖(运行时)
    }

    // 暂时不知是什么依赖
    if (mcVersion >= 12001) {
        modImplementation("dev.isxander:yet-another-config-lib:${props["yacl"]}")
        modImplementation("red.jackf.jackfredlib:jackfredlib:${props["jackfredlib"]}")
        modImplementation("com.blamejared.searchables:${props["searchables"]}")
    } else {
        modImplementation("maven.modrinth:cloth-config:${props["cloth_config"]}")
        modImplementation("io.github.cottonmc:LibGui:${props["LibGui"]}")
    }
    if (mcVersion < 11904) {
        modImplementation("me.shedaniel.cloth.api:cloth-api:${props["cloth_api"]}")
    }

    // Fabric 包装器（运行时, 正常情况下可以不用, 这里模拟用户环境, 一起加载到游戏）
    runtimeOnly(project(":fabricWrapper"))
}

// ========================== Loom 配置 ==========================
loom {
    mixin{
    }
    val commonVmArgs = listOf(
        "-Dmixin.debug.export=true",
        "-Dmixin.debug.verbose=true", // 新增：打印详细 Mixin 加载日志
        "-Dmixin.env.remapRefMap=true", // 新增：修复 Mixin RefMap 重映射问题
        "-Dfabric.debug.accessWidener=true" // 新增：打印 accessWidener 加载日志
        // "-Dmixin.debug.ignore=net.kyrptonaught.quickshulker.json:ScreenMixin" // 临时禁用冲突的 Mixin（按需）
    )
    val programArgs = listOf(
        "--width", "1280",
        "--height", "720",
        "--username", "PrinterTest"
    )
    runs {
        named("client") {
            ideConfigGenerated(true)
            vmArgs(commonVmArgs)
            programArgs(programArgs)
            runDir = "../../run/client"
        }

        named("server") {
            runDir = "../../run/server"
        }
    }
}

// ========================== Java 配置 ==========================
java {
    sourceCompatibility = javaCompatibility
    targetCompatibility = javaCompatibility
    withSourcesJar()
}

// ========================== 任务配置 ==========================
tasks.apply {
    // 资源处理
    withType<ProcessResources> {
        val resourceProps = mapOf(
            "mod_id" to modId,
            "mod_wrapper_id" to modWrapperId,
            "mod_name" to modName,
            "mod_version" to modVersion,
            "mod_description" to modDescription,
            "mod_homepage" to modHomepage,
            "mod_license" to modLicense,
            "mod_sources" to modSources,
            "loader_version" to loaderVersion,
            "minecraft_dependency" to minecraftDependency,
            "compatibility_level" to "JAVA_${mixinCompatibilityLevel.majorVersion}"
        )
        inputs.properties(resourceProps)
        filesMatching(listOf("fabric.mod.json", "*.mixins.json")) {
            expand(resourceProps)
        }
    }

    // Java 编译配置
    withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:unchecked"))
        if (javaCompatibility <= JavaVersion.VERSION_1_8) {
            options.compilerArgs.add("-Xlint:-options")
        }
    }

    // JAR 打包配置
    withType<Jar> {
        from(rootProject.file("LICENSE")) {
            rename { "${it}_${modArchivesBaseName}" }
        }
    }
}

// ========================== Yamlang 配置 ==========================
yamlang {
    targetSourceSets.set(setOf(sourceSets.main.get()))
    inputDir.set("assets/${modId}/lang")
}

// ========================== Maven 发布配置 ==========================
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = modId
            version = modVersion
        }
    }
    repositories {
        mavenLocal()
        maven {
            url = uri("$rootDir/publish")
        }
    }
}

/**
 * 通用文件下载工具
 * 特性：
 * 1. 支持任意 HTTP/HTTPS 链接
 * 2. 自定义输出目录（自动创建）
 * 3. 可选文件名（优先级：用户指定 > 服务器响应头 > 链接提取）
 * 4. 超时控制（连接10秒，读取30秒）
 * 5. 文件完整性校验（非空校验）
 * 6. 友好日志输出
 */
object ExternalModDownloader {
    // 默认超时配置（毫秒）
    private const val CONNECT_TIMEOUT = 10000
    private const val READ_TIMEOUT = 30000

    // 默认 User-Agent（避免部分服务器拒绝）
    private val USER_AGENT = "Gradle/${GradleVersion.current().version}"

    /**
     * 下载文件
     * @param project Gradle 项目实例（用于日志和路径处理）
     * @param downloadUrl 下载链接（必填）
     * @param outputDir 输出目录（必填，自动创建）
     * @param fileName 自定义文件名（可选，为 null 时自动识别）
     * @return 下载后的文件对象，失败返回 null
     */
    fun download(
        project: Project,
        downloadUrl: String,
        outputDir: File,
        fileName: String? = null
    ): File? {
        val trimmedUrl = downloadUrl.trim()
        require(trimmedUrl.isNotBlank()) { "下载链接不能为空！" }
        require(outputDir.isDirectory || outputDir.mkdirs()) { "无法创建输出目录：${outputDir.absolutePath}" }
        println()

        return try {
            // 2. 处理文件名（优先级：用户指定 > 响应头 > 链接提取）
            // val targetFileName = fileName ?: getFileNameFromResponse(connection) ?: extractFileNameFromUrl(trimmedUrl)
            val targetFileName = fileName ?: extractFileNameFromUrl(trimmedUrl)
            ?: throw IOException("无法识别文件名，请手动指定 fileName 参数")
            // 3. 构建目标文件
            val targetFile = outputDir.resolve(targetFileName)
            // 4. 检查文件是否已存在（避免重复下载）
            if (targetFile.exists() && targetFile.length() > 0) {
                project.logger.log(LogLevel.LIFECYCLE, "文件已存在，跳过下载：${targetFile.absolutePath}")
                return targetFile
            }
            project.logger.log(LogLevel.LIFECYCLE, "开始下载：$trimmedUrl")
            project.logger.log(LogLevel.LIFECYCLE, "输出目录：${outputDir.absolutePath}")
            // 1. 建立连接，获取响应信息（用于提取文件名和校验）
            val connection = createConnection(trimmedUrl)
            connection.connect()
            // 5. 执行下载
            project.logger.log(LogLevel.LIFECYCLE, "正在下载：${targetFile.absolutePath}")
            downloadFile(connection, targetFile)
            // 6. 校验文件完整性
            if (!targetFile.exists() || targetFile.length() == 0L) {
                throw IOException("下载的文件为空或损坏")
            }
            project.logger.log(LogLevel.LIFECYCLE, "下载成功：${targetFile.absolutePath}")
            targetFile

        } catch (e: IllegalArgumentException) {
            project.logger.log(LogLevel.ERROR, "下载参数错误：${e.message}")
            null
        } catch (e: IOException) {
            project.logger.log(LogLevel.ERROR, "下载失败：${e.message}", e)
            null
        } catch (e: Exception) {
            project.logger.log(LogLevel.ERROR, "未知错误：${e.message}", e)
            null
        }
    }

    /**
     * 创建 HTTP 连接并配置超时和请求头
     */
    private fun createConnection(urlString: String): HttpURLConnection {
        val url = URI.create(urlString).toURL()
        val connection = url.openConnection() as HttpURLConnection
        // 配置超时
        connection.connectTimeout = CONNECT_TIMEOUT
        connection.readTimeout = READ_TIMEOUT
        // 配置请求头
        connection.setRequestProperty("User-Agent", USER_AGENT)
        connection.setRequestProperty("Accept", "*/*")
        connection.instanceFollowRedirects = true  // 自动跟随重定向
        return connection
    }

    /**
     * 从服务器响应头提取文件名
     * 支持 Content-Disposition 响应头（如：attachment; filename="xxx.jar"）
     */
    private fun getFileNameFromResponse(connection: HttpURLConnection): String? {
        return try {
            val disposition = connection.getHeaderField("Content-Disposition")
            if (disposition.isNullOrBlank()) return null
            // 匹配 filename="xxx" 或 filename=xxx 格式
            val filenamePattern = Regex("filename[\"=]?([^\";]+)")
            val matchResult = filenamePattern.find(disposition)
            matchResult?.groupValues?.get(1)?.trim()?.takeIf { it.contains('.') }
        } catch (e: Exception) {
            null  // 提取失败时返回 null， fallback 到链接提取
        }
    }

    /**
     * 从 URL 提取文件名（处理带参数的链接）
     * 示例：
     * - https://xxx.com/mod.jar → mod.jar
     * - https://xxx.com/download?file=mod-1.0.jar → mod-1.0.jar
     * - https://xxx.com/mod.jar?v=123 → mod.jar
     */
    private fun extractFileNameFromUrl(urlString: String): String? {
        return try {
            // 去掉 ? 和 # 后面的参数
            val cleanUrl = urlString.split('?', '#').first()
            // 提取最后一个 / 后的部分
            val fileName = cleanUrl.substringAfterLast('/')
            // 确保文件名有扩展名（至少3个字符，如 .jar、.zip）
            if (fileName.contains('.') && fileName.substringAfterLast('.').length >= 2) {
                fileName
            } else {
                // 无有效扩展名时，默认用 .jar（针对模组场景）
                "downloaded-file-${System.currentTimeMillis()}.jar"
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 实际写入文件
     */
    private fun downloadFile(connection: HttpURLConnection, targetFile: File) {
        connection.inputStream.use { inputStream ->
            Files.copy(inputStream, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }
}

/**
 * Gradle 项目扩展函数（简化调用）
 * 示例：project.downloadFile("url", file("outputDir"), "custom.jar")
 */
fun Project.downloadFile(
    downloadUrl: String,
    outputDir: File,
    fileName: String? = null
): File? {
    return ExternalModDownloader.download(this, downloadUrl, outputDir, fileName)
}

/**
 * 重载扩展函数（支持字符串格式的输出目录路径）
 * 示例：project.downloadFile("url", "outputDir", "custom.jar")
 */
fun Project.downloadFile(
    downloadUrl: String,
    outputDirPath: String,
    fileName: String? = null
): File? {
    return downloadFile(downloadUrl, file(outputDirPath), fileName)
}
