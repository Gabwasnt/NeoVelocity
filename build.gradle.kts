import java.text.SimpleDateFormat
import java.util.Date

plugins {
    id("net.neoforged.moddev")
    id("me.modmuss50.mod-publish-plugin")
}

val mcVersionLabel =
    if (project.hasProperty("mod.version_label")) project.property("mod.version_label") else sc.current.version
version = "${property("mod.version")}+${mcVersionLabel}"
base.archivesName = property("mod.id") as String

val requiredJava = when {
    sc.current.parsed >= "26.1" -> JavaVersion.VERSION_25
    sc.current.parsed >= "1.20.6" -> JavaVersion.VERSION_21
    sc.current.parsed >= "1.18" -> JavaVersion.VERSION_17
    sc.current.parsed >= "1.17" -> JavaVersion.VERSION_16
    else -> JavaVersion.VERSION_1_8
}

val modId: String = project.findProperty("mod.id") as String

repositories {
    fun strictMaven(url: String, alias: String, vararg groups: String) = exclusiveContent {
        forRepository { maven(url) { name = alias } }
        filter { groups.forEach(::includeGroup) }
    }
    strictMaven("https://www.cursemaven.com", "CurseForge", "curse.maven")
    strictMaven("https://api.modrinth.com/maven", "Modrinth", "maven.modrinth")
}

dependencies {

}

neoForge {
    version = property("dependencies.neoforge") as String

    this.mods.create(modId) {
        modSourceSets.add(sourceSets.main)
        modSourceSets.add(sourceSets.test)
    }

    runs {
        register("server") {
            gameDirectory = file("../../run/")
            server()
        }
    }
}

java {
    withSourcesJar()
    targetCompatibility = requiredJava
    sourceCompatibility = requiredJava
}

tasks {
    processResources {
        inputs.property("id", project.property("mod.id"))
        inputs.property("name", project.property("mod.name"))
        inputs.property("license", project.property("mod.license"))
        inputs.property("version", project.property("mod.version"))
        inputs.property("authors", project.property("mod.authors"))
        inputs.property("description", project.property("mod.description"))
        inputs.property("minecraft", project.property("mod.version_range"))

        val props = mapOf(
            "id" to project.property("mod.id"),
            "name" to project.property("mod.name"),
            "license" to project.property("mod.license"),
            "version" to project.property("mod.version"),
            "authors" to project.property("mod.authors"),
            "description" to project.property("mod.description"),
            "minecraft" to project.property("mod.version_range"),
        )

        filesMatching("META-INF/neoforge.mods.toml") { expand(props) }

        val mixinJava = "JAVA_${requiredJava.majorVersion}"
        filesMatching("*.mixins.json") {
            filter { line ->
                line.replace("\${java}", mixinJava.toString())
            }
        }
    }

    named<Jar>("jar") {
        from(rootProject.file("LICENSE")) {
            into("META-INF")
        }

        val gitVersion = providers.exec {
            commandLine("git", "rev-parse", "--short", "HEAD")
            isIgnoreExitValue = true
        }.standardOutput.asText.map {
            if (it.isBlank()) "unknown" else it.trim()
        }.orElse("unknown")

        manifest {
            val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(Date())
            attributes(
                mapOf(
                    "Specification-Title"      to project.property("mod.name"),
                    "Specification-Vendor"     to project.property("mod.authors"),
                    "Specification-Version"    to "1",
                    "Implementation-Title"     to project.property("mod.name"),
                    "Implementation-Version"   to archiveVersion,
                    "Implementation-Vendor"    to project.property("mod.authors"),
                    "Implementation-Timestamp" to now,
                    "Minecraft-Version"        to sc.current.version,
                    "NeoForge-Version"         to project.property("dependencies.neoforge"),
                    "Main-Commit"              to gitVersion
                )
            )
        }
    }

    named("createMinecraftArtifacts") {
        dependsOn("stonecutterGenerate")
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        from(jar.map { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
        dependsOn("build")
    }
}

publishMods {
    val publishTitle = findProperty("publish.title") as? String?:""
    val publishTarget = (findProperty("publish.targets") as? String)?.split(' ') ?: emptyList()
    val versionString = property("mod.version") as String
    val modName = property("mod.name") as String
    val fullChangelog = rootProject.file("CHANGELOG.md").readText()
    val parsedChangelog = "(?ms)^## \\[\\Q$versionString\\E\\].*?(?=^## \\[|\\z)".toRegex()
        .find(fullChangelog)?.value?.substringAfter("\n")?.trim()?:""
    val modrinthSlug = property("publish.modrinth") as String

    file = tasks.jar.map { it.archiveFile.get() }
    additionalFiles.from(tasks.named<Jar>("sourcesJar").map { it.archiveFile.get() })
    displayName = "$modName $versionString for $publishTitle"
    version = versionString
    changelog = parsedChangelog
    type = STABLE
    modLoaders.add("neoforge")

    dryRun = publishTarget.isNotEmpty() && providers.environmentVariable("MODRINTH_TOKEN").getOrNull() == null

    modrinth {
        projectId = modrinthSlug
        accessToken = providers.environmentVariable("MODRINTH_TOKEN")
        minecraftVersions.addAll(publishTarget)
    }
}
