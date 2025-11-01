package ru.chtcholeg.aichat.http

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Choice(
    val message: Message,
    @SerialName("finish_reason")  val finishReason: String,
)

@Serializable
data class AIResponse(
    val choices: List<Choice>,
    val created: Long,
)