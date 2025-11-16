package agenticTools.common

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import kotlinx.coroutines.runBlocking
import org.example.agenticTools.common.ToolSetWithLLMCall
import org.example.environment.ExperimentEnvironment
import kotlin.io.path.div

@LLMDescription("""
    This tool set aims at adding explanations (such as code snippets or causes of errors) to errors obtained from verifier.
    Results from this tool can be used as an error message for other tools, such as addInvariants.
    """)
class ErrorsToolSet(
    env: ExperimentEnvironment
) : ToolSetWithLLMCall(env, env.promptDir / "ErrorsToolSet") {

    @Tool
    @LLMDescription("""
        Add an extended explanation of error: what pitfalls does current proof have, how should you refine your verification strategy. 
    """)
    fun addErrorExplanation(): String = runBlocking {
        val userPrompt =
            """
                You are given an error:
                ${env.lastTestResult.error}
                That verifier obtained running on the following code:
                ${env.lastTestResult.generatedCode}
                Return a message explaining error (lack of which invariants/preconditions/postconditions led to this, 
                which invariants were written wrong) explaining strategy of fixing error (which invariant/conditions will be removed/fixed/added).
            """.trimIndent()

        val errorExplanation = callLLM("adding error explanation prompt", "errorsSystem.txt", userPrompt)
        env.lastTestResult.error += "\n" + errorExplanation
        errorExplanation
    }
}
