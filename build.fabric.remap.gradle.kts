@file:Suppress("UnstableApiUsage")

import java.text.SimpleDateFormat
import java.util.*

plugins {
    id("mod-plugin")
    id("maven-publish")
    id("net.fabricmc.fabric-loom-remap")
    id("com.replaymod.preprocess")
}

val buildTimestamp: String? = SimpleDateFormat("yyMMdd").apply {
    timeZone = TimeZone.getTimeZone("GMT+08:00")
}.format(Date())

version = "$modVersion+$buildTimestamp"
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
}

// https://github.com/FabricMC/fabric-loader/issues/783
configurations.all {
    resolutionStrategy {
        force("net.fabricmc:fabric-loader:$fabricLoaderVersion")
    }
}

dependencies {
    minecraft("com.mojang:minecraft:$mcVersion")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:$fabricLoaderVersion")
    modImplementation("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")
    modImplementation("com.belerweb:pinyin4j:${prop("pinyin_version")}")?.let { include(it) }
    modImplementation("com.terraformersmc:modmenu:${prop("modmenu")}")

    // modImplementation("com.github.sakura-ryoko:malilib:${props["malilib"]}")
    // modImplementation("com.github.sakura-ryoko:litematica:${props["litematica"]}")
    // modImplementation("com.github.sakura-ryoko:tweakeroo:${props["tweakeroo"]}")
    modImplementation("maven.modrinth:malilib:${prop("malilib")}")
    modImplementation("maven.modrinth:litematica:${prop("litematica")}")
    modImplementation("maven.modrinth:tweakeroo:${prop("tweakeroo")}")

    // 箱子追踪相关（1.21.5 以下）
    if (mcVersionInt < 12105) {
        modImplementation("maven.modrinth:chest-tracker:${prop("chesttracker")}")
        modImplementation("maven.modrinth:where-is-it:${prop("whereisit")}")
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
        // 快捷潜影盒依赖(运行时)
        if (mcVersionInt == 12006) {  // 1.20.6 是 Haocen2004/quickshulker 分支, 所以还是使用之前老版本的依赖
            modImplementation("net.kyrptonaught:kyrptconfig:${prop("kyrptconfig")}") // 快捷潜影盒依赖(运行时)
        } else {
            modImplementation("me.fallenbreath:conditional-mixin-fabric:0.6.4")
        }
    } else {
        modImplementation("curse.maven:quick-shulker-362669:${prop("quick_shulker")}")
        modImplementation("net.kyrptonaught:kyrptconfig:${prop("kyrptconfig")}") // 快捷潜影盒依赖(运行时)
    }

    // 暂时不知是什么依赖
    if (mcVersionInt >= 12001) {
        modImplementation("dev.isxander:yet-another-config-lib:${prop("yacl")}")

        // TODO: 暂时不知道是什么模组的依赖, 不过在1.21.11中会报错, 因为这个库没有最新版本, 移除后正在运行, 不知道有什么BUG
        // java.lang.NoSuchMethodError: 'long net.minecraft.server.level.ServerLevel.method_8510()'
        // Jackfred maven复活
        if (mcVersionInt < 12111) {
            implementation("red.jackf.jackfredlib:jackfredlib:${prop("jackfredlib")}")
        }
        modImplementation("com.blamejared.searchables:${prop("searchables")}")
    } else {
        modImplementation("maven.modrinth:cloth-config:${prop("cloth_config")}")
        modImplementation("io.github.cottonmc:LibGui:${prop("LibGui")}")
    }
    if (mcVersionInt < 11904) {
        modImplementation("me.shedaniel.cloth.api:cloth-api:${prop("cloth_api")}")
    }

    // Fabric 包装器（运行时, 正常情况下可以不用, 这里模拟用户环境, 一起加载到游戏）
    runtimeOnly(project(":fabricWrapper"))
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
