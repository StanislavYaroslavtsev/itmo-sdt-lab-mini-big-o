plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.19.0"
}

group = "dev.syaroslavtsev.minibigo"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdea("2025.2.4")
        bundledPlugin("com.intellij.java")
    }
}

intellijPlatform {
    instrumentCode = false

    pluginConfiguration {
        ideaVersion {
            sinceBuild = "252.25557"
        }
        changeNotes = """
            Initial version
        """.trimIndent()
    }
}

tasks {
    withType<JavaCompile> {
        options.release.set(21)
        options.encoding = "UTF-8"
    }

    javadoc {
        (options as StandardJavadocDocletOptions).apply {
            encoding = "UTF-8"
            docEncoding = "UTF-8"
            charSet = "UTF-8"
            windowTitle = "MiniBigO ${project.version} API"
            docTitle = "MiniBigO ${project.version} API"
            memberLevel = JavadocMemberLevel.PRIVATE
            links("https://docs.oracle.com/en/java/javase/21/docs/api/")
            addStringOption("Xdoclint:all,-missing", "-quiet")
        }
    }
}