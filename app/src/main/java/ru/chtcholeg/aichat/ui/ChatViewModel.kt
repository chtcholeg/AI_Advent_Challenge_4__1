package ru.chtcholeg.aichat.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import ru.chtcholeg.aichat.core.ChatCore
import ru.chtcholeg.aichat.http.Message

data class ChatState(
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val inputText: String = ""
)

sealed interface ChatAction {
    data object SendMessage : ChatAction
    data class Input(val text: String) : ChatAction
    data object Clear : ChatAction
}

class ChatViewModel : ViewModel() {
    private val inputText = MutableStateFlow("")
    private val logicScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    val state = combine(
        inputText,
        ChatCore.currentState,
        ChatCore.messages,
    ) { inputText, coreState, messages ->
        ChatState(
            messages = messages,
            isLoading = coreState.isLoading,
            error = coreState.error,
            inputText = inputText,
        )
    }.stateIn(logicScope, SharingStarted.Companion.WhileSubscribed(5000), ChatState())

    fun onAction(action: ChatAction) {
        when(action) {
            ChatAction.SendMessage -> sendMessage()
            is ChatAction.Input -> updateInputText(action.text)
            ChatAction.Clear -> clearError()
        }
    }

    private fun updateInputText(text: String) {
        inputText.value = text
    }

    private fun sendMessage() {
        val messageText = inputText.value.trim()
        if (messageText.isBlank()) return

        inputText.value = ""
        ChatCore.sendMessage(messageText)
    }

    private fun clearError() {
        ChatCore.resetError()
    }
}