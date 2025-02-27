import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML

fun properties(key: String) = providers.gradleProperty(key)
fun environment(key: String) = providers.environmentVariable(key)

plugins {
    id("java") // Suporte a Java
    alias(libs.plugins.kotlin) // Suporte a Kotlin
    alias(libs.plugins.gradleIntelliJPlugin) // Plugin do IntelliJ
    alias(libs.plugins.changelog) // Plugin de Changelog
    alias(libs.plugins.qodana) // Plugin Qodana
    alias(libs.plugins.kover) // Plugin Kover
    kotlin("plugin.serialization") version "1.8.20" // ✅ Suporte à serialização JSON
    id("application") // Suporte para gerar a CLI executável
    id("com.github.johnrengelman.shadow") version "8.1.1" // Fat jar (Uber Jar)
}

group = properties("pluginGroup").get()
version = properties("pluginVersion").get()

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib")) // ✅ Inclui a biblioteca padrão do Kotlin
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.0") // ✅ Corrotinas
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1") // ✅ Serialização JSON
}

kotlin {
    jvmToolchain(17)
}

// Configuração do IntelliJ Plugin
intellij {
    pluginName = properties("pluginName")
    version = properties("platformVersion")
    type = properties("platformType")
    plugins = properties("platformPlugins").map { it.split(',').map(String::trim).filter(String::isNotEmpty) }
}

changelog {
    groups.empty()
    repositoryUrl = properties("pluginRepositoryUrl")
}

kover {
    reports {
        total {
            xml { onCheck = true }
        }
    }
}

// Configuração para rodar a CLI
application {
    mainClass.set("org.jetbrains.plugins.featurefilegenerator.cli.BatchGenerateFeatureCLI")
}

// Configuração do JAR normal
tasks.jar {
    manifest {
        attributes["Main-Class"] = "org.jetbrains.plugins.featurefilegenerator.cli.BatchGenerateFeatureCLI"
    }
    from(sourceSets.main.get().output)
}

// Configuração do Shadow Jar (Fat Jar)
tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    archiveBaseName.set("feature-generator")
    archiveClassifier.set("all")
    archiveVersion.set("")

    manifest {
        attributes["Main-Class"] = "org.jetbrains.plugins.featurefilegenerator.cli.BatchGenerateFeatureCLI"
    }

    from(sourceSets.main.get().output)

    // ✅ Forma correta de incluir todas as dependências no Gradle moderno
    val runtimeClasspathFiles = project.configurations.getByName("runtimeClasspath").files
    runtimeClasspathFiles.forEach { file ->
        from(zipTree(file))
    }

    dependencies {
        include(dependency("org.jetbrains.kotlinx:kotlinx-serialization-json"))
        include(dependency("org.jetbrains.kotlin:kotlin-stdlib"))
    }

    mergeServiceFiles()
}


// Configuração de tarefas adicionais
tasks {
    wrapper {
        gradleVersion = properties("gradleVersion").get()
    }

    patchPluginXml {
        version = properties("pluginVersion")
        sinceBuild = properties("pluginSinceBuild")
        untilBuild = properties("pluginUntilBuild")

        pluginDescription = providers.fileContents(layout.projectDirectory.file("README.md")).asText.map {
            val start = "<!-- Plugin description -->"
            val end = "<!-- Plugin description end -->"

            with(it.lines()) {
                if (!containsAll(listOf(start, end))) {
                    throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
                }
                subList(indexOf(start) + 1, indexOf(end)).joinToString("\n").let(::markdownToHTML)
            }
        }

        val changelog = project.changelog
        changeNotes = properties("pluginVersion").map { pluginVersion ->
            with(changelog) {
                renderItem(
                    (getOrNull(pluginVersion) ?: getUnreleased())
                        .withHeader(false)
                        .withEmptySections(false),
                    Changelog.OutputType.HTML,
                )
            }
        }
    }

    runIdeForUiTests {
        systemProperty("robot-server.port", "8082")
        systemProperty("ide.mac.message.dialogs.as.sheets", "false")
        systemProperty("jb.privacy.policy.text", "<!--999.999-->")
        systemProperty("jb.consents.confirmation.enabled", "false")
    }

    signPlugin {
        certificateChain = environment("CERTIFICATE_CHAIN")
        privateKey = environment("PRIVATE_KEY")
        password = environment("PRIVATE_KEY_PASSWORD")
    }

    publishPlugin {
        dependsOn("patchChangelog")
        token = environment("PUBLISH_TOKEN")
        channels = properties("pluginVersion").map {
            listOf(it.substringAfter('-', "").substringBefore('.').ifEmpty { "default" })
        }
    }
}
