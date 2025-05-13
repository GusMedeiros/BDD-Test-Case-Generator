package org.jetbrains.plugins.featurefilegenerator.cli

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Class responsible for reading LLM configurations from a JSON file provided by the user.
 */
class LLMSettingsCLI(configFilePath: String) {

    @Serializable
    data class LLMConfiguration(
        val name: String,
        val scriptFilePath: String,
        val command: String,
        val namedParameters: List<NamedParameter>
    )

    @Serializable
    sealed class NamedParameter {
        abstract val argName: String

        @Serializable
        @SerialName("string") // Maps "type": "string" to this class
        data class StringParam(
            override val argName: String,
            val value: String
        ) : NamedParameter()

        @Serializable
        @SerialName("int") // Example for integer
        data class IntParam(
            override val argName: String,
            val value: Int
        ) : NamedParameter()

        @Serializable
        @SerialName("boolean") // Example for boolean
        data class BooleanParam(
            override val argName: String,
            val value: Boolean
        ) : NamedParameter()

        @Serializable
        @SerialName("double") // Example for double
        data class DoubleParam(
            override val argName: String,
            val value: Double
        ) : NamedParameter()
    }

    @Serializable
    data class ConfigFile(val llms: List<LLMConfiguration>)

    private val configurations: List<LLMConfiguration> = parseJsonConfig(configFilePath)

    /**
     * Reads and parses the JSON provided by the user.
     */
    private fun parseJsonConfig(filePath: String): List<LLMConfiguration> {
        val file = File(filePath)
        return try {
            val jsonContent = file.readText()
            Json.decodeFromString<ConfigFile>(jsonContent).llms
        } catch (e: Exception) {
            throw IllegalArgumentException("Error reading the JSON file: ${e.message}")
        }
    }

    /**
     * Gets all LLM configurations.
     */
    fun getConfigurations(): List<LLMConfiguration> = configurations

    /**
     * Gets a specific configuration by name.
     */
    fun getConfigurationByName(name: String): LLMConfiguration? {
        return configurations.find { it.name == name }
    }
}
