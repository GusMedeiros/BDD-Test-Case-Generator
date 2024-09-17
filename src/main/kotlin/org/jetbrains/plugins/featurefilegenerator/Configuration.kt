package org.jetbrains.plugins.featurefilegenerator

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import org.jetbrains.annotations.Nullable

@Service(Service.Level.PROJECT)
@State(name = "com.plugin.settings.MyPluginSettings", storages = [Storage("UserSettings.xml")])
class UserSettings : PersistentStateComponent<UserSettings.State> {

    // Classe interna para armazenar o estado
    data class State(
        var apiKey: String = "",
        var outputDirPath: String = "",
        var temperature: Double = 0.0,
        var seed: Int = 0,
        var gptModel: String = "",
        var debug: Boolean = false
    )

    private var state = State()

    // Retorna o estado atual
    @Nullable
    override fun getState(): State {
        return state
    }

    // Carrega o estado salvo
    override fun loadState(state: State) {
        this.state = state
    }

    // Métodos setters
    fun setOutputDirPath(path: String) {
        state.outputDirPath = path
    }

    fun setDebug(debug: Boolean) {
        state.debug = debug
    }

    fun setTemperature(temperature: Double) {
        state.temperature = temperature
    }

    fun setApiKey(apiKey: String) {
        state.apiKey = apiKey
    }

    fun setSeed(seed: Int) {
        state.seed = seed
    }

    fun setGptModel(gptModel: String) {
        state.gptModel = gptModel
    }

    // Métodos getters
    fun getOutputDirPath() = state.outputDirPath
    fun isDebug() = state.debug
    fun getTemperature() = state.temperature
    fun getApiKey() = state.apiKey
    fun getSeed() = state.seed
    fun getGptModel() = state.gptModel
}
