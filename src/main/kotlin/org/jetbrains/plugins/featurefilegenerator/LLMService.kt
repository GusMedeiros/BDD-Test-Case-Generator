package org.jetbrains.plugins.featurefilegenerator

interface LLMService {
    fun generateFeatureFile(userStoryPath: String, settings: UserSettings.State): Pair<Boolean, String>
}
