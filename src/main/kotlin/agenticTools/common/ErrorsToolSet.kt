package org.example.tools.common

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import ai.koog.prompt.dsl.prompt
import kotlinx.coroutines.runBlocking
import org.example.environment.ExperimentEnvironment
import org.example.environment.dumpHistory
import java.nio.file.Files
import kotlin.io.path.div

@LLMDescription("""
    This tool set aims at adding explanations (such as code snippets or causes of errors) to errors obtained from verifier.
    Results from this tool can be used as an error message for other tools, such as addInvariants.
    """)
class ErrorsToolSet(
    private val env: ExperimentEnvironment
) : ToolSet {

    private val toolDir = env.promptDir / "ErrorsToolSet"

    @Tool
    @LLMDescription("""
        Add an extended explanation of error: what pitfalls does current proof have, how should you refine your verification strategy. 
    """)
    fun addErrorExplanation(
//        @LLMDescription("previousError is a some error from prover (that agent got when sending code to the prover)")
//        previousError: String,
//        @LLMDescription("code for the task that agent has by this time (and it need to be fixed)")
//        code: String,
    ): String = runBlocking {
        val userPrompt =
            """
                You are given an error:
                ${env.lastTestResult.error}
                That verifier obtained running on the following code:
                ${env.lastTestResult.generatedCode}
                Return a message explaining error (lack of which invariants/preconditions/postconditions led to this, 
                which invariants were written wrong) explaining strategy of fixing error (which invariant/conditions will be removed/fixed/added).
            """.trimIndent()

        val errorExplanation = env.promptExecutor.execute(
            prompt = prompt("adding error explanation prompt") {
                system(Files.readString(toolDir / "errorsSystem.txt"))
                user(env.historyManager.fetchHistory() + userPrompt)
            }, model = env.model, tools = emptyList()
        )[0].content
        env.lastTestResult.error += "\n" + errorExplanation

        env.historyManager.addAgentRequest(userPrompt)
        env.historyManager.addToolResponse(errorExplanation)
        env.dumpHistory()
        errorExplanation
    }
}
