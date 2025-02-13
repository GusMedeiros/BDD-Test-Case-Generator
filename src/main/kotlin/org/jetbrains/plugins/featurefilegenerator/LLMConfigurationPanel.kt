package org.jetbrains.plugins.featurefilegenerator

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.io.File
import javax.swing.*
import javax.swing.filechooser.FileNameExtensionFilter

class LLMConfigurationPanel : JPanel(BorderLayout()) {

    private val llmSettings = LLMSettings.getInstance()
    val configurationComboBox = ComboBox<String>()
    private val dynamicPanel = JPanel(GridBagLayout())
    private val addNewLabel = "Inserir novo"
    val parameterFieldMap = mutableMapOf<String, JComponent>()

    init {
        setupUI()
    }

    private fun setupUI() {
        val configurations = llmSettings.getConfigurations().map { it.name } + addNewLabel
        configurationComboBox.model = DefaultComboBoxModel(configurations.toTypedArray())
        configurationComboBox.addActionListener { onConfigurationSelected() }

        val topPanel = JPanel(GridBagLayout()).apply {
            val gbc = GridBagConstraints().apply {
                gridx = 0
                gridy = 0
                anchor = GridBagConstraints.WEST
                insets = Insets(5, 5, 5, 5)
            }
            add(JBLabel("Selecione ou insira uma nova LLM:"), gbc)

            gbc.gridx = 1
            gbc.fill = GridBagConstraints.HORIZONTAL
            gbc.weightx = 1.0
            add(configurationComboBox, gbc)
        }

        add(topPanel, BorderLayout.NORTH)
        add(JScrollPane(dynamicPanel), BorderLayout.CENTER)

        onConfigurationSelected()
    }

    private fun onConfigurationSelected() {
        dynamicPanel.removeAll()
        val selected = configurationComboBox.selectedItem as String
        if (selected == addNewLabel) {
            renderNewConfigurationFields()
        } else {
            val configuration = llmSettings.getConfigurationByName(selected)
            renderExistingConfigurationFields(configuration)
        }
        dynamicPanel.revalidate()
        dynamicPanel.repaint()
    }

    private fun renderNewConfigurationFields() {
        dynamicPanel.removeAll()
        parameterFieldMap.clear()

        val gbc = GridBagConstraints().apply {
            anchor = GridBagConstraints.WEST
            fill = GridBagConstraints.HORIZONTAL
            gridy = 0
            insets = Insets(5, 5, 5, 5)
        }

        fun addRow(label: String, component: JComponent) {
            gbc.gridx = 0
            gbc.weightx = 0.0
            dynamicPanel.add(JBLabel(label), gbc)

            gbc.gridx = 1
            gbc.weightx = 1.0
            dynamicPanel.add(component, gbc)
            gbc.gridy++

            parameterFieldMap[label] = component
        }

        val nameField = JBTextField()
        addRow("Nome da LLM:", nameField)

        val scriptField = JBTextField()
        val scriptButton = JButton("Procurar").apply {
            addActionListener {
                val fileChooser = JFileChooser()
                fileChooser.fileFilter = FileNameExtensionFilter("Arquivos de Script", "sh", "bat", "py")
                if (fileChooser.showOpenDialog(this@LLMConfigurationPanel) == JFileChooser.APPROVE_OPTION) {
                    scriptField.text = fileChooser.selectedFile.absolutePath
                }
            }
        }
        addRow("Selecionar Arquivo de Script:", createHorizontalPanel(scriptField, scriptButton))

        val configField = JBTextField()
        val configButton = JButton("Procurar").apply {
            addActionListener {
                val fileChooser = JFileChooser()
                fileChooser.fileFilter = FileNameExtensionFilter("Arquivos de Configuração", "json", "yaml", "xml")
                if (fileChooser.showOpenDialog(this@LLMConfigurationPanel) == JFileChooser.APPROVE_OPTION) {
                    configField.text = fileChooser.selectedFile.absolutePath
                    renderNewConfigurationFields()
                }
            }
        }
        addRow("Selecionar Arquivo de Configuração:", createHorizontalPanel(configField, configButton))

        val commandField = JBTextField()
        addRow("Comando para o Console:", commandField)

        if (configField.text.isNotBlank() && File(configField.text).exists()) {
            val specifications = loadParameterSpecifications(configField.text)
            for (spec in specifications) {
                val paramName = spec["name"]?.toString() ?: "Unnamed"
                val component = createUIComponentForParameter(spec, null)
                addRow(paramName, component)
            }
        }

        gbc.gridwidth = 2
        val saveButton = JButton("Salvar").apply {
            addActionListener {
                saveNewConfiguration(
                    nameField.text,
                    scriptField.text,
                    configField.text,
                    commandField.text
                )
            }
        }
        dynamicPanel.add(saveButton, gbc)

        dynamicPanel.revalidate()
        dynamicPanel.repaint()
    }

    private fun renderExistingConfigurationFields(existingConfig: LLMSettings.LLMConfiguration?) {
        dynamicPanel.removeAll()
        parameterFieldMap.clear()

        if (existingConfig == null) {
            dynamicPanel.add(JBLabel("Configuração não encontrada."), GridBagConstraints())
            return
        }

        val gbc = GridBagConstraints().apply {
            anchor = GridBagConstraints.WEST
            fill = GridBagConstraints.HORIZONTAL
            gridy = 0
            insets = Insets(5, 5, 5, 5)
        }

        fun addRow(label: String, component: JComponent) {
            gbc.gridx = 0
            gbc.weightx = 0.0
            dynamicPanel.add(JBLabel(label), gbc)

            gbc.gridx = 1
            gbc.weightx = 1.0
            dynamicPanel.add(component, gbc)
            gbc.gridy++

            parameterFieldMap[label] = component
        }

        // Preencher os campos principais com valores da configuração existente
        val nameField = JBTextField(existingConfig.name)
        addRow("Nome da LLM:", nameField)

        val scriptField = JBTextField(existingConfig.scriptFilePath)
        val scriptButton = JButton("Procurar").apply {
            addActionListener {
                val fileChooser = JFileChooser()
                fileChooser.fileFilter = FileNameExtensionFilter("Arquivos de Script", "sh", "bat", "py")
                if (fileChooser.showOpenDialog(this@LLMConfigurationPanel) == JFileChooser.APPROVE_OPTION) {
                    scriptField.text = fileChooser.selectedFile.absolutePath
                }
            }
        }
        addRow("Selecionar Arquivo de Script:", createHorizontalPanel(scriptField, scriptButton))

        val configField = JBTextField(existingConfig.parameterSpecFilePath)
        val configButton = JButton("Procurar").apply {
            addActionListener {
                val fileChooser = JFileChooser()
                fileChooser.fileFilter = FileNameExtensionFilter("Arquivos de Configuração", "json", "yaml", "xml")
                if (fileChooser.showOpenDialog(this@LLMConfigurationPanel) == JFileChooser.APPROVE_OPTION) {
                    configField.text = fileChooser.selectedFile.absolutePath
                    renderExistingConfigurationFields(existingConfig)
                }
            }
        }
        addRow("Selecionar Arquivo de Configuração:", createHorizontalPanel(configField, configButton))

        val commandField = JBTextField(existingConfig.command)
        addRow("Comando para o Console:", commandField)

        // Preencher os campos de parâmetros da configuração
        val configPath = existingConfig.parameterSpecFilePath
        if (configPath.isNotBlank() && File(configPath).exists()) {
            val specifications = loadParameterSpecifications(configPath)

            for (spec in specifications) {
                val paramName = spec["name"]?.toString() ?: "Unnamed"

                // Buscar o valor existente para esse parâmetro
                val paramValue = existingConfig.namedParameters.find { it.key == paramName }

                // Passar o valor salvo na configuração para a UI
                val component = createUIComponentForParameter(spec, paramValue)
                addRow(paramName, component)
            }
        } else {
            dynamicPanel.add(JBLabel("Arquivo de configuração inválido ou inexistente."), GridBagConstraints())
        }

        dynamicPanel.revalidate()
        dynamicPanel.repaint()
    }


    private fun createUIComponentForParameter(spec: Map<String, Any>, existingValue: LLMSettings.NamedParameter?): JComponent {
        val defaultValue = existingValue?.let {
            when (it) {
                is LLMSettings.StringParam -> it.value
                is LLMSettings.BooleanParam -> it.value.toString()
                is LLMSettings.ListParam -> it.value
                is LLMSettings.DoubleParam -> it.value.toString()
                else -> spec["default_value"]?.toString()
            }
        } ?: spec["default_value"]?.toString()

        return when (spec["ui_element"]?.toString()) {
            "textfield" -> JBTextField(defaultValue ?: "")
            "checkbox" -> JCheckBox(spec["name"].toString()).apply { isSelected = defaultValue?.toBoolean() ?: false }
            "combobox" -> {
                val allowedValues = (spec["allowed_values"] as? List<*>)?.map { it.toString() } ?: emptyList()
                ComboBox(allowedValues.toTypedArray()).apply { selectedItem = defaultValue }
            }
            "spinner" -> {
                val allowedValues = spec["allowed_values"] as? Map<*, *>
                val min = (allowedValues?.get("min") as? Number)?.toDouble() ?: 0.0
                val max = (allowedValues?.get("max") as? Number)?.toDouble() ?: 1.0
                val step = (spec["step"] as? Number)?.toDouble() ?: 0.1
                val value = defaultValue?.toDoubleOrNull() ?: min
                JSpinner(SpinnerNumberModel(value, min, max, step))
            }
            else -> JBTextField(defaultValue ?: "")
        }
    }


    private fun createHorizontalPanel(field: JBTextField, button: JButton): JPanel {
        return JPanel(BorderLayout()).apply {
            add(field, BorderLayout.CENTER)
            add(button, BorderLayout.EAST)
        }
    }

    fun loadParameterSpecifications(path: String): List<Map<String, Any>> {
        val file = File(path)
        return if (file.exists()) {
            ObjectMapper().readValue(file)
        } else {
            emptyList()
        }
    }

    private fun addRow(label: String, component: JComponent) {
        val gbc = GridBagConstraints().apply {
            anchor = GridBagConstraints.WEST
            fill = GridBagConstraints.HORIZONTAL
            gridy = dynamicPanel.componentCount / 2
            insets = Insets(5, 5, 5, 5)
        }

        gbc.gridx = 0
        gbc.weightx = 0.0
        dynamicPanel.add(JBLabel(label), gbc)

        gbc.gridx = 1
        gbc.weightx = 1.0
        dynamicPanel.add(component, gbc)
        gbc.gridy++

        parameterFieldMap[label] = component // Adiciona o componente ao mapa
    }

    private fun saveNewConfiguration(name: String, scriptPath: String, configPath: String, command: String) {
        if (name.isBlank() || scriptPath.isBlank() || configPath.isBlank() || command.isBlank()) {
            JOptionPane.showMessageDialog(this, "Todos os campos devem ser preenchidos!", "Erro", JOptionPane.ERROR_MESSAGE)
            return
        }
        if (!File(scriptPath).exists() || !File(configPath).exists()) {
            JOptionPane.showMessageDialog(this, "Os arquivos selecionados não existem!", "Erro", JOptionPane.ERROR_MESSAGE)
            return
        }

        // Verifica se já existe uma configuração com o mesmo nome
        val existingConfig = llmSettings.getConfigurationByName(name)
        if (existingConfig != null) {
            JOptionPane.showMessageDialog(this, "Já existe uma configuração com esse nome!", "Erro", JOptionPane.ERROR_MESSAGE)
            return
        }

        // Criar nova configuração
        val newConfiguration = LLMSettings.LLMConfiguration(
            name = name,
            scriptFilePath = scriptPath,
            parameterSpecFilePath = configPath,
            command = command // Adicionando o campo command aqui
        )


        try {
            // Adiciona a nova configuração sem remover as antigas
            llmSettings.addConfiguration(newConfiguration)

            // Atualiza apenas a parte do ComboBox
            val updatedConfigurations = llmSettings.getConfigurations().map { it.name } + addNewLabel
            configurationComboBox.model = DefaultComboBoxModel(updatedConfigurations.toTypedArray())

            JOptionPane.showMessageDialog(this, "Nova configuração adicionada com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE)
        } catch (e: Exception) {
            JOptionPane.showMessageDialog(this, "Erro ao salvar configuração: ${e.message}", "Erro", JOptionPane.ERROR_MESSAGE)
        }
    }


}
