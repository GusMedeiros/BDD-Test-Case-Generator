package org.jetbrains.plugins.featurefilegenerator

import java.awt.BorderLayout
import javax.swing.*

// Classe UIPanel que monta uma interface dinâmica
class UIPanel(private val llmParameters: LlmParameters) : JPanel() {

    private val comboBox: JComboBox<String> = JComboBox<String>().apply {
        addItem("Selecione um serviço")
        //serviceManager.getServices().keys.forEach { addItem(it) }
        addItem("Inserir um novo") // Placeholder
    }

    private val dynamicPanel: JPanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
    }

    init {
        layout = BorderLayout()
        add(comboBox, BorderLayout.NORTH)
        add(dynamicPanel, BorderLayout.CENTER)

        comboBox.addActionListener {
            val selectedService = comboBox.selectedItem as String
            updateDynamicPanel(selectedService)
        }
    }

    private fun updateDynamicPanel(serviceName: String) {
        dynamicPanel.removeAll()

        val serviceSpecs = llmParameters.getServiceParameters(serviceName)
        if (serviceSpecs != null) {
            serviceSpecs.forEach { parameter ->
                val field = createField(parameter)
                dynamicPanel.add(JLabel(parameter.name))
                dynamicPanel.add(field)
            }
        } else if (serviceName == "Inserir um novo") {
            dynamicPanel.add(JLabel("Funcionalidade em desenvolvimento"))
        }

        dynamicPanel.revalidate()
        dynamicPanel.repaint()
    }

    private fun createField(parameter: ParameterSpec): JComponent {
        return when (parameter.type) {
            ParameterType.TEXT -> JTextField(parameter.defaultValue.toString(), 20)
            ParameterType.NUMBER -> JSpinner(SpinnerNumberModel((parameter.defaultValue as Int?) ?: 0, (parameter.range as? IntRange)?.first, (parameter.range as? IntRange)?.last, 1))
            ParameterType.BOOLEAN -> JCheckBox().apply { isSelected = (parameter.defaultValue as? Boolean) ?: false }
        }
    }
}
