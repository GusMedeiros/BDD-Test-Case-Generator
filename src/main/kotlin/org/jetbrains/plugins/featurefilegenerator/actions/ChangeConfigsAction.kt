package org.jetbrains.plugins.featurefilegenerator.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBTextField
import org.jetbrains.plugins.featurefilegenerator.LLMConfigurationPanel
import org.jetbrains.plugins.featurefilegenerator.LLMSettings
import java.awt.BorderLayout
import javax.swing.*

class ChangeConfigsAction : AnAction() {

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return

        val dialog = object : DialogWrapper(project) {
            private val configurationPanel = LLMConfigurationPanel(project)
            private val llmSettings = LLMSettings.getInstance()
            private val deleteButton = JButton("Delete Configuration")

            init {
                title = "LLM Settings"
                init()
            }

            override fun createCenterPanel(): JComponent {
                val panel = JPanel(BorderLayout())
                panel.add(configurationPanel, BorderLayout.CENTER)

                val bottomPanel = JPanel()
                bottomPanel.add(deleteButton)
                panel.add(bottomPanel, BorderLayout.SOUTH)

                deleteButton.addActionListener {
                    deleteSelectedConfiguration()
                }

                return panel
            }

            override fun doOKAction() {
                val selectedConfigName = configurationPanel.configurationComboBox.selectedItem as? String ?: return
                val existingConfig = llmSettings.getConfigurationByName(selectedConfigName)

                if (existingConfig == null) {
                    JOptionPane.showMessageDialog(null, "Configuration not found!", "Error", JOptionPane.ERROR_MESSAGE)
                    return
                }

                val updatedParams = mutableListOf<LLMSettings.NamedParameter>()

                for ((key, component) in configurationPanel.parameterFieldMap) {
                    val paramSpec = configurationPanel.loadParameterSpecifications(existingConfig.parameterSpecFilePath)
                        .find { it["name"]?.toString() == key } ?: continue

                    val required = paramSpec["required"] as? Boolean ?: false
                    val description = paramSpec["description"]?.toString() ?: ""
                    val argName = paramSpec["argName"]?.toString() ?: "--${key.replace(" ", "_").lowercase()}"

                    val param: LLMSettings.NamedParameter? = when {
                        component is JBTextField -> {
                            val textValue = component.text.trim()
                            if (required && textValue.isEmpty()) {
                                showError("The field '$key' is required and cannot be empty.")
                                return
                            }
                            LLMSettings.StringParam(key, argName, required, description, textValue)
                        }
                        component is JCheckBox -> {
                            LLMSettings.BooleanParam(key, argName, required, description, component.isSelected)
                        }
                        component is ComboBox<*> -> {
                            val selectedValue = component.selectedItem?.toString() ?: ""
                            if (required && selectedValue.isEmpty()) {
                                showError("The field '$key' is required and must have a selected value.")
                                return
                            }
                            val allowedValues = (paramSpec["allowed_values"] as? List<*>)?.map { it.toString() } ?: emptyList()
                            LLMSettings.ListParam(key, argName, required, description, selectedValue, allowedValues)
                        }
                        component is JSpinner -> {
                            val value = (component.value as Number).toDouble()
                            LLMSettings.DoubleParam(key, argName, required, description, value)
                        }
                        else -> null
                    }

                    param?.let { updatedParams.add(it) }
                }

                // ✅ Extract script file, config file, and command
                val scriptField = configurationPanel.parameterFieldMap["Select Script File:"] as? JBTextField
                val configField = configurationPanel.parameterFieldMap["Select Configuration File:"] as? JBTextField
                val commandField = configurationPanel.parameterFieldMap["Console Command:"] as? JBTextField

                val updatedScriptPath = scriptField?.text?.trim() ?: existingConfig.scriptFilePath
                val updatedConfigPath = configField?.text?.trim() ?: existingConfig.parameterSpecFilePath
                val updatedCommand = commandField?.text?.trim() ?: existingConfig.command

                if (updatedScriptPath.isEmpty()) {
                    showError("The field 'Select Script File' is required and cannot be empty.")
                    return
                }

                if (updatedConfigPath.isEmpty()) {
                    showError("The field 'Select Configuration File' is required and cannot be empty.")
                    return
                }

                if (updatedCommand.isEmpty()) {
                    showError("The field 'Console Command' is required and cannot be empty.")
                    return
                }

                // ✅ Preserve unchecked booleans
                val existingBooleanParams = existingConfig.namedParameters.filterIsInstance<LLMSettings.BooleanParam>()
                for (param in existingBooleanParams) {
                    if (updatedParams.none { it.key == param.key }) {
                        updatedParams.add(
                            LLMSettings.BooleanParam(param.key, param.argName, param.required, param.description, false)
                        )
                    }
                }

                // ✅ Build and save updated config
                val updatedConfig = LLMSettings.LLMConfiguration(
                    name = existingConfig.name,
                    scriptFilePath = updatedScriptPath,
                    parameterSpecFilePath = updatedConfigPath,
                    command = updatedCommand,
                    namedParameters = updatedParams
                )

                llmSettings.updateConfiguration(existingConfig, updatedConfig)
                llmSettings.setSelectedLLM(selectedConfigName)

                super.doOKAction()
            }


            private fun deleteSelectedConfiguration() {
                val selectedConfigName = configurationPanel.configurationComboBox.selectedItem as? String ?: return
                val existingConfig = llmSettings.getConfigurationByName(selectedConfigName)

                if (existingConfig != null) {
                    val confirmation = JOptionPane.showConfirmDialog(
                        null,
                        "Are you sure you want to delete the configuration '$selectedConfigName'?",
                        "Confirmation",
                        JOptionPane.YES_NO_OPTION
                    )

                    if (confirmation == JOptionPane.YES_OPTION) {
                        llmSettings.removeConfiguration(existingConfig)
                        configurationPanel.configurationComboBox.removeItem(selectedConfigName)

                        JOptionPane.showMessageDialog(null, "Configuration '$selectedConfigName' deleted successfully!")
                    }
                } else {
                    JOptionPane.showMessageDialog(null, "Configuration not found!", "Error", JOptionPane.ERROR_MESSAGE)
                }
            }

            private fun showError(message: String) {
                JOptionPane.showMessageDialog(null, message, "Error", JOptionPane.ERROR_MESSAGE)
            }
        }
        dialog.show()
    }
}
