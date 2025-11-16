package org.example.agenticTools.common

import kotlinx.coroutines.runBlocking
import org.example.commonTools.codePrompt
import org.example.commonTools.previousErrorPrompt
import org.example.environment.ExperimentEnvironment
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.div

abstract class CommonToolSet(
    env: ExperimentEnvironment,
    toolDir: Path,
) : ToolSetWithLLMCall(env, toolDir) {

    protected fun commonToolCall(userPromptPath: String, systemPromptPath: String, promptName: String): String = runBlocking {
        val pathFile = toolDir / userPromptPath
        var userPrompt = Files.readString(pathFile)
            .previousErrorPrompt(env)
            .codePrompt(env)
        if (env.taskDescription != null) {
            userPrompt = userPrompt.replace("{ taskDescription }", env.taskDescription)
        }

        val response = callLLM(promptName, systemPromptPath, userPrompt)
        env.lastTestResult.generatedCode = response
        response
    }
}