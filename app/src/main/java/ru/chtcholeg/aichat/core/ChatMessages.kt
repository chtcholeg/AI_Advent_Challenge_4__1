package ru.chtcholeg.aichat.core

import kotlinx.coroutines.flow.StateFlow

data class ChatMessages(
    val id: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val messages: List<Message>,
)