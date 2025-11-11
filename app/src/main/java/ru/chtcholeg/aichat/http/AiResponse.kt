package ru.chtcholeg.aichat.http

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// This is a common JSON returned by a lot of APIs.
@Serializable
data class AiResponse(
    val choices: List<Choice>,
    val created: Long,
    val usage: Usage? = null,
) {
    @Serializable
    data class Choice(
        val message: ApiMessage,
        @SerialName("finish_reason") val finishReason: String,
    )

    @Serializable
    data class Usage(
        @SerialName("prompt_tokens") val promptTokens: Long? = null,
        @SerialName("completion_tokens") val completionTokens: Long? = null,
        @SerialName("total_token") val totalTokens: Long? = null,
    )
}
