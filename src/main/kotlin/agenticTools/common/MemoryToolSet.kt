package org.example.agenticTools.common

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import org.example.environment.ExperimentEnvironment

@LLMDescription("""
    A set of tools that helps agent to keep track of memory, e.g.
    backtrack to the starting version of program, compare previous results, etc.
""")
class MemoryToolSet(
    private  val env: ExperimentEnvironment,
) : ToolSet {

    @Tool
    @LLMDescription("""
        A tool that backtrack to the starting version of a program. 
        By using this tool, agent can reset all previous changes and start again. 
        This helps when agent stuck in a particular error - so it can start again with a different strategy of solving task.
    """)
    fun resetCode() {
        env.lastTestResult.error = null
        env.lastTestResult.success = false
        env.lastTestResult.generatedCode = env.startingCode
    }
}