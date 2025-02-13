package org.jetbrains.plugins.featurefilegenerator.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
        val config = settings.getConfigurationByName("gpt") // Nome da configuração

        if (config == null) {
            Messages.showErrorDialog("Configuração 'gpt' não encontrada.", "Erro de Configuração")
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            val result = withContext(Dispatchers.IO) {
                runProcess(config, filePath)
            }

            Messages.showMessageDialog(
                project,
                result,
                "Resultado da Execução",
                Messages.getInformationIcon()
            )
        }
    }

    private fun runProcess(config: LLMSettings.LLMConfiguration, filePath: String): String {
        return try {
            val commandList = mutableListOf<String>().apply {
                add(config.command) // Exemplo: "python"
                add(config.scriptFilePath) // Exemplo: "A:/Projetos A/Plugin-BDD-GPT/src/main/resources/python/gpt_main.py"

                val paramMap = config.namedParameters.associateBy { it.key }

                // Garantindo que os argumentos são passados como flags nomeadas corretamente
                add("--prompt_instruction_path")
                add(paramMap["Path do prompt de instrução"]?.let { (it as? LLMSettings.StringParam)?.value } ?: "")

                add("--user_story_path")
                add(filePath) // O arquivo selecionado

                add("--api_key")
                add(paramMap["Chave API"]?.let { (it as? LLMSettings.StringParam)?.value } ?: "")

                add("--output_dir_path")
                add(paramMap["Output dir"]?.let { (it as? LLMSettings.StringParam)?.value } ?: "")

                add("--temperature")
                add(paramMap["Temperatura"]?.let { (it as? LLMSettings.DoubleParam)?.value.toString() } ?: "0.7")

                add("--model")
                add(paramMap["Modelo"]?.let { (it as? LLMSettings.StringParam)?.value } ?: "gpt-3.5-turbo")

                if (paramMap.containsKey("Seed")) {
                    add("--seed")
                    add(paramMap["Seed"]?.let { (it as? LLMSettings.StringParam)?.value } ?: "")
                }

                if (paramMap.containsKey("Debug")) {
                    add("--debug")
                }
            }

            val process = ProcessBuilder(commandList)
                .directory(File(config.namedParameters.find { it.key == "Output dir" }?.let { (it as? LLMSettings.StringParam)?.value } ?: "."))
                .redirectErrorStream(true)
                .start()

            process.inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            "Erro ao executar o processo: ${e.message}"
        }
    }
}
