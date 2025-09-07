import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.div

@LLMDescription("""
    This tool set aims at adding explanations (such as code snippets or causes of errors) to errors obtained from verifier.
    Results from this tool can be used as an error message for other tools, such as addInvariants.
    """)
class ErrorsToolSet(
    val promptExecutor: PromptExecutor,
    val model: LLModel,
    val promptsPath: Path,
) : ToolSet {

    @Tool
    @LLMDescription("""
        Add an extended explanation of error: what pitfalls does current proof have, how should you refine your verification strategy. 
    """)
    fun addErrorExplanation(
        @LLMDescription("previousError is a some error from prover (that agent got when sending code to the prover)")
        previousError: String,
        @LLMDescription("code for the task that agent has by this time (and it need to be fixed)")
        code: String,
    ): String = runBlocking {
        val userPrompt =
            """
                You are given an error:
                $previousError
                That verifier obtained running on the following code:
                $code
                Return a message explaining error (lack of which invariants/preconditions/postconditions led to this, 
                which invariants were written wrong) explaining strategy of fixing error (which invariant/conditions will be removed/fixed/added).
            """.trimIndent()
        val errorExplanation = promptExecutor.execute(
            prompt = prompt("adding error explanation prompt") {
                system(Files.readString(promptsPath / "invariantsSystem.txt"))
                user(userPrompt)
            }, model = model, tools = emptyList()
        )[0].content
        previousError + "\n" + errorExplanation
    }
}
