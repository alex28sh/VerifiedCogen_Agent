package org.example

import ai.grazie.api.gateway.client.SuspendableAPIGatewayClient
import ai.grazie.client.common.SuspendableClientWithBackoff
import ai.grazie.client.common.SuspendableHTTPClient
import ai.grazie.client.ktor.GrazieKtorHTTPClient
import ai.grazie.model.auth.GrazieAgent
import ai.grazie.model.auth.v5.AuthData
import ai.jetbrains.code.prompt.executor.clients.grazie.koog.GrazieLLMClient

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.example.agents.TestResult
import org.example.commonTools.getTool
import org.example.config.*
import org.example.languages.AnnotationTypes
import org.example.strategies.getDefaultStrategy
import org.example.verifierTools.*
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
    val language = cliConfig.filterByExt.ctor(mode.removeAnnotations)
    val code = language.removeMarkup(file.readText())
    val testResult = TestResult(code, false, null, 0)

    val promptExecutor = SingleLLMPromptExecutor(
        GrazieLLMClient(
            SuspendableAPIGatewayClient(
                serverUrl = "https://api.app.stgn.grazie.aws.intellij.net/",
                httpClient = SuspendableHTTPClient.WithV5(
                    SuspendableClientWithBackoff(
                        GrazieKtorHTTPClient.Client.WithExtendedTimeout,
                    ), AuthData(
                        token = cliConfig.token,
                        grazieAgent = GrazieAgent("verified-cogen-agent", "dev")
                    )
                )
            )
        )
    )
//        SingleLLMPromptExecutor(OpenAILLMClient(cliConfig.token))

    val tools = toolsArgs.map { getTool(mode, file, it, promptDir, cliConfig, promptExecutor) }

    tools.forEach { println(it.name) }

    val verifier = Verifier(cliConfig.verifierCommand)
    var checker: ResponseChecker = EmptyChecker()
    println(cliConfig.checkers)
    for (checkerArt in cliConfig.checkers) {
        checker = when(checkerArt) {
            CheckerArt.ProofSufficiency ->
                ProofSufficiencyChecker(verifier, promptDir, checker)
            CheckerArt.ConditionsFormalEquality ->
                ConditionsFormalEqualityVerifier(
                    verifier,
                    promptDir,
                    language,
                    code,
                    AnnotationTypes.PURE in mode.removeAnnotations,
                    checker
                )
        }
    }

//    println(checker.checkResponseFolded())

    val strategy = getDefaultStrategy(
        testResult,
        tools,
        historyPath,
        cliConfig,
        file.nameWithoutExtension,
        checker
    )

    val systemPrompt = (promptDir / "systemAgent.txt").readText().replace("{ framework }", cliConfig.filterByExt.name)
    val agent = AIAgent(
        executor = promptExecutor,
        llmModel = cliConfig.llmProfile.model,
        strategy = strategy,
        maxIterations = 100,
        systemPrompt = systemPrompt,
        toolRegistry = ToolRegistry {
            tools(tools)
        }
    )

    agent.run(code)
    if (!testResult.success) {
        testResult.try_ = -1
    }
    testResult.try_
}

private val json = Json { prettyPrint = true }

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

        val modeResultsPath = config.resultsPath / "results_${idx}_$mode"
        val atLeastOnce = benchmarks.associate { it.name to -1 }.toMutableMap()

        for (run in 1..config.runs) {

            val runPath = modeResultsPath / "run${run}"

            val historyPath = runPath / "history"
            historyPath.createDirectories()

            val resultsPath = runPath / "${idx}_${mode}_${run}_results.json"
            if (!resultsPath.exists()) {
                resultsPath.createFile()
            } else if (!resultsPath.isRegularFile()) {
                throw Exception("$resultsPath should be a file")
            }

            val limitedDispatcher = Dispatchers.IO.limitedParallelism(config.maxJobs)

            val lock = Any()
            val results = mutableMapOf<String, Int>()

            val jobs = benchmarks.map { benchmark ->
                launch(limitedDispatcher) {
                    val result = runBenchmark(mode, historyPath, benchmark, tools, promptDir, config)
                    println("${benchmark.name}: $result")

                    synchronized(lock) {
                        results[benchmark.name] = result
                        val mapJson = json.encodeToString(
                            MapSerializer(String.serializer(), Int.serializer()),
                            results
                        )
                        resultsPath.writeText(mapJson) // truncates and overwrites
                    }
                }
            }
            jobs.joinAll()

            results.filter { it.value != -1 }.keys.forEach { atLeastOnce[it] = 1 }
        }

        val atLeastOnceFile = modeResultsPath / "atLeastOnce.json"
        if (!atLeastOnceFile.exists()) {
            atLeastOnceFile.createFile()
        } else if (!atLeastOnceFile.isRegularFile()) {
            throw Exception("$atLeastOnceFile should be a file")
        }

        val atLeastOnceJson = json.encodeToString(
            MapSerializer(String.serializer(), Int.serializer()),
            atLeastOnce
        )
        atLeastOnceFile.writeText(atLeastOnceJson)
    }
}