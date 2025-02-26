package org.jetbrains.plugins.featurefilegenerator

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.Tag
import com.intellij.util.xmlb.annotations.XCollection
import java.io.File

@State(
    name = "LLMSettings",
    storages = [Storage("LLMSettings.xml")]
)
class LLMSettings : PersistentStateComponent<LLMSettings.State> {

    private var myState: State = State()

    class State {
        @XCollection(propertyElementName = "configurations")
        var configurations: MutableList<LLMConfiguration> = mutableListOf()

        @Attribute("selectedLLMName")
        var selectedLLMName: String? = null  // Armazena a LLM selecionada
    }

    fun getSelectedLLM(): String? {
        return myState.selectedLLMName
    }

    fun setSelectedLLM(name: String?) {
        myState.selectedLLMName = name
    }

    @Tag("LLMConfiguration")
    data class LLMConfiguration(
        @Attribute("name")
        var name: String = "",

        @Attribute("scriptFilePath")
        var scriptFilePath: String = "",

        @Attribute("parameterSpecFilePath")
        var parameterSpecFilePath: String = "",

        @Attribute("command")
        var command: String = "",

        @XCollection(
            propertyElementName = "parameters",
            style = XCollection.Style.v2,
            elementTypes = [StringParam::class, IntParam::class, DoubleParam::class, BooleanParam::class, ListParam::class]
        )
        var namedParameters: MutableList<NamedParameter> = mutableListOf()
    ) {
        init {
            namedParameters = namedParameters.filterNotNull().toMutableList()
        }
    }

    @Tag("NamedParameter")
    abstract class NamedParameter(
        @Attribute("key")
        open var key: String = "",

        @Attribute("argName") // Novo campo padronizado para o nome do argumento CLI
        open var argName: String = "",

        @Attribute("required")
        open var required: Boolean = false,

        @Attribute("description")
        open var description: String = ""
    )

    @Tag("StringParam")
    class StringParam(
        key: String = "",
        argName: String = "",
        required: Boolean = false,
        description: String = "",
        @Attribute("value")
        var value: String = ""
    ) : NamedParameter(key, argName, required, description)

    @Tag("IntParam")
    class IntParam(
        key: String = "",
        argName: String = "",
        required: Boolean = false,
        description: String = "",
        @Attribute("value")
        var value: Int = 0
    ) : NamedParameter(key, argName, required, description)

    @Tag("DoubleParam")
    class DoubleParam(
        key: String = "",
        argName: String = "",
        required: Boolean = false,
        description: String = "",
        @Attribute("value")
        var value: Double = 0.0
    ) : NamedParameter(key, argName, required, description)

    @Tag("BooleanParam")
    class BooleanParam(
        key: String = "",
        argName: String = "",
        required: Boolean = false,
        description: String = "",
        @Attribute("value")
        var value: Boolean = false
    ) : NamedParameter(key, argName, required, description)

    @Tag("ListParam")
    class ListParam(
        key: String = "",
        argName: String = "",
        required: Boolean = false,
        description: String = "",
        @Attribute("value")
        var value: String = "",

        @XCollection(propertyElementName = "allowedValues", elementName = "option")
        var allowedValues: List<String> = emptyList()
    ) : NamedParameter(key, argName, required, description)

    companion object {
        fun getInstance(): LLMSettings {
            return ApplicationManager.getApplication().getService(LLMSettings::class.java)
        }
    }

    override fun getState(): State {
        return myState
    }

    override fun loadState(state: State) {
        myState = state
        println("DEBUG: Carregando estado de LLMSettings")
        myState.configurations.forEach { config ->
            println("DEBUG: Configuração carregada -> ${config.name}")
            config.namedParameters = ensureNamedParameters(config.namedParameters)
            println("DEBUG: Parâmetros corrigidos -> ${config.namedParameters}")
        }
    }

    fun addConfiguration(config: LLMConfiguration) {
        if (!isValidFilePath(config.scriptFilePath) || !isValidFilePath(config.parameterSpecFilePath)) {
            throw IllegalArgumentException("Invalid file path(s) provided.")
        }

        // Verifica se já existe uma configuração com o mesmo nome
        val existingConfig = myState.configurations.find { it.name == config.name }

        if (existingConfig == null) {
            myState.configurations.add(config) // Adiciona a nova configuração sem apagar as antigas
        } else {
            throw IllegalArgumentException("Configuration with the same name already exists.")
        }
    }

    fun removeConfiguration(config: LLMConfiguration) {
        myState.configurations.remove(config)
    }

    fun getConfigurations(): List<LLMConfiguration> {
        return myState.configurations
    }

    fun updateConfiguration(oldConfig: LLMConfiguration, newConfig: LLMConfiguration) {
        val index = myState.configurations.indexOf(oldConfig)
        if (index != -1) {
            myState.configurations[index] = newConfig
        }
    }

    fun getConfigurationByName(name: String): LLMConfiguration? {
        val config = myState.configurations.find { it.name == name }

        if (config == null) {
            println("DEBUG: Nenhuma configuração encontrada com o nome '$name'")
        } else {
            println("DEBUG: Configuração encontrada -> ${config.name}")
            config.namedParameters = ensureNamedParameters(config.namedParameters)
            println("DEBUG: Parâmetros corrigidos -> ${config.namedParameters}")
        }

        return config
    }

    private fun ensureNamedParameters(parameters: MutableList<NamedParameter>?): MutableList<NamedParameter> {
        if (parameters == null) return mutableListOf()

        val fixedParameters = mutableListOf<NamedParameter>()

        parameters.forEach { param ->
            when (param) {
                is NamedParameter -> {
                    fixedParameters.add(param)
                }
                else -> {
                    println("DEBUG: ⚠ Tipo inesperado encontrado em namedParameters: ${param?.javaClass?.name}")
                }
            }
        }

        return fixedParameters
    }

    private fun isValidFilePath(path: String): Boolean {
        return File(path).exists()
    }
}
