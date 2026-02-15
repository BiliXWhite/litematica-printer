plugins {
    `kotlin-dsl`
}

repositories {
    mavenLocal()
    mavenCentral()
    gradlePluginPortal()
//    maven("https://maven.fabricmc.net")
}

kotlin {
    jvmToolchain(21)
}

gradlePlugin {
    plugins {
        register("mod-plugin") {
            id = "mod-plugin"
            implementationClass = "ModPlugin"
        }
    }
}
