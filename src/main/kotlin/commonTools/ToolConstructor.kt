package org.example.commonTools

import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.reflect.asTools
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import org.example.config.AgenticTools
import org.example.config.AgenticTools.*
import org.example.config.CliConfig
import org.example.config.Modes
import org.example.tools.common.InvariantsToolSet
import java.nio.file.Path
import kotlin.io.path.div
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.readText

fun getTool(
    mode: Modes,
    file: Path,
    toolEnumEntry: AgenticTools,
    promptDir: Path,
    cliConfig: CliConfig,
) : Tool<*, *> {
    val promptExecutor = SingleLLMPromptExecutor(OpenAILLMClient(cliConfig.token))
    val model = cliConfig.llmProfile.model
    val description = if (mode.testDescription) {
        (file.parent / "text-description" / (file.nameWithoutExtension + ".txt")).readText()
    } else {
        null
    }

    val toolSet =  when(toolEnumEntry) {
        InvariantsInserter, InvariantsRemover, InvariantsRewriter -> {
            InvariantsToolSet(
                promptExecutor,
                model,
                promptDir,
                description,
            )
        }
        else -> {
            throw UnsupportedOperationException("${toolEnumEntry.strRepl} toolSet is not yet supported")
        }
    }

    return when(toolEnumEntry) {
        InvariantsInserter -> {
            toolSet.asTools().find { it.name == "addInvariants" }!!
        }

        InvariantsRemover -> {
            toolSet.asTools().find { it.name == "removeInvariants" }!!
        }

        InvariantsRewriter -> {
            toolSet.asTools().find { it.name == "rewriteInvariants" }!!
        }

        else -> {
            throw UnsupportedOperationException("${toolEnumEntry.strRepl} tool is not yet supported")
        }
    }
}
