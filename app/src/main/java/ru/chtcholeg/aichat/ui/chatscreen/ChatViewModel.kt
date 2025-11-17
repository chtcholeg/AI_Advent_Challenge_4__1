package ru.chtcholeg.aichat.ui.chatscreen

import android.content.Context
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.chtcholeg.aichat.core.AgentHolder
import ru.chtcholeg.aichat.core.CompositeAgent
import ru.chtcholeg.aichat.core.Message
import ru.chtcholeg.aichat.core.ResponseFormat
import ru.chtcholeg.aichat.core.SingleAgent
import ru.chtcholeg.aichat.core.Summarizator
import ru.chtcholeg.aichat.core.api.AiApiHolder
import ru.chtcholeg.aichat.core.api.Model
import ru.chtcholeg.aichat.core.asText
import ru.chtcholeg.aichat.database.Chat
import ru.chtcholeg.aichat.database.ChatRepository
import ru.chtcholeg.aichat.http.ApiMessage
import ru.chtcholeg.aichat.ui.chatscreen.ChatViewModel.Companion.JSON_DESCRIPTION_EXAMPLE
import ru.chtcholeg.aichat.ui.chatscreen.ChatViewModel.Companion.XML_DESCRIPTION_EXAMPLE
import ru.chtcholeg.aichat.utils.ClipboardUtils
import ru.chtcholeg.aichat.utils.ParserUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ChatState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val inputText: String = "",
    val shouldSetFocusOnInput: Boolean = false,
    val dialog: Dialog? = null,
) {
    sealed interface Dialog {
        data class Settings(
            val model: Model = Model.GigaChat,
            val temperature: Float = 1f,
            val maxTokens: Int? = null,
            val selectedTab: Tab = Tab.Behaviour,
            val selectedAgent: Agent = Agent.Single(),
        ) : Dialog {
            sealed interface Tab {

                data object Behaviour : Tab {
                    val items = listOf(
                        SingleAgent.Type.Regular,
                        SingleAgent.Type.StepByStepSolver,
                        // SingleAgent.Type.FullFledgedAssistant,
                        SingleAgent.Type.SequentialAssistant,
                    )
                }

                data object OutputFormat : Tab {
                    val items = listOf(
                        SingleAgent.Type.Json(JSON_DESCRIPTION_EXAMPLE),
                        SingleAgent.Type.Xml(XML_DESCRIPTION_EXAMPLE),
                    )
                }

                data object CompositeAgents : Tab {
                    val items = listOf(
                        CompositeAgent.Type.SeveralTaskSolvers,
                    )
                }
            }

            sealed interface Agent {
                data class Single(val type: SingleAgent.Type = SingleAgent.Type.Regular) : Agent
                data class Composite(val type: CompositeAgent.Type = CompositeAgent.Type.SeveralTaskSolvers) : Agent
            }

            companion object {
                val MODELS =
                    listOf(Model.GigaChat, Model.Llama323BInstruct, Model.MetaLlama370BInstruct, Model.DeepSeekV3)
            }
        }

        data object SummarizationConfirmation : Dialog

        data class History(
            val items: List<Item>,
        ) : Dialog {
            sealed interface Item {
                data object Title : Item
                data class Header(val text: String) : Item
                data class ChatInfo(val chat: Chat) : Item
            }
        }
    }
}

sealed interface ChatAction {
    data object SendMessage : ChatAction
    data class Input(val text: String) : ChatAction
    data object ResetNeedForInputFocus : ChatAction
    data object ResetChat : ChatAction
    data object AskToSummarize : ChatAction
    data class Summarize(val shouldSaveSystemPrompt: Boolean) : ChatAction
    data object HideDialog : ChatAction
    data object ShowSettings : ChatAction
    data object RefreshToken : ChatAction
    data class SetModel(val model: Model) : ChatAction
    data class SetTemperature(val temperature: Float?) : ChatAction
    data class SetMaxTokens(val maxTokens: Int?) : ChatAction
    data class SelectTab(val tab: ChatState.Dialog.Settings.Tab) : ChatAction
    data class SetSingleAgent(val type: SingleAgent.Type) : ChatAction
    data class SetCompositeAgent(val type: CompositeAgent.Type) : ChatAction
    data class Copy(val context: Context, val chatMessage: ChatMessage) : ChatAction
    data class CopyAll(val context: Context) : ChatAction
    data object ShowHistory : ChatAction
    data class LoadChat(val chatId: String) : ChatAction
    data class DeleteChat(val chatId: String) : ChatAction
}

class ChatViewModel : ViewModel() {
    private val inputText = MutableStateFlow("")
    private val shouldSetFocusOnInput = MutableStateFlow(false)

    private val logicScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val dialogType = MutableStateFlow<DialogType>(DialogType.NO)

    private val selectedSettingsAgentsTab =
        MutableStateFlow<ChatState.Dialog.Settings.Tab>(ChatState.Dialog.Settings.Tab.Behaviour)

    private val selectedAgentItem: Flow<ChatState.Dialog.Settings.Agent> = AgentHolder.agent
        .map { aiAgent ->
            when (aiAgent) {
                is SingleAgent -> ChatState.Dialog.Settings.Agent.Single(aiAgent.type)
                is CompositeAgent -> ChatState.Dialog.Settings.Agent.Composite(aiAgent.type)
                else -> throw RuntimeException("Unknown agent type ($aiAgent)")
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val dialog = dialogType.flatMapLatest { dialogType ->
        when (dialogType) {
            DialogType.NO -> MutableStateFlow(null)
            DialogType.SETTINGS -> createSettingsDialogFlow()
            DialogType.SUMMARIZATION_CONFIRMATION -> createSummarizationConfirmationDialogFlow()
            DialogType.HISTORY -> createHistoryDialogFlow()
        }
    }

    private val messages: Flow<List<ChatMessage>> = combine(
        AgentHolder.messages,
        selectedAgentItem,
    ) { messages, selectedAgentItem ->
        val result = messages
            .filter { !it.isSystemPrompt }
            .map { ChatMessage.RegularMessage(it) }
        val parsedMessage = messages
            .lastOrNull()
            ?.let {
                if (it.expectedFormat == ResponseFormat.PLAIN_TEXT) return@let null
                it.originalApiMessage?.parse(it.expectedFormat)
            }

        (result + listOfNotNull(parsedMessage)).reversed()
    }

    private fun ApiMessage.parse(format: ResponseFormat): ChatMessage {
        return try {
            val parsedMessage = when (format) {
                ResponseFormat.XML -> ParserUtils.parseXml(content)
                ResponseFormat.JSON -> ParserUtils.parseJson(content)
                ResponseFormat.PLAIN_TEXT -> emptyMap()
            }
            ChatMessage.Parsed(
                format = format,
                title = (parsedMessage["title"] as? String) ?: "<no title>",
                beautifulFraming = (parsedMessage["uncode_symbols"] as? String) ?: "<no beautiful framing>",
                message = (parsedMessage["message"] as? String) ?: "<no message>",
            )
        } catch (e: Exception) {
            ChatMessage.ErrorOnParsing(format, e.message ?: "unknown error")
        }
    }

    val state = combine(
        inputText,
        shouldSetFocusOnInput,
        dialog,
        AiApiHolder.currentState,
        messages,
    ) { inputText, shouldSetFocusOnInput, dialog, coreState, messages ->
        ChatState(
            messages = messages,
            isLoading = coreState.isLoading,
            error = coreState.error,
            inputText = inputText,
            shouldSetFocusOnInput = shouldSetFocusOnInput,
            dialog = dialog,
        )
    }.stateIn(logicScope, SharingStarted.WhileSubscribed(5000), ChatState())

    fun onAction(action: ChatAction) {
        when (action) {
            ChatAction.SendMessage -> sendMessage()
            is ChatAction.Input -> updateInputText(action.text)
            ChatAction.ResetNeedForInputFocus -> resetNeedForInputFocus()
            ChatAction.HideDialog -> dialogType.value = DialogType.NO
            ChatAction.ShowSettings -> dialogType.value = DialogType.SETTINGS
            ChatAction.ResetChat -> resetChat()
            ChatAction.AskToSummarize -> askToSummarize()
            is ChatAction.Summarize -> summarize(action.shouldSaveSystemPrompt)
            ChatAction.RefreshToken -> refreshToken()
            is ChatAction.SetModel -> setModel(action.model)
            is ChatAction.SetTemperature -> setTemperature(action.temperature)
            is ChatAction.SetMaxTokens -> setMaxTokens(action.maxTokens)
            is ChatAction.SelectTab -> selectSettingsAgentsTab(action.tab)
            is ChatAction.SetSingleAgent -> setSingleAgent(action.type)
            is ChatAction.SetCompositeAgent -> setCompositeAgent(action.type)
            is ChatAction.Copy -> copy(action.context, action.chatMessage)
            is ChatAction.CopyAll -> copyAll(action.context)
            is ChatAction.ShowHistory -> dialogType.value = DialogType.HISTORY
            is ChatAction.LoadChat -> loadChat(action.chatId)
            is ChatAction.DeleteChat -> deleteChat(action.chatId)
        }
    }

    private fun updateInputText(text: String) {
        inputText.value = text
    }

    private fun resetNeedForInputFocus() {
        shouldSetFocusOnInput.value = false
    }

    private fun sendMessage() {
        val messageText = inputText.value.trim()
        if (messageText.isBlank()) return

        inputText.value = ""
        shouldSetFocusOnInput.value = true
        AgentHolder.processUserRequest(messageText)
    }

    private fun resetChat() {
        AgentHolder.resetCurrentChat()
    }

    private fun askToSummarize() {
        dialogType.value = DialogType.SUMMARIZATION_CONFIRMATION
    }

    private fun summarize(shouldSaveSystemPrompt: Boolean) {
        dialogType.value = DialogType.NO
        Summarizator.summarizeCurrentChat(shouldSaveSystemPrompt)
    }

    private fun refreshToken() {
        AiApiHolder.refreshToken()
    }

    private fun setModel(model: Model) {
        if (AiApiHolder.setModel(model)) {
            resetChat()
        }
    }

    private fun setTemperature(temperature: Float?) {
        AiApiHolder.setTemperature(temperature ?: 1.0f)
    }

    private fun setMaxTokens(maxTokens: Int?) {
        AiApiHolder.setMaxTokens(maxTokens)
    }

    private fun selectSettingsAgentsTab(tab: ChatState.Dialog.Settings.Tab) {
        selectedSettingsAgentsTab.value = tab
    }

    private fun setSingleAgent(aiAnswer: SingleAgent.Type) {
        AgentHolder.setSingleAgent(aiAnswer)
    }

    private fun setCompositeAgent(type: CompositeAgent.Type) {
        AgentHolder.setCompositeAgent(type)
    }

    private fun copy(context: Context, chatMessage: ChatMessage) {
        try {
            ClipboardUtils.copy(context, "message_content", chatMessage.stringToCopy)
        } catch (e: Exception) {
            android.util.Log.e("ChatViewModel", e.message ?: "unknown error")
        }
    }

    private fun copyAll(context: Context) {
        try {
            val text = buildString {
                val aiAgent = AgentHolder.agent.value
                val messages = aiAgent.messages.value.filter { !it.isSystemPrompt }
                messages.asText(true)
            }
            ClipboardUtils.copy(context, "all_messages", text)
        } catch (e: Exception) {
            android.util.Log.e("ChatViewModel", e.message ?: "unknown error")
        }
    }

    private fun loadChat(chatId: String) {
        logicScope.launch {
            val chat = ChatRepository.getChatById(chatId) ?: return@launch
            val messages = ChatRepository.getMessagesByChatId(chatId)
            AgentHolder.setChat(
                chat,
                messages = messages.map { dbMessage -> Message(originalApiMessage = dbMessage.apiMessage()) },
            )
        }
    }

    private fun ru.chtcholeg.aichat.database.Message.apiMessage() = ApiMessage(
        role = toApiRole(role),
        content = content,
    )

    private fun toApiRole(roleStr: String): ApiMessage.Role {
        ApiMessage.Role.entries.forEach {
            if (it.name == roleStr) return it
        }
        return ApiMessage.Role.ASSISTANT
    }

    private fun deleteChat(chatId: String) {
        logicScope.launch { ChatRepository.deleteChat(chatId) }
    }

    private fun createSettingsDialogFlow() = combine(
        AiApiHolder.model,
        AiApiHolder.temperature,
        AiApiHolder.maxTokens,
        selectedSettingsAgentsTab,
        selectedAgentItem,
    ) { model, temperature, maxTokens, selectedTab, selectedAgent ->
        ChatState.Dialog.Settings(model, temperature, maxTokens, selectedTab, selectedAgent)
    }

    private fun createSummarizationConfirmationDialogFlow() =
        MutableStateFlow(ChatState.Dialog.SummarizationConfirmation).asStateFlow()

    private fun createHistoryDialogFlow() = ChatRepository.getAllChats()
        .map { chats ->
            val groups = groupChatsByDay(chats)
            val items = buildList {
                add(ChatState.Dialog.History.Item.Title)
                groups.forEach { group ->
                    add(ChatState.Dialog.History.Item.Header(group.first))
                    group.second.forEach { chat ->
                        add(ChatState.Dialog.History.Item.ChatInfo(chat))
                    }
                }
            }
            ChatState.Dialog.History(items)
        }

    private fun groupChatsByDay(chats: List<Chat>): List<Pair<String, List<Chat>>> {
        return chats
            .groupBy { chat -> formatDate(chat.updated_at) }
            .toList()
            .sortedBy { it.first }
    }

    private fun formatDate(timestamp: Long): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timestamp))
    }

    private enum class DialogType { NO, SETTINGS, SUMMARIZATION_CONFIRMATION, HISTORY }

    companion object {
        const val JSON_DESCRIPTION_EXAMPLE = "{\n" +
                "  \"title\": \"заголовок твоего ответа в виде одной короткой строки - выжимки из вопроса\",\n" +
                "  \"message\": \"основной ответ\",\n" +
                "  \"uncode_symbols\": \"строка из нескольких (3-5) юникодных символов-картинок, которые имеют какое-то отношение к запросу\"" +
                "}"

        const val XML_DESCRIPTION_EXAMPLE = "<response>\n" +
                "  <title>заголовок твоего ответа в виде одной короткой строки - выжимки из вопроса</title>\n" +
                "  <message>основной ответ</apiMessage>\n" +
                "  <uncode_symbols>строка из нескольких (3-5) юникодных символов-картинок, которые имеют какое-то отношение к запросу</uncode_symbols>\n" +
                "</response>"
    }
}