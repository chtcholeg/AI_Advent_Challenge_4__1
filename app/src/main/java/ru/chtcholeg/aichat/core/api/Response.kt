package ru.chtcholeg.aichat.core.api

import ru.chtcholeg.aichat.http.AiResponse

data class Response(
    val content: String,
    val requestCompletionTimeMs: Long,
    val originalAiResponse: AiResponse,
) {
    val promptTokens = originalAiResponse.usage?.promptTokens
    val completionTokens = originalAiResponse.usage?.completionTokens
}