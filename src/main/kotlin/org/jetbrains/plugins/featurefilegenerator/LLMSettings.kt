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
        var selectedLLMName: String? = null
    }

    fun getSelectedLLM(): String? = myState.selectedLLMName

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

        @Attribute("argName")
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

        init {
            val settings = getInstance()
            if (settings.getConfigurations().isEmpty()) {
                println("DEBUG: No configurations found on init. Adding defaults...")
                settings.addDefaultConfigurationsIfMissing()
            }
        }
    }

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
        println("DEBUG: Loading LLMSettings state")

        myState.configurations.forEach { config ->
            println("DEBUG: Loaded configuration -> ${config.name}")

            val (scriptResource, specResource) = when (config.name.lowercase()) {
                "chatgpt" -> "python/gpt_main.py" to "python/gpt_specifications.json"
                "gemini" -> "python/gemini_main.py" to "python/gemini_specifications.json"
                "deepseek" -> "python/deepseek_main.py" to "python/deepseek_specifications.json"
                else -> null to null
            }

            if (scriptResource != null && !File(config.scriptFilePath).exists()) {
                config.scriptFilePath = copyResourceToTempFile(scriptResource, ".py")
            }

            if (specResource != null && !File(config.parameterSpecFilePath).exists()) {
                config.parameterSpecFilePath = copyResourceToTempFile(specResource, ".json")
            }

            config.namedParameters = ensureNamedParameters(config.namedParameters)

            config.namedParameters.forEach { param ->
                if (param is StringParam && param.key == "Instruction Prompt Path") {
                    if (!File(param.value).exists()) {
                        println("DEBUG: Instruction prompt file ${param.value} does not exist. Recreating...")
                        param.value = copyResourceToTempFile(
                            "python/message_1_response=user.txt",
                            ".txt"
                        )
                    }
                }
            }

            println("DEBUG: Fixed parameters -> ${config.namedParameters}")
        }

        addDefaultConfigurationsIfMissing()
    }


    fun addDefaultConfigurationsIfMissing() {
        val configs = listOf(
            Triple("ChatGPT", "gpt_main.py", "gpt_specifications.json"),
            Triple("Gemini", "gemini_main.py", "gemini_specifications.json"),
            Triple("DeepSeek", "deepseek_main.py", "deepseek_specifications.json")
        )

        val instructionFilePath = getDefaultInstructionFilePath()

        configs.forEach { (name, scriptFile, specFile) ->
            if (myState.configurations.none { it.name == name }) {
                val scriptPath = copyResourceToTempFile("python/$scriptFile", ".py")
                val specPath = copyResourceToTempFile("python/$specFile", ".json")

                val config = LLMConfiguration(
                    name = name,
                    scriptFilePath = scriptPath,
                    parameterSpecFilePath = specPath,
                    command = "python3",
                    namedParameters = mutableListOf(
                        StringParam(
                            key = "Instruction Prompt Path",
                            argName = "--instruction_file",
                            required = true,
                            description = "Path to the instruction prompt file",
                            value = instructionFilePath
                        )
                    )
                )

                println("DEBUG: Adding default config -> $name")
                myState.configurations.add(config)
            }
        }
    }

    private fun getDefaultInstructionFilePath(): String {
        return copyResourceToTempFile("python/message_1_response=user.txt", ".txt")
    }

    private fun copyResourceToTempFile(resourcePath: String, suffix: String = ""): String {
        val inputStream = javaClass.classLoader.getResourceAsStream(resourcePath)
            ?: throw IllegalArgumentException("Resource not found: $resourcePath")

        val tempFile = File.createTempFile(
            resourcePath.substringAfterLast("/").substringBeforeLast("."),
            suffix.ifEmpty { ".tmp" }
        )
        tempFile.deleteOnExit()

        inputStream.use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        println("DEBUG: Copied resource $resourcePath to temp file ${tempFile.absolutePath}")
        return tempFile.absolutePath
    }

    fun addConfiguration(config: LLMConfiguration) {
        if (!isValidFilePath(config.scriptFilePath) || !isValidFilePath(config.parameterSpecFilePath)) {
            throw IllegalArgumentException("Invalid file path(s) provided.")
        }

        val existingConfig = myState.configurations.find { it.name == config.name }

        if (existingConfig == null) {
            myState.configurations.add(config)
        } else {
            throw IllegalArgumentException("Configuration with the same name already exists.")
        }
    }

    fun removeConfiguration(config: LLMConfiguration) {
        myState.configurations.remove(config)
    }

    fun getConfigurations(): List<LLMConfiguration> = myState.configurations

    fun updateConfiguration(oldConfig: LLMConfiguration, newConfig: LLMConfiguration) {
        val index = myState.configurations.indexOf(oldConfig)
        if (index != -1) {
            myState.configurations[index] = newConfig
        }
    }

    fun getConfigurationByName(name: String): LLMConfiguration? {
        val config = myState.configurations.find { it.name == name }

        if (config == null) {
            println("DEBUG: No configuration found with name '$name'")
        } else {
            println("DEBUG: Configuration found -> ${config.name}")
            config.namedParameters = ensureNamedParameters(config.namedParameters)
            println("DEBUG: Fixed parameters -> ${config.namedParameters}")
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
                    println("DEBUG: ⚠ Unexpected type found in namedParameters: ${param?.javaClass?.name}")
                }
            }
        }

        return fixedParameters
    }

    private fun isValidFilePath(path: String): Boolean = File(path).exists()
}
