@file:Suppress("UnstableApiUsage")

plugins {
    id("mod-plugin")
    id("maven-publish")
    id("net.fabricmc.fabric-loom")
    id("com.replaymod.preprocess")
}

version = fullProjectVersion
group = modMavenGroup
base {
    archivesName.set(modArchivesBaseName)
}

repositories {
    mavenLocal()
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
    maven("https://maven.pkg.github.com/BiliXWhite/remote-inventory-next") {
        name = "GitHub"
        credentials {
            username = System.getenv("GH_USERNAME") ?: ""
            password = System.getenv("GH_TOKEN") ?: ""
        }
    }
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
    implementation("net.fabricmc:fabric-loader:$fabricLoaderVersion")
    implementation("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")
    implementation("com.belerweb:pinyin4j:${prop("pinyin_version")}")?.let { include(it) }
    implementation("com.terraformersmc:modmenu:${prop("modmenu")}")

    // 远程容器
    // 允许单独覆盖远程容器所对应的 MC 版本后缀（部分 MC 版本暂无对应构建时，可复用相邻版本的构建）
    val remoteInventoryMcSuffix = propOrNull("remote_inventory_mc_suffix")?.toString() ?: mcVersion
    implementation("dev.blinkwhite.remoteinventory:remote-inventory-next:${prop("remote_inventory_version")}+${remoteInventoryMcSuffix}")

    // Masa
    implementation("fi.dy.masa.malilib:${prop("malilib")}:${prop("malilib_dependency")}")
    implementation("fi.dy.masa.litematica:${prop("litematica")}:${prop("litematica_dependency")}")
    implementation("fi.dy.masa.tweakeroo:${prop("tweakeroo")}:${prop("tweakeroo_dependency")}")

    if (mcVersionInt == 260200) {
        implementation(files("/versions/26.2/libs/tweakermore-v3.33.2-mc26.2.jar"))
    }

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
            generateRunConfig.set(true)
            jvmArguments.set(commonVmArgs)
            programArguments.set(programArgs)
            runDirectory.dir("../../run/client")
        }
    }
}

tasks {
    register<Copy>("buildAndCollect") {
        description = "Build and collect the jar to the root project build directory"
        group = "build"
        from(jar.map { it.archiveFile })
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
        maven {
            url = uri("$rootDir/publish")
        }
    }
}