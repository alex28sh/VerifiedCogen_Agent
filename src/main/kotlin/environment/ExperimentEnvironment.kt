package org.example.environment

import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import java.nio.file.Path
import kotlin.io.path.appendText

data class ExperimentEnvironment(
    val historyManager: HistoryManager,
    val promptDir: Path,
    val promptExecutor: PromptExecutor,
    val model: LLModel,
    val taskDescription: String?,
    val conversationDump: Path,
)

fun ExperimentEnvironment.dumpHistory() {
    conversationDump.appendText(historyManager.fetchHistory())
}