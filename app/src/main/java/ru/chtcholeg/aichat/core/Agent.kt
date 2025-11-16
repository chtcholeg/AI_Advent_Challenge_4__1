package ru.chtcholeg.aichat.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

interface Agent {
    val name: String
    val messages: StateFlow<List<Message>>

    fun addMessage(message: Message)

    suspend fun processUserRequest(request: String): Result<String>
}

fun Agent.launchProcessingUserRequest(request: String) {
    CoroutineScope(Dispatchers.Default + SupervisorJob()).launch {
        processUserRequest(request)
    }
}