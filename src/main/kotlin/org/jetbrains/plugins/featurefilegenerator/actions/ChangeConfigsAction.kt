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
            private val configurationPanel = LLMConfigurationPanel()
            private val llmSettings = LLMSettings.getInstance()
            private val deleteButton = JButton("Excluir Configuração")

            init {
                title = "Configurações de LLM"
                init()
            }

            override fun createCenterPanel(): JComponent {
                val panel = JPanel(BorderLayout())
                panel.add(configurationPanel, BorderLayout.CENTER)

                // Painel para segurar o botão de exclusão abaixo do ComboBox
                val bottomPanel = JPanel()
                bottomPanel.add(deleteButton)

                panel.add(bottomPanel, BorderLayout.SOUTH)

                // Configurar ação do botão de exclusão
                deleteButton.addActionListener {
                    deleteSelectedConfiguration()
                }

                return panel
            }

            override fun doOKAction() {
                val selectedConfigName = configurationPanel.configurationComboBox.selectedItem as? String ?: return
                val existingConfig = llmSettings.getConfigurationByName(selectedConfigName)

                if (existingConfig == null) {
                    JOptionPane.showMessageDialog(null, "Configuração não encontrada!", "Erro", JOptionPane.ERROR_MESSAGE)
                    return
                }

                val updatedParams = mutableListOf<LLMSettings.NamedParameter>()

                for ((key, component) in configurationPanel.parameterFieldMap) {
                    val paramSpec = configurationPanel.loadParameterSpecifications(existingConfig.parameterSpecFilePath)
                        .find { it["name"]?.toString() == key } ?: continue

                    val required = paramSpec["required"] as? Boolean ?: false
                    val description = paramSpec["description"]?.toString() ?: ""

                    val param: LLMSettings.NamedParameter? = when {
                        component is JBTextField -> {
                            val textValue = component.text.trim()
                            if (required && textValue.isEmpty()) {
                                showError("O campo '$key' é obrigatório e não pode estar vazio.")
                                return
                            }
                            LLMSettings.StringParam(key, required, description, textValue)
                        }
                        component is JCheckBox -> {
                            LLMSettings.BooleanParam(key, required, description, component.isSelected)
                        }
                        component is ComboBox<*> -> {
                            val selectedValue = component.selectedItem?.toString() ?: ""
                            if (required && selectedValue.isEmpty()) {
                                showError("O campo '$key' é obrigatório e deve ter um valor selecionado.")
                                return
                            }
                            val allowedValues = (paramSpec["allowed_values"] as? List<*>)?.map { it.toString() } ?: emptyList()
                            LLMSettings.ListParam(key, required, description, selectedValue, allowedValues)
                        }
                        component is JSpinner -> {
                            val value = (component.value as Number).toDouble()
                            LLMSettings.DoubleParam(key, required, description, value)
                        }
                        else -> null
                    }

                    param?.let { updatedParams.add(it) }
                }

                // **Recuperar o campo `command` da interface**
                val commandField = configurationPanel.parameterFieldMap["Comando para o Console:"] as? JBTextField
                val updatedCommand = commandField?.text?.trim() ?: existingConfig.command
                if (updatedCommand.isEmpty()) {
                    showError("O campo 'Comando para o Console' é obrigatório e não pode estar vazio.")
                    return
                }

                // **Correção do Problema com Booleanos**
                val existingBooleanParams = existingConfig.namedParameters.filterIsInstance<LLMSettings.BooleanParam>()
                for (param in existingBooleanParams) {
                    if (updatedParams.none { it.key == param.key }) {
                        updatedParams.add(
                            LLMSettings.BooleanParam(param.key, param.required, param.description, false)
                        )
                    }
                }

                // **Criar e salvar a configuração**
                val updatedConfig = LLMSettings.LLMConfiguration(
                    name = existingConfig.name,
                    scriptFilePath = existingConfig.scriptFilePath,
                    parameterSpecFilePath = existingConfig.parameterSpecFilePath,
                    command = updatedCommand,
                    namedParameters = updatedParams
                )

                llmSettings.updateConfiguration(existingConfig, updatedConfig)

                super.doOKAction()
            }


            private fun deleteSelectedConfiguration() {
                val selectedConfigName = configurationPanel.configurationComboBox.selectedItem as? String ?: return
                val existingConfig = llmSettings.getConfigurationByName(selectedConfigName)

                if (existingConfig != null) {
                    val confirmation = JOptionPane.showConfirmDialog(
                        null,
                        "Tem certeza de que deseja excluir a configuração '$selectedConfigName'?",
                        "Confirmação",
                        JOptionPane.YES_NO_OPTION
                    )

                    if (confirmation == JOptionPane.YES_OPTION) {
                        llmSettings.removeConfiguration(existingConfig)
                        configurationPanel.configurationComboBox.removeItem(selectedConfigName)

                        JOptionPane.showMessageDialog(null, "Configuração '$selectedConfigName' excluída com sucesso!")
                    }
                } else {
                    JOptionPane.showMessageDialog(null, "Configuração não encontrada!", "Erro", JOptionPane.ERROR_MESSAGE)
                }
            }

            private fun showError(message: String) {
                JOptionPane.showMessageDialog(null, message, "Erro", JOptionPane.ERROR_MESSAGE)
            }
        }
        dialog.show()
    }
}
