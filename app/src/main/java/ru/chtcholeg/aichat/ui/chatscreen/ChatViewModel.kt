package ru.chtcholeg.aichat.ui.chatscreen

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import ru.chtcholeg.aichat.core.ChatCore
import ru.chtcholeg.aichat.http.Message

enum class OutputContent { PLAIN_TEXT, JSON, XML }

data class ChatState(
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val inputText: String = "",
    val dialog: Dialog? = null,
) {
    sealed interface Dialog {
        data class Settings(
            val temperature: Float = 1f,
            val outputContent: OutputContent = OutputContent.PLAIN_TEXT,
        ) : Dialog
    }

}

sealed interface ChatAction {
    data object SendMessage : ChatAction
    data class Input(val text: String) : ChatAction
    data object ResetChat : ChatAction
    data object HideDialog : ChatAction
    data object ShowSettings : ChatAction
    data object RefreshToken : ChatAction
    data class SetTemperature(val temperature: Float?) : ChatAction
    data class SetOutputContent(val outputContent: OutputContent) : ChatAction
}

class ChatViewModel : ViewModel() {
    private val inputText = MutableStateFlow("")

    private val logicScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val dialogType = MutableStateFlow<DialogType>(DialogType.NO)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val dialog = dialogType.flatMapLatest { dialogType ->
        when (dialogType) {
            DialogType.NO -> MutableStateFlow<ChatState.Dialog?>(null)
            DialogType.SETTINGS -> createSettingsDialogFlow()
        }
    }

    private val messages = ChatCore.messages
        .map { messages ->
            messages
                .filter { it.isNotSystem }
                .reversed()
        }

    val state = combine(
        inputText,
        dialog,
        ChatCore.currentState,
        messages,
    ) { inputText, dialog, coreState, messages ->
        ChatState(
            messages = messages,
            isLoading = coreState.isLoading,
            error = coreState.error,
            inputText = inputText,
            dialog = dialog,
        )
    }.stateIn(logicScope, SharingStarted.Companion.WhileSubscribed(5000), ChatState())

    fun onAction(action: ChatAction) {
        when (action) {
            ChatAction.SendMessage -> sendMessage()
            is ChatAction.Input -> updateInputText(action.text)
            ChatAction.HideDialog -> dialogType.value = DialogType.NO
            ChatAction.ShowSettings -> dialogType.value = DialogType.SETTINGS
            ChatAction.ResetChat -> resetChat()
            ChatAction.RefreshToken -> refreshToken()
            is ChatAction.SetTemperature -> setTemperature(action.temperature)
            is ChatAction.SetOutputContent -> setOutputContent(action.outputContent)
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

    private fun resetChat() {
        ChatCore.resetChat()
    }

    private fun refreshToken() {
        ChatCore.refreshToken()
    }

    private fun setTemperature(temperature: Float?) {
        ChatCore.setTemperature(temperature ?: 1.0f)
    }

    private fun setOutputContent(outputContent: OutputContent) {
        ChatCore.setOutputContent(outputContent)
    }

    private fun createSettingsDialogFlow() = combine(
        ChatCore.temperature,
        ChatCore.outputContent,
    ) { temperature, outputContent ->
        ChatState.Dialog.Settings(
            temperature,
            outputContent,
        )
    }

    private enum class DialogType { NO, SETTINGS }
}