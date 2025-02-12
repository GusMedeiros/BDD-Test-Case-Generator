package org.jetbrains.plugins.featurefilegenerator.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.DialogWrapper
import org.jetbrains.plugins.featurefilegenerator.LLMConfigurationPanel
import javax.swing.JComponent

class ChangeConfigsAction : AnAction() {

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return

        // Cria e exibe o diálogo
        val dialog = object : DialogWrapper(project) {
            init {
                title = "Configurações de LLM"
                init()
            }

            override fun createCenterPanel(): JComponent? {
                return LLMConfigurationPanel()
            }
        }
        dialog.show()
    }
}
