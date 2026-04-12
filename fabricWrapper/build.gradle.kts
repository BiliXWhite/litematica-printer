import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import java.text.SimpleDateFormat
import java.util.*

plugins {
    id("java-library")
    id("maven-publish")
    id("mod-plugin")
}

repositories {
    mavenLocal()
    mavenCentral()
    gradlePluginPortal()
}

val localBuildInfoFile = file(".local_build_counter.json")
val jsonSlurper = JsonSlurper()

group = modMavenGroup

val time = SimpleDateFormat("yyMMdd")
    .apply { timeZone = TimeZone.getTimeZone("GMT+08:00") }
    .format(Date())
    .toString()

var fullProjectVersion = "$modVersion+$time"

val isCiBuild = System.getenv("GITHUB_ACTIONS") == "true"
if (isCiBuild) {
    val commitSha = System.getenv("GITHUB_SHA")?.take(8)
    if (commitSha != null) {
        fullProjectVersion += "+$commitSha"
    }
}

version = fullProjectVersion

base {
    archivesName.set("$modArchivesBaseName-versionpack")
}

// 获取所有子项目（排除当前项目）
val fabricSubprojects = rootProject.subprojects.filter { it.name != "fabricWrapper" }

// 确保先评估所有子项目
fabricSubprojects.forEach {
    evaluationDependsOn(":${it.name}")
}

tasks {
    val collectSubModules by registering {
        outputs.upToDateWhen { false }

        dependsOn(fabricSubprojects.map { it.tasks.named("build") })

        doLast {
            val destDir = layout.buildDirectory.dir("tmp/submods/META-INF/jars").get().asFile
            destDir.deleteRecursively()
            destDir.mkdirs()

            fabricSubprojects.forEach { sub ->
                val jarTask = sub.tasks.named<Jar>("jar")
                val jarFile = jarTask.get().archiveFile.get().asFile
                if (jarFile.exists()) {
                    jarFile.copyTo(destDir.resolve(jarFile.name), overwrite = true)
                    println("Copied: ${jarFile.name}")
                }
            }
        }
    }

    named<Jar>("jar") {
        dependsOn(collectSubModules)
        dependsOn("processResources")

        from(rootProject.file("LICENSE"))
        from(layout.buildDirectory.dir("tmp/submods"))
    }

    named<ProcessResources>("processResources") {
        dependsOn(collectSubModules)

        doLast {
            val jarsDir = layout.buildDirectory.dir("tmp/submods/META-INF/jars").get().asFile
            val jars = mutableListOf<Map<String, String>>()

            jarsDir.listFiles()?.filter { it.name.endsWith(".jar") }?.forEach { jar ->
                jars.add(mapOf("file" to "META-INF/jars/${jar.name}"))
            }

            val minecraftVersions = fabricSubprojects.mapNotNull { sub ->
                try {
                    sub.property("minecraft_dependency") as? String
                } catch (e: Exception) {
                    null
                }
            }.distinct()

            val jsonFile = layout.buildDirectory.file("resources/main/fabric.mod.json").get().asFile
            if (jsonFile.exists()) {
                val json = JsonSlurper().parse(jsonFile) as MutableMap<String, Any>
                json["jars"] = jars
                (json["depends"] as? MutableMap<String, Any>)?.put("minecraft", minecraftVersions)
                jsonFile.writeText(JsonBuilder(json).toPrettyString())

                println("JAR files: ${jars.size}, Minecraft: $minecraftVersions")
            }
        }
    }
}