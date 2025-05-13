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
                Messages.showErrorDialog("Could not retrieve the file path.", "Error")
                return
            }

        val settings = LLMSettings.getInstance()

        error_check(settings)

        // Already asserted in error_check
        val selectedLLM = settings.getSelectedLLM()!!
        val config = settings.getConfigurationByName(selectedLLM)!!

        // Start the execution of the process with a progress indicator
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Generating .feature File", true) {
            override fun run(indicator: ProgressIndicator) {
                indicator.text = "Running the LLM script..."
                indicator.isIndeterminate = true // Indicates unknown duration

                val result = runProcess(config, filePath)

                // Display the result to the user
                ApplicationManager.getApplication().invokeLater {
                    Messages.showMessageDialog(
                        project,
                        result,
                        "Execution Result",
                        Messages.getInformationIcon()
                    )
                }
            }
        })
    }

    private fun error_check(settings: LLMSettings) {
        val selectedLLM = settings.getSelectedLLM()

        if (selectedLLM.isNullOrBlank()) {
            Messages.showErrorDialog(
                "No LLM has been selected. Please configure an LLM before continuing.",
                "Configuration Error"
            )
            return
        }

        val config = settings.getConfigurationByName(selectedLLM)

        if (config == null) {
            Messages.showErrorDialog("Configuration '$selectedLLM' not found.", "Configuration Error")
            return
        }
    }

    private fun runProcess(config: LLMSettings.LLMConfiguration, filePath: String): String {
        return try {
            val commandList = mutableListOf<String>().apply {
                add(config.command) // Example: "python"
                add(config.scriptFilePath) // Example: "gpt_main.py" or "gemini_main.py"

                val paramMap = config.namedParameters.associateBy { it.argName }

                // Add all parameters defined in the configuration, ensuring argName usage
                config.namedParameters.forEach { param ->
                    if (param.argName.isNotBlank()) { // Avoid unnamed arguments
                        if (param is LLMSettings.BooleanParam) {
                            // For booleans, only add the flag if it is "true"
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
                            if (value.isNotBlank()) add(value) // Add the value only if valid
                        }
                    }
                }

                // Dynamically add the user story file path
                add("--user_story_path")
                add(filePath)
            }

            // Get output directory, if it exists
            val outputDir = config.namedParameters.find { it.argName == "--output_dir_path" }
                ?.let { (it as? LLMSettings.StringParam)?.value } ?: "."

            // Execute the process
            val process = ProcessBuilder(commandList)
                .directory(File(outputDir))
                .redirectErrorStream(true)
                .start()

            process.inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            "Error executing the process: ${e.message}"
        }
    }
}
