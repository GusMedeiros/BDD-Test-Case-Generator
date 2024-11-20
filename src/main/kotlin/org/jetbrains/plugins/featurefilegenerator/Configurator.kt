package org.jetbrains.plugins.featurefilegenerator.actions

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import org.jetbrains.plugins.featurefilegenerator.UIPanel
import org.jetbrains.plugins.featurefilegenerator.UserSettings

class Configurator(private val project: Project) {

    fun showDialogAndConfigure(dialog: UIPanel, configurator: Configurator): Boolean {
        val userSettings = project.service<UserSettings>()
        dialog.apply {
            setApiKey(userSettings.getApiKey())
            setOutputDirPath(userSettings.getOutputDirPath())
            setTemperature(userSettings.getTemperature())
            setFixedSeed(userSettings.getFixedSeed())
            setSeed(userSettings.getSeed())
            setDebug(userSettings.getDebug())
            setGptModel(userSettings.getGptModel())
        }
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
        if (!validateInputs(apiKey, outputDirPath, temperature, fixedSeed, seed, debug, gptModel)) {
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

    private fun validateInputs(
        apiKey: String?,
        outputDirPath: String?,
        temperature: Double?,
        fixedSeed: Boolean?,
        seed: Int?,
        debug: Boolean?,
        gptModel: String?
    ): Boolean {
        try {
            // Verificar se algum valor está indefinido (nulo)
            if (apiKey == null || outputDirPath == null || temperature == null ||
                fixedSeed == null || seed == null || debug == null || gptModel == null
            ) {
                println("Erro: Um ou mais valores estão nulos:")
                println("apiKey=$apiKey, outputDirPath=$outputDirPath, temperature=$temperature, fixedSeed=$fixedSeed, seed=$seed, debug=$debug, gptModel=$gptModel")
                return false
            }

            // Validação de campos obrigatórios
            if (apiKey.isEmpty() || outputDirPath.isEmpty() || gptModel.isEmpty()) {
                println("Erro: Um ou mais campos obrigatórios estão vazios:")
                println("apiKey=$apiKey, outputDirPath=$outputDirPath, gptModel=$gptModel")
                return false
            }

            // Validação da temperatura
            if (temperature !in 0.0..1.0) {
                println("Erro: Temperatura fora do intervalo permitido (0.0 a 1.0): temperature=$temperature")
                return false
            }

            // Validação do seed com base em fixedSeed
            if (fixedSeed) {
                if (seed < 0) {
                    println("Erro: Seed inválido. Deve ser não-negativo quando fixedSeed está habilitado. seed=$seed")
                    throw IllegalArgumentException("Seed deve ser um valor não-negativo quando fixedSeed está habilitado.")
                }
            }

            // Todos os valores são válidos
            println("Validação bem-sucedida: Todos os valores são válidos.")
            return true
        } catch (e: Exception) {
            // Captura exceções de validação e retorna false
            println("Exceção capturada durante a validação: ${e.message}")
            return false
        }
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
