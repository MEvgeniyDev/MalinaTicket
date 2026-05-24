import java.security.MessageDigest
import org.gradle.jvm.tasks.Jar

plugins {
    java
}

group = "ru.mevgeniy"
version = "26.5.8"

val pluginVersion = version.toString()
val buildRevision = providers.environmentVariable("GIT_COMMIT").orElse("local")

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.65-stable")

    testImplementation("io.papermc.paper:paper-api:26.1.2.build.65-stable")
    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release.set(25)
    }

    processResources {
        filteringCharset = "UTF-8"
        filesMatching("plugin.yml") {
            expand("version" to pluginVersion)
        }
    }

    test {
        useJUnitPlatform()
    }

    jar {
        archiveBaseName.set("MalinaTicket")
        archiveVersion.set(pluginVersion)
        manifest {
            attributes(
                "Implementation-Title" to "MalinaTicket",
                "Implementation-Version" to pluginVersion,
                "Implementation-Vendor" to "MEvgeniy",
                "Built-From-Revision" to buildRevision.get()
            )
        }
    }
}

val writeJarChecksum by tasks.registering {
    group = "verification"
    description = "Writes a SHA-256 checksum next to the release JAR."

    val jarTask = tasks.named<Jar>("jar")
    dependsOn(jarTask)

    val jarFile = jarTask.flatMap { it.archiveFile }
    inputs.file(jarFile)
    outputs.file(jarFile.map { it.asFile.resolveSibling("${it.asFile.name}.sha256") })

    doLast {
        val file = jarFile.get().asFile
        val digest = MessageDigest.getInstance("SHA-256").digest(file.readBytes())
        val hash = digest.joinToString("") { "%02x".format(it) }
        file.resolveSibling("${file.name}.sha256").writeText("$hash  ${file.name}\n", Charsets.UTF_8)
    }
}

tasks.named("build") {
    finalizedBy(writeJarChecksum)
}
