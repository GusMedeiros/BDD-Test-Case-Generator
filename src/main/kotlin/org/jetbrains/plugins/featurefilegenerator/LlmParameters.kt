package org.jetbrains.plugins.featurefilegenerator

import javax.swing.JFrame
import javax.swing.SwingUtilities


// Classe para gerenciar serviços e suas especificações
class LlmParameters {
    private val services: MutableMap<String, List<ParameterSpec>> = mutableMapOf()

    fun addService(name: String, parameters: List<ParameterSpec>) {
        services[name] = parameters
    }

    fun getSerchvices(): Map<String, List<ParameterSpec>> {
        return services
    }

    fun getServiceParameters(name: String): List<ParameterSpec>? {
        return services[name]
    }
}

// Classe para especificações de parâmetros
data class ParameterSpec(
    val name: String,
    val type: ParameterType,
    val defaultValue: Any?,
    val range: Any? = null // Intervalo de valores permitidos
)

// Tipos de parâmetros suportados
enum class ParameterType {
    TEXT,
    NUMBER,
    BOOLEAN
}

// Teste da UI com ServiceManager
fun main() {
    val llmParameters = LlmParameters().apply {
        addService("Serviço A", listOf(
            ParameterSpec("Parâmetro 1", ParameterType.TEXT, ""),
            ParameterSpec("Parâmetro 2", ParameterType.NUMBER, 0..100)
        ))
        addService("Serviço B", listOf(
            ParameterSpec("Opção", ParameterType.BOOLEAN, false)
        ))
    }

    SwingUtilities.invokeLater {
        val frame = JFrame("Dynamic UI Panel")
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.add(UIPanel(llmParameters))
        frame.setSize(400, 300)
        frame.isVisible = true
    }
}
