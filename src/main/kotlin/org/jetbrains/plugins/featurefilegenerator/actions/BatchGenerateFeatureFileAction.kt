package org.jetbrains.plugins.featurefilegenerator.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.ui.Messages
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import org.jetbrains.plugins.featurefilegenerator.LLMSettings
import java.io.File

class BatchGenerateFeatureFileAction : AnAction() {

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val filePath = event.getData(com.intellij.openapi.actionSystem.CommonDataKeys.VIRTUAL_FILE)?.path
            ?: run {
                Messages.showErrorDialog("Não foi possível obter o caminho do arquivo.", "Erro")
                return
            }

        val settings = LLMSettings.getInstance()
        val configuredLLMs = settings.getConfigurations()

        if (configuredLLMs.isEmpty()) {
            Messages.showErrorDialog("Nenhuma configuração de LLM encontrada.", "Erro de Configuração")
            return
        }

        // Lançar cada LLM em paralelo
        CoroutineScope(Dispatchers.IO).launch {
            val jobs = configuredLLMs.map { config ->
                launch {
                    ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Gerando Arquivo .feature (${config.name})", true) {
                        override fun run(indicator: ProgressIndicator) {
                            indicator.text = "Executando script da LLM: ${config.name}..."
                            indicator.isIndeterminate = true

                            val result = runProcess(config, filePath)

//                            // Executar na UI Thread usando invokeLater
//                            ApplicationManager.getApplication().invokeLater {
//                                Messages.showMessageDialog(
//                                    project,
//                                    result,
//                                    "Resultado da Execução (${config.name})",
//                                    Messages.getInformationIcon()
//                                )
//                            }
                        }
                    })
                }
            }

            // Aguarda todas as execuções paralelas finalizarem
            jobs.joinAll()
        }
    }

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
