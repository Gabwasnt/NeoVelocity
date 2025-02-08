@file:Suppress("SpellCheckingInspection")

import org.slf4j.event.Level
import java.text.SimpleDateFormat
import java.util.*

var envVersion: String = System.getenv("VERSION") ?: project.findProperty("mod_version") as String
if (envVersion.startsWith("v"))
    envVersion = envVersion.trimStart('v')

val modId: String = project.findProperty("mod_id") as String
val modName: String = project.findProperty("mod_name") as String
val modLicense: String = project.findProperty("mod_license") as String
val modAuthors: String = project.findProperty("mod_authors") as String
val modDescription: String = project.findProperty("mod_description") as String

plugins {
    java
    id("idea")
    id("eclipse")
    id("maven-publish")
    alias(neoforged.plugins.moddev)
}

base {
    archivesName.set(modId)
    group = project.findProperty("mod_group_id") as String
    version = envVersion
}

java {
    toolchain.vendor.set(JvmVendorSpec.JETBRAINS)
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

sourceSets.main {
    java {
        srcDir("src/main/java")
    }

    resources {
        srcDir("src/main/resources")
        srcDir("src/generated/resources")
    }
}

sourceSets.test {
    java {
        srcDir("src/test/java")
    }

    resources {
        srcDir("src/test/resources")
    }
}

neoForge {
    version = neoforged.versions.neoforge.get()

    this.mods.create(modId) {
        modSourceSets.add(sourceSets.main)
        modSourceSets.add(sourceSets.test)
    }

    unitTest {
        enable()
        testedMod = mods.named(modId)
    }

    parchment {
        enabled = true
        mappingsVersion = libs.versions.parchment
        minecraftVersion = libs.versions.parchmentMC
    }

    runs {
        configureEach {
            logLevel.set(Level.DEBUG)
            sourceSet = project.sourceSets.main
        }

        create("server") {
            server()
            gameDirectory.set(file("runs/server"))

            systemProperty("forge.enabledGameTestNamespaces", modId)
            programArgument("nogui")

            environment.put("CM_TEST_RESOURCES", file("src/test/resources").path)

            sourceSet = project.sourceSets.test
        }
    }
}

repositories {
    mavenLocal()
    mavenCentral()

    maven("https://maven.blamejared.com/") {
        // location of the maven that hosts JEI files since January 2023
        name = "Jared's maven"
    }

    maven("https://www.cursemaven.com") {
        content {
            includeGroup("curse.maven")
        }
    }

    maven("https://modmaven.dev") {
        // location of a maven mirror for JEI files, as a fallback
        name = "ModMaven"
    }
}

dependencies {
    // Core Projects and Libraries
    //this {}

    runtimeOnly(neoforged.testframework)
    testImplementation(neoforged.testframework)
}

tasks.withType<ProcessResources> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(arrayOf("-Xmaxerrs", "9000"))
}

tasks.withType<Jar> {
    val gitVersion = providers.exec {
        commandLine("git", "rev-parse", "HEAD")
    }.standardOutput.asText.get()

    from(rootDir.resolve("LICENSE"))

    manifest {
        val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(Date())
        attributes(
            mapOf(
                "Specification-Title" to "G_ab/Gabwasnt",
                "Specification-Vendor" to "G_ab/Gabwasnt",
                "Specification-Version" to "1",
                "Implementation-Title" to "G_ab/Gabwasnt",
                "Implementation-Version" to archiveVersion,
                "Implementation-Vendor" to "G_ab/Gabwasnt",
                "Implementation-Timestamp" to now,
                "Minecraft-Version" to mojang.versions.minecraft.get(),
                "NeoForge-Version" to neoforged.versions.neoforge.get(),
                "Main-Commit" to gitVersion
            )
        )
    }
}

tasks.withType<ProcessResources>().configureEach {
    val replaceProperties: Map<String, Any> = mapOf(
        "minecraft_version" to mojang.versions.minecraft.get(),
        "neo_version" to neoforged.versions.neoforge.get(),
        "minecraft_version_range" to mojang.versions.minecraftRange.get(),
        "neo_version_range" to neoforged.versions.neoforgeRange.get(),
        "loader_version_range" to neoforged.versions.loaderRange.get(),
        "mod_id" to modId,
        "mod_version" to envVersion,
        "mod_name" to modName,
        "mod_license" to modLicense,
        "mod_authors" to modAuthors,
        "mod_description" to modDescription
    )

    inputs.properties(replaceProperties)
    filesMatching("META-INF/neoforge.mods.toml") {
        expand(replaceProperties)
    }
}

val PACKAGES_URL = System.getenv("GH_PKG_URL") ?: "https://maven.pkg.github.com/Gabwasnt/NeoVelocity"
publishing {
    publications.register<MavenPublication>("NeoVelocity") {
        artifactId = "$modId-neoforge"
        from(components.getByName("java"))
    }

    repositories {
        // GitHub Packages
        maven {
            name = "GitHubPackages"
            url = uri(PACKAGES_URL)
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
