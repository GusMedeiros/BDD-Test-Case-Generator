package org.jetbrains.plugins.featurefilegenerator.actions

import ChatGPTService
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import org.jetbrains.plugins.featurefilegenerator.LLMService

class GenerateFeatureFileAction : AnAction() {

    private val llmService: LLMService = ChatGPTService()

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val generator = LLMServiceManager(project, llmService)
        generator.generate(event)
    }
}
