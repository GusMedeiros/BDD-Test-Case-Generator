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
    private val dynamicPanel = JPanel()
    private val addNewLabel = "Inserir novo"

    private val scriptField = JBTextField()
    private val configField = JBTextField()
    private val commandField = JBTextField()

    init {
        setupUI()
    }

    private fun setupUI() {
        // Inicializar a combobox com configurações salvas
        val configurations = llmSettings.getConfigurations().map { it.name } + addNewLabel
        configurationComboBox.model = DefaultComboBoxModel(configurations.toTypedArray())
        configurationComboBox.addActionListener { onConfigurationSelected() }

        // Configurar o layout principal
        add(JPanel().apply {
            layout = BorderLayout()
            add(JBLabel("Selecione ou insira uma nova LLM:"), BorderLayout.WEST)
            add(configurationComboBox, BorderLayout.CENTER)
        }, BorderLayout.NORTH)

        // Painel dinâmico onde os campos serão adicionados
        dynamicPanel.layout = BoxLayout(dynamicPanel, BoxLayout.Y_AXIS)
        add(dynamicPanel, BorderLayout.CENTER)

        // Atualizar campos ao inicializar
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

    private fun renderNewConfigurationFields() {
        // Campo para inserir o nome da LLM
        val nameLabel = JBLabel("Nome da LLM:")
        val nameField = JBTextField()

        // Campo para selecionar o arquivo de script
        val scriptLabel = JBLabel("Selecionar Arquivo de Script:")
        val scriptButton = JButton("Procurar").apply {
            addActionListener {
                val fileChooser = JFileChooser()
                fileChooser.fileFilter = FileNameExtensionFilter("Arquivos de Script", "sh", "bat", "py")
                if (fileChooser.showOpenDialog(this@LLMConfigurationPanel) == JFileChooser.APPROVE_OPTION) {
                    scriptField.text = fileChooser.selectedFile.absolutePath
                }
            }
        }

        // Campo para selecionar o arquivo de configuração
        val configLabel = JBLabel("Selecionar Arquivo de Configuração:")
        val configButton = JButton("Procurar").apply {
            addActionListener {
                val fileChooser = JFileChooser()
                fileChooser.fileFilter = FileNameExtensionFilter("Arquivos de Configuração", "json", "yaml", "xml")
                if (fileChooser.showOpenDialog(this@LLMConfigurationPanel) == JFileChooser.APPROVE_OPTION) {
                    configField.text = fileChooser.selectedFile.absolutePath
                }
            }
        }

        // Campo para a string de comando do console
        val commandLabel = JBLabel("Comando para o Console:")

        // Botão para salvar a nova configuração
        val saveButton = JButton("Salvar").apply {
            addActionListener { saveNewConfiguration(nameField.text) }
        }

        // Adicionar campos ao painel dinâmico
        dynamicPanel.add(nameLabel)
        dynamicPanel.add(nameField)
        dynamicPanel.add(scriptLabel)
        dynamicPanel.add(createHorizontalPanel(scriptField, scriptButton))
        dynamicPanel.add(configLabel)
        dynamicPanel.add(createHorizontalPanel(configField, configButton))
        dynamicPanel.add(commandLabel)
        dynamicPanel.add(commandField)
        dynamicPanel.add(Box.createVerticalStrut(10)) // Espaço entre campos e botão
        dynamicPanel.add(saveButton)
    }


    private fun saveNewConfiguration(customName: String) {
        val name = customName.trim()
        val scriptPath = scriptField.text.trim()
        val configPath = configField.text.trim()
        val command = commandField.text.trim()

        // Validação básica
        if (name.isEmpty() || scriptPath.isEmpty() || configPath.isEmpty() || command.isEmpty()) {
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

        // Salvar a nova configuração
        try {
            llmSettings.addConfiguration(newConfiguration)
            JOptionPane.showMessageDialog(this, "Nova configuração adicionada com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE)
            // Atualizar a combobox
            val updatedConfigurations = llmSettings.getConfigurations().map { it.name } + addNewLabel
            configurationComboBox.model = DefaultComboBoxModel(updatedConfigurations.toTypedArray())
        } catch (e: Exception) {
            JOptionPane.showMessageDialog(this, e.message, "Erro", JOptionPane.ERROR_MESSAGE)
        }
    }


    private fun renderDynamicFields(configuration: LLMSettings.LLMConfiguration?) {
        val parameterSpecPath = configuration?.parameterSpecFilePath

        // Carregar as especificações de parâmetros
        val specifications = if (parameterSpecPath != null && File(parameterSpecPath).exists()) {
            loadParameterSpecifications(parameterSpecPath)
        } else {
            emptyList()
        }

        // Configurando o layout
        dynamicPanel.layout = GridBagLayout()
        val gbc = GridBagConstraints().apply {
            gridx = 0
            gridy = 0
            anchor = GridBagConstraints.WEST
            fill = GridBagConstraints.HORIZONTAL
            weightx = 1.0
            insets = Insets(2, 5, 2, 5) // Espaçamento menor por padrão
        }

        for (spec in specifications) {
            val name = spec["name"]?.toString() ?: "Unnamed"
            val uiElement = spec["ui_element"]?.toString()
            val defaultValue = spec["default_value"]?.toString()

            when (uiElement) {
                "textfield" -> {
                    val field = JBTextField()
                    field.text = defaultValue ?: ""
                    dynamicPanel.add(JBLabel(name), gbc)

                    // Reduz espaçamento entre título e campo
                    gbc.insets = Insets(0, 5, 5, 5)
                    gbc.gridy++
                    dynamicPanel.add(field, gbc)

                    // Reseta o espaçamento para o próximo campo
                    gbc.insets = Insets(2, 5, 2, 5)
                    gbc.gridy++
                }
                "checkbox" -> {
                    val checkbox = JBCheckBox(name)
                    checkbox.isSelected = defaultValue?.toBoolean() ?: false
                    gbc.gridwidth = 2
                    dynamicPanel.add(checkbox, gbc)
                    gbc.gridwidth = 1
                    gbc.gridy++
                }
                "combobox" -> {
                    val allowedValues = (spec["allowed_values"] as? List<*>)?.map { it.toString() } ?: emptyList()
                    val comboBox = ComboBox(allowedValues.toTypedArray())
                    comboBox.selectedItem = defaultValue
                    dynamicPanel.add(JBLabel(name), gbc)

                    // Reduz espaçamento entre título e campo
                    gbc.insets = Insets(0, 5, 5, 5)
                    gbc.gridy++
                    dynamicPanel.add(comboBox, gbc)

                    // Reseta o espaçamento para o próximo campo
                    gbc.insets = Insets(2, 5, 2, 5)
                    gbc.gridy++
                }
                "spinner" -> {
                    val allowedValues = spec["allowed_values"] as? Map<*, *>
                    val min = (allowedValues?.get("min") as? Number)?.toInt() ?: 0
                    val max = (allowedValues?.get("max") as? Number)?.toInt() ?: Int.MAX_VALUE
                    val step = (spec["step"] as? Number)?.toInt() ?: 1
                    val value = (defaultValue?.toIntOrNull() ?: min).coerceIn(min, max)

                    val spinnerModel = SpinnerNumberModel(value, min, max, step)
                    val spinner = JSpinner(spinnerModel)

                    dynamicPanel.add(JBLabel(name), gbc)

                    // Reduz espaçamento entre título e campo
                    gbc.insets = Insets(0, 5, 5, 5)
                    gbc.gridy++
                    dynamicPanel.add(spinner, gbc)

                    // Reseta o espaçamento para o próximo campo
                    gbc.insets = Insets(2, 5, 2, 5)
                    gbc.gridy++
                }
                "filePicker" -> {
                    val field = JBTextField(20) // Define um tamanho padrão para o campo de texto
                    field.text = defaultValue ?: ""
                    val button = JButton("Selecionar Arquivo")
                    button.addActionListener {
                        val fileChooser = JFileChooser()
                        if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                            field.text = fileChooser.selectedFile.absolutePath
                        }
                    }

                    // Adiciona o título na linha acima
                    dynamicPanel.add(JBLabel(name), gbc)

                    // Reduz espaçamento entre título e campo
                    gbc.insets = Insets(0, 5, 5, 5)
                    gbc.gridy++

                    // Adiciona o campo de texto e o botão na mesma linha
                    val filePickerPanel = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
                        add(field)
                        add(button)
                    }
                    gbc.gridwidth = 2
                    dynamicPanel.add(filePickerPanel, gbc)
                    gbc.gridwidth = 1

                    // Reseta o espaçamento para o próximo campo
                    gbc.insets = Insets(2, 5, 2, 5)
                    gbc.gridy++
                }
                "optionalField" -> {
                    val checkbox = JBCheckBox(name)
                    checkbox.isSelected = false

                    val optionalField = JBTextField()
                    optionalField.isVisible = false
                    optionalField.text = defaultValue ?: ""

                    checkbox.addActionListener {
                        optionalField.isVisible = checkbox.isSelected
                        dynamicPanel.revalidate()
                        dynamicPanel.repaint()
                    }

                    dynamicPanel.add(checkbox, gbc)
                    gbc.gridy++
                    dynamicPanel.add(optionalField, gbc)
                    gbc.gridy++
                }
            }
        }
    }





    private fun createHorizontalPanel(field: JBTextField, button: JButton): JPanel {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.X_AXIS)
        panel.add(field)
        panel.add(button)
        return panel
    }

    private fun loadParameterSpecifications(path: String): List<Map<String, Any>> {
        val file = File(path)
        if (!file.exists()) {
            throw IllegalArgumentException("Arquivo não encontrado: $path")
        }

        return try {
            // carregar os parâmetros do arquivo JSON
            val objectMapper = ObjectMapper()
            objectMapper.readValue(file)
        } catch (e: Exception) {
            throw IllegalArgumentException("Erro ao carregar as especificações: ${e.message}", e)
        }
    }

}
