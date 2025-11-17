package ru.chtcholeg.aichat.core

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import ru.chtcholeg.aichat.core.SingleAgent.Type
import ru.chtcholeg.aichat.database.Chat

object AgentHolder {

    private val _agent = MutableStateFlow<Agent>(SingleAgent(Type.Regular))
    val agent: StateFlow<Agent> = _agent.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val messages
        get() = agent.flatMapLatest { it.messages }

    fun resetCurrentChat() {
        _agent.update { currentAgent ->
            when (currentAgent) {
                is SingleAgent -> SingleAgent(currentAgent.type)
                is CompositeAgent -> CompositeAgent(currentAgent.type)
                else -> SingleAgent(Type.Regular)
            }
        }
    }

    fun setChat(chat: Chat, messages: List<Message>) {
        _agent.value = SingleAgent(
            type = chat.toMessageType(),
            initChatSettings = SingleAgent.InitChatSettings.Defined(chat.toChatMessages(messages)),
        )
    }

    fun addMessage(message: Message) = agent.value.addMessage(message)

    fun setSingleAgent(type: Type) {
        _agent.update { currentAgent ->
            if (currentAgent.isSame(type)) currentAgent else SingleAgent(type)
        }
    }

    fun setCompositeAgent(type: CompositeAgent.Type) {
        _agent.update { currentAgent ->
            if (currentAgent.isSame(type)) currentAgent else CompositeAgent(type)
        }
    }

    fun processUserRequest(message: String) {
        agent.value.launchProcessingUserRequest(message)
    }

    private fun Chat.toMessageType(): Type = when(type) {
        Type.Regular.ID -> Type.Regular
        Type.StepByStepSolver.ID -> Type.StepByStepSolver
        Type.Custom.ID -> Type.Custom(
            displayName = name,
            responseFormat = ResponseFormat.PLAIN_TEXT,
            systemPrompt = null,
        )
        Type.Json.ID -> Type.Json("")
        Type.Xml.ID -> Type.Xml("")
        Type.FullFledgedAssistant.ID -> Type.FullFledgedAssistant
        Type.SequentialAssistant.ID -> Type.SequentialAssistant
        else -> Type.Regular
    }

    private fun Chat.toChatMessages(messages: List<Message>): ChatMessages {
        return ChatMessages(
            id = this.id,
            name = this.name,
            createdAt = this.created_at,
            updatedAt = this.updated_at,
            messages = messages,
        )
    }

    private fun Agent.isSame(type: Type) = (this as? SingleAgent)?.type == type
    private fun Agent.isSame(type: CompositeAgent.Type) = (this as? CompositeAgent)?.type == type
}