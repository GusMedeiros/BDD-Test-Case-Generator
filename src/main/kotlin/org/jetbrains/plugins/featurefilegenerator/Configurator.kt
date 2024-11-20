package org.jetbrains.plugins.featurefilegenerator.actions

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import org.jetbrains.plugins.featurefilegenerator.UIPanel
import org.jetbrains.plugins.featurefilegenerator.UserSettings

class Configurator(private val project: Project) {

    fun showDialogAndConfigure(dialog: UIPanel, configurator: Configurator): Boolean {
        val closedWithOk = dialog.showAndGet()
        if(closedWithOk) {
            configurator.configure(dialog)
            return true
        }
        return false
    }

    fun configure(dialog: UIPanel): Boolean {
        // Coleta os valores do diálogo
        val apiKey = dialog.apiKeyField.text
        val outputDirPath = dialog.outputDirPathField.text
        val temperature = (dialog.temperatureSpinner.value as Number).toDouble()
        val fixedSeed = dialog.fixedSeedCheckBox.isSelected
        val seed = (dialog.seedSpinner.value as Number).toInt()
        val debug = dialog.debugCheckBox.isSelected
        val gptModel = dialog.gptModelComboBox.selectedItem?.toString() ?: ""

        // Valida os campos obrigatórios
        if (!validateInputs(apiKey, outputDirPath)) {
            Messages.showMessageDialog(
                project,
                "Todos os campos são obrigatórios. A operação foi cancelada.",
                "Erro",
                Messages.getErrorIcon()
            )
            return false
        }

        // Persiste as configurações
        saveSettings(apiKey, outputDirPath, temperature, fixedSeed, seed, debug, gptModel)

        return true
    }

    private fun validateInputs(apiKey: String, outputDirPath: String): Boolean {
        return apiKey.isNotEmpty() && outputDirPath.isNotEmpty()
    }

    private fun saveSettings(
        apiKey: String,
        outputDirPath: String,
        temperature: Double,
        fixedSeed: Boolean,
        seed: Int,
        debug: Boolean,
        gptModel: String
    ) {
        val userSettings = project.service<UserSettings>()
        userSettings.setApiKey(apiKey)
        userSettings.setOutputDirPath(outputDirPath)
        userSettings.setTemperature(temperature)
        userSettings.setFixedSeed(fixedSeed)
        userSettings.setSeed(seed)
        userSettings.setDebug(debug)
        userSettings.setGptModel(gptModel)
    }
}
