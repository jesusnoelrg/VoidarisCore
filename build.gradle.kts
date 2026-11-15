plugins {
    java
    id("xyz.jpenilla.run-paper") version "3.1.0"
}

group = "club.voidaris"
version = "1.0.0"

val paperVersion = "26.3.build.248-stable"
val targetJava = 25

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") { name = "papermc" }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:$paperVersion")
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
        val props = mapOf("version" to project.version)
        inputs.properties(props)
        filteringCharset = "UTF-8"
        filesMatching("plugin.yml") { expand(props) }
    }

    runServer {
        minecraftVersion("26.3")
    }
}
