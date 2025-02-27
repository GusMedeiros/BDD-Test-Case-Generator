package org.jetbrains.plugins.featurefilegenerator.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.ui.Messages
import org.jetbrains.plugins.featurefilegenerator.LLMSettings
import java.io.File

class GenerateFeatureFileAction : AnAction() {

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val filePath = event.getData(com.intellij.openapi.actionSystem.CommonDataKeys.VIRTUAL_FILE)?.path
            ?: run {
                Messages.showErrorDialog("Não foi possível obter o caminho do arquivo.", "Erro")
                return
            }

        val settings = LLMSettings.getInstance()

        error_check(settings)

        // Já foram asserted em error_check
        val selectedLLM = settings.getSelectedLLM()!!
        val config = settings.getConfigurationByName(selectedLLM)!!

        // Inicia a execução do processo com um indicador de progresso
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Gerando Arquivo .feature", true) {
            override fun run(indicator: ProgressIndicator) {
                indicator.text = "Executando script da LLM..."
                indicator.isIndeterminate = true // Indica que a duração é desconhecida

                val result = runProcess(config, filePath)

                // Exibe o resultado ao usuário
                ApplicationManager.getApplication().invokeLater {
                    Messages.showMessageDialog(
                        project,
                        result,
                        "Resultado da Execução",
                        Messages.getInformationIcon()
                    )
                }
            }
        })
    }

    private fun error_check(settings: LLMSettings, ) {
        val selectedLLM = settings.getSelectedLLM()

        if (selectedLLM.isNullOrBlank()) {
            Messages.showErrorDialog(
                "Nenhuma LLM foi selecionada. Configure uma LLM antes de continuar.",
                "Erro de Configuração"
            )
            return
        }

        val config = settings.getConfigurationByName(selectedLLM)

        if (config == null) {
            Messages.showErrorDialog("Configuração '$selectedLLM' não encontrada.", "Erro de Configuração")
            return
        }
    }
    private fun runProcess(config: LLMSettings.LLMConfiguration, filePath: String): String {
        return try {
            val commandList = mutableListOf<String>().apply {
                add(config.command) // Exemplo: "python"
                add(config.scriptFilePath) // Exemplo: "gpt_main.py" ou "gemini_main.py"

                val paramMap = config.namedParameters.associateBy { it.argName }

                // Adiciona todos os parâmetros definidos na configuração, garantindo o uso de argName
                config.namedParameters.forEach { param ->
                    if (param.argName.isNotBlank()) { // Evita argumentos sem nome correto
                        if (param is LLMSettings.BooleanParam) {
                            // Para booleanos, só adicionamos a flag se for "true"
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
                            if (value.isNotBlank()) add(value) // Adiciona o valor apenas se for válido
                        }
                    }
                }

                // Adiciona dinamicamente o caminho do arquivo da história do usuário
                add("--user_story_path")
                add(filePath)
            }

            // Obtém o diretório de saída, se existir
            val outputDir = config.namedParameters.find { it.argName == "--output_dir_path" }
                ?.let { (it as? LLMSettings.StringParam)?.value } ?: "."

            // Executa o processo
            val process = ProcessBuilder(commandList)
                .directory(File(outputDir))
                .redirectErrorStream(true)
                .start()

            process.inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            "Erro ao executar o processo: ${e.message}"
        }
    }
}
