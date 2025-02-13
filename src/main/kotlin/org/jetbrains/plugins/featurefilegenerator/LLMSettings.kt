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

        @XCollection(propertyElementName = "parameters")
        var namedParameters: MutableList<NamedParameter> = mutableListOf()
    )

    /**
     * Alteração: Substituímos `sealed class` por `open class` para permitir serialização XML.
     */
    @Tag("NamedParameter")
    open class NamedParameter(
        @Attribute("key")
        open val key: String = "",

        @Attribute("required")
        open val required: Boolean = false,

        @Attribute("description")
        open val description: String = ""
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
        var value: Int = 0,
        @Attribute("min")
        val min: Int = Int.MIN_VALUE,
        @Attribute("max")
        val max: Int = Int.MAX_VALUE,
        @Attribute("step")
        val step: Int = 1
    ) : NamedParameter(key, required, description)

    @Tag("DoubleParam")
    class DoubleParam(
        key: String = "",
        required: Boolean = false,
        description: String = "",
        @Attribute("value")
        var value: Double = 0.0,
        @Attribute("min")
        val min: Double = Double.MIN_VALUE,
        @Attribute("max")
        val max: Double = Double.MAX_VALUE,
        @Attribute("step")
        val step: Double = 0.1
    ) : NamedParameter(key, required, description)

    @Tag("BooleanParam")
    class BooleanParam(
        key: String = "",
        required: Boolean = false,
        description: String = "",
        @Attribute("value")
        var value: Boolean = false
    ) : NamedParameter(key, required, description)

    @Tag("FilePathParam")
    class FilePathParam(
        key: String = "",
        required: Boolean = false,
        description: String = "",
        @Attribute("value")
        var value: String = ""
    ) : NamedParameter(key, required, description)

    @Tag("ListParam")
    class ListParam(
        key: String = "",
        required: Boolean = false,
        description: String = "",
        @Attribute("value")
        var value: String = "",
        @XCollection
        val allowedValues: List<String> = emptyList()
    ) : NamedParameter(key, required, description)

    @Tag("OptionalParam")
    class OptionalParam(
        key: String = "",
        required: Boolean = false,
        description: String = "",
        @Attribute("value")
        var value: String? = null,
        @Attribute("enabled")
        var enabled: Boolean = false,
        @Attribute("min")
        val min: Int = Int.MIN_VALUE,
        @Attribute("max")
        val max: Int = Int.MAX_VALUE,
        @Attribute("step")
        val step: Int = 1
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
        return myState.configurations.find { it.name == name }
    }

    fun configurationExists(name: String): Boolean {
        return myState.configurations.any { it.name == name }
    }

    fun replaceConfigurations(newConfigurations: List<LLMConfiguration>) {
        myState.configurations.clear()
        myState.configurations.addAll(newConfigurations)
    }

    private fun isValidFilePath(path: String): Boolean {
        return File(path).exists()
    }
}
