package org.example

import ai.grazie.api.gateway.client.SuspendableAPIGatewayClient
import ai.grazie.client.common.SuspendableClientWithBackoff
import ai.grazie.client.common.SuspendableHTTPClient
import ai.grazie.client.ktor.GrazieKtorHTTPClient
import ai.grazie.model.auth.GrazieAgent
import ai.grazie.model.auth.v5.AuthData
import ai.grazie.model.cloud.AuthType
import ai.jetbrains.code.prompt.executor.clients.grazie.koog.GrazieLLMClient

import ai.koog.prompt.executor.clients.retry.RetryingLLMClient
import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.features.eventHandler.feature.handleEvents
import ai.koog.agents.features.tracing.feature.Tracing
import ai.koog.agents.features.tracing.writer.*
import ai.koog.prompt.executor.clients.retry.RetryConfig
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.params.LLMParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.io.buffered
import kotlinx.io.files.Path as PathKt
import kotlinx.io.files.SystemFileSystem
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.example.LLMClients.RateLimiterLLMClient
import org.example.agents.TestResult
import org.example.commonTools.ToolFailure
import org.example.commonTools.getTool
import org.example.config.*
import org.example.environment.ExperimentEnvironment
import org.example.environment.HistoryManager
import org.example.languages.AnnotationTypes
import org.example.strategies.getDefaultStrategy
import org.example.verifierTools.*
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.*
import kotlin.time.Duration.Companion.seconds


fun regularFileCreation(path: Path): Path {
    if (!path.exists()) {
        path.createFile()
    } else if (!path.isRegularFile()) {
        throw Exception("$path should be a file")
    }
    return path
}

fun runBenchmark(
    run: Int,
    mode: Modes,
    historyPath: Path,
    file: Path,
    toolsArgs: Set<AgenticTools>,
    promptDir: Path,
    cliConfig: CliConfig,
    promptExecutor: PromptExecutor,
) : Int = runBlocking {
    val language = cliConfig.filterByExt.ctor(mode.removeAnnotations)
    val originalCode = file.readText()
    val code = language.removeMarkup(originalCode)
    val testResult = TestResult(code, false, null, 0)

    val historyManager = HistoryManager(promptDir, cliConfig.filterByExt.name)
    val description = if (mode.textDescription) {
        (file.parent / "text-descriptions" / (file.nameWithoutExtension + ".txt")).readText()
    } else {
        null
    }

    val conversationPath = historyPath / (file.nameWithoutExtension + "_conversation.txt")
    regularFileCreation(conversationPath)

    val errorPath = regularFileCreation(historyPath / "${file.nameWithoutExtension}_error.txt")

    val env = ExperimentEnvironment(
        historyManager,
        promptDir,
        promptExecutor,
        cliConfig.llmProfile.model,
        description,
        conversationPath,
        errorPath,
        testResult,
        code,
    )

    val tools = toolsArgs.map { getTool(it, env) }

    println("Tools:")
    tools.forEach { println(it.name) }

    println("Checkers:")
    println(cliConfig.checkers)

    val verifier = Verifier(cliConfig.verifierCommand)
    var checker: ResponseChecker = EmptyChecker()
    for (checkerArt in cliConfig.checkers) {
        checker = when(checkerArt) {
            CheckerArt.ProofSufficiency ->
                ProofSufficiencyChecker(verifier, promptDir, checker)
            CheckerArt.ConditionsFormalEquality ->
                ConditionsFormalEqualityVerifier(
                    verifier,
                    promptDir,
                    language,
                    originalCode,
                    AnnotationTypes.PURE in mode.removeAnnotations,
                    checker
                )
        }
    }

//    println(checker.checkResponseFolded())

    val strategy = getDefaultStrategy(
        env,
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
        maxIterations = cliConfig.maxIterations,
        systemPrompt = systemPrompt,
        toolRegistry = ToolRegistry {
            tools(tools)
        }
    ) {
        handleEvents {
            onToolCallFailure { ctx ->
                throw ToolFailure(
                    run,
                    mode.name,
                    file.nameWithoutExtension,
                    ctx.tool.name,
                    ctx.toolArgs,
                    ctx.throwable.toString(),
                    ctx.throwable.cause
                )
            }
        }
        install(Tracing) {
            addMessageProcessor(TraceFeatureMessageFileWriter(
                PathKt((historyPath / (file.nameWithoutExtension + "_agent.txt")).toString()),
                { path -> SystemFileSystem.sink(path).buffered() },
            ))
        }
    }

    try {
        agent.run(code)
    } catch (e: Throwable) {
        errorPath.appendText(e.message ?: "")
    }

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

    val promptExecutor = SingleLLMPromptExecutor(
        RateLimiterLLMClient(
            RetryingLLMClient(
                GrazieLLMClient(
                    SuspendableAPIGatewayClient(
                        serverUrl = "https://api.app.stgn.grazie.aws.intellij.net/",
                        httpClient = SuspendableHTTPClient.WithV5(
                            SuspendableClientWithBackoff(
                                GrazieKtorHTTPClient.Client.WithExtendedTimeout,
                            ), AuthData(
                                token = config.token,
                                grazieAgent = GrazieAgent("verified-cogen-agent", "dev")
                            )
                        ),
                        //                    authType = AuthType.Application,
                    ),
                    LLMParams(
                        temperature = config.temperature,
                    )
                ),
                RetryConfig(
                    maxAttempts = 5,
                    initialDelay = 2.seconds,
                )
            ),
            config.llmProfile.model,
        )
    )
//        SingleLLMPromptExecutor(OpenAILLMClient(cliConfig.token))

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
            regularFileCreation(resultsPath)

            val limitedDispatcher = Dispatchers.IO.limitedParallelism(config.maxJobs)

            val lock = Any()
            val results = mutableMapOf<String, Int>()

            val jobs = benchmarks.map { benchmark ->
                launch(limitedDispatcher) {
                    val result = runBenchmark(
                        run,
                        mode,
                        historyPath,
                        benchmark,
                        tools,
                        promptDir,
                        config,
                        promptExecutor,
                    )
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
        regularFileCreation(atLeastOnceFile)

        val atLeastOnceJson = json.encodeToString(
            MapSerializer(String.serializer(), Int.serializer()),
            atLeastOnce
        )
        atLeastOnceFile.writeText(atLeastOnceJson)
    }
}