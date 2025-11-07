package ru.chtcholeg.aichat.http

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiMessage(
    val role: Role, // system, user, assistant, function
    val content: String,
) {
    val isNotSystem: Boolean get() = role == Role.USER || role == Role.ASSISTANT

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