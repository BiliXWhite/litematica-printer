import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.JavaVersion
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.language.jvm.tasks.ProcessResources
import org.gradle.kotlin.dsl.*
import org.gradle.api.plugins.JavaPlugin

abstract class ModPlugin : Plugin<Project> {
    override fun apply(project: Project) = with(project) {
        pluginManager.apply("java")

        configureJava()
        configureLombok()
        configureJavaCompile()
        configureResources()
        configureJar()
    }

    private fun Project.configureJava() {
        extensions.configure<JavaPluginExtension> {
            sourceCompatibility = javaVersion
            targetCompatibility = javaVersion
            // withSourcesJar()
        }
    }

    private fun Project.configureLombok() {
        pluginManager.withPlugin("java") {
            dependencies.add(
                JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME,
                "org.projectlombok:lombok:$lombokVersion"
            )
            dependencies.add(
                JavaPlugin.ANNOTATION_PROCESSOR_CONFIGURATION_NAME,
                "org.projectlombok:lombok:$lombokVersion"
            )
        }
    }

    private fun Project.configureJavaCompile() {
        tasks.withType<JavaCompile>().configureEach {
            options.encoding = "UTF-8"
            options.compilerArgs.addAll(
                listOf(
                    "-Xlint:deprecation",
                    "-Xlint:unchecked"
                )
            )
            if (javaVersion <= JavaVersion.VERSION_1_8) {
                options.compilerArgs.add("-Xlint:-options")
            }
        }
    }

    private fun Project.configureResources() {
        tasks.withType<ProcessResources>().configureEach {
            inputs.properties(placeholderProps)
            filesMatching(listOf("*.mixins.json", "*.mod.json", "META-INF/*mods.toml")) {
                expand(placeholderProps)
            }
        }
    }

    private fun Project.configureJar() {
        tasks.withType<Jar>().configureEach {
            from(rootProject.file("LICENSE")) {
                rename { originalName ->
                    "${originalName}_${modArchivesBaseName}"
                }
            }
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
            manifest {
                attributes(
                    mapOf(
                        "Implementation-Title" to project.name,
                        "Implementation-Version" to project.version
                    )
                )
            }
        }
    }
}
