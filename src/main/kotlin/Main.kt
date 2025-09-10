package org.example

import ai.jetbrains.code.prompt.executor.clients.grazie.koog.model.GrazieEnvironment
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.openai.OpenAIModels

import ai.jetbrains.code.prompt.llm.JetBrainsAIModels
import ai.jetbrains.code.prompt.executor.clients.grazie.koog.model.GrazieEnvironment.Staging
import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.llm.LLModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.example.agents.TestResult
import org.example.commonTools.getTool
import org.example.config.AgenticTools
import org.example.config.CliConfig
import org.example.config.Modes
import org.example.config.cliParse
import org.example.strategies.getDefaultStrategy
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.*

fun runBenchmark(
    mode: Modes,
    historyPath: Path,
    file: Path,
    toolsArgs: Set<AgenticTools>,
    promptDir: Path,
    cliConfig: CliConfig,
) : Int = runBlocking {
    val code = file.readText()
    val testResult = TestResult(code, false, null, 0)

    val tools = toolsArgs.map { getTool(mode, file, it, promptDir, cliConfig) }
    val strategy = getDefaultStrategy(testResult, tools, historyPath, promptDir, cliConfig, file.nameWithoutExtension)
    val promptExecutor = SingleLLMPromptExecutor(OpenAILLMClient(cliConfig.token))
    val systemPrompt = (promptDir / "systemAgent.txt").readText().replace("{ framework }", cliConfig.filterByExt.name)
    val agent = AIAgent(
        executor = promptExecutor,
        llmModel = cliConfig.llmProfile.model,
        strategy = strategy,
        maxIterations = 100,
        systemPrompt = systemPrompt
    )

    agent.run(code)
    if (!testResult.success) {
        testResult.try_ = -1
    }
    testResult.try_
}

fun main(args: Array<String>) = runBlocking {

    args.forEach {
        println(it)
    }
    val config = cliParse(args)

    println(config)

    val benchmarks = Files.newDirectoryStream(config.dir, "*.py").toList()

    for ((idx, modePromptPair) in config.modes.zip(config.promptsDirectories).withIndex()) {
        val (mode, promptDir) = modePromptPair
        val tools = config.toolsPerMode[mode] ?: error("tools for $mode weren't found")

        val modeResultsPath = config.resultsPath / "results_$mode$idx"

        for (run in 1..config.runs) {

            val historyPath = modeResultsPath / "$idx${mode}_history"
            historyPath.createDirectories()

            val resultsPath = modeResultsPath / "$idx${mode}_results.json"
            if (!resultsPath.exists()) {
                resultsPath.createFile()
            } else if (!resultsPath.isRegularFile()) {
                throw Exception("$resultsPath should be a file")
            }

            val limitedDispatcher = Dispatchers.IO.limitedParallelism(config.maxJobs)

            resultsPath.toFile().printWriter().use { writer ->
                val results = mutableMapOf<String, Int>()

                val jobs = benchmarks.map { benchmark ->
                    launch(limitedDispatcher) {
                        val result = runBenchmark(mode, historyPath, benchmark, tools, promptDir, config)
                        results[benchmark.name] = result
                        val mapJson = Json.encodeToString(MapSerializer(String.serializer(), Int.serializer()),
                            results
                        )
                        synchronized(resultsPath) {
                            writer.write(mapJson)
                        }
                    }
                }

                jobs.joinAll()
            }
        }
    }
}