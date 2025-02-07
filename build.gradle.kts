plugins {
    //id("org.jetbrains.gradle.plugin.idea-ext") version ("1.1.7")
    alias(neoforged.plugins.moddev).apply(false)
}

subprojects {
    repositories {
        mavenLocal()
    }
}