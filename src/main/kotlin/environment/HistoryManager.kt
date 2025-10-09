package org.example.environment

import java.nio.file.Path
import kotlin.io.path.div
import kotlin.io.path.readText

class HistoryManager(
    promptDir: Path,
    framework: String,
    private val maxHistoryChars: Int = System.getenv("MAX_HIST_CHARS")?.toInt() ?: 2_000, // tune: ~8k tokens (≈4 chars/token)
) {
    private val parts = ArrayDeque<String>()
    private var currentChars = 0

    init {
        val systemAgentPrompt = (promptDir / "systemAgent.txt").readText().replace("{ framework }", framework)
        val header = """
            Here is some history of previous conversation of an agent, that called you.
            The agent was given the following system prompt:
            $systemAgentPrompt
            Then followed an agent conversation of agent with tools and LLMs:
        """.trimIndent()
        appendPart(header)
    }

    private fun appendPart(text: String) {
        parts.addLast(text)
        currentChars += text.length
        // Evict oldest until under budget
        while (currentChars > maxHistoryChars && parts.size > 1) { // keep at least header
            val removed = parts.removeFirst()
            currentChars -= removed.length
        }
    }

    fun addAgentRequest(prompt: String) {
        appendPart("""
               Agent request:
               $prompt
        """.trimIndent())
    }

    fun addLLMResponse(prompt: String) {
        appendPart("""
               LLM response:
               $prompt
        """.trimIndent())
    }

    fun addToolResponse(prompt: String) {
        appendPart("""
               Tool response:
               $prompt
        """.trimIndent())
    }

    fun fetchHistory(): String {
        val joined = parts.joinToString(separator = "\n")
        return "$joined\n\nThe current request is:\n"
    }
}