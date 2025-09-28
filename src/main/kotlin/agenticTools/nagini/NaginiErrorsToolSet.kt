package agenticTools.nagini

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import org.example.commonTools.insertAt
import org.example.environment.ExperimentEnvironment
import org.example.environment.dumpHistory
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
        if (env.lastTestResult.error == null || ("timed out" in env.lastTestResult.error!!)) {
            return env.lastTestResult.error ?: "Verifier wasn't yet run on this code"
        }
        val patternToFind = ".py@"

        val code = env.lastTestResult.generatedCode
        var extendedError = env.lastTestResult.error!!
        var index = extendedError.indexOf(patternToFind)
        while (index >= 0) {
            val pointIdx = extendedError.indexOf(".", startIndex = index + patternToFind.length)
            val lineNumber = extendedError.substring(index + patternToFind.length, pointIdx).toInt()
            val codeSnippet = code.lines().subList(max(lineNumber - 3, 0), min(lineNumber + 3, code.lines().size)).joinToString("\n")
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

        val userPrompt =
            """
                You are given an error:
                ${env.lastTestResult.error}
                That verifier obtained running on the following code:
                $code
                Return a message with a code snippet, where error points to.
            """.trimIndent()
        env.historyManager.addAgentRequest(userPrompt)
        env.historyManager.addLLMResponse(extendedError)
        env.dumpHistory()

        env.lastTestResult.error = extendedError
        return extendedError
    }
}
