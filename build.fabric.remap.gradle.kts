@file:Suppress("UnstableApiUsage")

plugins {
    id("mod-plugin")
    id("maven-publish")
    id("net.fabricmc.fabric-loom-remap")
    id("com.replaymod.preprocess")
}

version = fullProjectVersion
group = modMavenGroup

repositories {
    maven("https://maven.fabricmc.net") { name = "FabricMC" }
    maven("https://maven.fallenbreath.me/releases") { name = "FallenBreath" }
    maven("https://api.modrinth.com/maven") { name = "Modrinth" }
    maven("https://www.cursemaven.com") { name = "CurseMaven" }
    maven("https://maven.terraformersmc.com/releases") { name = "TerraformersMC" }
    maven("https://maven.nucleoid.xyz") { name = "Nucleoid" }
    maven("https://masa.dy.fi/maven") { name = "Masa" }
    maven("https://masa.dy.fi/maven/sakura-ryoko") { name = "SakuraRyoko" }
    maven("https://maven.kyrptonaught.dev") { name = "Kyrptonaught" }
    maven("https://jitpack.io") { name = "Jitpack" }
    maven("https://mvnrepository.com/artifact/com.belerweb/pinyin4j") {
        name = "Pinyin4j"
        content { includeGroupAndSubgroups("com.belerweb") }
    }
    maven("https://maven.pkg.github.com/BiliXWhite/remote-inventory-next") {
        name = "GitHub"
        credentials {
            username = System.getenv("GH_USERNAME") ?: ""
            password = System.getenv("GH_TOKEN") ?: ""
        }
    }
    mavenLocal()
}

// 锁定依赖版本防冲突
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
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:$fabricLoaderVersion")
    modImplementation("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")
    modImplementation("com.belerweb:pinyin4j:${prop("pinyin_version")}")?.let { include(it) }

    modImplementation("com.terraformersmc:modmenu:${prop("modmenu")}")

    // 1.21.1+ 用 sakura-ryoko 源, 更低版本走 modrinth
    if (mcVersionInt >= 12101) {
        modImplementation("fi.dy.masa.malilib:${prop("malilib")}")
        modImplementation("fi.dy.masa.litematica:${prop("litematica")}")
        modImplementation("fi.dy.masa.tweakeroo:${prop("tweakeroo")}")
    } else {
        modImplementation("maven.modrinth:malilib:${prop("malilib_dependency")}")
        modImplementation("maven.modrinth:litematica:${prop("litematica_dependency")}")
        modImplementation("maven.modrinth:tweakeroo:${prop("tweakeroo_dependency")}")
    }

    // 快捷潜影盒 / AxShulkers
    if (mcVersionInt >= 12006) {
        val quickshulkerUrl = prop("quickshulker").toString()
        if (quickshulkerUrl.isNotEmpty()) {
            val quickshulkerFile = downloadDependencyMod(quickshulkerUrl)
            if (quickshulkerFile != null && quickshulkerFile.exists()) {
                modImplementation(files(quickshulkerFile))
            }
        }
            modImplementation("me.fallenbreath:conditional-mixin-fabric:0.6.4")
    } else {
        modImplementation("curse.maven:quick-shulker-362669:${prop("quick_shulker")}")
    }

    if (mcVersionInt <= 12006) {
        modImplementation("net.kyrptonaught:kyrptconfig:${prop("kyrptconfig")}")
    }

    // remote-inventory-next - provides remote container protocol support
    modImplementation("dev.blinkwhite.remoteinventory:remote-inventory-next:${prop("remote_inventory_version")}+${mcVersion}")
}

loom {
    val commonVmArgs = listOf("-Dmixin.debug.export=true", "-Dmixin.debug.verbose=true", "-Dmixin.env.remapRefMap=true")
    val programArgs = listOf("--width", "1280", "--height", "720", "--username", "PrinterTest")
    runs {
        named("client") {
            generateRunConfig.set(true)
            jvmArguments.set(commonVmArgs)
            programArguments.set(programArgs)
            runDirectory.dir("../../run/client")
        }
    }
}

tasks {
    register<Copy>("buildAndCollect") {
        description = "构建并收集重映射后的 jar 到 build/libs 目录"
        group = "build"
        from(remapJar.map { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod_version")}"))
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
        maven { url = uri("$rootDir/publish") }
    }
}