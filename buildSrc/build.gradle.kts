plugins {
    `kotlin-dsl`
}

repositories {
    mavenLocal()
    mavenCentral()
    gradlePluginPortal()
}

kotlin {
    jvmToolchain(25)
}

gradlePlugin {
    plugins {
        register("mod-plugin") {
            id = "mod-plugin"
            implementationClass = "ModPlugin"
        }
    }
}