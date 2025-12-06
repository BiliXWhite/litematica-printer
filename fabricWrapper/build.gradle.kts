import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import java.text.SimpleDateFormat
import java.util.*

plugins {
    id("java-library")
    id("maven-publish")
}

// --- 项目属性定义 ---
val modId = project.property("mod_id") as String
val modWrapperId = project.property("mod_wrapper_id") as String
val modName = project.property("mod_name") as String
val modMavenGroup = project.property("mod_maven_group") as String
val modVersion = project.property("mod_version") as String
val modArchivesBaseName = project.property("mod_archives_base_name") as String
val modDescription = project.property("mod_description") as String
val modHomepage = project.property("mod_homepage") as String
val modLicense = project.property("mod_license") as String
val modSources = project.property("mod_sources") as String
val loaderVersion = project.property("loader_version") as String

// 生成时间戳
val time = SimpleDateFormat("yyMMdd")
    .apply { timeZone = TimeZone.getTimeZone("GMT+08:00") }
    .format(Date())
    .toString()

var fullProjectVersion: String;
val buildNumber = System.getenv("GITHUB_RUN_NUMBER")
if (buildNumber != null) {
    fullProjectVersion = "$modVersion+$time+build.$buildNumber"
} else {
    fullProjectVersion = "$modVersion+$time"
}

group = modMavenGroup
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
    // 收集子模块 JAR 文件任务
    register("collectSubModules") {
        description = "收集所有子模块的 JAR 文件"
        outputs.upToDateWhen { false }

        // 依赖所有子项目的 remapJar 任务
        dependsOn(fabricSubprojects.map { it.tasks.named("remapJar") })

        doFirst {
            // 复制所有重映射后的 JAR 文件
            copy {
                from(fabricSubprojects.map { sub ->
                    sub.tasks.named("remapJar").get().outputs.files
                })
                into(layout.buildDirectory.dir("tmp/submods/META-INF/jars"))
            }
        }
    }

    // JAR 打包任务
    named<Jar>("jar") {
        outputs.upToDateWhen { false }

        from(rootProject.file("LICENSE"))
        from(layout.buildDirectory.dir("tmp/submods"))
    }

    // 资源处理任务
    named<ProcessResources>("processResources") {
        outputs.upToDateWhen { false }

        // 清理相关目录
        delete(layout.buildDirectory.dir("libs"))
        delete(layout.buildDirectory.dir("resources"))
        delete(layout.buildDirectory.dir("tmp/submods/META-INF/jars"))

        dependsOn("collectSubModules")

        val rootIcon = rootProject.file("src/main/resources/assets/$modId/icon.png")
        val resourcesFile = layout.projectDirectory.file("src/main/resources/assets/$modWrapperId/icon.png").asFile
        val buildIconFile = layout.buildDirectory.file("resources/main/assets/$modWrapperId/icon.png").get().asFile

        doLast {
            if (rootIcon.exists()) {
                if (!resourcesFile.exists()) {
                    println("⚠ 子项目未找到图标文件，准备从根项目中复制图标")
                    buildIconFile.parentFile.mkdirs()
                    rootIcon.copyTo(buildIconFile, overwrite = true)
                    println("✓ 图标复制成功: ${rootIcon.name} -> ${buildIconFile.name}")
                }
            } else {
                println("⚠ 根项目中未找到图标文件，跳过图标复制")
            }
        }

        val propertyMap = mapOf(
            "mod_wrapper_id" to modWrapperId,
            "mod_name" to modName,
            "mod_version" to modVersion,
            "mod_description" to modDescription,
            "mod_homepage" to modHomepage,
            "mod_license" to modLicense,
            "mod_sources" to modSources,
            "loader_version" to loaderVersion,
        )
        inputs.properties(propertyMap)
        filesMatching(listOf("*.mixins.json", "*.mod.json", "META-INF/*mods.toml")) {
            expand(propertyMap)
        }

        doLast {
            val jars = ArrayList<Map<String, String>>()
            val jarsDir = layout.buildDirectory.dir("tmp/submods/META-INF/jars").get().asFile

            if (jarsDir.exists() && jarsDir.isDirectory) {
                val jarFiles = jarsDir.listFiles { file ->
                    file.isFile && file.name.endsWith(".jar") &&
                            !file.name.contains("-dev.jar") &&
                            !file.name.contains("-sources.jar") &&
                            !file.name.contains("-shadow.jar")
                }

                jarFiles?.forEach { jarFile ->
                    jars.add(mapOf("file" to "META-INF/jars/${jarFile.name}"))
                }
            }

            val minecraftVersions = mutableListOf<String>()
            fabricSubprojects.forEach { subproject ->
                try {
                    val minecraftVersion = subproject.property("minecraft_dependency") as String
                    if (minecraftVersion.isNotBlank()) {
                        minecraftVersions.add(minecraftVersion)
                        println("收集到 Minecraft 版本: $minecraftVersion")
                    }
                } catch (e: Exception) {
                    println("⚠ 无法从子项目 ${subproject.name} 获取 Minecraft 版本")
                }
            }

            // 更新 fabric.mod.json 文件
            val jsonFile = layout.buildDirectory.file("resources/main/fabric.mod.json").get().asFile
            if (jsonFile.exists()) {
                val slurper = JsonSlurper()

                @Suppress("UNCHECKED_CAST")
                val jsonContent = slurper.parse(jsonFile) as MutableMap<String, Any>

                // 设置 jars 数组
                jsonContent["jars"] = jars

                // 更新 Minecraft 依赖
                @Suppress("UNCHECKED_CAST")
                val depends = jsonContent["depends"] as? MutableMap<String, Any>
                depends?.put("minecraft", minecraftVersions)

                // 写回文件
                val builder = JsonBuilder(jsonContent)
                jsonFile.bufferedWriter().use { writer ->
                    writer.write(builder.toPrettyString())
                }

                println("- JAR 文件数量: ${jars.size}")
                jars.forEach { jar ->
                    println("  - ${jar["file"]}")
                }
                println("✅ Minecraft 依赖已更新为: $minecraftVersions")
            } else {
                println("警告: 找不到生成的 fabric.mod.json 文件: ${jsonFile.absolutePath}")
            }
        }
    }
}

// Java 配置
java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}


// 发布配置
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            groupId = modMavenGroup
            artifactId = modId
            version = "versionpack-${project.version}"
            from(components["java"])
        }
    }

    repositories {
        mavenLocal()
        maven {
            url = uri("${rootProject.projectDir}/publish")
        }
    }
}