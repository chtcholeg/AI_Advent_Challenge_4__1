package ru.chtcholeg.aichat.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import ru.chtcholeg.aichat.BuildConfig
import ru.chtcholeg.aichat.http.Message
import ru.chtcholeg.aichat.http.Message.Role
import ru.chtcholeg.aichat.ui.chatscreen.OutputContent

object ChatCore {
    private const val CLIENT_ID = BuildConfig.CLIENT_ID
    private const val CLIENT_SECRET = BuildConfig.CLIENT_SECRET
    private const val AI_MODEL = "GigaChat"
    private const val JSON_PROMPT =
        "Ты должен ВСЕГДА отвечать ТОЛЬКО в формате JSON. Любой другой формат ответа запрещен.\n" +
                "- Ответ должен быть ВАЛИДНЫМ JSON объектом\n" +
                "- Не добавляй никакого текста до или после JSON\n" +
                "- Не используй markdown formatting (```json```)\n" +
                "- Если не можешь выполнить запрос, верни JSON с полем \"error\"\n" +
                "Структура ответа ДОЛЖНА быть такой:\n" +
                "{\n" +
                "  \"title\": \"заголовок твоего ответа в виде одной короткой строки - выжимки из вопроса\",\n" +
                "  \"message\": \"основной ответ\",\n" +
                "  \"status\": \"success|error\",\n" +
                "  \"uncode_symbols\": \"строка из нескольких (3-5) юникодных символов-картинок, которые имеют какое-то отношение к запросу\"" +
                "}"
    private const val XML_PROMPT =
        "Ты должен ВСЕГДА отвечать ТОЛЬКО в формате XML. Любой другой формат ответа запрещен.\n" +
                "\n" +
                "Требования к ответу:\n" +
                "- Ответ должен быть ВАЛИДНЫМ XML документом\n" +
                "- Не добавляй никакого текста до или после XML\n" +
                "- Не используй markdown formatting (```xml```)\n" +
                "- Все теги должны быть правильно закрыты\n" +
                "- Используй XML декларацию в начале\n" +
                "\n" +
                "Структура ответа ДОЛЖНА быть такой:\n" +
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<response>\n" +
                "  <title>заголовок твоего ответа в виде одной короткой строки - выжимки из вопроса</title>\n" +
                "  <message>основной ответ</message>\n" +
                "  <status>success|error</status>\n" +
                "  <uncode_symbols>строка из нескольких (3-5) юникодных символов-картинок, которые имеют какое-то отношение к запросу</uncode_symbols>\n" +
                "</response>"

    private val logicScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val token = MutableStateFlow<String?>(null).apply {
        filter { token -> token != null }.onEach {
            _currentState.update { state ->
                if (state is State.Stopped) {
                    State.Live.Idle()
                } else {
                    state
                }
            }
        }.launchIn(logicScope)
    }

    private val _currentState = MutableStateFlow<State>(State.Stopped.Default)
    val currentState = _currentState.asStateFlow()

    private val _outputContent = MutableStateFlow(OutputContent.PLAIN_TEXT)
    val outputContent = _outputContent.asStateFlow()

    private val _temperature = MutableStateFlow(1f)
    val temperature = _temperature.asStateFlow()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages = _messages.asStateFlow()

    fun initChat() {
        if (_currentState.compareAndSet(State.Stopped.Default, State.Stopped.Starting)) {
            receiveToken()
        }
    }

    fun receiveToken() {
        logicScope.launch {
            KtorClient.getToken(CLIENT_ID, CLIENT_SECRET)
                .onSuccess { token.value = it.token }
                .onFailure {
                    token.value = null
                    _currentState.value = State.Stopped.CriticalError(it.message ?: "Undefined error")
                }
        }
    }

    fun sendMessage(text: String) {
        val newState = _currentState.updateAndGet { state ->
            if (state is State.Live.Idle) State.Live.Requesting else state
        }
        if (newState !is State.Live.Requesting) return

        val token = token.value
        if (token == null) {
            _currentState.compareAndSet(State.Live.Requesting, State.Stopped.CriticalError("No token"))
            return
        }

        logicScope.launch {
            var errorMessage: String? = null
            addMessage(role = Role.USER, text = text)
            KtorClient.send(token, AI_MODEL, getMessageListToSend(), temperature.value)
                .onSuccess { response ->
                    val text = response.choices.firstOrNull()?.message?.content ?: "<Empty answer>"
                    addMessage(role = Role.ASSISTANT, text = text)
                }.onFailure { error ->
                    errorMessage = error.message
                }
            _currentState.compareAndSet(State.Live.Requesting, State.Live.Idle(errorMessage))
        }
    }

    private fun addMessage(role: Role, text: String) {
        _messages.update { messages ->
            messages + Message(role = role, content = text)
        }
    }

    fun resetChat() {
        _messages.value = emptyList()
    }

    fun refreshToken() {
        token.value = null
        receiveToken()
    }

    fun setTemperature(temperature: Float) {
        _temperature.value = temperature
    }

    fun setOutputContent(outputContent: OutputContent) {
        _outputContent.value = outputContent
    }

    private fun getMessageListToSend(): List<Message> {
        val systemMessage = when (_outputContent.value) {
            OutputContent.PLAIN_TEXT -> null
            OutputContent.JSON -> Message(Role.SYSTEM, JSON_PROMPT)
            OutputContent.XML -> Message(Role.SYSTEM, XML_PROMPT)
        }
        return listOfNotNull(systemMessage) + _messages.value
    }

    sealed interface State {
        val isLoading: Boolean get() = false
        val error: String? get() = null

        sealed interface Stopped : State {
            data object Default : Stopped
            data object Starting : Stopped {
                override val isLoading = true
            }

            data class CriticalError(override val error: String) : Stopped
        }

        sealed interface Live : State {
            data class Idle(override val error: String? = null) : Live

            data object Requesting : Live {
                override val isLoading = true
            }
        }
    }

}