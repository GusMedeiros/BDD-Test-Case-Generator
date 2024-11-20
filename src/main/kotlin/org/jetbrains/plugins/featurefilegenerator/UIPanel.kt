package org.jetbrains.plugins.featurefilegenerator

import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.file.Files
import java.util.*
import javax.swing.*
import kotlin.math.absoluteValue
import kotlin.random.Random

class UIPanel(private val project: com.intellij.openapi.project.Project) : DialogWrapper(true) {
    val outputDirPathField = JTextField()
    val debugCheckBox = JCheckBox("Debug")
    val temperatureSpinner = JSpinner(SpinnerNumberModel(0.0, 0.0, 2.0, 0.1)).apply {
        editor = JSpinner.NumberEditor(this, "1.0")
    }
    val apiKeyField = JTextField().apply {
        preferredSize = java.awt.Dimension(300, preferredSize.height)
    }
    val loadModelsButton = JButton("Load Models").apply {
        isEnabled = false
    }
    val seedSpinner = JSpinner(SpinnerNumberModel(Random.nextInt().absoluteValue, 0, Integer.MAX_VALUE, 1)).apply {
        preferredSize = java.awt.Dimension(200, preferredSize.height)
    }
    val fixedSeedCheckBox = JCheckBox("Fixed Seed").apply {
        addActionListener {
            seedSpinner.isVisible = isSelected // Exibe o seedSpinner se estiver selecionado, esconde caso contrário
        }
        seedSpinner.isVisible = false
    }



    val gptModelComboBox = JComboBox<String>()

    init {
        init()
        title = "BDDGPT Settings"
        loadModelsButton.addActionListener { populateGptModelComboBox() }
        apiKeyField.document.addDocumentListener(object : javax.swing.event.DocumentListener {
            override fun insertUpdate(e: javax.swing.event.DocumentEvent?) {
                checkApiKeyField()
            }

            override fun removeUpdate(e: javax.swing.event.DocumentEvent?) {
                checkApiKeyField()
            }

            override fun changedUpdate(e: javax.swing.event.DocumentEvent?) {
                checkApiKeyField()
            }
        })
    }

    override fun createCenterPanel(): JComponent? {
        return panel {
            row("Api Key:") {
                cell(apiKeyField)
                    .horizontalAlign(HorizontalAlign.FILL)
                cell(createTooltipLabel("Enter OpenAI API key here"))
            }
            row("Output Directory:") {
                cell(outputDirPathField)
                    .horizontalAlign(HorizontalAlign.FILL)
                    .resizableColumn()
                button("Explore") {
                    val descriptor = FileChooserDescriptor(false, true, false, false, false, false)
                    val file = FileChooser.chooseFile(descriptor, project, null)
                    file?.let { outputDirPathField.text = it.path }
                }
                cell(createTooltipLabel("Select the output directory"))
            }

            // Adicionando a nova linha com o CheckBox FixedSeed
            row("Fixed Seed:") {
                cell(fixedSeedCheckBox)
                    .horizontalAlign(HorizontalAlign.LEFT)
                cell(createTooltipLabel("Enable fixed seed to enter a seed value manually"))
            }

            // A linha do Seed Spinner
            row("Seed:") {
                cell(seedSpinner)
                    .horizontalAlign(HorizontalAlign.FILL)
                cell(createTooltipLabel("Default: Random Int+. Enter a seed value if you want semi-reproducible results"))
            }

            row("Temperature:") {
                cell(temperatureSpinner)
                cell(createTooltipLabel("Set the temperature for the model (default is 1)"))
            }
            row("GPT Models:") {
                cell(gptModelComboBox)
                    .horizontalAlign(HorizontalAlign.FILL)
                cell(loadModelsButton)
                cell(createTooltipLabel("Load models after entering a valid API key"))
            }
            row {
                cell(debugCheckBox)
                    .horizontalAlign(HorizontalAlign.LEFT)
                cell(createTooltipLabel("Enable debug mode"))
            }
        }
    }

    private fun createTooltipLabel(tooltipText: String): JLabel {
        return JLabel("\u2753").apply {
            toolTipText = tooltipText
        }
    }

    private fun checkApiKeyField() {
        loadModelsButton.isEnabled = apiKeyField.text.isNotBlank()
    }


    private fun populateGptModelComboBox() {
        try {
            val apiKey = apiKeyField.text
            if (apiKey.isBlank()) {
                JOptionPane.showMessageDialog(null, "API Key cannot be empty", "Error", JOptionPane.ERROR_MESSAGE)
                return
            }

            val resourceStream = this::class.java.getResourceAsStream("/python/list_models.py")
                ?: throw Exception("Resource not found: /python/list_models.py")

            val tempScriptFile = Files.createTempFile("list_models", ".py")
            resourceStream.bufferedReader().use { reader ->
                Files.newBufferedWriter(tempScriptFile).use { writer ->
                    reader.copyTo(writer)
                }
            }
            val os = System.getProperty("os.name").lowercase(Locale.getDefault())
            val pythonCommand = if (os.contains("win")) "python" else "python3"
            val processBuilder = ProcessBuilder(pythonCommand, tempScriptFile.toString())
            processBuilder.environment()["OPENAI_API_KEY"] = apiKey
            val process = processBuilder.start()

            val stdoutReader = BufferedReader(InputStreamReader(process.inputStream))
            val stderrReader = BufferedReader(InputStreamReader(process.errorStream))

            val modelList = mutableListOf<String>()
            var line: String?

            line = stdoutReader.readLine()
            while (line != null) {
                modelList.add(line)
                line = stdoutReader.readLine()
            }

            val errorList = mutableListOf<String>()
            line = stderrReader.readLine()
            while (line != null) {
                errorList.add(line)
                line = stderrReader.readLine()
            }

            stdoutReader.close()
            stderrReader.close()

            if (errorList.isNotEmpty()) {
                throw Exception("Error during script execution: ${errorList.joinToString("\n")}")
            }

            SwingUtilities.invokeLater {
                gptModelComboBox.model = DefaultComboBoxModel(modelList.toTypedArray())
            }

            Files.deleteIfExists(tempScriptFile)
        } catch (e: Exception) {
            e.printStackTrace()
            SwingUtilities.invokeLater {
                JOptionPane.showMessageDialog(
                    null,
                    "An error occurred: ${e.message}",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
                )
            }
        }
    }
}