package org.jetbrains.plugins.featurefilegenerator

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import java.io.File

@State(
    name = "LLMSettings",
    storages = [Storage("LLMSettings.xml")]
)
class LLMSettings : PersistentStateComponent<LLMSettings.State> {

    private var myState: State = State()

    class State {
        var configurations: MutableList<LLMConfiguration> = mutableListOf()
    }

    data class LLMConfiguration(
        var name: String = "",
        var scriptFilePath: String = "",
        var parameterSpecFilePath: String = ""
    )

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

    // Método para adicionar uma nova configuração de LLM
    fun isValidFilePath(path: String): Boolean {
        return File(path).exists()
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


    // Método para remover uma configuração de LLM
    fun removeConfiguration(config: LLMConfiguration) {
        myState.configurations.remove(config)
    }

    // Método para obter todas as configurações de LLM
    fun getConfigurations(): List<LLMConfiguration> {
        return myState.configurations
    }

    // Método para atualizar uma configuração existente
    fun updateConfiguration(oldConfig: LLMConfiguration, newConfig: LLMConfiguration) {
        val index = myState.configurations.indexOf(oldConfig)
        if (index != -1) {
            myState.configurations[index] = newConfig
        }
    }

    // Método para buscar uma configuração por nome
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
}
