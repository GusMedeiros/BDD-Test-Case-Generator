package org.jetbrains.plugins.featurefilegenerator.executor

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import org.jetbrains.plugins.featurefilegenerator.LLMSettings
import java.io.File

class LLMExecutor {

    /**
     * Executa uma única LLM baseada no nome dela.
     * @param llmName Nome da LLM a ser executada
     * @param filePath Caminho do arquivo de entrada
     * @param onResult Callback com o resultado
     */
    fun execute(llmName: String, filePath: String, onResult: (String, String) -> Unit) {
        val settings = LLMSettings.getInstance()
        val config = settings.getConfigurationByName(llmName)
            ?: throw IllegalArgumentException("LLM '$llmName' não encontrada.")

        ProgressManager.getInstance().run(object : Task.Backgroundable(null, "Gerando Arquivo .feature ($llmName)", true) {
            override fun run(indicator: ProgressIndicator) {
                indicator.text = "Executando script da LLM: $llmName..."
                indicator.isIndeterminate = true

                val result = runProcess(config, filePath)

                // Callback para processar o resultado (UI ou CLI)
                onResult(llmName, result)
            }
        })
    }

    /**
     * Executa todas as LLMs configuradas em paralelo.
     * @param filePath Caminho do arquivo de entrada
     * @param onResult Callback chamado com o resultado de cada LLM
     */
    fun executeBatch(filePath: String, onResult: (String, String) -> Unit) {
        val settings = LLMSettings.getInstance()
        val configuredLLMs = settings.getConfigurations()

        if (configuredLLMs.isEmpty()) {
            throw IllegalStateException("Nenhuma configuração de LLM encontrada.")
        }

        CoroutineScope(Dispatchers.IO).launch {
            val jobs = configuredLLMs.map { config ->
                launch {
                    execute(config.name, filePath, onResult)
                }
            }
            jobs.joinAll() // Aguarda todas as execuções paralelas finalizarem
        }
    }

    /**
     * Executa o script da LLM com os parâmetros configurados.
     * @param config Configuração da LLM
     * @param filePath Caminho do arquivo de entrada
     * @return Resultado da execução
     */
    private fun runProcess(config: LLMSettings.LLMConfiguration, filePath: String): String {
        return try {
            val commandList = mutableListOf<String>().apply {
                add(config.command) // Exemplo: "python"
                add(config.scriptFilePath) // Exemplo: "gpt_main.py"

                config.namedParameters.forEach { param ->
                    if (param.argName.isNotBlank()) {
                        if (param is LLMSettings.BooleanParam) {
                            if (param.value) add(param.argName)
                        } else {
                            add(param.argName)
                            val value = when (param) {
                                is LLMSettings.StringParam -> param.value
                                is LLMSettings.IntParam -> param.value.toString()
                                is LLMSettings.DoubleParam -> param.value.toString()
                                is LLMSettings.ListParam -> param.value
                                else -> ""
                            }
                            if (value.isNotBlank()) add(value)
                        }
                    }
                }

                add("--user_story_path")
                add(filePath)
            }

            val outputDir = config.namedParameters.find { it.argName == "--output_dir_path" }
                ?.let { (it as? LLMSettings.StringParam)?.value } ?: "."

            val process = ProcessBuilder(commandList)
                .directory(File(outputDir))
                .redirectErrorStream(true)
                .start()

            process.inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            "Erro ao executar o processo para LLM '${config.name}': ${e.message}"
        }
    }
}
