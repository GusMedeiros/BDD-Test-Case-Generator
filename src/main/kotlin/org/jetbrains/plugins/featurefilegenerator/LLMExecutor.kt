package org.jetbrains.plugins.featurefilegenerator.executor

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import kotlinx.coroutines.runBlocking
import org.jetbrains.plugins.featurefilegenerator.LLMSettings
import org.jetbrains.plugins.featurefilegenerator.cli.LLMSettingsCLI
import java.io.File

class LLMExecutor(private val llmSettings: Any) {

    /**
     * Executa uma única LLM com base no nome e no caminho do arquivo.
     * Essa versão é utilizada tanto no modo CLI quanto no plugin, mas no plugin usamos o ProgressManager.
     */
    fun execute(llmName: String, filePath: String, onResult: (String, String) -> Unit) {
        val config = when (llmSettings) {
            is LLMSettings -> llmSettings.getConfigurationByName(llmName)
            is LLMSettingsCLI -> llmSettings.getConfigurationByName(llmName)
            else -> null
        } ?: throw IllegalArgumentException("LLM '$llmName' não encontrada.")

        // Se estivermos em modo plugin, usaremos o ProgressManager
        if (llmSettings is LLMSettings) {
            ProgressManager.getInstance().run(object : Task.Backgroundable(null, "Gerando Arquivo .feature ($llmName)", true) {
                override fun run(indicator: ProgressIndicator) {
                    indicator.text = "Executando script da LLM: $llmName..."
                    indicator.isIndeterminate = true
                    val result = runProcess(config, filePath)
                    onResult(llmName, result)
                }
            })
        } else {
            // Modo CLI: execução direta
            val result = runProcess(config, filePath)
            onResult(llmName, result)
        }
    }

    /**
     * Executa todas as LLMs configuradas em sequência de forma assíncrona (no modo plugin).
     * Cada execução ocorrerá dentro de uma única tarefa de background.
     */
    fun executeBatchAsync(filePath: String, onResult: (String, String) -> Unit) {
        if (llmSettings !is LLMSettings) {
            throw IllegalStateException("Modo plugin requer LLMSettings, não LLMSettingsCLI.")
        }
        val configurations = llmSettings.getConfigurations()
        if (configurations.isEmpty()) {
            throw IllegalStateException("Nenhuma configuração de LLM encontrada.")
        }

        ProgressManager.getInstance().run(object : Task.Backgroundable(null, "Executando LLMs", true) {
            override fun run(indicator: ProgressIndicator) {
                for (config in configurations) {
                    indicator.text = "Executando ${config.name}..."
                    val result = runProcess(config, filePath)
                    // O callback onResult pode ser chamado fora da thread do progress, então usamos invokeLater
                    com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                        onResult(config.name, result)
                    }
                }
            }
        })
    }

    /**
     * Monta e executa o processo da LLM com base na configuração e no arquivo de entrada.
     */
    private fun runProcess(config: Any, filePath: String): String {
        return try {
            val commandList = mutableListOf<String>()

            when (config) {
                is LLMSettings.LLMConfiguration -> {
                    commandList.add(config.command)
                    commandList.add(config.scriptFilePath)
                    config.namedParameters.forEach { param ->
                        if (param.argName.isNotBlank()) {
                            when (param) {
                                is LLMSettings.StringParam -> {
                                    val value = param.value.trim()
                                    if (value.isNotBlank()) {
                                        commandList.add(param.argName)
                                        commandList.add(value)
                                    }
                                }
                                is LLMSettings.ListParam -> {
                                    val value = param.value.trim()
                                    if (value.isNotBlank()) {
                                        commandList.add(param.argName)
                                        commandList.add(value)
                                    }
                                }
                                is LLMSettings.IntParam -> {
                                    commandList.add(param.argName)
                                    commandList.add(param.value.toString())
                                }
                                is LLMSettings.DoubleParam -> {
                                    commandList.add(param.argName)
                                    commandList.add(param.value.toString().replace(',', '.'))
                                }
                                is LLMSettings.BooleanParam -> {
                                    if (param.value) commandList.add(param.argName)
                                }
                            }
                        }
                    }
                }
                is LLMSettingsCLI.LLMConfiguration -> {
                    commandList.add(config.command)
                    commandList.add(config.scriptFilePath)
                    config.namedParameters.forEach { param ->
                        if (param.argName.isNotBlank()) {
                            when (param) {
                                is LLMSettingsCLI.NamedParameter.StringParam -> {
                                    val value = param.value.trim()
                                    if (value.isNotBlank()) {
                                        commandList.add(param.argName)
                                        commandList.add(value)
                                    }
                                }
                                is LLMSettingsCLI.NamedParameter.IntParam -> {
                                    commandList.add(param.argName)
                                    commandList.add(param.value.toString())
                                }
                                is LLMSettingsCLI.NamedParameter.DoubleParam -> {
                                    commandList.add(param.argName)
                                    commandList.add(param.value.toString().replace(',', '.'))
                                }
                                is LLMSettingsCLI.NamedParameter.BooleanParam -> {
                                    if (param.value) commandList.add(param.argName)
                                }
                            }
                        }
                    }
                }
            }

            // Adiciona o parâmetro do arquivo de história
            commandList.add("--user_story_path")
            commandList.add(filePath)

            // Debug: imprime o comando a ser executado
            println("🔍 Executando comando: ${commandList.joinToString(" ")}")

            val process = ProcessBuilder(commandList)
                .directory(File("."))
                .redirectErrorStream(true)
                .start()

            val output = process.inputStream.bufferedReader().use { it.readText() }
            val exitCode = process.waitFor()
            println("🔍 Processo finalizado com código: $exitCode")
            println("🔍 Saída do processo: $output")
            output
        } catch (e: Exception) {
            "Erro ao executar o processo: ${e.message}"
        }
    }

    fun executeBatchCli(filePath: String, onResult: (String, String) -> Unit) = runBlocking {
        if (llmSettings !is LLMSettingsCLI) {
            throw IllegalStateException("Modo CLI requer LLMSettingsCLI, não ${llmSettings::class.simpleName}.")
        }
        val configurations = llmSettings.getConfigurations()
        if (configurations.isEmpty()) {
            throw IllegalStateException("Nenhuma configuração de LLM encontrada.")
        }
        configurations.forEach { config ->
            // Executa cada LLM de forma síncrona
            val result = runProcess(config, filePath)
            onResult(config.name, result)
        }
    }
}
