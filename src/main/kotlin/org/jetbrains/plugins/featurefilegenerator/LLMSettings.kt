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
    }

    @Tag("LLMConfiguration")
    data class LLMConfiguration(
        @Attribute("name")
        var name: String = "",

        @Attribute("scriptFilePath")
        var scriptFilePath: String = "",

        @Attribute("parameterSpecFilePath")
        var parameterSpecFilePath: String = "",

        @Attribute("command") // Adicionando o campo command
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

        @Attribute("required")
        open var required: Boolean = false,

        @Attribute("description")
        open var description: String = ""
    )

    @Tag("StringParam")
    class StringParam(
        key: String = "",
        required: Boolean = false,
        description: String = "",
        @Attribute("value")
        var value: String = ""
    ) : NamedParameter(key, required, description)

    @Tag("IntParam")
    class IntParam(
        key: String = "",
        required: Boolean = false,
        description: String = "",
        @Attribute("value")
        var value: Int = 0
    ) : NamedParameter(key, required, description)

    @Tag("DoubleParam")
    class DoubleParam(
        key: String = "",
        required: Boolean = false,
        description: String = "",
        @Attribute("value")
        var value: Double = 0.0
    ) : NamedParameter(key, required, description)

    @Tag("BooleanParam")
    class BooleanParam(
        key: String = "",
        required: Boolean = false,
        description: String = "",
        @Attribute("value")
        var value: Boolean = false
    ) : NamedParameter(key, required, description)

    @Tag("ListParam")
    class ListParam(
        key: String = "",
        required: Boolean = false,
        description: String = "",
        @Attribute("value")
        var value: String = "",

        @XCollection(propertyElementName = "allowedValues", elementName = "option")
        var allowedValues: List<String> = emptyList()
    ) : NamedParameter(key, required, description)

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
        if (myState.configurations.none { it.name == config.name }) {
            myState.configurations.add(config)
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
