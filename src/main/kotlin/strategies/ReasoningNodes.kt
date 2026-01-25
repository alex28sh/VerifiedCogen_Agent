package org.example.strategies

import ai.koog.agents.core.dsl.builder.AIAgentGraphStrategyBuilder
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.tools.Tool
import ai.koog.prompt.executor.model.PromptExecutor
import org.example.environment.ExperimentEnvironment

fun AIAgentGraphStrategyBuilder<String, String>.getDefaultReasoningNode(
) = subgraph<String, String>(
    name = "reasoning"
) {
    edge((nodeStart forwardTo nodeFinish)
        transformed { it }
    )
}

fun getActionsToPerform(
    tools: List<Tool<*, *>>,
    code: String,
    error: String?,
    promptExecutor: PromptExecutor,
): String {
    val toolDescriptions = tools.map { it.name to it.description }

    return ""
}

fun AIAgentGraphStrategyBuilder<String, String>.getReasoningNode(
    env: ExperimentEnvironment,
    tools: List<Tool<*, *>>
) = subgraph<String, String>(
    tools = tools,
    name = "reasoning"
) {
    edge((nodeStart forwardTo nodeFinish)
        transformed {
            val error = env.lastTestResult.error
            val code = env.lastTestResult.generatedCode
            val actionsToPerform = getActionsToPerform(tools, code, error, env.promptExecutor)
            actionsToPerform
        }
    )
}

