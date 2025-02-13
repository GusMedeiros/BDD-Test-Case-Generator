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
                            LLMSettings.BooleanParam(key, required, description, component.isSelected)
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

                // Atualiza a configuração no LLMSettings
                val updatedConfig = existingConfig.copy(namedParameters = updatedParams)
                llmSettings.updateConfiguration(existingConfig, updatedConfig)

                super.doOKAction()
            }
        }
        dialog.show()
    }
}
