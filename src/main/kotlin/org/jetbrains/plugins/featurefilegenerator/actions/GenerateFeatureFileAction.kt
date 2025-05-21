package org.jetbrains.plugins.featurefilegenerator.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.ui.Messages
import org.jetbrains.plugins.featurefilegenerator.LLMSettings
import org.jetbrains.plugins.featurefilegenerator.executor.LLMExecutor
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
                val executor = LLMExecutor(settings)

                executor.execute(selectedLLM, filePath) { llmName, result ->
                    ApplicationManager.getApplication().invokeLater {
                        Messages.showMessageDialog(
                            project,
                            result,
                            "Execution Result - $llmName",
                            Messages.getInformationIcon()
                        )
                    }
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
}
