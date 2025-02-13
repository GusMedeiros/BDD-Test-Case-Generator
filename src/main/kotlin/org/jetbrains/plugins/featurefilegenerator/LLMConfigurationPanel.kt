package org.jetbrains.plugins.featurefilegenerator

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import java.awt.*
import java.io.File
import javax.swing.*
import javax.swing.filechooser.FileNameExtensionFilter

class LLMConfigurationPanel : JPanel(BorderLayout()) {

    private val llmSettings = LLMSettings.getInstance()
    private val configurationComboBox = ComboBox<String>()
    private val dynamicPanel = JPanel(GridBagLayout())
    private val addNewLabel = "Inserir novo"

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
            renderDynamicFields(configuration)
        }

        dynamicPanel.revalidate()
        dynamicPanel.repaint()
    }

    private fun renderDynamicFields(configuration: LLMSettings.LLMConfiguration?) {
        dynamicPanel.removeAll()
        val parameterSpecPath = configuration?.parameterSpecFilePath

        val specifications = if (parameterSpecPath != null && File(parameterSpecPath).exists()) {
            loadParameterSpecifications(parameterSpecPath)
        } else {
            emptyList()
        }

        val gbc = GridBagConstraints().apply {
            anchor = GridBagConstraints.WEST
            fill = GridBagConstraints.HORIZONTAL
            gridy = 0
            insets = Insets(5, 5, 5, 5)
        }

        for (spec in specifications) {
            val name = spec["name"]?.toString() ?: "Unnamed"
            val uiElement = spec["ui_element"]?.toString()
            val defaultValue = spec["default_value"]?.toString()

            gbc.gridx = 0
            when (uiElement) {
                "textfield" -> {
                    dynamicPanel.add(JBLabel(name), gbc)
                    gbc.gridx = 1
                    dynamicPanel.add(JBTextField(defaultValue ?: ""), gbc)
                }
                "checkbox" -> {
                    val checkbox = JBCheckBox(name)
                    checkbox.isSelected = defaultValue?.toBoolean() ?: false
                    dynamicPanel.add(checkbox, gbc)
                }
                "combobox" -> {
                    val allowedValues = (spec["allowed_values"] as? List<*>)?.map { it.toString() } ?: emptyList()
                    dynamicPanel.add(JBLabel(name), gbc)
                    gbc.gridx = 1
                    dynamicPanel.add(ComboBox(allowedValues.toTypedArray()).apply { selectedItem = defaultValue }, gbc)
                }
                "filePicker" -> {
                    val field = JBTextField(20)
                    field.text = defaultValue ?: ""
                    val button = JButton("Selecionar Arquivo").apply {
                        addActionListener {
                            val fileChooser = JFileChooser()
                            if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                                field.text = fileChooser.selectedFile.absolutePath
                            }
                        }
                    }
                    dynamicPanel.add(JBLabel(name), gbc)
                    gbc.gridx = 1
                    dynamicPanel.add(createHorizontalPanel(field, button), gbc)
                }
                "spinner" -> {
                    val allowedValues = spec["allowed_values"] as? Map<*, *>
                    val min = (allowedValues?.get("min") as? Number)?.toDouble() ?: 0.0
                    val max = (allowedValues?.get("max") as? Number)?.toDouble() ?: 1.0
                    val step = (spec["step"] as? Number)?.toDouble() ?: 0.1
                    val value = (defaultValue?.toDoubleOrNull() ?: min).coerceIn(min, max)

                    val spinnerModel = SpinnerNumberModel(value, min, max, step)
                    val spinner = JSpinner(spinnerModel)

                    dynamicPanel.add(JBLabel(name), gbc)
                    gbc.gridx = 1
                    dynamicPanel.add(spinner, gbc)
                }
                "optionalField" -> {
                    // Cria o checkbox e o campo de texto (o campo persistirá mesmo ao trocar layouts)
                    val checkbox = JBCheckBox(name)
                    val field = JBTextField(defaultValue ?: "", 15)

                    // Inicialmente, usamos FlowLayout para exibir somente o checkbox (primeira solução)
                    val panel = JPanel(FlowLayout(FlowLayout.LEFT, 5, 0))
                    panel.add(checkbox)

                    // Listener que troca o layout conforme o estado do checkbox
                    checkbox.addActionListener {
                        if (checkbox.isSelected) {
                            // Se selecionado, troca para GridBagLayout (segunda solução)
                            panel.removeAll()
                            panel.layout = GridBagLayout()
                            // Reserva o espaço para o campo: define o tamanho preferencial como mínimo
                            val preferredSize = field.preferredSize
                            field.minimumSize = preferredSize
                            field.preferredSize = preferredSize

                            val innerGbc = GridBagConstraints().apply {
                                anchor = GridBagConstraints.WEST
                                gridx = 0
                                insets = Insets(0, 0, 0, 5) // Pequeno espaçamento entre checkbox e campo
                            }
                            panel.add(checkbox, innerGbc)

                            innerGbc.gridx = 1
                            innerGbc.weightx = 1.0
                            innerGbc.fill = GridBagConstraints.HORIZONTAL
                            panel.add(field, innerGbc)
                        } else {
                            // Se desmarcado, volta para FlowLayout exibindo somente o checkbox
                            panel.removeAll()
                            panel.layout = FlowLayout(FlowLayout.LEFT, 5, 0)
                            panel.add(checkbox)
                        }
                        panel.revalidate()
                        panel.repaint()
                    }

                    gbc.gridx = 0
                    gbc.gridwidth = 2
                    gbc.fill = GridBagConstraints.HORIZONTAL
                    dynamicPanel.add(panel, gbc)
                    gbc.gridwidth = 1
                }



            }

            gbc.gridy++
        }

        dynamicPanel.revalidate()
        dynamicPanel.repaint()
    }

    private fun createHorizontalPanel(field: JBTextField, button: JButton): JPanel {
        return JPanel(BorderLayout()).apply {
            add(field, BorderLayout.CENTER)
            add(button, BorderLayout.EAST)
        }
    }

    private fun loadParameterSpecifications(path: String): List<Map<String, Any>> {
        val file = File(path)
        if (!file.exists()) {
            throw IllegalArgumentException("Arquivo não encontrado: $path")
        }

        return try {
            val objectMapper = ObjectMapper()
            objectMapper.readValue(file)
        } catch (e: Exception) {
            throw IllegalArgumentException("Erro ao carregar as especificações: ${e.message}", e)
        }
    }

    private fun renderNewConfigurationFields() {
        dynamicPanel.removeAll()
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
                }
            }
        }
        addRow("Selecionar Arquivo de Configuração:", createHorizontalPanel(configField, configButton))

        val commandField = JBTextField()
        addRow("Comando para o Console:", commandField)

        val saveButton = JButton("Salvar").apply {
            addActionListener { saveNewConfiguration(nameField.text, scriptField.text, configField.text, commandField.text) }
        }
        gbc.gridwidth = 2
        dynamicPanel.add(saveButton, gbc)

        dynamicPanel.revalidate()
        dynamicPanel.repaint()
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

        val newConfiguration = LLMSettings.LLMConfiguration(
            name = name,
            scriptFilePath = scriptPath,
            parameterSpecFilePath = configPath
        )

        try {
            llmSettings.addConfiguration(newConfiguration)
            JOptionPane.showMessageDialog(this, "Nova configuração adicionada com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE)
            val updatedConfigurations = llmSettings.getConfigurations().map { it.name } + addNewLabel
            configurationComboBox.model = DefaultComboBoxModel(updatedConfigurations.toTypedArray())
        } catch (e: Exception) {
            JOptionPane.showMessageDialog(this, e.message, "Erro", JOptionPane.ERROR_MESSAGE)
        }
    }
}
