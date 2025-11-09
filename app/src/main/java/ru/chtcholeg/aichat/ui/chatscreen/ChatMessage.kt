package ru.chtcholeg.aichat.ui.chatscreen

import ru.chtcholeg.aichat.core.Message
import ru.chtcholeg.aichat.core.ResponseFormat
import ru.chtcholeg.aichat.http.ApiMessage.Role

sealed interface ChatMessage {
    val stringToCopy: String

    data class RegularMessage(
        val originalMessage: Message,
    ) : ChatMessage {
        override val stringToCopy: String get() = originalMessage.content ?: "<no content>"
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