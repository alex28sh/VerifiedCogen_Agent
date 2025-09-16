package org.example.commonTools

import ai.koog.agents.core.tools.ToolArgs

class ToolFailure(
    private val run: Int,
    private val mode: String,
    private val benchName: String,
    private val toolName: String,
    private val toolArgs: ToolArgs,
    private val toolMessage: String,
    private val toolCause: Throwable?
) : RuntimeException(toolMessage, toolCause) {

    override fun toString(): String {
        return "\n\n\nTOOL EXECUTION EXCEPTION:\n (run=$run, mode=$mode, benchName=$benchName, tool=$toolName, args=$toolArgs, message=${toolMessage}, cause=${toolCause?.javaClass?.simpleName})\n\n\n"
    }
}