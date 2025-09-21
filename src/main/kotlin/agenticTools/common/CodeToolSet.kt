package agenticTools.common

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import ai.koog.prompt.dsl.prompt
import kotlinx.coroutines.runBlocking
import org.example.commonTools.codePrompt
import org.example.commonTools.previousErrorPrompt
import org.example.environment.ExperimentEnvironment
import org.example.environment.dumpHistory
import java.nio.file.Files
import kotlin.io.path.div

@LLMDescription("""
    A set of tools for adding/rewriting/removing code to the methods.
    Tools here, either to fill in methods implementation (for all methods, that require it).
    Or they rewrite current code in a more provable way - for example, instead of appending elements into the list 
    it could first create a list of fixed size and then assign them sequentially.
    """)
class CodeToolSet(
    private val env: ExperimentEnvironment,
) : ToolSet {

    private val toolDir = env.promptDir / "CodeToolSet"

    @Tool
    @LLMDescription(
        """
            This tool creates an implementation for functions in the given code. 
            If you have a textual description or/and pre and postconditions of methods this tool will create code fitting them.
            Please, use this tool only in the beginning stages of solving task, when methods don't have yet implementation.
        """
    )
    fun addCode(): String = runBlocking {
        val pathFile = toolDir / "addCode.txt"
        var promptText = Files.readString(pathFile)
            .codePrompt(env)
        if (env.taskDescription != null) {
            promptText = promptText.replace("{ taskDescription }", env.taskDescription)
        }
        val response = env.promptExecutor.execute(
            prompt = prompt("adding code prompt") {
                system(Files.readString(toolDir / "codeSystem.txt"))
                user(env.historyManager.fetchHistory() + promptText)
            }, model = env.model, tools = emptyList()
        )[0].content
        env.lastTestResult.generatedCode = response
        env.historyManager.addAgentRequest(promptText)
        env.historyManager.addLLMResponse(response)
        env.dumpHistory()
        response
    }

    @Tool
    @LLMDescription(
        """
            This tool modifies implementation of methods, so that pre/postconditions or/and invariants and assertions could be proven. 
            It can rewrite cycles (for example, by replacing cyclic append to the list with fixed length list initialization and further sequential assignments.
            Please, use this tool only when you already have implementation. Preferably, use it when given error from previous verification attempt. 
            With the code, this tool can modify invariants and assertions.
        """
    )
    fun rewriteCode(): String = runBlocking {
        val pathFile = toolDir / "rewriteCode.txt"
        var promptText = Files.readString(pathFile)
            .previousErrorPrompt(env)
            .codePrompt(env)
        if (env.taskDescription != null) {
            promptText = promptText.replace("{ taskDescription }", env.taskDescription)
        }
        val response = env.promptExecutor.execute(
            prompt = prompt("rewriting code prompt") {
                system(Files.readString(toolDir / "codeSystem.txt"))
                user(env.historyManager.fetchHistory() + promptText)
            }, model = env.model, tools = emptyList()
        )[0].content
        env.lastTestResult.generatedCode = response
        env.historyManager.addAgentRequest(promptText)
        env.historyManager.addLLMResponse(response)
        env.dumpHistory()
        response
    }
}
