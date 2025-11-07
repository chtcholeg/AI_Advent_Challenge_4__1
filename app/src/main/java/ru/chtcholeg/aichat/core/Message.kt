package ru.chtcholeg.aichat.core

import ru.chtcholeg.aichat.http.ApiMessage

data class Message(
    val displayableName: String? = null,
    val expectedFormat: ResponseFormat = ResponseFormat.PLAIN_TEXT,
    val originalApiMessage: ApiMessage? = null,
) {

    val isNotSystemPrompt: Boolean get() = originalApiMessage?.isNotSystem ?: false

    companion object {
        operator fun invoke(
            role: ApiMessage.Role,
            content: String,
            expectedFormat: ResponseFormat = ResponseFormat.PLAIN_TEXT,
        ) =
            Message(
                expectedFormat = expectedFormat,
                originalApiMessage = ApiMessage(role, content),
            )
    }
}