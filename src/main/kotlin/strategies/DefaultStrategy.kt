package org.example.strategies

import ai.koog.agents.core.agent.entity.AIAgentStrategy
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.*
import ai.koog.agents.core.tools.Tool
import org.example.agents.TestResult
import org.example.config.CliConfig
import org.example.verifierTools.Verifier
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.div
import kotlin.io.path.readText
import kotlin.io.path.writeText

fun getDefaultStrategy(
    lastTestResult: TestResult,
    tools: List<Tool<*, *>>,
    historyPath: Path,
    promptDir: Path,
    cliConfig: CliConfig,
    name: String,
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

        val verifier = Verifier(cliConfig.verifierCommand)

        val checkNode by subgraph<String, String>(
            name = "verify-code"
        ) {
            edge((nodeStart forwardTo nodeFinish)
                transformed { code ->
                    lastTestResult.generatedCode = code
                    lastTestResult.try_++

                    val file = storingPath / (name + "_" + lastTestResult.try_ + "." + cliConfig.filterByExt.strRepl)
                    file.writeText(code)

                    val resVerifier = verifier.verify(file)

                    if (resVerifier == null) {
                        lastTestResult.success = false
                        lastTestResult.error = (promptDir / "timeout.txt").readText()
                    } else {
                        lastTestResult.success = resVerifier.first
                        lastTestResult.error = resVerifier.second
                    }

                    code
                }
            )
        }

        edge(nodeStart forwardTo repairNode)
        edge(repairNode forwardTo checkNode)
        edge((checkNode forwardTo nodeFinish)
            onCondition { lastTestResult.try_ ==  cliConfig.tries || lastTestResult.success }
        )
        edge((repairNode forwardTo repairNode)
            onCondition { lastTestResult.try_ !=  cliConfig.tries && !lastTestResult.success }
        )
    }
}