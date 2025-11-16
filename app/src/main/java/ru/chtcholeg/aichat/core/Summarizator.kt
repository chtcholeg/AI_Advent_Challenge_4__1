package ru.chtcholeg.aichat.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import ru.chtcholeg.aichat.core.api.AiApiHolder
import ru.chtcholeg.aichat.http.ApiMessage
import ru.chtcholeg.aichat.http.ApiMessage.Role

object Summarizator {
    fun summarizeCurrentChat(shouldSaveSystemPrompt: Boolean) {
        CoroutineScope(Dispatchers.Default + SupervisorJob()).launch {
            val currentMessages = AgentHolder.agent.value.messages.value

            // Getting summary
            val apiMessageForSummarization = ApiMessage(
                role = Role.SYSTEM,
                content = SystemPrompts.summarizator(currentMessages.asText(false)),
            )
            val response = AiApiHolder.processUserRequest(listOf(apiMessageForSummarization))
            val summaryResponse = response.getOrNull()
            if (summaryResponse == null) {
                // TODO implement processing error
                return@launch
            }

            // Prepare system messgae for new chat
            val systemMessage = if (shouldSaveSystemPrompt) {
                currentMessages.firstOrNull()?.takeIf { it.isSystemPrompt }
            } else {
                null
            }

            // New chat
            AgentHolder.setSingleAgent(SingleAgent.Type.Regular)
            AgentHolder.resetCurrentChat()
            systemMessage?.let { AgentHolder.addMessage(it) }

            val summaryApiMessage = ApiMessage(
                role = Role.ASSISTANT,
                content = summaryResponse.content,
            )
            AgentHolder.addMessage(
                Message(
                    displayableTitle = "Summary of the previous chat",
                    originalApiMessage = summaryApiMessage,
                )
            )
        }
    }
}