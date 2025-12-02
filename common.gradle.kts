@file:Suppress("UnstableApiUsage")

import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.text.SimpleDateFormat
import java.util.*

plugins {
    id("java")
    id("maven-publish")
    id("fabric-loom")
    id("com.replaymod.preprocess")
    id("me.fallenbreath.yamlang")
}

val mcVersion = project.property("mcVersion") as Int
val modId = project.property("mod_id") as String
val modWrapperId = project.property("mod_wrapper_id") as String
val modName = project.property("mod_name") as String
val modMavenGroup = project.property("mod_maven_group") as String
val modVersion = project.property("mod_version") as String
val modArchivesBaseName = project.property("mod_archives_base_name") as String
val modDescription = project.property("mod_description") as String
val modHomepage = project.property("mod_homepage") as String
val modLicense = project.property("mod_license") as String
val modSources = project.property("mod_sources") as String
val loaderVersion = project.property("loader_version") as String

val minecraftDependency = project.property("minecraft_dependency") as String
val minecraftVersion = project.property("minecraft_version") as String
val fabricApiVersion = project.property("fabric_version") as String

// 根据 Minecraft 版本确定所需的 Java 兼容性版本
val javaCompatibility = when {
    mcVersion >= 12005 -> JavaVersion.VERSION_21    // 1.20.5+      需要 Java 21
    mcVersion >= 11800 -> JavaVersion.VERSION_17    // 1.18-1.20.4  需要 Java 17
    mcVersion >= 11700 -> JavaVersion.VERSION_16    // 1.17.x       需要 Java 16
    else -> JavaVersion.VERSION_1_8                 // 1.16.x 及以下使用 Java 8
}
val mixinCompatibilityLevel = javaCompatibility // Mixin 兼容性级别与 Java 兼容性版本保持一致

val time = SimpleDateFormat("yyMMdd").apply {
    timeZone = TimeZone.getTimeZone("GMT+08:00")
}.format(Date())

version = "$modVersion+$time"
group = modMavenGroup

repositories {
    maven {
        name = "Masa"
        url = uri("https://masa.dy.fi/maven")
        content {
            includeGroupAndSubgroups("fi.dy.masa")
        }
    }
    maven {
        name = "CurseMaven"
        url = uri("https://www.cursemaven.com")
        content {
            includeGroupAndSubgroups("curse.maven")
        }
    }
    maven {
        name = "FabricMC"
        url = uri("https://maven.fabricmc.net")
        content {
            includeGroupAndSubgroups("net.fabricmc")
        }
    }
    maven {
        // 快捷潜影盒1.20.6以下所需要的
        name = "Kyrptonaught Maven"
        url = uri("https://maven.kyrptonaught.dev")
        content {
            includeGroupAndSubgroups("net.kyrptonaught")
        }
    }
    maven {
        // ModMenu
        name = "TerraformersMC"
        url = uri("https://maven.terraformersmc.com/releases")
        content {
            includeGroupAndSubgroups("com.terraformersmc")
        }
    }
    maven {
        name = "Nucleoid"
        url = uri("https://maven.nucleoid.xyz/")
        content {
            includeGroupAndSubgroups("eu.pb4")
        }
    }
    maven {
        name = "Shedaniel Maven"
        url = uri("https://maven.shedaniel.me/")
        content {
            includeGroupAndSubgroups("me.shedaniel.cloth")
        }
    }
    maven {
        name = "Modrinth"
        url = uri("https://api.modrinth.com/maven")
        content {
            includeGroupAndSubgroups("maven.modrinth")
        }
    }
    maven {
        name = "CottonMC"
        url = uri("https://server.bbkr.space/artifactory/libs-release")
        content {
            includeGroupAndSubgroups("io.github.cottonmc")
        }
    }
    maven {
        name = "jackfredReleases"
        url = uri("https://maven.jackf.red/releases")
        content {
            includeGroupAndSubgroups("red.jackf")
        }
    }
    maven {
        name = "BlameJared"
        url = uri("https://maven.blamejared.com")
        content {
            includeGroupAndSubgroups("com.blamejared.searchables")
        }
    }
    maven {
        // 拼音搜索
        name = "Pinyin4j"
        url = uri("https://mvnrepository.com/artifact/com.belerweb/pinyin4j")
        content {
            includeGroupAndSubgroups("com.belerweb")
        }
    }
    // YACL
    maven {
        name = "Xander Maven"
        url = uri("https://maven.isxander.dev/releases")
        content {
            includeGroupAndSubgroups("dev.isxander")
            includeGroupAndSubgroups("org.quiltmc")
        }
    }
    // YACL快照
    maven {
        name = "Xander Snapshot Maven"
        url = uri("https://maven.isxander.dev/snapshots")
        content {
            includeGroup("dev.isxander")
            includeGroupAndSubgroups("org.quiltmc")
        }
    }
    maven {
        name = "Jitpack"
        url = uri("https://jitpack.io")
        content {
            includeGroupAndSubgroups("com.github")
        }
    }
}

fun downloadExternalMod(urlStr: String, customFilename: String? = null): File {
    val client = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()
    val request = HttpRequest.newBuilder()
        .uri(URI.create(urlStr))
        .GET()
        .build()
    val response = client.send(request, HttpResponse.BodyHandlers.ofInputStream())
    if (response.statusCode() != 200) {
        throw IOException("Failed to download: HTTP ${response.statusCode()}")
    }
    // 确定文件名
    var filename: String
    // 1. 优先使用自定义文件名
    if (customFilename != null && customFilename.isNotEmpty()) {
        filename = customFilename
    } else {
        // 2. 尝试从 Content-Disposition 头获取文件名
        val contentDisposition = response.headers().firstValue("Content-Disposition").orElse(null)
        if (contentDisposition != null && contentDisposition.contains("filename=")) {
            // 提取文件名，处理带引号和不带引号的情况
            val pattern = """filename=["']?([^"']+)["']?""".toRegex()
            val matcher = pattern.find(contentDisposition)
            if (matcher != null) {
                filename = matcher.groupValues[1]
            } else {
                filename = getFilenameFromUrl(urlStr)
            }
        } else {
            // 3. 如果仍未获取到文件名，从URL路径中提取
            filename = getFilenameFromUrl(urlStr)
        }
    }

    // 确保文件名以.jar结尾（如果不是）
    if (!filename.endsWith(".jar")) {
        filename += ".jar"
    }

    val libsDir = File("$rootDir/libs")
    libsDir.mkdirs()
    val target = File(libsDir, filename)

    if (!target.exists()) {
        println("Downloading external mod: $filename from $urlStr")
        target.outputStream().use { out ->
            response.body().transferTo(out)
        }
        println("Downloaded: ${target.name} (${target.length()} bytes)")
    } else {
        println("File already exists: $filename")
    }

    return target
}

// 辅助函数：从URL路径中提取文件名
private fun getFilenameFromUrl(urlStr: String): String {
    val path = URI.create(urlStr).path
    val filename = path.substring(path.lastIndexOf('/') + 1)

    // 如果URL中的文件名是空的或无效，使用默认文件名
    return if (filename.isEmpty() || filename.contains("?")) {
        val domain = URI.create(urlStr).host
        val timestamp = System.currentTimeMillis()
        "downloaded-from-$domain-$timestamp.jar"
    } else {
        filename
    }
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")
    // mappings "net.fabricmc:yarn:${yarnMappings}:v2"
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:$loaderVersion")
    modImplementation("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")
    implementation(project.dependencies.create("com.belerweb:pinyin4j:${property("pinyin_version")}") as Any)
    include("com.belerweb:pinyin4j:${property("pinyin_version")}")
    modImplementation("com.terraformersmc:modmenu:${property("modmenu")}")       // 模组菜单

    // masa相关引用
    if (mcVersion > 12006) {
        modImplementation("com.github.sakura-ryoko:malilib:${property("malilib")}")
        modImplementation("com.github.sakura-ryoko:litematica:${property("litematica")}")
        modImplementation("com.github.sakura-ryoko:tweakeroo:${property("tweakeroo")}")
    } else {
        // 冲突根源：tweakeroo 和 litematica 这两个mod都对同一个类 ServerPlayNetworkHandler 中的同一个方法使用了 @Redirect 注入。
        // 优先级问题：litematica 的Mixin优先级是1010，tweakeroo 的优先级是1005。由于litematica优先级更高，它的注入先被应用，导致tweakeroo的注入找不到目标方法而失败。
        // 这应该就是点进设置崩溃的原因吧
        modImplementation("fi.dy.masa.malilib:${property("malilib")}")
        modImplementation("curse.maven:litematica-308892:${property("litematica")}")
        modImplementation("curse.maven:tweakeroo-297344:${property("tweakeroo")}")
    }

    if (mcVersion < 12105) {
        // 箱子追踪
        modImplementation("maven.modrinth:chest-tracker:${property("chesttracker")}")
        modImplementation("maven.modrinth:where-is-it:${property("whereisit")}")
    }

    //快捷潜影盒
    if (mcVersion >= 12006) {

        val selectedUrl = property("quickshulker").toString()
        if (selectedUrl.isNotEmpty()) {
            val quickshulkerFile = downloadExternalMod(selectedUrl)
            if (quickshulkerFile.exists()) {
                modImplementation(files(quickshulkerFile))
            }
        }
    } else {
        modImplementation("curse.maven:quick-shulker-362669:${property("quick_shulker")}")
        modImplementation("net.kyrptonaught:kyrptconfig:${property("kyrptconfig")}") // 快捷潜影盒依赖
    }

    if (mcVersion >= 12001) {
        modImplementation("dev.isxander:yet-another-config-lib:${property("yacl")}")
        modImplementation("red.jackf.jackfredlib:jackfredlib:${property("jackfredlib")}")
        modImplementation("com.blamejared.searchables:${property("searchables")}")
    } else {
        modImplementation("maven.modrinth:cloth-config:${property("cloth_config")}")
        modImplementation("io.github.cottonmc:LibGui:${property("LibGui")}")
    }

    if (mcVersion < 11904) {
        modImplementation("me.shedaniel.cloth.api:cloth-api:${property("cloth_api")}")
    }

    //这是Fabric平台的运行时包装器，仅运行时需要
    runtimeOnly(project(":fabricWrapper"))
}

configurations {
    create("productionRuntimeClient") {
        configurations.filter {
            it.name in listOf(
                "minecraftLibraries",
                "loaderLibraries",
                "minecraftRuntimeLibraries"
            )
        }.forEach { superConfiguration ->
            extendsFrom(superConfiguration)
        }
    }
}


base {
    archivesName.set("$modArchivesBaseName-$minecraftVersion")
}

loom {
    val commonVmArgs = listOf(
        "-Dmixin.debug.export=true",
        "-Dmixin.debug.countInjections=true",
        "-DmixinAuditor.audit=true"
    )

    runs {
        named("client") {
            ideConfigGenerated(true)
            vmArgs(commonVmArgs)
            programArgs(
                listOf(
                    "--width", "1280",
                    "--height", "720",
                    "--username", "PrinterTest"
                )
            )
            runDir = "../../run/client"
        }

        named("server") {
            runDir = "../../run/server"
        }
    }
}

java {
    sourceCompatibility = javaCompatibility
    targetCompatibility = javaCompatibility

    withSourcesJar()
}

tasks {
    // --- 资源处理 (Resource Processing) ---
    // 如果 IDEA 抱怨 "Cannot resolve resource filtering of MatchingCopyAction"，并且你想知道原因
    // 请参阅 https://youtrack.jetbrains.com/issue/IDEA-296490
    withType<ProcessResources> {
        val propertyMap = mapOf(
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
        inputs.properties(propertyMap)
        filesMatching(listOf("fabric.mod.json", "*.mixins.json")) {
            expand(propertyMap)
        }
    }

    // --- Java 编译配置 ---
    // 确保编码设置为 UTF-8，无论系统默认值是什么
    // 这修复了某些特殊字符无法正确显示的边缘情况
    // 参阅 http://yodaconditions.net/blog/fix-for-java-file-encoding-problems-with-gradle.html
    withType<JavaCompile> {
        options.encoding = "UTF-8"
        // 添加编译器参数以显示弃用和未检查的警告
        options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:unchecked"))
        if (javaCompatibility <= JavaVersion.VERSION_1_8) {
            // 如果使用 Java 8 或更低版本，压制 "source/target value 8 is obsolete..." 的警告
            options.compilerArgs.add("-Xlint:-options")
        }
    }

    withType<Jar> {
        // 将 LICENSE 文件添加到 JAR 包中
        from(rootProject.file("LICENSE")) {
            rename { "${it}_${modArchivesBaseName}" }
        }
    }
}

// https://github.com/Fallen-Breath/yamlang
yamlang {
    targetSourceSets.set(setOf(sourceSets.main.get())) // 指定要处理的源集
    inputDir.set("assets/${modId}/lang") // 指定语言文件目录
}

// --- Maven 发布配置 ---
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"]) // 从 Java 插件获取要发布的组件
            artifactId = modId // 设置产物 ID (Artifact ID)
            version = modVersion // 设置产物版本
        }
    }
    // 请参阅 https://docs.gradle.org/current/userguide/publishing_maven.html 了解如何设置发布。
    repositories {
        mavenLocal()
        maven {
            url = uri("$rootDir/publish")
        }
    }
}