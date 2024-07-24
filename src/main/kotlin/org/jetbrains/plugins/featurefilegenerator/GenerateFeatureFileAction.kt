package org.jetbrains.plugins.featurefilegenerator

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

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

            val (success, featureOutput) = runPythonScript(userStoryPath, apiKey, outputDirPath, temperature, seed, debug, gptModel)
            val message = if (success) {
                "Script python executado com sucesso."
            } else {
                "Erro ao executar o script python. Output: $featureOutput"
            }
            Messages.showMessageDialog(project, message, "Info", Messages.getInformationIcon())
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

            val processBuilder = ProcessBuilder(
                "python",
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
}
