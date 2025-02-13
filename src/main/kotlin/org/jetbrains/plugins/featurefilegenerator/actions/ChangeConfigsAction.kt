package org.jetbrains.plugins.featurefilegenerator.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBTextField
import org.jetbrains.plugins.featurefilegenerator.LLMConfigurationPanel
import org.jetbrains.plugins.featurefilegenerator.LLMSettings
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JOptionPane
import javax.swing.JSpinner

class ChangeConfigsAction : AnAction() {

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return

        // Cria e exibe o diálogo
        val dialog = object : DialogWrapper(project) {
            private val configurationPanel = LLMConfigurationPanel()
            private val llmSettings = LLMSettings.getInstance()

            init {
                title = "Configurações de LLM"
                init()
            }

            override fun createCenterPanel(): JComponent {
                return configurationPanel
            }

            override fun doOKAction() {
                val selectedConfigName = configurationPanel.configurationComboBox.selectedItem as? String ?: return
                val existingConfig = llmSettings.getConfigurationByName(selectedConfigName)

                if (existingConfig == null) {
                    JOptionPane.showMessageDialog(null, "Configuração não encontrada!", "Erro", JOptionPane.ERROR_MESSAGE)
                    return
                }

                // **1. Recuperar os parâmetros da UI**
                val updatedParams = configurationPanel.parameterFieldMap.mapNotNull { (key, component) ->
                    val paramSpec = configurationPanel.loadParameterSpecifications(existingConfig.parameterSpecFilePath)
                        .find { it["name"]?.toString() == key } ?: return@mapNotNull null

                    val required = paramSpec["required"] as? Boolean ?: false
                    val description = paramSpec["description"]?.toString() ?: ""

                    when {
                        component is JBTextField -> {
                            LLMSettings.StringParam(key, required, description, component.text)
                        }
                        component is JCheckBox -> {
                            val booleanValue = component.isSelected
                            LLMSettings.BooleanParam(key, required, description, booleanValue)
                        }
                        component is ComboBox<*> -> {
                            val selectedValue = component.selectedItem?.toString() ?: ""
                            val allowedValues = (paramSpec["allowed_values"] as? List<*>)?.map { it.toString() } ?: emptyList()
                            LLMSettings.ListParam(key, required, description, selectedValue, allowedValues)
                        }
                        component is JSpinner -> {
                            val value = (component.value as Number).toDouble()
                            LLMSettings.DoubleParam(key, required, description, value)
                        }
                        else -> null
                    }
                }.toMutableList()

                // **2. Recuperar o campo `command` da interface**
                val commandField = configurationPanel.parameterFieldMap["Comando para o Console:"] as? JBTextField
                val updatedCommand = commandField?.text ?: existingConfig.command

                // **Correção do Problema com Booleanos**
                val existingBooleanParams = existingConfig.namedParameters.filterIsInstance<LLMSettings.BooleanParam>()
                for (param in existingBooleanParams) {
                    if (updatedParams.none { it.key == param.key }) {
                        updatedParams.add(
                            LLMSettings.BooleanParam(param.key, param.required, param.description, false)
                        ) // Mantemos o valor como `false` se não foi alterado na UI
                    }
                }

                // **3. Atualizar a configuração com o campo `command`**
                val updatedConfig = LLMSettings.LLMConfiguration(
                    name = existingConfig.name,
                    scriptFilePath = existingConfig.scriptFilePath,
                    parameterSpecFilePath = existingConfig.parameterSpecFilePath,
                    command = updatedCommand, // Adicionando o comando atualizado
                    namedParameters = updatedParams
                )

                // **4. Salvar a configuração**
                llmSettings.updateConfiguration(existingConfig, updatedConfig)

                super.doOKAction()
            }
        }
        dialog.show()
    }
}
