package org.jetbrains.plugins.featurefilegenerator.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.Messages
import org.jetbrains.plugins.featurefilegenerator.LLMSettings
import org.jetbrains.plugins.featurefilegenerator.executor.LLMExecutor

class BatchGenerateFeatureFileAction : AnAction() {

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val filePath = event.getData(com.intellij.openapi.actionSystem.CommonDataKeys.VIRTUAL_FILE)?.path
            ?: run {
                Messages.showErrorDialog("Could not retrieve the file path.", "Error")
                return
            }

        // Uses LLMSettings from the IntelliJ Plugin
        val llmSettings = LLMSettings.getInstance()
        val executor = LLMExecutor(llmSettings)

        executor.executeBatchAsync(filePath) { llmName, result ->
            ApplicationManager.getApplication().invokeLater {
                Messages.showMessageDialog(
                    project,
                    result,
                    "Execution Result ($llmName)",
                    Messages.getInformationIcon()
                )
            }
        }
    }

}
