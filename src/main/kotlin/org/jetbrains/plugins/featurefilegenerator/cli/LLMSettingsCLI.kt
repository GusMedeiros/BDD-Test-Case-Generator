package org.jetbrains.plugins.featurefilegenerator.cli

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Classe responsável por ler as configurações das LLMs a partir de um arquivo JSON fornecido pelo usuário.
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
        @SerialName("string") // Mapeia o "type": "string" para esta classe
        data class StringParam(
            override val argName: String,
            val value: String
        ) : NamedParameter()

        @Serializable
        @SerialName("int") // Exemplo para inteiro
        data class IntParam(
            override val argName: String,
            val value: Int
        ) : NamedParameter()

        @Serializable
        @SerialName("boolean") // Exemplo para booleano
        data class BooleanParam(
            override val argName: String,
            val value: Boolean
        ) : NamedParameter()

        @Serializable
        @SerialName("double") // Exemplo para double
        data class DoubleParam(
            override val argName: String,
            val value: Double
        ) : NamedParameter()
    }


    @Serializable
    data class ConfigFile(val llms: List<LLMConfiguration>)

    private val configurations: List<LLMConfiguration> = parseJsonConfig(configFilePath)

    /**
     * Lê e analisa o JSON fornecido pelo usuário.
     */
    private fun parseJsonConfig(filePath: String): List<LLMConfiguration> {
        val file = File(filePath)
        return try {
            val jsonContent = file.readText()
            Json.decodeFromString<ConfigFile>(jsonContent).llms
        } catch (e: Exception) {
            throw IllegalArgumentException("Erro ao ler o arquivo JSON: ${e.message}")
        }
    }

    /**
     * Obtém todas as configurações de LLMs.
     */
    fun getConfigurations(): List<LLMConfiguration> = configurations

    /**
     * Obtém uma configuração específica pelo nome.
     */
    fun getConfigurationByName(name: String): LLMConfiguration? {
        return configurations.find { it.name == name }
    }
}
