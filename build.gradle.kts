plugins {
    java
    id("xyz.jpenilla.run-paper") version "3.1.0"
}

group = "club.voidaris"
version = "1.0.0"

val paperVersion = "26.3.build.248-stable"
val targetJava = 25
// Resolved at runtime through plugin.yml "libraries", so they are not shaded into the jar.
val hikariVersion = "7.1.0"
val mariadbVersion = "3.5.11"

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
        minecraftVersion("26.3")
    }
}
