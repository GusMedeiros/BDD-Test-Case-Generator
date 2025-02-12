package org.jetbrains.plugins.featurefilegenerator

import javax.swing.JOptionPane

class LlmRunner {

    fun runCommand(command: String): Pair<Boolean, String> {
        return try {
            val processBuilder = ProcessBuilder(command.split(" "))
            val process = processBuilder.start()

            // Captura a saída padrão e de erro
            val output = process.inputStream.bufferedReader().readText()
            val errorOutput = process.errorStream.bufferedReader().readText()

            // Aguarda o término do processo e verifica o código de saída
            val exitCode = process.waitFor()
            val success = exitCode == 0

            // Retorna um par indicando sucesso e a saída correspondente
            Pair(success, if (success) output else errorOutput)
        } catch (e: Exception) {
            // Em caso de exceção, retorna falha com a mensagem de erro
            Pair(false, "Erro ao executar comando: ${e.message}")
        }
    }

    fun showResultDialog(result: Pair<Boolean, String>) {
        val (success, message) = result
        val dialogMessage = if (success) {
            "Sucesso:\n$message"
        } else {
            "Falha:\n$message"
        }
        JOptionPane.showMessageDialog(null, dialogMessage, "Resultado do Comando", JOptionPane.INFORMATION_MESSAGE)
    }
}
