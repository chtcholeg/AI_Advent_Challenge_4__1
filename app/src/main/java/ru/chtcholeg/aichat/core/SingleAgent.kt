package ru.chtcholeg.aichat.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.chtcholeg.aichat.core.Agent.Companion.DEFAULT_TEMPERATURE
import ru.chtcholeg.aichat.http.ApiMessage
import ru.chtcholeg.aichat.http.ApiMessage.Role

class SingleAgent(
    val type: Type,
) : Agent {

    private val logicScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val initMessageList: List<Message> get() = listOfNotNull(type.systemPrompt?.asSystemMessage())
    private val _messages = MutableStateFlow(initMessageList)
    override val messages = _messages.asStateFlow()

    private val _temperature = MutableStateFlow(DEFAULT_TEMPERATURE)
    override val temperature = _temperature.asStateFlow()

    override val name: String = when (type) {
        Type.Regular -> "Regular agent"
        is Type.Json -> "Returns JSON"
        is Type.Xml -> "Returns XML"
        Type.FullFledgedAssistant -> "Asks all questions at once"
        Type.SequentialAssistant -> "Asks questions sequentially"
    }

    override fun setTemperature(temperature: Float) {
        _temperature.value = temperature
    }

    override fun resetMessages() {
        _messages.value = initMessageList
    }

    override fun processUserRequest(request: String) {
        addMessage(Role.USER, request)
        logicScope.launch {
            AiApi.processUserRequest(messages.value.asApiMessages(), temperature.value)
                .onSuccess { content ->
                    addMessage(Role.ASSISTANT, content, type.responseFormat)
                }
        }
    }

    private fun addMessage(role: Role, content: String, expectedFormat: ResponseFormat = ResponseFormat.PLAIN_TEXT) {
        _messages.update { messages ->
            messages + Message(role, content, expectedFormat)
        }
    }

    private fun List<Message>.asApiMessages(): List<ApiMessage> = mapNotNull { it.originalApiMessage }

    private fun String.asSystemMessage() = Message(Role.SYSTEM, this)

    sealed interface Type {
        val responseFormat: ResponseFormat get() = ResponseFormat.PLAIN_TEXT
        val systemPrompt: String? get() = null

        data object Regular : Type
        data class Json(val jsonDescription: String) : Type {
            override val responseFormat = ResponseFormat.JSON
            override val systemPrompt = SystemPrompts.json(jsonDescription)
        }

        data class Xml(val xmlDescription: String) : Type {
            override val responseFormat = ResponseFormat.XML
            override val systemPrompt = SystemPrompts.xml(xmlDescription)
        }

        data object FullFledgedAssistant : Type {
            override val systemPrompt = SystemPrompts.FULL_FLEDGED_ASSISTANT
        }

        data object SequentialAssistant : Type {
            override val systemPrompt = SystemPrompts.SEQUENTIAL_ASSISTANT
        }
    }
}
