package org.example.agenticTools.common

import kotlinx.coroutines.withContext
import org.example.commonTools.codePrompt
import org.example.commonTools.previousErrorPrompt
import org.example.environment.ExperimentEnvironment
import kotlinx.coroutines.Dispatchers
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.div

abstract class CommonToolSet(
    env: ExperimentEnvironment,
    toolDir: Path,
) : ToolSetWithLLMCall(env, toolDir) {

    protected suspend fun commonToolCall(userPromptPath: String, systemPromptPath: String, promptName: String): String = withContext(Dispatchers.IO) {
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