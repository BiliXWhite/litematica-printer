plugins {
    id("maven-publish")
    id("net.fabricmc.fabric-loom") version "1.15-SNAPSHOT" apply false
    id("net.fabricmc.fabric-loom-remap") version "1.15-SNAPSHOT" apply false

    // https://github.com/ReplayMod/preprocessor
    // https://github.com/Fallen-Breath/preprocessor
    // https://jitpack.io/#Fallen-Breath/preprocessor
    id("com.replaymod.preprocess") version "c5abb4fb12"
}

preprocess {
    val mc11802 = createNode("1.18.2", 1_18_02, "mojang")
    val mc11904 = createNode("1.19.4", 1_19_04, "mojang")
    val mc12001 = createNode("1.20.1", 1_20_01, "mojang")
    val mc12002 = createNode("1.20.2", 1_20_02, "mojang")
    val mc12004 = createNode("1.20.4", 1_20_04, "mojang")
    val mc12006 = createNode("1.20.6", 1_20_06, "mojang")
    val mc12101 = createNode("1.21.1", 1_21_01, "mojang")
    val mc12103 = createNode("1.21.3", 1_21_03, "mojang")
    val mc12104 = createNode("1.21.4", 1_21_04, "mojang")
    val mc12105 = createNode("1.21.5", 1_21_05, "mojang")
    val mc12106 = createNode("1.21.6", 1_21_06, "mojang")
    val mc12109 = createNode("1.21.9", 1_21_09, "mojang")
    val mc12111 = createNode("1.21.11", 1_21_11, "mojang")

    mc11802.link(mc11904, file("versions/mapping-1.18.2-1.19.4.txt"))
    mc11904.link(mc12001, null)
    mc12001.link(mc12002, null)
    mc12002.link(mc12004, null)
    mc12004.link(mc12006, null)
    mc12006.link(mc12101, null)
    mc12101.link(mc12103, null)
    mc12103.link(mc12104, null)
    mc12104.link(mc12105, file("versions/mapping-1.21.4-1.21.5.txt"))
    mc12105.link(mc12106, null)
    mc12105.link(mc12106, null)
    mc12106.link(mc12109, null)
    mc12109.link(mc12111, null)

    strictExtraMappings.set(false)

    // See https://github.com/Fallen-Breath/fabric-mod-template/blob/1d72d77a1c5ce0bf060c2501270298a12adab679/build.gradle#L55-L63
    for (node in getNodes()) {
        findProject(node.project)
            ?.ext
            ?.set("mcVersion", node.mcVersion)
    }
}

tasks.register("cleanPreprocessSources") {
    group = "${project.property("mod_id")}"

    doFirst {
        subprojects {
            val path = project.projectDir.toPath().resolve("build/preprocessed")
            path.toFile().deleteRecursively()
        }
    }
}