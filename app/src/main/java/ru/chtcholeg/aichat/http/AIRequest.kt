package ru.chtcholeg.aichat.http

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AIRequest(
    val model: String,
    val messages: List<Message>,
    val temperature: Float? = null,
    @SerialName("top_p") val topP: Float? = null,
    @SerialName("max_tokens") val maxTokens: Int? = null,
    @SerialName("repetition_penalty") val repetitionPenalty: Float? = null,
)