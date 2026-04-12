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

val fabricSubprojects = rootProject.subprojects.filter { it.name != "fabricWrapper" }

fabricSubprojects.forEach {
    evaluationDependsOn(":${it.name}")
}

tasks {
    val collectSubModules by registering {
        outputs.upToDateWhen { false }

        dependsOn(fabricSubprojects.map { it.tasks.named("buildAndCollect") })

        doLast {
            val destDir = layout.buildDirectory.dir("tmp/submods/META-INF/jars").get().asFile
            destDir.deleteRecursively()
            destDir.mkdirs()

            val rootBuildDir = rootProject.layout.buildDirectory.get().asFile

            fabricSubprojects.forEach { sub ->
                val subDir = rootBuildDir.resolve("libs/${sub.property("mod_version")}")
                if (subDir.exists() && subDir.isDirectory) {
                    subDir.listFiles()
                        ?.filter { it.extension == "jar" }
                        ?.forEach { jar ->
                            val destFile = destDir.resolve(jar.name)
                            if (!destFile.exists() || jar.readBytes().contentEquals(destFile.readBytes()).not()) {
                                jar.copyTo(destFile, overwrite = true)
                                println("Copied: ${jar.name}")
                            }
                        }
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
            val jars = jarsDir.listFiles()
                ?.filter { it.extension == "jar" }
                ?.map { mapOf("file" to "META-INF/jars/${it.name}") }
                ?: emptyList()

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