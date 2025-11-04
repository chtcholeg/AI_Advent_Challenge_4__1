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
    val temperature: Float = 1.0f,
    val isLoading: Boolean = false,
    val error: String? = null,
    val inputText: String = ""
)

sealed interface ChatAction {
    data object SendMessage : ChatAction
    data class Input(val text: String) : ChatAction
    data object Clear : ChatAction
    data object RefreshToken : ChatAction
    data class NewTemperature(val temperature: Float?) : ChatAction
}

class ChatViewModel : ViewModel() {
    private val temperature = MutableStateFlow(1.0f)
    private val inputText = MutableStateFlow("")
    private val logicScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    val state = combine(
        inputText,
        temperature,
        ChatCore.currentState,
        ChatCore.messages,
    ) { inputText, temperature, coreState, messages ->
        ChatState(
            messages = messages,
            temperature = temperature,
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
            ChatAction.RefreshToken -> refreshToken()
            is ChatAction.NewTemperature -> setNewTemperature(action.temperature)
        }
    }

    private fun updateInputText(text: String) {
        inputText.value = text
    }

    private fun sendMessage() {
        val messageText = inputText.value.trim()
        if (messageText.isBlank()) return

        inputText.value = ""
        ChatCore.sendMessage(messageText, temperature.value)
    }

    private fun clearError() {
        ChatCore.resetError()
    }

    private fun refreshToken() {
        ChatCore.refreshToken()
    }

    private fun setNewTemperature(temperature: Float?) {
        this.temperature.value = temperature ?: 1.0f
    }
}