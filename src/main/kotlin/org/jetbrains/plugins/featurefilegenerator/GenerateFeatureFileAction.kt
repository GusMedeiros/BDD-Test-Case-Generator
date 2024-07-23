package org.jetbrains.plugins.featurefilegenerator

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class GenerateFeatureFileAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val fileChooserDescriptor = FileChooserDescriptor(true, false, false, false, false, false)
        val virtualFile: VirtualFile? = FileChooser.chooseFile(fileChooserDescriptor, project, null)

        virtualFile?.let {
            val userStoryPath = it.path

            // Solicita ao usuário a chave
            val apiKey = Messages.showInputDialog(
                project,
                "Por favor, insira sua chave API:",
                "Chave API Necessária",
                Messages.getQuestionIcon()
            )

            if (apiKey.isNullOrEmpty()) {
                Messages.showMessageDialog(project, "Chave API não fornecida. A operação foi cancelada.", "Erro", Messages.getErrorIcon())
                return@let
            }

            val (success, feature_output) = runPythonScript(userStoryPath, apiKey)
            val message = if (success) {
                "Script python executado com sucesso. Output: $feature_output"
            } else {
                "Erro ao executar o script python. Output: $feature_output"
            }
            Messages.showMessageDialog(project, message, "Info", Messages.getInformationIcon())
        }
    }

    private fun runPythonScript(userStoryFilepath: String, apiKey: String): Pair<Boolean, String> {
        return try {
            // Carrega o recurso do script Python
            val resourceStream: InputStream? = this::class.java.getResourceAsStream("/python/Main.py")
            if (resourceStream == null) {
                val message = "Recurso não encontrado: /python/Main.py"
                println(message)
                return Pair(false, message)
            }
            // Cria um arquivo temporário
            val tempScript = File.createTempFile("Main", ".py")
            tempScript.deleteOnExit()

            // Copia o conteúdo do recurso para o arquivo temporário
            Files.copy(resourceStream, tempScript.toPath(), StandardCopyOption.REPLACE_EXISTING)

            // Carrega o recurso do arquivo txt
            val resourceStream2: InputStream? = this::class.java.getResourceAsStream("/python/message_1_response=user.txt")
            if (resourceStream2 == null) {
                val message = "Recurso não encontrado: /python/message_1_response=user.txt"
                println(message)
                return Pair(false, message)
            }

            // Cria um arquivo temporário
            val tempPrompt = File.createTempFile("prompt", ".txt")
            tempPrompt.deleteOnExit()

            // Copia o conteúdo do recurso para o arquivo temporário
            Files.copy(resourceStream2, tempPrompt.toPath(), StandardCopyOption.REPLACE_EXISTING)

            // Monta o comando para executar o script Python
            val processBuilder = ProcessBuilder("python", tempScript.absolutePath, tempPrompt.absolutePath, userStoryFilepath, apiKey)
            val process = processBuilder.start()

            // Captura a saída do processo
            val output = process.inputStream.bufferedReader().readText()
            val errorOutput = process.errorStream.bufferedReader().readText()

            // Aguarda o processo terminar e obtém o código de saída
            val exitCode = process.waitFor()
            val success = exitCode == 0

            val message = if (success) {
                output
            } else {
                errorOutput
            }
            Pair(success, message)
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(false, "Exceção ao executar o script Python: ${e.message}")
        }
    }
}
