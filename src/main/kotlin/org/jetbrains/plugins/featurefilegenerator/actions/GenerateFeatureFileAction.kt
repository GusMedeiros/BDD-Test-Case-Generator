package org.jetbrains.plugins.featurefilegenerator.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.plugins.featurefilegenerator.LlmRunner

class GenerateFeatureFileAction : AnAction() {


    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val runner = LlmRunner()
        val command = "placeholder chamada UI" // Substitua pelo comando desejado

        // Chamada assíncrona usando corrotinas
        CoroutineScope(Dispatchers.Main).launch {
            val (success, output) = withContext(Dispatchers.IO) {
                runner.runCommand(command) // Executa o comando na thread de I/O
            }

            // Exibe a mensagem na interface gráfica
            val message = if (success) {
                "Comando executado com sucesso:\n$output"
            } else {
                "Erro ao executar comando:\n$output"
            }

            Messages.showMessageDialog(project, message, "Resultado do Comando", Messages.getInformationIcon())
        }
    }
}
