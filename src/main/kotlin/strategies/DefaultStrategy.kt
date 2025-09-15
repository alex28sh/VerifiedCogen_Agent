package org.example.strategies

import ai.koog.agents.core.agent.entity.AIAgentStrategy
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.*
import ai.koog.agents.core.tools.Tool
import org.example.agents.TestResult
import org.example.config.CliConfig
import org.example.environment.ExperimentEnvironment
import org.example.verifierTools.ResponseChecker
import java.nio.file.Path
import kotlin.io.path.*

fun getDefaultStrategy(
    env: ExperimentEnvironment,
    tools: List<Tool<*, *>>,
    historyPath: Path,
    cliConfig: CliConfig,
    name: String,
    responseChecker: ResponseChecker,
) : AIAgentStrategy<String, String> {

    val storingPath = historyPath / "running"
    storingPath.createDirectories()

    return strategy("verified-cogen-strategy") {
        val repairNode by subgraph<String, String>(
            tools = tools,
            name = "generate-code"
        ) {
            val nodeCallLLM by nodeLLMRequestMultiple()
            val nodeExecuteToolMultiple by nodeExecuteMultipleTools(parallelTools = true)
            val nodeSendToolResultMultiple by nodeLLMSendMultipleToolResults()
            edge(nodeStart forwardTo nodeCallLLM)

            edge(
                (nodeCallLLM forwardTo nodeFinish)
                transformed { it.first() }
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
                transformed { it.first() }
                onAssistantMessage { true }
            )
        }

        val checkNode by subgraph<String, String>(
            name = "verify-code"
        ) {
            edge((nodeStart forwardTo nodeFinish)
                transformed { code ->
//                    testResult.lastTestResult.generatedCode = code
                    env.lastTestResult.try_++

                    val file = storingPath / (name + "_" + env.lastTestResult.try_ + "." + cliConfig.filterByExt.strRepl)
                    file.writeText(code)

                    responseChecker.checkResponseFolded(file).also { (success, error) ->
                        env.lastTestResult.success = success
                        env.lastTestResult.error = error
                    }

                    println("Checker: ${file.name} ${env.lastTestResult.success}")

                    code
                }
            )
        }

        edge(nodeStart forwardTo repairNode)
        edge(repairNode forwardTo checkNode)
        edge((checkNode forwardTo nodeFinish)
            onCondition { env.lastTestResult.try_ ==  cliConfig.tries || env.lastTestResult.success }
        )
        edge((checkNode forwardTo repairNode)
            onCondition { env.lastTestResult.try_ !=  cliConfig.tries && !env.lastTestResult.success }
        )
    }
}