package org.example

import ai.jetbrains.code.prompt.executor.clients.grazie.koog.model.GrazieEnvironment
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.openai.OpenAIModels

import ai.jetbrains.code.prompt.llm.JetBrainsAIModels
import ai.jetbrains.code.prompt.executor.clients.grazie.koog.model.GrazieEnvironment.Staging
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import kotlinx.coroutines.runBlocking
import org.example.config.cliParse

fun main(args: Array<String>) {

    args.forEach {
        println(it)
    }
    val config = cliParse(args)

    println(config)
}