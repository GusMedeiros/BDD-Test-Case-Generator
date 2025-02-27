package org.jetbrains.plugins.featurefilegenerator.cli

import kotlinx.coroutines.runBlocking
import org.jetbrains.plugins.featurefilegenerator.executor.LLMExecutor

object BatchGenerateFeatureCLI {

    @JvmStatic


    fun main(args: Array<String>) = runBlocking {
        // Verifica se o argumento --config foi informado e obtém o caminho
        val configIndex = args.indexOf("--config")
        if (configIndex < 0 || configIndex + 1 >= args.size) {
            println("Arquivo de configuração não informado.")
            return@runBlocking
        }
        val configFilePath = args[configIndex + 1]
        val inputFilePath = args.last()  // supondo que o último argumento seja o arquivo de entrada

        // Instancia a configuração exclusiva para CLI
        val llmSettings = LLMSettingsCLI(configFilePath)
        val executor = LLMExecutor(llmSettings)

        println("? Executando todas as LLMs configuradas...")
        // Chama o método específico para CLI (veja abaixo)
        executor.executeBatchCli(inputFilePath) { llmName, result ->
            println("LLM: $llmName retornou:\n$result")
        }
    }


}
