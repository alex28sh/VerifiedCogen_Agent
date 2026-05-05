package org.example.environment

import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import org.example.agents.TestResult
import org.example.config.Extensions
import java.nio.file.Path
import kotlin.io.path.writeText

data class Checkpoint(
    val label: String,
    val code: String,
    val success: Boolean,
    val structuredError: String?,
)

data class ExperimentEnvironment(
    val ext: Extensions,
    val historyManager: HistoryManager,
    val promptDir: Path,
    val promptExecutor: PromptExecutor,
    val model: LLModel,
    val taskDescription: String?,
    val conversationDump: Path,
    val errorPath: Path,
    val lastTestResult: TestResult,
    val startingCode: String,
    var LLMQueriesTokens: Double = 0.0,
    var agentTokens: Double = 0.0,
    val checkpoints: MutableList<Checkpoint> = mutableListOf(),
)

fun ExperimentEnvironment.dumpHistory() {
    conversationDump.writeText(historyManager.fetchFullHistory())
}