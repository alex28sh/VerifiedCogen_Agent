package org.example.strategies

import ai.koog.agents.core.agent.entity.AIAgentGraphStrategy
import ai.koog.agents.core.dsl.builder.AIAgentGraphStrategyBuilder
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.tools.Tool
import org.example.config.CliConfig
import org.example.environment.ExperimentEnvironment
import org.example.mcp.parseVerifierOutput
import org.example.mcp.toJsonString
import org.example.verifierTools.ResponseChecker
import java.nio.file.Path
import kotlin.io.path.*

fun AIAgentGraphStrategyBuilder<String, String>.getCheckNode(
    env: ExperimentEnvironment,
    storingPath: Path,
    cliConfig: CliConfig,
    name: String,
    responseChecker: ResponseChecker,
) = subgraph<String, String>(
    name = "verify-code"
) {
    edge((nodeStart forwardTo nodeFinish)
        transformed { _ ->
            env.lastTestResult.try_++

            val file = storingPath / (name + "_" + env.lastTestResult.try_ + "." + cliConfig.filterByExt.strRepl)
            val fileMsg = storingPath / (name + "_" + env.lastTestResult.try_ + ".txt")
            file.writeText(env.lastTestResult.generatedCode)

            responseChecker.checkResponseFolded(file).also { (success, rawError) ->
                env.lastTestResult.success = success
                env.lastTestResult.rawError = rawError
                val structured = parseVerifierOutput(success, rawError, env.ext)
                val structuredJson = structured.toJsonString()
//                env.lastTestResult.error = structuredJson
                env.lastTestResult.error = rawError
                fileMsg.writeText(rawError + "\n\n--- Structured ---\n" + structuredJson)
            }

            println("Checker: ${file.name} ${env.lastTestResult.success}")

            """
                Verification finished with the following output:
                ${env.lastTestResult.error}
            """.trimIndent()
        }
    )
}

fun getDefaultStrategy(
    env: ExperimentEnvironment,
    tools: List<Tool<*, *>>,
    storingPath: Path,
    cliConfig: CliConfig,
    name: String,
    responseChecker: ResponseChecker,
) : AIAgentGraphStrategy<String, String> {

    return strategy("verified-cogen-strategy") {
        val repairNode by getSingleRepairNode(tools)

        val checkNode by getCheckNode(env, storingPath, cliConfig, name, responseChecker)

        val codeSnippetAndErrorExplainerNode by getCodeSnippetAndErrorExplainerNode(env)

        val planningNode by getPlanningNode(env, tools)

        val syntaxFixNode by getFixNode(env)

        edge(nodeStart forwardTo repairNode)
        edge(repairNode forwardTo syntaxFixNode)
        edge(syntaxFixNode forwardTo checkNode)

//        edge(repairNode forwardTo checkNode)
        edge((checkNode forwardTo nodeFinish)
            onCondition { env.lastTestResult.try_ ==  cliConfig.tries || env.lastTestResult.success }
        )
        edge((checkNode forwardTo codeSnippetAndErrorExplainerNode)
            onCondition { env.lastTestResult.try_ !=  cliConfig.tries && !env.lastTestResult.success }
        )
        edge(codeSnippetAndErrorExplainerNode forwardTo planningNode)
        edge(planningNode forwardTo repairNode)
    }
}