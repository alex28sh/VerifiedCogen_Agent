package org.example.strategies

import ai.koog.agents.core.dsl.builder.AIAgentGraphStrategyBuilder
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.extension.nodeExecuteMultipleTools
import ai.koog.agents.core.dsl.extension.nodeExecuteTool
import ai.koog.agents.core.dsl.extension.nodeLLMRequest
import ai.koog.agents.core.dsl.extension.nodeLLMRequestMultiple
import ai.koog.agents.core.dsl.extension.nodeLLMSendMultipleToolResults
import ai.koog.agents.core.dsl.extension.nodeLLMSendToolResult
import ai.koog.agents.core.dsl.extension.onAssistantMessage
import ai.koog.agents.core.dsl.extension.onMultipleToolCalls
import ai.koog.agents.core.dsl.extension.onToolCall
import ai.koog.agents.core.tools.Tool
import kotlin.collections.firstOrNull

fun AIAgentGraphStrategyBuilder<String, String>.getSingleRepairNode(
    tools: List<Tool<*, *>>
) = subgraph<String, String>(
    tools = tools,
    name = "generate-code"
) {
    val nodeCallLLM by nodeLLMRequest()
    val nodeExecuteTool by nodeExecuteTool()
    val nodeSendToolResult by nodeLLMSendToolResult()
    edge(nodeStart forwardTo nodeCallLLM)

    edge(
        (nodeCallLLM forwardTo nodeFinish)
                onAssistantMessage { true }
    )

    edge(
        (nodeCallLLM forwardTo nodeExecuteTool)
                onToolCall { true }
    )

    edge(nodeExecuteTool forwardTo nodeSendToolResult)

    edge(
        (nodeSendToolResult forwardTo nodeExecuteTool)
                onToolCall { true }
    )

    edge(
        (nodeSendToolResult forwardTo nodeFinish)
                onAssistantMessage { true }
    )
}

fun AIAgentGraphStrategyBuilder<String, String>.getMultipleRepairNode(
    tools: List<Tool<*, *>>
) = subgraph<String, String>(
    tools = tools,
    name = "generate-code"
) {
    val nodeCallLLM by nodeLLMRequestMultiple()
    val nodeExecuteToolMultiple by nodeExecuteMultipleTools(parallelTools = true)
    val nodeSendToolResultMultiple by nodeLLMSendMultipleToolResults()
    edge(nodeStart forwardTo nodeCallLLM)

    edge(
        (nodeCallLLM forwardTo nodeFinish)
                transformed { it.firstOrNull { s -> s.content.isNotEmpty() } ?: "default message" }
                onAssistantMessage { true }
    )

    edge(
        (nodeCallLLM forwardTo nodeExecuteToolMultiple)
                onMultipleToolCalls { true }
    )

    edge(nodeExecuteToolMultiple forwardTo nodeSendToolResultMultiple)

    edge(
        (nodeSendToolResultMultiple forwardTo nodeExecuteToolMultiple)
                onMultipleToolCalls { true }
    )

    edge(
        (nodeSendToolResultMultiple forwardTo nodeFinish)
                transformed { it.firstOrNull { s -> s.content.isNotEmpty() } ?: "default message" }
                onAssistantMessage { true }
    )
}
