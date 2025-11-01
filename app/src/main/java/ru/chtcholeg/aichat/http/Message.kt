package ru.chtcholeg.aichat.http

import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val role: String, // system, user, assistant, function
    val content: String,
) {
    val isFromUser: Boolean get() = role.lowercase() == USER

    companion object {
        const val USER = "user"
        const val ASSISTANT = "assistant"
    }
}