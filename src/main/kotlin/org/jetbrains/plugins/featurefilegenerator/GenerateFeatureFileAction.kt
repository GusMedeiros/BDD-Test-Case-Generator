package org.jetbrains.plugins.featurefilegenerator

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
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

        val errors = checkSettings(project)
        if (errors.isNotEmpty()) {
            // Juntar todas as mensagens de erro em uma string
            val errorMessage = errors.joinToString(separator = "\n") {
                "- $it"
            }

            // Exibir a mensagem de erro com todos os detalhes
            Messages.showMessageDialog(
                event.project,
                "Settings are invalid:\n\n$errorMessage",
                "Error",
                Messages.getErrorIcon()
            )
            return
        }
        val userStoryPath: String
        try {
            userStoryPath = getUserStoryPath(event)
        } catch (e: IllegalArgumentException) {
            Messages.showMessageDialog(event.project, e.message, "Error", Messages.getErrorIcon())
            return
        }

        val settings = project.getService(UserSettings::class.java)
        val state = settings?.state
        val apiKey = state?.apiKey
        val outputDirPath = state?.outputDirPath
        val temperature = state?.temperature
        val seed = state?.seed
        val gptModel = state?.gptModel
        val debug = state?.debug

        requireNotNull(apiKey) { "API Key cannot be null" }
        requireNotNull(outputDirPath) { "Output Directory Path cannot be null" }
        requireNotNull(temperature) { "Temperature cannot be null" }
        requireNotNull(seed) { "Seed cannot be null" }
        requireNotNull(gptModel) { "GPT Model cannot be null" }
        requireNotNull(debug) { "Debug cannot be null" }


        // Execução do script Python em background
        CoroutineScope(Dispatchers.Main).launch {
            installRequirements()  // Se necessário instalar dependências
            val (success, featureOutput) = withContext(Dispatchers.IO) {
                runPythonScript(userStoryPath, apiKey, outputDirPath, temperature.toString(),
                    seed.toString(), debug.toString(), gptModel)
            }
            val message = if (success) {
                "Script python executado com sucesso."
            } else {
                "Erro ao executar o script python. Output: $featureOutput"
            }
            Messages.showMessageDialog(project, message, "Info", Messages.getInformationIcon())
        }
    }
    fun checkSettings(project: Project): List<String> {
        val settings = project.getService(UserSettings::class.java)
        val state = settings?.state

        val errors = mutableListOf<String>()

        if (state?.apiKey.isNullOrEmpty()) {
            errors.add("API Key is missing or invalid.")
        }

        if (state?.outputDirPath.isNullOrEmpty()) {
            errors.add("Output Directory Path is missing.")
        }

        if (state?.temperature == null || state.temperature !in 0.0..2.0) {
            errors.add("Temperature is out of range (must be between 0.0 and 2.0).")
        }

        if (state?.seed == null) {
            errors.add("Seed value is missing.")
        }

        if (state?.gptModel.isNullOrEmpty()) {
            errors.add("GPT Model is missing or invalid.")
        }

        return errors
    }

    private fun getUserStoryPath(event: AnActionEvent): String {
        // Tenta obter o arquivo aberto no editor
        val editorFile: VirtualFile? = event.getData(CommonDataKeys.VIRTUAL_FILE)

        // Se o editorFile for nulo, tenta obter o arquivo selecionado no Project View
        val projectViewFile: VirtualFile? = event.getData(CommonDataKeys.VIRTUAL_FILE)

        // Prioriza o arquivo no editor, caso contrário, tenta o Project View
        val selectedFile = editorFile ?: projectViewFile

        if (selectedFile != null) {
            // Obter o caminho do arquivo
            return selectedFile.path
        } else {
            // Lança uma exceção se nenhum arquivo foi encontrado
            throw IllegalArgumentException("Nenhum arquivo foi selecionado.")
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
        val resourcePath = "python/requirements.txt"
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
