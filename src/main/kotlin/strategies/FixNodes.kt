package org.example.strategies

import ai.koog.agents.core.dsl.builder.AIAgentGraphStrategyBuilder
import ai.koog.agents.core.dsl.builder.forwardTo
import org.example.environment.ExperimentEnvironment

fun AIAgentGraphStrategyBuilder<String, String>.getDefaultFixNode(
    env: ExperimentEnvironment,
) = subgraph<String, String>(
    name = "fixNode"
) {
    edge (nodeStart forwardTo nodeFinish)
}

fun AIAgentGraphStrategyBuilder<String, String>.getFixNode(
    env: ExperimentEnvironment,
) = subgraph<String, String>(
    name = "fixNode"
) {
    edge ((nodeStart forwardTo nodeFinish)
        transformed { msg ->
            val code = env.lastTestResult.generatedCode
            val language = env.ext.ctor(emptyList()) // Or another way to get the language instance
            env.lastTestResult.generatedCode = language.fixSyntaxErrors(code)
            msg
        }
    )
}

