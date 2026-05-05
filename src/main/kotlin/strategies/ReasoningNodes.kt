package org.example.strategies

import ai.koog.agents.core.dsl.builder.AIAgentGraphStrategyBuilder
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.tools.Tool
import ai.koog.prompt.dsl.prompt
import org.example.commonTools.fetchTokens
import org.example.environment.ExperimentEnvironment
import org.example.environment.dumpHistory

fun AIAgentGraphStrategyBuilder<String, String>.getPlanningNode(
    env: ExperimentEnvironment,
    tools: List<Tool<*, *>>,
) = subgraph<String, String>(
    name = "planning"
) {
    edge((nodeStart forwardTo nodeFinish)
        transformed { _ ->
            val toolDescriptions = tools.joinToString("\n") { tool ->
                "- ${tool.name}: ${tool.descriptor.description}"
            }
            val userPrompt = """
                You are given a program that failed verification.

                Current program:
                ${env.lastTestResult.generatedCode}

                Verification error:
                ${env.lastTestResult.error}

                Available tools:
                $toolDescriptions

                Based on the error and the program, produce a concrete plan of tool calls to fix the verification.
                For each step, name the tool and briefly explain why it should be called.
                Output only the numbered plan, nothing else.
            """.trimIndent()

            val responses = env.promptExecutor.execute(
                prompt = prompt("planning") {
                    system("You are an expert in program verification. Your task is to plan the minimal sequence of tool calls needed to fix a verification failure.")
                    user(userPrompt)
                },
                model = env.model,
                tools = emptyList()
            )

            env.LLMQueriesTokens += responses.sumOf { fetchTokens(it) ?: 0.0 }

            val plan = responses[0].content
            env.historyManager.addAgentRequest(userPrompt)
            env.historyManager.addLLMResponse(plan)
            env.dumpHistory()

            plan
        }
    )
}