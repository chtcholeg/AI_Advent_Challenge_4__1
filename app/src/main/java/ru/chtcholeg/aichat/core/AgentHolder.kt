package ru.chtcholeg.aichat.core

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update

object AgentHolder {

    private val _agent = MutableStateFlow<Agent>(SingleAgent(SingleAgent.Type.Regular))
    val agent: StateFlow<Agent> = _agent.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val messages get() = agent.flatMapLatest { it.messages }

    @OptIn(ExperimentalCoroutinesApi::class)
    val temperature get() = agent.flatMapLatest { it.temperature }

    fun resetCurrentChat() = agent.value.resetMessages()

    fun setTemperature(temperature: Float) = agent.value.setTemperature(temperature)

    fun setSingleAgent(type: SingleAgent.Type) {
        _agent.update { currentAgent ->
            if (currentAgent.isSame(type)) return@update currentAgent
            SingleAgent(type).apply {
                setTemperature(currentAgent.temperature.value)
            }
        }
    }

    fun setCompositeAgent(type: CompositeAgent.Type) {
        _agent.update { currentAgent ->
            if (currentAgent.isSame(type)) return@update currentAgent
            CompositeAgent(type).apply {
                setTemperature(currentAgent.temperature.value)
            }
        }
    }

    fun processUserRequest(message: String) {
        agent.value.processUserRequest(message)
    }

    private fun Agent.isSame(type: SingleAgent.Type) = (this as? SingleAgent)?.type == type
    private fun Agent.isSame(type: CompositeAgent.Type) = (this as? CompositeAgent)?.type == type
}