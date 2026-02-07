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
}

// https://github.com/FabricMC/fabric-loader/issues/783
configurations.all {
    resolutionStrategy {
        force("net.fabricmc:fabric-loader:$fabricLoaderVersion")
    }
}

dependencies {
    minecraft("com.mojang:minecraft:$mcVersion")
    implementation("net.fabricmc:fabric-loader:$fabricLoaderVersion")
    implementation("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")
    implementation("com.belerweb:pinyin4j:${prop("pinyin_version")}")?.let { include(it) }
    implementation("com.terraformersmc:modmenu:${prop("modmenu")}")

    // implementation("com.github.sakura-ryoko:malilib:${props["malilib"]}")
    // implementation("com.github.sakura-ryoko:litematica:${props["litematica"]}")
    // implementation("com.github.sakura-ryoko:tweakeroo:${props["tweakeroo"]}")
    implementation("maven.modrinth:malilib:${prop("malilib")}")
    implementation("maven.modrinth:litematica:${prop("litematica")}")
    implementation("maven.modrinth:tweakeroo:${prop("tweakeroo")}")

    // 箱子追踪相关（1.21.5 以下）
    if (mcVersionInt < 12105) {
        implementation("maven.modrinth:chest-tracker:${prop("chesttracker")}")
        implementation("maven.modrinth:where-is-it:${prop("whereisit")}")
    }

    // 快捷潜影盒
    if (mcVersionInt >= 12006) {
        val quickshulkerUrl = prop("quickshulker").toString()
        if (quickshulkerUrl.isNotEmpty()) {
            val quickshulkerFile = downloadDependencyMod(quickshulkerUrl)
            if (quickshulkerFile != null && quickshulkerFile.exists()) {
                implementation(files(quickshulkerFile))
            }
        }
        // 快捷潜影盒依赖(运行时)
        if (mcVersionInt == 12006) {  // 1.20.6 是 Haocen2004/quickshulker 分支, 所以还是使用之前老版本的依赖
            implementation("net.kyrptonaught:kyrptconfig:${prop("kyrptconfig")}") // 快捷潜影盒依赖(运行时)
        } else {
            implementation("me.fallenbreath:conditional-mixin-fabric:0.6.4")
        }
    } else {
        implementation("curse.maven:quick-shulker-362669:${prop("quick_shulker")}")
        implementation("net.kyrptonaught:kyrptconfig:${prop("kyrptconfig")}") // 快捷潜影盒依赖(运行时)
    }

    // 暂时不知是什么依赖
    if (mcVersionInt >= 12001) {
        implementation("dev.isxander:yet-another-config-lib:${prop("yacl")}")

        // TODO: 暂时不知道是什么模组的依赖, 不过在1.21.11中会报错, 因为这个库没有最新版本, 移除后正在运行, 不知道有什么BUG
        // java.lang.NoSuchMethodError: 'long net.minecraft.server.level.ServerLevel.method_8510()'
        // JackFred maven复活
        if (mcVersionInt < 12111) {
            implementation("red.jackf.jackfredlib:jackfredlib:${prop("jackfredlib")}")
        }
        implementation("com.blamejared.searchables:${prop("searchables")}")
    } else {
        implementation("maven.modrinth:cloth-config:${prop("cloth_config")}")
        implementation("io.github.cottonmc:LibGui:${prop("LibGui")}")
    }
    if (mcVersionInt < 11904) {
        implementation("me.shedaniel.cloth.api:cloth-api:${prop("cloth_api")}")
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
