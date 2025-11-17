package ru.chtcholeg.aichat.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ru.chtcholeg.aichat.http.ApiMessage

interface Agent {
    val name: String
    val messages: StateFlow<List<Message>>

    fun addMessage(message: Message): List<ApiMessage>

    suspend fun processUserRequest(request: String): Result<String>
}

fun Agent.launchProcessingUserRequest(request: String) {
    CoroutineScope(Dispatchers.Default + SupervisorJob()).launch {
        processUserRequest(request)
    }
}