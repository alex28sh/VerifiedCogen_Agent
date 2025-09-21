package org.example.commonTools

import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.reflect.asTools
import org.example.config.AgenticTools
import org.example.config.AgenticTools.*
import org.example.environment.ExperimentEnvironment

fun getTool(
    toolEnumEntry: AgenticTools,
    env: ExperimentEnvironment,
) : Tool<*, *> {

    val toolSetCtor = toolEnumEntry.ctor ?: error("${toolEnumEntry.strRepl} toolSet is not yet supported")
    val toolSet = toolSetCtor(env)

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

        CodeInserter -> {
            toolSet.asTools().find { it.name == "addCode" }!!
        }

        CodeRewriter -> {
            toolSet.asTools().find { it.name == "rewriteCode" }!!
        }

        else -> {
            throw UnsupportedOperationException("${toolEnumEntry.strRepl} tool is not yet supported")
        }
    }
}
