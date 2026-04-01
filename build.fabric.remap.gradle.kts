@file:Suppress("UnstableApiUsage")

import groovy.json.JsonSlurper
import java.text.SimpleDateFormat
import java.util.*

plugins {
    id("mod-plugin")
    id("maven-publish")
    id("net.fabricmc.fabric-loom-remap")
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
    maven("https://staging.alexiil.uk/maven/") { name = "CottonMC" }   // LibGui 依赖
    maven("https://jitpack.io") { name = "Jitpack" }
    maven("https://mvnrepository.com/artifact/com.belerweb/pinyin4j") { // 拼音库
        name = "Pinyin4j"
        content {
            includeGroupAndSubgroups("com.belerweb")
        }
    }

    // pkg.github.com. needs authentication(system environment)
    // GH_USERNAME 和 GH_TOKEN 需要在系统环境变量中设置(windows) Github则需要在仓库机密中设置
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
        if (mcVersionInt >= 12101) {
            force("maven.modrinth:malilib:${prop("malilib_dependency")}")
            force("maven.modrinth:litematica:${prop("litematica_dependency")}")
            force("maven.modrinth:tweakeroo:${prop("tweakeroo_dependency")}")
        } else {
            force("maven.modrinth:malilib:${prop("malilib")}")
            force("maven.modrinth:litematica:${prop("litematica")}")
            force("maven.modrinth:tweakeroo:${prop("tweakeroo")}")
        }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:$mcVersion")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:$fabricLoaderVersion")
    modImplementation("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")
    modImplementation("com.belerweb:pinyin4j:${prop("pinyin_version")}")?.let { include(it) }

    modImplementation("com.terraformersmc:modmenu:${prop("modmenu")}")

    if (mcVersionInt >= 12101) {    // use https://masa.dy.fi/maven/sakura-ryoko
        modImplementation("fi.dy.masa.malilib:${prop("malilib")}")
        modImplementation("fi.dy.masa.litematica:${prop("litematica")}")
        modImplementation("fi.dy.masa.tweakeroo:${prop("tweakeroo")}")
    } else { //mcVersionInt < 12101 // use https://api.modrinth.com/maven
        modImplementation("maven.modrinth:malilib:${prop("malilib")}")
        modImplementation("maven.modrinth:litematica:${prop("litematica")}")
        modImplementation("maven.modrinth:tweakeroo:${prop("tweakeroo")}")
    }

    // 箱子追踪
    if (mcVersionInt > 12006) {
        modImplementation("red.jackf.jackfredlib:jackfredlib:${prop("jackfredlib")}")
        modImplementation("red.jackf:chesttracker:${prop("chesttracker")}")
        modImplementation("red.jackf:whereisit:${prop("whereisit")}")
    } else {
        modImplementation("maven.modrinth:chest-tracker:${prop("chesttracker")}")
        modImplementation("maven.modrinth:where-is-it:${prop("whereisit")}") // JackFred 写 WhereIsIt 1.20.6的时候用的是jitpack的yacl ...

        if (mcVersionInt >= 12001) {
            modImplementation("red.jackf.jackfredlib:jackfredlib:${prop("jackfredlib")}")
        } else {
            modImplementation("me.shedaniel.cloth:cloth-config-fabric:${prop("cloth_config")}")
            if (mcVersionInt < 11904) {
                modImplementation("me.shedaniel.cloth.api:cloth-api:${prop("cloth_api")}")
            }
            if (mcVersionInt <= 11904) {
                modImplementation("io.github.cottonmc:LibGui:${prop("LibGui")}")
            }
        }
    }
    if (mcVersionInt >= 12001) {
        modImplementation("dev.isxander:yet-another-config-lib:${prop("yacl")}")
        modImplementation("com.blamejared.searchables:${prop("searchables")}")
    }

    // 快捷潜影盒
    if (mcVersionInt >= 12006) {
        val quickshulkerUrl = prop("quickshulker").toString()
        if (quickshulkerUrl.isNotEmpty()) {
            val quickshulkerFile = downloadDependencyMod(quickshulkerUrl)
            if (quickshulkerFile != null && quickshulkerFile.exists()) {
                modImplementation(files(quickshulkerFile))
            }
        }
        if (mcVersionInt == 12006) {  // 1.20.6 是 Haocen2004/quickshulker 分支, 所以还是使用之前老版本的依赖
            modImplementation("net.kyrptonaught:kyrptconfig:${prop("kyrptconfig")}")
        } else {
            modImplementation("me.fallenbreath:conditional-mixin-fabric:0.6.4")
        }
    } else {
        modImplementation("curse.maven:quick-shulker-362669:${prop("quick_shulker")}")
        modImplementation("net.kyrptonaught:kyrptconfig:${prop("kyrptconfig")}")
    }
}

loom {
    val commonVmArgs = listOf("-Dmixin.debug.export=true", "-Dmixin.debug.verbose=true", "-Dmixin.env.remapRefMap=true")
    var programArgs = listOf("--width", "1280", "--height", "720")
    val profileFile = file("../../profile.json")
    if (profileFile.exists()) {
        @Suppress("UNCHECKED_CAST")
        val profile = JsonSlurper().parseText(profileFile.readText()) as Map<String, List<String>>
        val username = profile["username"].toString()
        val uuid = profile["uuid"].toString()
        val xuid = profile["xuid"].toString()
        val accessToken = profile["accessToken"].toString()
        programArgs = programArgs + listOf(
            "--username", username,
            "--uuid", uuid,
            "--xuid", xuid,
            "--accessToken", accessToken,
            "--userType", "msa",
            "--versionType", "release"
        )
    } else {
        programArgs = programArgs + listOf("--username", "PrinterTest")
    }
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
