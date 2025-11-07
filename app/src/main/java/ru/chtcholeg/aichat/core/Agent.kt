package ru.chtcholeg.aichat.core

import kotlinx.coroutines.flow.StateFlow

interface Agent {
    val name: String
    val messages: StateFlow<List<Message>>
    val temperature: StateFlow<Float>

    fun setTemperature(temperature: Float)
    fun resetMessages()

    fun processUserRequest(request: String)

    companion object {
        const val DEFAULT_TEMPERATURE = 1f
    }
}
