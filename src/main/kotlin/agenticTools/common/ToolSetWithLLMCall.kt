package org.example.agenticTools.common

import ai.koog.agents.core.tools.reflect.ToolSet
import ai.koog.prompt.dsl.prompt
import org.example.commonTools.fetchTokens
import org.example.environment.ExperimentEnvironment
import org.example.environment.dumpHistory
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.div

abstract class ToolSetWithLLMCall(
    protected val env: ExperimentEnvironment,
    protected val toolDir: Path
): ToolSet {

    protected suspend fun callLLM(promptName: String, systemPromptPath: String, userPrompt: String, log: Boolean = true): String {

        val systemPrompt = Files.readString(toolDir / systemPromptPath)
        val userPrompt = env.historyManager.fetchHistory() + userPrompt
        if (systemPrompt.isEmpty()) {
            throw IllegalStateException("system prompt empty")
        }
        if (userPrompt.isEmpty()) {
            throw IllegalStateException("user prompt empty")
        }

        val responses = env.promptExecutor.execute(
            prompt = prompt(promptName) {
                system(systemPrompt)
                user(userPrompt)
            }, model = env.model, tools = emptyList()
        )

        env.LLMQueriesTokens += responses.sumOf { fetchTokens(it) ?: 0.0 }
        println("LLM Query Token Count: ${env.LLMQueriesTokens}")

        val response = responses[0].content
        if (log) {
            env.historyManager.addAgentRequest(userPrompt)
            env.historyManager.addLLMResponse(response)
            env.dumpHistory()
        }

        return response
    }
}