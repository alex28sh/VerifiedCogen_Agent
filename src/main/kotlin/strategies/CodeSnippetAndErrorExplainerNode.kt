package org.example.strategies

import agenticTools.common.ErrorsToolSet
import agenticTools.nagini.NaginiErrorsToolSet
import ai.koog.agents.core.dsl.builder.AIAgentGraphStrategyBuilder
import ai.koog.agents.core.dsl.builder.forwardTo
import org.example.config.Extensions
import org.example.environment.ExperimentEnvironment
import kotlin.io.path.createFile
import kotlin.io.path.div
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.writeText

fun AIAgentGraphStrategyBuilder<String, String>.getCodeSnippetAndErrorExplainerNode(
    env: ExperimentEnvironment,
) = subgraph<String, String>(
    name = "get-code-snippet-and-explain-error"
) {
    edge ((nodeStart forwardTo nodeFinish)
        transformed { error ->
            val stripped = stripJvmCrashNoise(error)
            val name = env.errorPath.nameWithoutExtension
            if ("silicon" in error) {
                (env.errorPath.parent / (name + "_error_init.txt")).createFile().writeText(error)
                (env.errorPath.parent / (name + "_error_stripped.txt")).createFile().writeText(stripped)
            }
            env.lastTestResult.rawError = stripped
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

private fun stripJvmCrashNoise(output: String): String {
    val lines = output.split("\n")
    val result = mutableListOf<String>()
    var inJvmBlock = false

    for (line in lines) {
        val trimmed = line.trim()
        when {
            // JVM stack frame lines — always skip
            trimmed.startsWith("at ") && trimmed.contains("(") -> {
                inJvmBlock = true
                continue
            }
            // Java exception header lines (e.g. "java.util.concurrent.ExecutionException: ...")
            trimmed.matches(Regex("""[a-zA-Z_${'$'}][a-zA-Z0-9_${'$'}]*(\.[a-zA-Z_${'$'}][a-zA-Z0-9_${'$'}]*)+:.*""")) &&
                    !trimmed.startsWith("File ") -> {
                inJvmBlock = true
                continue
            }
            // "Caused by:" chains
            trimmed.startsWith("Caused by:") -> {
                inJvmBlock = true
                continue
            }
            // Python traceback lines reset the mode — these are meaningful
            trimmed.startsWith("Traceback (most recent call last)") ||
                    trimmed.startsWith("File \"") ||
                    trimmed.startsWith("AttributeError") ||
                    trimmed.startsWith("Exception:") -> {
                inJvmBlock = false
                result.add(line)
            }
            else -> {
                inJvmBlock = false
                result.add(line)
            }
        }
    }
    return result.joinToString("\n")
}
