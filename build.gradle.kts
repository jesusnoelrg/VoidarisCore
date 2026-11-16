import java.util.Properties

plugins {
    java
    id("xyz.jpenilla.run-paper") version "3.1.0"
}

group = "club.voidaris"
version = "1.0.0"

val minecraftVersion = "26.3"
// Shared by the compile API and runServer, so the test server runs the same Paper build the code is compiled against.
val paperBuild = 248
val paperVersion = "$minecraftVersion.build.$paperBuild-stable"
val targetJava = 25
// Resolved at runtime through plugin.yml "libraries", so they are not shaded into the jar.
val hikariVersion = "7.1.0"
val mariadbVersion = "3.5.11"

// Machine-specific paths live in local.properties (ignored by git), so each developer can point at their own server.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.reader(Charsets.UTF_8).use(::load)
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") { name = "papermc" }
    maven("https://repo.extendedclip.com/releases/") { name = "placeholderapi" }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:$paperVersion")
    compileOnly("net.luckperms:api:5.4")
    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly("com.zaxxer:HikariCP:$hikariVersion")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(targetJava)
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release = targetJava
    }

    processResources {
        val props = mapOf(
            "version" to project.version,
            "hikariVersion" to hikariVersion,
            "mariadbVersion" to mariadbVersion,
        )
        inputs.properties(props)
        filteringCharset = "UTF-8"
        filesMatching("plugin.yml") { expand(props) }
    }

    runServer {
        minecraftVersion(minecraftVersion)
        build(paperBuild)
        // Test-only: lets cosmetic and other permission nodes be granted with /lp, and placeholders be resolved, on the local server.
        downloadPlugins {
            modrinth("luckperms", "v5.5.71-bukkit")
            modrinth("placeholderapi", "2.12.3")
        }
    }

    register<Copy>("deployPlugin") {
        group = "voidaris"
        description = "Builds the plugin jar and copies it into deploy.dir from local.properties."
        from(jar)
        into(provider {
            localProperties.getProperty("deploy.dir")
                ?: throw GradleException("Set deploy.dir in local.properties to the server's plugins folder.")
        })
    }
}
