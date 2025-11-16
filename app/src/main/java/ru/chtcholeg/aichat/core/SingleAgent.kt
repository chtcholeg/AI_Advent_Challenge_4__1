package ru.chtcholeg.aichat.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.chtcholeg.aichat.core.api.AiApiHolder
import ru.chtcholeg.aichat.core.api.Response
import ru.chtcholeg.aichat.database.ChatRepository
import ru.chtcholeg.aichat.http.ApiMessage
import ru.chtcholeg.aichat.http.ApiMessage.Role
import java.lang.System
import java.util.UUID

class SingleAgent(
    val type: Type,
    private val initChat: ChatMessages = createDefaultChat(type),
) : Agent {

    private val chatId = initChat.id

    private val logicScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override val name: String = when (type) {
        Type.Regular -> "Regular agent"
        Type.StepByStepSolver -> "Step-by-step solver agent"
        is Type.Custom -> type.displayName
        is Type.Json -> "Returns JSON"
        is Type.Xml -> "Returns XML"
        Type.FullFledgedAssistant -> "Asks all questions at once"
        Type.SequentialAssistant -> "Asks questions sequentially"
    }

    private val chat = MutableStateFlow<ChatMessages>(initChat)
    override val messages = chat.map {
        it.messages
    }.stateIn(logicScope, SharingStarted.Eagerly, emptyList())

    var isChatCreated = false

    override fun addMessage(message: Message) {
        chat.update { chat ->
            chat.copy(
                updatedAt = System.currentTimeMillis(),
                messages = chat.messages + message,
            )
        }
        if (!isChatCreated) {
            storeChat(initChat, type)
            isChatCreated = true
        }
        storeMessage(chatId, message)
    }

    override suspend fun processUserRequest(request: String): Result<String> {
        addMessage(Role.USER, request)
        return AiApiHolder.processUserRequest(messages.value.asApiMessages())
            .onSuccess { response -> addAssistantMessage(response) }
            .map { it.content }
    }

    private fun addMessage(
        role: Role,
        content: String,
        expectedFormat: ResponseFormat = ResponseFormat.PLAIN_TEXT,
        title: String? = null,
        requestCompletionTimeMs: Long? = null,
        promptTokens: Long? = null,
        completionTokens: Long? = null,
    ) {
        val newMessage = Message(
            role = role,
            content = content,
            expectedFormat = expectedFormat,
            displayableTitle = title,
            requestCompletionTimeMs = requestCompletionTimeMs,
            promptTokens = promptTokens,
            completionTokens = completionTokens,
        )
        addMessage(newMessage)
    }

    private fun addAssistantMessage(response: Response) {
        val title = if (type is Type.Custom) type.displayName else null
        addMessage(
            role = Role.ASSISTANT,
            content = response.content,
            expectedFormat = type.responseFormat,
            title = title,
            requestCompletionTimeMs = response.requestCompletionTimeMs,
            promptTokens = response.promptTokens,
            completionTokens = response.completionTokens,
        )
    }

    private fun List<Message>.asApiMessages(): List<ApiMessage> = mapNotNull { it.originalApiMessage }

    sealed interface Type {
        val idInDatabase: String // Do not change this ID unnecessarily: it is used in the database
        val displayName: String
        val responseFormat: ResponseFormat get() = ResponseFormat.PLAIN_TEXT
        val systemPrompt: String? get() = null

        data object Regular : Type {
            override val idInDatabase = ID
            override val displayName = "Regular agent"
            const val ID = "regular"
        }

        data object StepByStepSolver : Type {
            override val idInDatabase = ID
            override val displayName: String = "Step-by-step solver agent"
            override val systemPrompt: String? = SystemPrompts.STEP_BY_STEP_SOLVER
            const val ID = "step-by-step"
        }

        data class Custom(
            override val idInDatabase: String = ID,
            override val displayName: String,
            override val responseFormat: ResponseFormat,
            override val systemPrompt: String?,
        ) : Type {
            companion object {
                const val ID = "custom"
            }
        }

        data class Json(val jsonDescription: String) : Type {
            override val idInDatabase: String = ID
            override val displayName: String = "JSON"
            override val responseFormat = ResponseFormat.JSON
            override val systemPrompt = SystemPrompts.json(jsonDescription)

            companion object {
                const val ID = "json"
            }
        }

        data class Xml(val xmlDescription: String) : Type {
            override val idInDatabase: String = ID
            override val displayName: String = "XML"
            override val responseFormat = ResponseFormat.XML
            override val systemPrompt = SystemPrompts.xml(xmlDescription)

            companion object {
                const val ID = "xml"
            }
        }

        data object FullFledgedAssistant : Type {
            override val idInDatabase = ID
            override val displayName: String = "Asks all questions at once"
            override val systemPrompt = SystemPrompts.FULL_FLEDGED_ASSISTANT
            const val ID = "full-fledged-assistant"
        }

        data object SequentialAssistant : Type {
            override val idInDatabase = ID
            override val displayName: String = "Asks questions sequentially"
            override val systemPrompt = SystemPrompts.SEQUENTIAL_ASSISTANT
            const val ID = "sequential-assistant"
        }
    }

    companion object {
        private val dbScope = CoroutineScope(Dispatchers.IO.limitedParallelism(1) + SupervisorJob())

        fun custom(
            name: String,
            responseFormat: ResponseFormat,
            systemPrompt: String,
        ) = SingleAgent(
            Type.Custom(
                displayName = name,
                responseFormat = responseFormat,
                systemPrompt = systemPrompt,
            )
        )

        private fun createDefaultChat(type: Type): ChatMessages {
            val now = System.currentTimeMillis()
            return ChatMessages(
                id = UUID.randomUUID().toString(),
                name = type.displayName,
                createdAt = now,
                updatedAt = now,
                messages = listOfNotNull(type.systemPrompt?.asSystemMessage()),
            )
        }

        private fun storeChat(chat: ChatMessages, type: Type) {
            dbScope.launch {
                ChatRepository.createChat(
                    id = chat.id,
                    name = chat.name,
                    type = type.idInDatabase,
                    createdAt = chat.createdAt,
                )
                chat.messages.forEach { addMessage(chat.id, it) }
            }
        }

        private fun storeMessage(chatId: String, message: Message) {
            dbScope.launch { addMessage(chatId, message) }
        }

        private suspend fun addMessage(chatId: String, message: Message) {
            ChatRepository.addMessage(
                chatId = chatId,
                role = (message.originalApiMessage?.role ?: Role.ASSISTANT).name,
                content = message.content.orEmpty(),
                contentType = message.expectedFormat.idInDatabase,
            )
        }

        private fun String.asSystemMessage() = Message(Role.SYSTEM, this)

    }
}
