package org.example.agenticTools.common

import ai.koog.agents.core.tools.reflect.ToolSet
import ai.koog.prompt.dsl.prompt
import kotlinx.coroutines.runBlocking
import org.example.commonTools.codePrompt
import org.example.commonTools.previousErrorPrompt
import org.example.environment.ExperimentEnvironment
import org.example.environment.dumpHistory
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.div

abstract class CommonToolSet(
    protected val env: ExperimentEnvironment,
    private val toolDir: Path,
) : ToolSet {

    protected fun commonToolCall(userPrompt: String, systemPrompt: String, promptName: String): String = runBlocking {
        val pathFile = toolDir / userPrompt
        var promptText = Files.readString(pathFile)
            .previousErrorPrompt(env)
            .codePrompt(env)
        if (env.taskDescription != null) {
            promptText = promptText.replace("{ taskDescription }", env.taskDescription)
        }
        val response = env.promptExecutor.execute(
            prompt = prompt(promptName) {
                system(Files.readString(toolDir / systemPrompt))
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