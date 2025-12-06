plugins {
    id("maven-publish")
    id("com.github.hierynomus.license") version "0.16.1" apply false
    id("fabric-loom") version "1.13-SNAPSHOT" apply false

    // https://github.com/ReplayMod/preprocessor
    // https://github.com/Fallen-Breath/preprocessor
    id("com.replaymod.preprocess") version "d452ef7612"

    // https://github.com/Fallen-Breath/yamlang
    id("me.fallenbreath.yamlang") version "1.5.0" apply false
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

    mc11802.link(mc11904, file("versions/mapping-1.18.2-1.19.4.txt"))
    mc11904.link(mc12001, null)
    mc12001.link(mc12002, null)
    mc12002.link(mc12004, null)
    mc12004.link(mc12006, null)
    mc12006.link(mc12101, null)
    mc12101.link(mc12103, null)
    mc12103.link(mc12104, null)
    mc12104.link(mc12105, file("versions/mapping-1.21.4-1.21.5.txt"))
    mc12105.link(mc12106, file("versions/mapping-1.21.5-1.21.6.txt"))
    mc12105.link(mc12106, null)
    mc12106.link(mc12109, null)

    strictExtraMappings.set(false)
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

fun detectSystemProxy(): Any {
    // Try to get WinHTTP proxy using 'netsh winhttp show advproxy' on Windows
    if (System.getProperty("os.name").lowercase().contains("windows")) {
        try {
            val proc = Runtime.getRuntime().exec(arrayOf("cmd", "/c", "netsh winhttp show advproxy"))
            val output = proc.inputStream.bufferedReader().use { it.readText() }
            val enabledMatcher = """"ProxyIsEnabled":\s*(true|false)""".toRegex().find(output)
            if (enabledMatcher != null) {
                val isEnabled = enabledMatcher.groupValues[1].toBoolean()
                if (!isEnabled) {
                    return "disabled"
                }
            } else {
                println("ProxyIsEnabled field not detected.")
                return false
            }
            val matcher = """"Proxy":\s*"([^"]+)"""".toRegex().find(output)
            if (matcher != null) {
                var proxy = matcher.groupValues[1]
                if (proxy.contains(":")) {
                    proxy = proxy.replaceFirst("""https?://""".toRegex(), "")
                    val (host, port) = proxy.split(":", limit = 2)
                    return mapOf("host" to host, "port" to port.toInt())
                }
            }
        } catch (e: Exception) {
            println("WinHTTP proxy detection failed: ${e.message}")
            return false
        }
    }

    // Try to get system proxy settings on Unix-like systems (Linux and macOS)
    if (System.getProperty("os.name").lowercase().contains("linux") || System.getProperty("os.name").lowercase()
            .contains("mac")
    ) {
        try {
            val env = System.getenv()
            val httpProxy = env["http_proxy"] ?: env["HTTP_PROXY"]
            val httpsProxy = env["https_proxy"] ?: env["HTTPS_PROXY"]

            if (httpProxy != null || httpsProxy != null) {
                var proxy = (httpProxy ?: httpsProxy)!!
                if (proxy.contains(":")) {
                    proxy = proxy.replaceFirst("""https?://""".toRegex(), "")
                    val (host, port) = proxy.split(":", limit = 2)
                    return mapOf("host" to host, "port" to port.toInt())
                }
            }
        } catch (e: Exception) {
            println("Unix-like system proxy detection failed: ${e.message}")
            return false
        }
    }

    return false
}

val proxy = detectSystemProxy()
when {
    proxy == "disabled" -> {
        println("System proxy is disabled.")
    }

    proxy is Map<*, *> -> {
        val proxyHost = proxy["host"] as String
        val proxyPort = proxy["port"] as Int
        System.setProperty("http.proxyHost", proxyHost)
        System.setProperty("http.proxyPort", proxyPort.toString())
        System.setProperty("https.proxyHost", proxyHost)
        System.setProperty("https.proxyPort", proxyPort.toString())
        println("Detected system proxy: $proxyHost:$proxyPort")
    }

    else -> {
        println("No system proxy detected.")
    }
}