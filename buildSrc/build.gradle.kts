plugins {
    `kotlin-dsl`
}

repositories {
    mavenLocal()
    mavenCentral()
    gradlePluginPortal()
}

gradlePlugin {
    plugins {
        register("mod-plugin") {
            id = "mod-plugin"
            implementationClass = "ModPlugin"
        }
    }
}