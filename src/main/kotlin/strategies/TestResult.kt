package org.example.agents

import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.ToolResult
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class TestResult(
    var generatedCode: String,
    var success: Boolean,
    var error: String?,
    var try_: Int,
) : Tool.Args, ToolResult {
    override fun toStringDefault(): String =
        Json.encodeToString(serializer(), this)
}
