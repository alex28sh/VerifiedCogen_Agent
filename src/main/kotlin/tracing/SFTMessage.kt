package org.example.tracing

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SFTTrace(
    val metadata: SFTTraceMetadata,
    val success: Boolean,
    val tries: Int,
    val messages: List<SFTMessage>,
)

@Serializable
data class SFTTraceMetadata(
    val benchmark: String,
    val mode: String,
    val run: Int,
    val model: String,
    val language: String,
)

@Serializable
sealed class SFTMessage {
    abstract val role: String

    @Serializable
    @SerialName("system")
    data class System(val content: String) : SFTMessage() {
        override val role: String get() = "system"
    }

    @Serializable
    @SerialName("user")
    data class User(val content: String) : SFTMessage() {
        override val role: String get() = "user"
    }

    @Serializable
    @SerialName("assistant")
    data class Assistant(
        val content: String? = null,
        val tool_calls: List<SFTToolCall>? = null,
    ) : SFTMessage() {
        override val role: String get() = "assistant"
    }

    @Serializable
    @SerialName("tool")
    data class Tool(
        val tool_call_id: String,
        val name: String,
        val content: String,
    ) : SFTMessage() {
        override val role: String get() = "tool"
    }
}

@Serializable
data class SFTToolCall(
    val id: String,
    val type: String = "function",
    val function: SFTFunction,
)

@Serializable
data class SFTFunction(
    val name: String,
    val arguments: String,
)
