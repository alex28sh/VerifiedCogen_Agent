package org.example.strategies

import agenticTools.common.ErrorsToolSet
import agenticTools.nagini.NaginiErrorsToolSet
import ai.koog.agents.core.dsl.builder.AIAgentGraphStrategyBuilder
import ai.koog.agents.core.dsl.builder.forwardTo
import org.example.config.Extensions
import org.example.environment.ExperimentEnvironment

fun AIAgentGraphStrategyBuilder<String, String>.getCodeSnippetAndErrorExplainerNode(
    env: ExperimentEnvironment,
) = subgraph<String, String>(
    name = "get-code-snippet-and-explain-error"
) {
    edge ((nodeStart forwardTo nodeFinish)
        transformed { error ->
            val errorWithCodeSnippet: String = if (env.ext == Extensions.Nagini) {
                NaginiErrorsToolSet(env).addCodeSnippet()
            } else {
                error
            }
            env.lastTestResult.error = errorWithCodeSnippet
            ErrorsToolSet(env).addErrorExplanation()
            env.lastTestResult.error ?: errorWithCodeSnippet
        }
    )
}

fun AIAgentGraphStrategyBuilder<String, String>.getDefaultCodeSnippetAndErrorExplainerNode(
) = subgraph<String, String>(
    name = "get-code-snippet-and-explain-error"
) {
    edge((nodeStart forwardTo nodeFinish)
            transformed { it }
    )
}