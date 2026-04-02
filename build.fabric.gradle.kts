@file:Suppress("UnstableApiUsage")

import org.gradle.kotlin.dsl.project
import java.text.SimpleDateFormat
import java.util.*

plugins {
    id("mod-plugin")
    id("maven-publish")
    id("net.fabricmc.fabric-loom")
    id("com.replaymod.preprocess")
}

val time = SimpleDateFormat("yyMMdd")
    .apply { timeZone = TimeZone.getTimeZone("GMT+08:00") }
    .format(Date())
    .toString()

var fullProjectVersion = "$modVersion+$time"
if (System.getenv("IS_THIS_RELEASE") == "false") {
    val buildNumber: String? = System.getenv("GITHUB_RUN_NUMBER")
    if (buildNumber != null) {
        fullProjectVersion += "+build.$buildNumber"
    }
}
version = fullProjectVersion
group = modMavenGroup

repositories {
    maven("https://maven.fabricmc.net") { name = "FabricMC" }
    maven("https://maven.fallenbreath.me/releases") { name = "FallenBreath" }
    maven("https://api.modrinth.com/maven") { name = "Modrinth" }
    maven("https://www.cursemaven.com") { name = "CurseMaven" }
    maven("https://maven.terraformersmc.com/releases") { name = "TerraformersMC" } // ModMenu 源
    maven("https://maven.nucleoid.xyz") { name = "Nucleoid" }  // ModMenu依赖 Text Placeholder API
    maven("https://masa.dy.fi/maven") { name = "Masa" }
    maven("https://masa.dy.fi/maven/sakura-ryoko") { name = "SakuraRyoko" }
    maven("https://maven.shedaniel.me") { name = "Shedaniel" }  // Cloth API/Config 官方源
    maven("https://maven.isxander.dev/releases") { name = "XanderReleases" }
    maven("https://maven.jackf.red/releases") { name = "Jackfred" }   // JackFredLib 依赖
    maven("https://maven.blamejared.com") { name = "BlameJared" }   // Searchables 配置库
    maven("https://maven.kyrptonaught.dev") { name = "Kyrptonaught" }   // KyrptConfig 依赖
    maven("https://server.bbkr.space/artifactory/libs-release") { name = "CottonMC" }   // LibGui 依赖
    maven("https://jitpack.io") { name = "Jitpack" }
    maven("https://mvnrepository.com/artifact/com.belerweb/pinyin4j") { // 拼音库
        name = "Pinyin4j"
        content {
            includeGroupAndSubgroups("com.belerweb")
        }
    }

    // pkg.github.com. needs authentication(system environment)
    // GH_USERNAME 和 GH_TOKEN 需要在系统环境变量中设置(windows) Github可设置自己用户名 也可以使用默认GITHUB_TOKEN( github-actions[bot] )
    maven { // JackFredLib ponuing
        url = uri("https://maven.pkg.github.com/ponuing/JackFredLib")
        credentials {
            username = System.getenv("GH_USERNAME")?: "github-actions[bot]"
            password = System.getenv("GH_TOKEN")?: System.getenv("GITHUB_TOKEN")
        }
    }
    maven { // ChestTracker ponuing
        url = uri("https://maven.pkg.github.com/ponuing/ChestTracker")
        credentials {
            username = System.getenv("GH_USERNAME")?: "github-actions[bot]"
            password = System.getenv("GH_TOKEN")?: System.getenv("GITHUB_TOKEN")
        }
    }
    maven { // WhereIsIt ponuing
        url = uri("https://maven.pkg.github.com/ponuing/WhereIsIt")
        credentials {
            username = System.getenv("GH_USERNAME")?: "github-actions[bot]"
            password = System.getenv("GH_TOKEN")?: System.getenv("GITHUB_TOKEN")
        }
    }
}

// https://github.com/FabricMC/fabric-loader/issues/783
configurations.all {
    resolutionStrategy {
        force("net.fabricmc:fabric-loader:$fabricLoaderVersion")
        force("com.terraformersmc:modmenu:${prop("modmenu")}")
        force("maven.modrinth:malilib:${prop("malilib_dependency")}")
        force("maven.modrinth:litematica:${prop("litematica_dependency")}")
        force("maven.modrinth:tweakeroo:${prop("tweakeroo_dependency")}")
    }
}

dependencies {
    minecraft("com.mojang:minecraft:$mcVersion")
    implementation("net.fabricmc:fabric-loader:$fabricLoaderVersion")
    implementation("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")
    implementation("com.belerweb:pinyin4j:${prop("pinyin_version")}")?.let { include(it) }
    implementation("com.terraformersmc:modmenu:${prop("modmenu")}")
    implementation("dev.isxander:yet-another-config-lib:${prop("yacl")}")
    implementation("com.blamejared.searchables:${prop("searchables")}")

    // Masa
    implementation("fi.dy.masa.malilib:${prop("malilib")}")
    implementation("fi.dy.masa.litematica:${prop("litematica")}")
    implementation("fi.dy.masa.tweakeroo:${prop("tweakeroo")}")

    // 箱子追踪相关
    implementation("red.jackf.jackfredlib:jackfredlib:${prop("jackfredlib")}")
    implementation("red.jackf:chesttracker:${prop("chesttracker")}")
    implementation("red.jackf:whereisit:${prop("whereisit")}")

    // 快捷潜影盒
    val quickshulkerUrl = prop("quickshulker").toString()
    if (quickshulkerUrl.isNotEmpty()) {
        val quickshulkerFile = downloadDependencyMod(quickshulkerUrl)
        if (quickshulkerFile != null && quickshulkerFile.exists()) {
            implementation(files(quickshulkerFile))
        }
    }
}

loom {
    val commonVmArgs = listOf("-Dmixin.debug.export=true", "-Dmixin.debug.verbose=true", "-Dmixin.env.remapRefMap=true")
    val programArgs = listOf("--width", "1280", "--height", "720", "--username", "PrinterTest")
    runs {
        named("client") {
            ideConfigGenerated(true)
            vmArgs(commonVmArgs)
            programArgs(programArgs)
            runDir = "../../run/client"
        }
    }
}

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
