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

version = fullProjectVersion
group = modMavenGroup

repositories {
    mavenLocal()
    maven { url = uri("$rootDir/project_maven") }
    fun strictMaven(url: String, vararg groups: String) = exclusiveContent {
        forRepository { maven(url) }
        filter {
            groups.forEach {
                includeGroupAndSubgroups(it)
                includeGroupAndSubgroups("$it.*")
            }
        }
    }
    strictMaven("https://mvnrepository.com/artifact/com.belerweb/pinyin4j")

    strictMaven("https://maven.fabricmc.net")
    strictMaven("https://maven.fallenbreath.me/releases")
    strictMaven("https://www.cursemaven.com", "curse.maven")
    strictMaven("https://api.modrinth.com/maven", "maven.modrinth")

    if (mcVersionInt <= 12006) {
        strictMaven("https://maven.kyrptonaught.dev", "net.kyrptonaught")  // KyrptConfig依赖
    }

    strictMaven("https://maven.terraformersmc.com/releases", "com.terraformersmc")  // ModMenu
    strictMaven("https://maven.nucleoid.xyz", "eu.pb4") // ModMenu依赖TextPlaceholderAPI
    strictMaven("https://maven.jackf.red/releases", "red.jackf")  // JackFredLib 依赖
    strictMaven("https://maven.blamejared.com") // Searchables 配置库
    strictMaven("https://staging.alexiil.uk/maven/")
    strictMaven("https://maven.isxander.dev/releases")
    strictMaven("https://maven.shedaniel.me")  // Cloth API/Config 官方源

    // if (mcVersionInt <= 11904) {
    //     strictMaven("https://maven.cottonmc.re", "io.github.cottonmc")   // LibGui 依赖
    // }
    strictMaven("https://jitpack.io")
}

// https://github.com/FabricMC/fabric-loader/issues/783
configurations.all {
    resolutionStrategy {
        force("net.fabricmc:fabric-loader:$fabricLoaderVersion")
        force("maven.modrinth:malilib:${prop("malilib")}")
        force("maven.modrinth:litematica:${prop("litematica")}")
        force("maven.modrinth:tweakeroo:${prop("tweakeroo")}")
        force("com.terraformersmc:modmenu:${prop("modmenu")}")
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

    modImplementation("maven.modrinth:malilib:$malilib")
    modImplementation("maven.modrinth:litematica:$litematica")
    modImplementation("maven.modrinth:tweakeroo:${prop("tweakeroo")}")

    // 箱子追踪
    if (mcVersionInt >= 12106) {
        modImplementation("maven.modrinth:chest-tracker-port:${prop("chesttracker")}")
        if (mcVersionInt >= 12106) {
            modImplementation("com.github.bunnyi116:JackFredLib:${prop("jackfredlib")}")
        } else {
            modImplementation("red.jackf.jackfredlib:jackfredlib:${prop("jackfredlib")}")
        }
        modImplementation("maven.modrinth:where-is-it-port:${prop("whereisit")}")
    } else {
        modImplementation("maven.modrinth:chest-tracker:${prop("chesttracker")}")
        modImplementation("maven.modrinth:where-is-it:${prop("whereisit")}")
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

tasks {
    register<Copy>("buildAndCollect") {
        group = "build"
        from(remapJar.map { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/$modVersion"))
        dependsOn("build")
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
