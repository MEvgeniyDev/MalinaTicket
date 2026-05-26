import java.security.MessageDigest
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.jvm.tasks.Jar

plugins {
    java
    id("com.gradleup.shadow") version "9.4.1"
}

group = "ru.mevgeniy"
version = "26.5.9"

val pluginVersion = version.toString()
val buildRevision = providers.environmentVariable("GIT_COMMIT").orElse("local")

dependencies {
    compileOnly("com.destroystokyo.paper:paper-api:1.16.5-R0.1-SNAPSHOT")

    implementation("net.kyori:adventure-platform-bukkit:4.4.1")
    implementation("net.kyori:adventure-text-minimessage:4.25.0")
    implementation("net.kyori:adventure-text-serializer-legacy:4.25.0")

    testImplementation("com.destroystokyo.paper:paper-api:1.16.5-R0.1-SNAPSHOT")
    testImplementation(platform("org.junit:junit-bom:5.10.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release.set(16)
    }

    compileTestJava {
        options.encoding = "UTF-8"
        options.release.set(16)
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

    named<ShadowJar>("shadowJar") {
        archiveBaseName.set("MalinaTicket")
        archiveVersion.set(pluginVersion)
        archiveClassifier.set("")
        relocate("net.kyori", "ru.mevgeniy.malinaticket.libs.kyori") {
            skipStringConstants = true
        }
        manifest {
            attributes(
                "Implementation-Title" to "MalinaTicket",
                "Implementation-Version" to pluginVersion,
                "Implementation-Vendor" to "MEvgeniy",
                "Built-From-Revision" to buildRevision.get()
            )
        }
    }

    build {
        dependsOn(shadowJar)
    }
}

val writeJarChecksum by tasks.registering {
    group = "verification"
    description = "Writes a SHA-256 checksum next to the release JAR."

    val jarTask = tasks.named<ShadowJar>("shadowJar")
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
