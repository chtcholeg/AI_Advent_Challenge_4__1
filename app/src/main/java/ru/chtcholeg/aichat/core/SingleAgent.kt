package ru.chtcholeg.aichat.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import ru.chtcholeg.aichat.core.Agent.Companion.DEFAULT_TEMPERATURE
import ru.chtcholeg.aichat.http.ApiMessage
import ru.chtcholeg.aichat.http.ApiMessage.Role

class SingleAgent(
    val type: Type,
) : Agent {

    override val name: String = when (type) {
        Type.Regular -> "Regular agent"
        Type.StepByStepSolver -> "Step-by-step solver agent"
        is Type.Custom -> type.name
        is Type.Json -> "Returns JSON"
        is Type.Xml -> "Returns XML"
        Type.FullFledgedAssistant -> "Asks all questions at once"
        Type.SequentialAssistant -> "Asks questions sequentially"
    }

    private val initMessageList: List<Message> get() = listOfNotNull(type.systemPrompt?.asSystemMessage())
    private val _messages = MutableStateFlow(initMessageList)
    override val messages = _messages.asStateFlow()

    private val _temperature = MutableStateFlow(DEFAULT_TEMPERATURE)
    override val temperature = _temperature.asStateFlow()

    override fun setTemperature(temperature: Float) {
        _temperature.value = temperature
    }

    override fun resetMessages() {
        _messages.value = initMessageList
    }

    override suspend fun processUserRequest(request: String): Result<String> {
        addMessage(Role.USER, request)
        return AiApi.processUserRequest(messages.value.asApiMessages(), temperature.value)
            .onSuccess { content ->
                val title = if (type is Type.Custom) type.name else null
                addMessage(Role.ASSISTANT, content, type.responseFormat, title)
            }
    }

    private fun addMessage(
        role: Role,
        content: String,
        expectedFormat: ResponseFormat = ResponseFormat.PLAIN_TEXT,
        title: String? = null,
    ) {
        _messages.update { messages ->
            messages + Message(role, content, expectedFormat, title)
        }
    }

    private fun List<Message>.asApiMessages(): List<ApiMessage> = mapNotNull { it.originalApiMessage }

    private fun String.asSystemMessage() = Message(Role.SYSTEM, this)

    sealed interface Type {
        val responseFormat: ResponseFormat get() = ResponseFormat.PLAIN_TEXT
        val systemPrompt: String? get() = null

        data object Regular : Type
        data object StepByStepSolver : Type {
            override val systemPrompt: String? = SystemPrompts.STEP_BY_STEP_SOLVER
        }

        data class Custom(
            val name: String,
            override val responseFormat: ResponseFormat,
            override val systemPrompt: String?,
        ) : Type

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

    companion object {
        fun custom(
            name: String,
            responseFormat: ResponseFormat,
            systemPrompt: String,
        ) = SingleAgent(Type.Custom(name, responseFormat, systemPrompt))
    }
}
