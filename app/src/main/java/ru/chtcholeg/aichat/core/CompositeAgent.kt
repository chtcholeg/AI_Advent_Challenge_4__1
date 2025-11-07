package ru.chtcholeg.aichat.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class CompositeAgent(
    val type: Type,
) : Agent {
    override val name = "Composite"
    override val messages: StateFlow<List<Message>> = MutableStateFlow(emptyList())
    override val temperature: StateFlow<Float> = MutableStateFlow(1f)
    override fun setTemperature(temperature: Float) {
        TODO("Not yet implemented")
    }

    override fun resetMessages() {
        TODO("Not yet implemented")
    }

    override fun processUserRequest(message: String) {
        TODO("Not yet implemented")
    }

    private val agents = mutableListOf<Agent>()

    enum class Type {
        SEPARATE_TASK_SOLVER,
        SEVERAL_TASK_SOLVERS,
    }
}