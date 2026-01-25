package org.example

import ai.koog.prompt.executor.clients.retry.RetryingLLMClient
import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.features.eventHandler.feature.handleEvents
import ai.koog.agents.features.tracing.feature.Tracing
import ai.koog.agents.features.tracing.writer.*
import ai.koog.prompt.executor.clients.retry.RetryConfig
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.executor.model.PromptExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.io.buffered
import kotlinx.io.files.Path as PathKt
import kotlinx.io.files.SystemFileSystem
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.TripleSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.example.LLMClients.RateLimiterLLMClient
import org.example.LLMClients.overallTokenCount
import org.example.agents.TestResult
import org.example.commonTools.ToolFailure
import org.example.commonTools.fetchTokens
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
import kotlin.math.max
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
) : Triple<Int, Double, Double> = runBlocking {
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
        historyManager = historyManager,
        promptDir = promptDir,
        promptExecutor = promptExecutor,
        model = cliConfig.llmProfile.model,
        taskDescription = description,
        conversationDump = conversationPath,
        errorPath = errorPath,
        lastTestResult = testResult,
        startingCode = code,
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
                    verifier = verifier,
                    promptDir = promptDir,
                    language = language,
                    originalProgram = originalCode,
                    removeHelpers = AnnotationTypes.PURE in mode.removeAnnotations,
                    innerChecker = checker
                )
        }
    }

    val storingPath = historyPath / "running"
    storingPath.createDirectories()
    val templateProgramFile = storingPath / (file.nameWithoutExtension + "_" + env.lastTestResult.try_ + "." + cliConfig.filterByExt.strRepl)
    if (checker.checkResponseFolded(templateProgramFile).first) {
        return@runBlocking Triple(0, 0.0, 0.0)
    }

    val strategy = getDefaultStrategy(
        env = env,
        tools = tools,
        storingPath = storingPath,
        cliConfig = cliConfig,
        name = file.nameWithoutExtension,
        responseChecker = checker
    )

    val systemPrompt = (promptDir / "systemAgent.txt").readText().replace("{ framework }", cliConfig.filterByExt.name)
    val agent = AIAgent(
        promptExecutor = promptExecutor,
        llmModel = cliConfig.llmProfile.model,
        strategy = strategy,
        maxIterations = cliConfig.maxIterations,
        systemPrompt = systemPrompt,
        toolRegistry = ToolRegistry {
            tools(tools)
        }
    ) {
        handleEvents {
            onToolCallFailed { ctx ->
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
            onLLMCallCompleted { ctx ->
                ctx.responses.forEach {
                    val tokens = fetchTokens(it) ?: 0.0
                    env.agentTokens = max(env.agentTokens, tokens)
                    println("OnAfterLLMCall ${env.agentTokens} (${tokens})")
                }
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

    if (errorPath.readText().isEmpty()) {
        errorPath.deleteExisting()
    }

    if (!testResult.success) {
        testResult.try_ = -1
    }
    Triple(testResult.try_, env.agentTokens, env.LLMQueriesTokens)
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
                delegate = getBaseLLMClient(config),
                config = RetryConfig(
                    maxAttempts = 5,
                    initialDelay = 2.seconds,
                )
            ),
            config.llmProfile.model,
        )
    )

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
            val tokensPath = runPath / "${idx}_${mode}_${run}_tokens.json"
            regularFileCreation(tokensPath)

            val limitedDispatcher = Dispatchers.IO.limitedParallelism(config.maxJobs)

            val lock = Any()
            val results = mutableMapOf<String, Int>()
            val tokens = mutableMapOf<String, Triple<Double, Double, Double>>()

            val jobs = benchmarks.map { benchmark ->
                launch(limitedDispatcher) {
                    val result = runBenchmark(
                        run = run,
                        mode = mode,
                        historyPath = historyPath,
                        file = benchmark,
                        toolsArgs = tools,
                        promptDir = promptDir,
                        cliConfig = config,
                        promptExecutor = promptExecutor,
                    )
                    println("${benchmark.name}: $result")

                    synchronized(lock) {
                        results[benchmark.name] = result.first
                        val mapJson = json.encodeToString(
                            MapSerializer(String.serializer(), Int.serializer()),
                            results
                        )
                        resultsPath.writeText(mapJson) // truncates and overwrites

                        overallTokenCount += result.second + result.third
                        println("Overall Token Count: $overallTokenCount")
                        tokens[benchmark.name] = Triple(result.second, result.third, overallTokenCount)
                        val tokensJson = json.encodeToString(
                            MapSerializer(
                                String.serializer(),
                                TripleSerializer(
                                    Double.serializer(), Double.serializer(), Double.serializer()
                                )
                            ),
                            tokens
                        )
                        tokensPath.writeText(tokensJson)
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