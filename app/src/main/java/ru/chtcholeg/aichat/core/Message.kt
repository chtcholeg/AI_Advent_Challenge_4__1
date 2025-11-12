package ru.chtcholeg.aichat.core

import ru.chtcholeg.aichat.http.ApiMessage
import java.util.concurrent.atomic.AtomicLong
import kotlin.String

data class Message(
    val displayableTitle: String? = null,
    val expectedFormat: ResponseFormat = ResponseFormat.PLAIN_TEXT,
    val originalApiMessage: ApiMessage? = null,
    val id: Long = currentId.andIncrement,
    val overriddenContent: String? = null,
    val requestCompletionTimeMs: Long? = null,
    val promptTokens: Long? = null,
    val completionTokens: Long? = null,
) {

    val isSystemPrompt: Boolean get() = originalApiMessage?.isSystemPrompt ?: false
    val isUserRequest: Boolean get() = originalApiMessage?.isUserRequest ?: false

    val content: String? get() = overriddenContent ?: originalApiMessage?.content

    companion object {
        val currentId = AtomicLong(0L)

        operator fun invoke(
            role: ApiMessage.Role,
            content: String,
            expectedFormat: ResponseFormat = ResponseFormat.PLAIN_TEXT,
            displayableTitle: String? = null,
            requestCompletionTimeMs: Long? = null,
            promptTokens: Long? = null,
            completionTokens: Long? = null,
        ) =
            Message(
                displayableTitle = displayableTitle,
                expectedFormat = expectedFormat,
                originalApiMessage = ApiMessage(role, content),
                requestCompletionTimeMs = requestCompletionTimeMs,
                promptTokens = promptTokens,
                completionTokens = completionTokens,
            )
    }
}