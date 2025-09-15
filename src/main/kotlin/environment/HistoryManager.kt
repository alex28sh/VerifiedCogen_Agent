package org.example.environment

import java.nio.file.Path
import kotlin.io.path.div
import kotlin.io.path.readText

class HistoryManager(promptDir: Path, framework: String) {

    val builder: StringBuilder

    init {
        val systemAgentPrompt = (promptDir / "systemAgent.txt").readText().replace("{ framework }", framework)
        builder = StringBuilder(
            """
            Here is some history of previous conversation of an agent, that called you.
            The agent was given the following system prompt:
            $systemAgentPrompt
            Then followed an agent conversation of agent with tools and LLMs:
            """.trimIndent()
        )
    }

    fun addAgentRequest(prompt: String) {
        builder.append(
            """
               Agent request:
               $prompt
            """.trimIndent()
        )
    }

    fun addLLMResponse(prompt: String) {
        builder.append(
            """
               LLM response:
               $prompt
            """.trimIndent()
        )
    }

    fun addToolResponse(prompt: String) {
        builder.append(
            """
               Tool response:
               $prompt
            """.trimIndent()
        )
    }

    fun fetchHistory(): String {
        return "$builder\n\nThe current request is:\n"
    }
}