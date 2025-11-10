package ru.chtcholeg.aichat.http

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiMessage(
    val role: Role, // system, user, assistant, function
    val content: String,
) {
    val isSystemPrompt: Boolean get() = role == Role.SYSTEM
    val isUserRequest: Boolean get() = role == Role.USER

    @Serializable
    enum class Role {
        @SerialName("user")
        USER,
        @SerialName("assistant")
        ASSISTANT,
        @SerialName("system")
        SYSTEM,
        @SerialName("function")
        FUNCTION,
        ;
    }
}