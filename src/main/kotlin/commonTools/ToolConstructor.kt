package org.example.commonTools

import ErrorsToolSet
import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.reflect.asTools
import ai.koog.prompt.executor.model.PromptExecutor
import org.example.config.AgenticTools
import org.example.config.AgenticTools.*
import org.example.config.CliConfig
import org.example.config.Modes
import org.example.tools.common.InvariantsToolSet
import org.example.tools.nagini.NaginiErrorsToolSet
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
    promptExecutor: PromptExecutor,
) : Tool<*, *> {
    val model = cliConfig.llmProfile.model
    val description = if (mode.textDescription) {
        (file.parent / "text-description" / (file.nameWithoutExtension + ".txt")).readText()
    } else {
        null
    }

    val toolSet =  when(toolEnumEntry) {
        InvariantsInserter, InvariantsRemover, InvariantsRewriter -> {
            InvariantsToolSet(
                promptExecutor,
                model,
                promptDir / "InvariantsToolSet",
                description,
            )
        }

        ErrorExplainer -> {
            ErrorsToolSet(
                promptExecutor,
                model,
                promptDir / "ErrorsToolSet",
            )
        }

        CodeSnippetExtractor -> {
            NaginiErrorsToolSet()
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

        ErrorExplainer -> {
            toolSet.asTools().find { it.name == "addErrorExplanation" }!!
        }

        CodeSnippetExtractor -> {
            toolSet.asTools().find { it.name == "addCodeSnippet" }!!
        }

        else -> {
            throw UnsupportedOperationException("${toolEnumEntry.strRepl} tool is not yet supported")
        }
    }
}
