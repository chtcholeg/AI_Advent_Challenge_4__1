package ru.chtcholeg.aichat.ui.chatscreen

import ru.chtcholeg.aichat.core.Message
import ru.chtcholeg.aichat.core.ResponseFormat
import ru.chtcholeg.aichat.http.ApiMessage.Role
import ru.chtcholeg.aichat.utils.msToSecStr

sealed interface ChatMessage {
    val stringToCopy: String

    data class RegularMessage(
        val originalMessage: Message,
    ) : ChatMessage {
        override val stringToCopy: String
            get() = buildString {
                append(originalMessage.content ?: "<no content>")
                append("\n")
                originalMessage.requestCompletionTimeMs?.let { append("\nRequest completion time = ${it.msToSecStr()} sec") }
                originalMessage.promptTokens?.let { append("\nPrompt token count = $it") }
                originalMessage.completionTokens?.let { append("\nCompletion token count = $it") }
            }
    }

    data class Parsed(
        val format: ResponseFormat,
        val title: String,
        val beautifulFraming: String,
        val message: String,
    ) : ChatMessage {
        override val stringToCopy: String get() = "$title\n$beautifulFraming\n$message"
    }

    data class ErrorOnParsing(
        val format: ResponseFormat,
        val message: String,
    ) : ChatMessage {
        override val stringToCopy: String get() = message
    }
}

val ChatMessage.isFromUser: Boolean
    get() = if (this is ChatMessage.RegularMessage) {
        originalMessage.originalApiMessage?.role == Role.USER
    } else {
        false
    }