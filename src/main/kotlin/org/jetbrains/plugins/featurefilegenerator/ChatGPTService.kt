import org.jetbrains.plugins.featurefilegenerator.LLMService
import org.jetbrains.plugins.featurefilegenerator.UserSettings
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.*
import kotlin.math.absoluteValue
import kotlin.random.Random

class ChatGPTService : LLMService {

    override fun generateFeatureFile(userStoryPath: String, settings: UserSettings.State): Pair<Boolean, String> {
        val apiKey = settings.apiKey
        val outputDirPath = settings.outputDirPath
        val temperature = settings.temperature
        val fixedSeed = settings.fixedSeed
        val seed = if (fixedSeed == true) {
            requireNotNull(settings.seed) { "Seed value cannot be null when FixedSeed is true." }
            settings.seed
        } else {
            Random.nextInt().absoluteValue
        }
        val gptModel = settings.gptModel
        val debug = settings.debug

        requireNotNull(apiKey) { "API Key cannot be null" }
        requireNotNull(outputDirPath) { "Output Directory Path cannot be null" }
        requireNotNull(temperature) { "Temperature cannot be null" }
        requireNotNull(seed) { "Seed cannot be null" }
        requireNotNull(fixedSeed) { "FixedSeed cannot be null" }
        requireNotNull(gptModel) { "GPT Model cannot be null" }
        requireNotNull(debug) { "Debug cannot be null" }

        return try {
            installRequirements()
            val (success, featureOutput) = runPythonScript(
                userStoryFilepath = userStoryPath,
                apiKey = apiKey,
                outputDirPath = outputDirPath,
                temperature = temperature.toString(),
                seed = seed.toString(),
                debug = debug.toString(),
                gptModel = gptModel
            )
            Pair(success, featureOutput)
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(false, "Exception occurred: ${e.message}")
        }
    }

    private fun installRequirements() {
        val resourcePath = "python/requirements.txt"
        val inputStream: InputStream = this::class.java.classLoader.getResourceAsStream(resourcePath)
            ?: throw IllegalArgumentException("File $resourcePath not found.")

        val tempFile = Files.createTempFile(null, ".txt").toFile()
        tempFile.deleteOnExit()

        inputStream.use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        val command = listOf("pip", "install", "-r", tempFile.absolutePath)

        val process = ProcessBuilder(command)
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()

        val exitCode = process.waitFor()
        if (exitCode == 0) {
            println("Packages installed successfully.")
        } else {
            println("Error installing packages. Exit code: $exitCode")
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
                ?: return Pair(false, "Resource not found: /python/Main.py")

            val tempScript = File.createTempFile("Main", ".py").apply { deleteOnExit() }
            Files.copy(resourceStream, tempScript.toPath(), StandardCopyOption.REPLACE_EXISTING)

            val resourceStream2 = this::class.java.getResourceAsStream("/python/message_1_response=user.txt")
                ?: return Pair(false, "Resource not found: /python/message_1_response=user.txt")

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
            Pair(false, "Exception while executing Python script: ${e.message}")
        }
    }
}
