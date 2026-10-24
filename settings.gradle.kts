pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/") { name = "Fabric" }
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    // One source tree, one Gradle project per Minecraft version (https://stonecutter.kikugie.dev).
    id("dev.kikugie.stonecutter") version "0.9.8"
    // Applies the remapping Loom on obfuscated versions (1.21.x) and the plain one on 26.x.
    id("dev.kikugie.loom-back-compat") version "0.4.3"
}

stonecutter {
    create(rootProject) {
        // Every supported Minecraft version: add one here AND a section in stonecutter.properties.toml.
        versions("1.21.4", "1.21.5", "1.21.6", "1.21.7", "1.21.8", "1.21.9", "1.21.10", "1.21.11", "26.1.2", "26.2", "26.3")
        // The version the code in src/ is written for (what the IDE sees).
        vcsVersion = "26.2"
    }
}

rootProject.name = "swiftclient-mod"
