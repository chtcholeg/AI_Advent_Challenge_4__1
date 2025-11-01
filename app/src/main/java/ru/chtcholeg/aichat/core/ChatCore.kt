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

object ChatCore {
    private const val CLIENT_ID = BuildConfig.CLIENT_ID
    private const val CLIENT_SECRET = BuildConfig.CLIENT_SECRET
    private const val AI_MODEL = "GigaChat"

    private val logicScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var token = MutableStateFlow<String?>(null).apply {
        filter { token -> token != null }
        onEach {
            _currentState.update { state ->
                if (state is State.Stopped)  {
                    State.Live.Idle()
                } else {
                    state
                }
            }
        }.launchIn(logicScope)
    }

    private val _currentState = MutableStateFlow<State>(State.Stopped.Default)
    val currentState = _currentState.asStateFlow()

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
            addMessage(role = Message.USER, text = text)
            KtorClient.sendMessage(token, AI_MODEL, text)
                .onSuccess { response ->
                    val text = response.choices.firstOrNull()?.message?.content ?: "<Empty answer>"
                    addMessage(role = Message.ASSISTANT, text = text)
                }.onFailure { error ->
                    errorMessage = error.message
                }
            _currentState.compareAndSet(State.Live.Requesting, State.Live.Idle(errorMessage))
        }
    }

    private fun addMessage(role: String, text: String) {
        _messages.update { messages ->
            listOf(Message(role = role, content = text)) + messages
        }
    }

    fun resetError() {

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