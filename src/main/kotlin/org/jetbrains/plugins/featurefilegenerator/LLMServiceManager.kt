package org.jetbrains.plugins.featurefilegenerator.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.plugins.featurefilegenerator.LLMService
import org.jetbrains.plugins.featurefilegenerator.UIPanel
import org.jetbrains.plugins.featurefilegenerator.UserSettings

class LLMServiceManager(private val project: Project, private val llmService: LLMService) {

    fun generate(event: AnActionEvent) {
        val dialog = UIPanel(project)
        val configurator = Configurator(project)

        configurator.showDialogAndConfigure(dialog, configurator)

        val errors = checkSettings()

        if (errors.isNotEmpty()) {
            val errorMessage = errors.joinToString(separator = "\n") { "- $it" }
            Messages.showMessageDialog(
                project,
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
            Messages.showMessageDialog(project, e.message, "Error", Messages.getErrorIcon())
            return
        }

        val settings = project.getService(UserSettings::class.java)
        val state = settings?.state ?: return

        // Execution in background
        CoroutineScope(Dispatchers.Main).launch {
            val (success, featureOutput) = withContext(Dispatchers.IO) {
                llmService.generateFeatureFile(userStoryPath, state)
            }
            val message = if (success) {
                "Feature file generated successfully."
            } else {
                "Error generating feature file. Output: $featureOutput"
            }
            Messages.showMessageDialog(project, message, "Info", Messages.getInformationIcon())
        }
    }

    private fun checkSettings(): List<String> {
        val settings = project.getService(UserSettings::class.java)
        val state = settings?.state

        val errors = mutableListOf<String>()

        if (state == null) {
            errors.add("Settings are not configured.")
            return errors
        }

        // Add generic validation if needed
        if (state.outputDirPath.isNullOrEmpty()) {
            errors.add("Output Directory Path is missing.")
        }

        return errors
    }

    private fun getUserStoryPath(event: AnActionEvent): String {
        val editorFile: VirtualFile? = event.getData(CommonDataKeys.VIRTUAL_FILE)
        val projectViewFile: VirtualFile? = event.getData(CommonDataKeys.VIRTUAL_FILE)
        val selectedFile = editorFile ?: projectViewFile

        if (selectedFile != null) {
            return selectedFile.path
        } else {
            throw IllegalArgumentException("No file was selected.")
        }
    }
}
