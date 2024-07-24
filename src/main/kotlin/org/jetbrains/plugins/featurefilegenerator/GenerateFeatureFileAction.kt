package org.jetbrains.plugins.featurefilegenerator

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.*

class GenerateFeatureFileAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return

        val dialog = UIPanel(project)
        if (dialog.showAndGet()) {
            val apiKey = dialog.apiKeyField.text
            val userStoryPath = dialog.userStoryPathField.text
            val outputDirPath = dialog.outputDirPathField.text
            val temperature = dialog.temperatureSpinner.value.toString()
            val seed = dialog.seedSpinner.value.toString()
            val debug = dialog.debugCheckBox.isSelected.toString()
            val gptModel = dialog.gptModelComboBox.selectedItem.toString()

            if (apiKey.isEmpty() || userStoryPath.isEmpty() || outputDirPath.isEmpty()) {
                Messages.showMessageDialog(
                    project,
                    "Todos os campos são obrigatórios. A operação foi cancelada.",
                    "Erro",
                    Messages.getErrorIcon()
                )
                return
            }

            CoroutineScope(Dispatchers.Main).launch {
                installRequirements()
                val (success, featureOutput) = withContext(Dispatchers.IO) {
                    runPythonScript(userStoryPath, apiKey, outputDirPath, temperature, seed, debug, gptModel)
                }
                val message = if (success) {
                    "Script python executado com sucesso."
                } else {
                    "Erro ao executar o script python. Output: $featureOutput"
                }
                Messages.showMessageDialog(project, message, "Info", Messages.getInformationIcon())
            }
        }
    }

    private fun runPythonScript(
        userStoryFilepath: String,
        apiKey: String,
        outputDirPath: String,
        temperature: String,
        seed: String,
        debug: String,
        gptModel: String
    ): Pair<Boolean, String> {
        return try {
            val resourceStream = this::class.java.getResourceAsStream("/python/Main.py")
                ?: return Pair(false, "Recurso não encontrado: /python/Main.py")

            val tempScript = File.createTempFile("Main", ".py").apply { deleteOnExit() }
            Files.copy(resourceStream, tempScript.toPath(), StandardCopyOption.REPLACE_EXISTING)

            val resourceStream2 = this::class.java.getResourceAsStream("/python/message_1_response=user.txt")
                ?: return Pair(false, "Recurso não encontrado: /python/message_1_response=user.txt")

            val tempPrompt = File.createTempFile("prompt", ".txt").apply { deleteOnExit() }
            Files.copy(resourceStream2, tempPrompt.toPath(), StandardCopyOption.REPLACE_EXISTING)

            val os = System.getProperty("os.name").lowercase(Locale.getDefault())
            val pythonCommand = if (os.contains("win")) "python" else "python3"

            val processBuilder = ProcessBuilder(
                pythonCommand,
                tempScript.absolutePath,
                tempPrompt.absolutePath,
                userStoryFilepath,
                apiKey,
                outputDirPath,
                temperature,
                seed,
                debug,
                gptModel
            )
            val process = processBuilder.start()

            val output = process.inputStream.bufferedReader().readText()
            val errorOutput = process.errorStream.bufferedReader().readText()

            val exitCode = process.waitFor()
            val success = exitCode == 0

            Pair(success, if (success) output else errorOutput)
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(false, "Exceção ao executar o script Python: ${e.message}")
        }
    }

    fun installRequirements() {
        // Carregar o arquivo requirements.txt do resources
        val resourcePath = "resources/python/requirements.txt"
        val inputStream: InputStream = this::class.java.classLoader.getResourceAsStream(resourcePath)
            ?: throw IllegalArgumentException("Arquivo $resourcePath não encontrado.")

        // Criar um arquivo temporário para escrever o conteúdo de requirements.txt
        val tempFile = Files.createTempFile(null, ".txt").toFile()
        tempFile.deleteOnExit() // Garantir que o arquivo temporário será deletado após o uso

        // Escrever o conteúdo do InputStream no arquivo temporário
        inputStream.use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        // Construir o comando pip install
        val command = "pip install -r ${tempFile.absolutePath}"

        // Executar o comando
        val process = ProcessBuilder(command.split(" "))
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()

        val exitCode = process.waitFor()
        if (exitCode == 0) {
            println("Pacotes instalados com sucesso.")
        } else {
            println("Erro ao instalar os pacotes. Código de saída: $exitCode")
        }
    }
}
