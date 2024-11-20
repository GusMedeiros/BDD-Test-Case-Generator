package org.jetbrains.plugins.featurefilegenerator.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import org.jetbrains.plugins.featurefilegenerator.UIPanel

class ChangeConfigsAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val configurator = Configurator(project)
        val dialog = UIPanel(project)

        configurator.showDialogAndConfigure(dialog, configurator)
    }
}
