package agenticTools.nagini

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import org.example.commonTools.insertAt
import org.example.environment.ExperimentEnvironment
import org.example.environment.dumpHistory
import kotlin.io.path.appendText
import kotlin.math.max
import kotlin.math.min

@LLMDescription("""
    This tool set aims at adding explanations (such as code snippets or causes of errors) to errors obtained from verifier.
    Results from this tool can be used as an error message for other tools, such as addInvariants.
    """)
class NaginiErrorsToolSet(
    private val env: ExperimentEnvironment,
) : ToolSet {

    @Tool
    @LLMDescription("""
        Error messages of Nagini verifier often contain only line number and a limited code snippet.
        With this tool, you can add more context to a place, where the error happened.
    """)
    fun addCodeSnippet(): String {
        val error = env.lastTestResult.error
        val code = env.lastTestResult.generatedCode

        if (error == null || ("timed out" in error)) {
            return error ?: "Verifier wasn't yet run on this code"
        }

        val extendedError = try {
            addCodeSnippetInner(code, error)
        } catch (e : Throwable) {
            env.errorPath.appendText(code)
            env.errorPath.appendText(error)
            env.errorPath.appendText(e.message ?: "empty error")
            error
        }

        val userPrompt =
            """
                You are given an error:
                $error
                That verifier obtained running on the following code:
                $code
                Return a message with a code snippet, where error points to.
            """.trimIndent()
        env.historyManager.addAgentRequest(userPrompt)
        env.historyManager.addToolResponse(extendedError)
        env.dumpHistory()

        env.lastTestResult.error = extendedError
        return extendedError
    }

    private fun addCodeSnippetInner(code: String, error: String): String {

        val patternToFind = ".py@"
        var extendedError: String = error

        var index = extendedError.indexOf(patternToFind)
        while (index >= 0) {
            val pointIdx = extendedError.indexOf(".", startIndex = index + patternToFind.length)
            val lineNumber = extendedError.substring(index + patternToFind.length, pointIdx).toInt()

            if (lineNumber > code.lines().size) {
                break
            }

            val codeSnippet =
                code.lines().subList(max(lineNumber - 3, 0), min(lineNumber + 3, code.lines().size))
                    .joinToString("\n")
            val explanation = """
                ---
                We added a code snippet around the place error occurs for you to better understand the error:
                $codeSnippet
                ---
            """.trimIndent()

            val newLineIter = extendedError.indexOf("\n", startIndex = index)
            extendedError = extendedError.insertAt(newLineIter + 1, explanation)
            index = extendedError.indexOf(patternToFind, startIndex = newLineIter + explanation.length)
        }

        return extendedError
    }
}
