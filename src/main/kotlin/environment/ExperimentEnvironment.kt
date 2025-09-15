package org.example.environment

import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import java.nio.file.Path

data class ExperimentEnvironment(
    val historyManager: HistoryManager,
    val promptDir: Path,
    val promptExecutor: PromptExecutor,
    val model: LLModel,
    val taskDescription: String?,
)
