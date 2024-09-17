package org.jetbrains.plugins.featurefilegenerator

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.ui.Messages

class ChangeConfigsAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return

        val dialog = UIPanel(project)
        if (dialog.showAndGet()) {
            val apiKey = dialog.apiKeyField.text
            val outputDirPath = dialog.outputDirPathField.text
            val temperature = (dialog.temperatureSpinner.value as Number).toDouble()
            val seed = (dialog.seedSpinner.value as Number).toInt()
            val debug = dialog.debugCheckBox.isSelected
            val gptModel = dialog.gptModelComboBox.selectedItem?.toString() ?: ""

            // Verificar se os campos obrigatórios estão preenchidos
            if (apiKey.isEmpty() || outputDirPath.isEmpty()) {
                Messages.showMessageDialog(
                    project,
                    "Todos os campos são obrigatórios. A operação foi cancelada.",
                    "Erro",
                    Messages.getErrorIcon()
                )
                return
            }

            // Obter a instância de UserSettings
            val userSettings = project.service<UserSettings>()

            // Atribuir os valores às configurações persistentes
            userSettings.setApiKey(apiKey)
            userSettings.setOutputDirPath(outputDirPath)
            userSettings.setTemperature(temperature)
            userSettings.setSeed(seed)
            userSettings.setDebug(debug)
            userSettings.setGptModel(gptModel)
        }
    }
}
