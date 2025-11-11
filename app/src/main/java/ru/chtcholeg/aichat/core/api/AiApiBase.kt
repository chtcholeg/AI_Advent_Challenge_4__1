package ru.chtcholeg.aichat.core.api

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
import ru.chtcholeg.aichat.http.AiResponse
import ru.chtcholeg.aichat.http.ApiMessage
import ru.chtcholeg.aichat.http.OAuthResponse

open class AiApiBase {

    protected val logicScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    protected val _currentState = MutableStateFlow<AiApiState>(AiApiState.Stopped.Default)
    val currentState = _currentState.asStateFlow()

    private val token = MutableStateFlow<String?>(null).apply {
        filter { token -> token != null }.onEach {
            _currentState.update { state ->
                if (state is AiApiState.Stopped) {
                    AiApiState.Live.Idle()
                } else {
                    state
                }
            }
        }.launchIn(logicScope)
    }

    fun init() {
        if (_currentState.compareAndSet(AiApiState.Stopped.Default, AiApiState.Stopped.Starting)) {
            receiveToken()
        }
    }

    fun receiveToken() {
        logicScope.launch {
            getToken()
                .onSuccess { token.value = it.token }
                .onFailure {
                    token.value = null
                    _currentState.value = AiApiState.Stopped.CriticalError(it.message ?: "Undefined error")
                }
        }
    }

    fun refreshToken() {
        token.value = null
        receiveToken()
    }

    suspend fun processUserRequest(apiMessages: List<ApiMessage>, temperature: Float): Result<String> {
        val newState = _currentState.updateAndGet { state ->
            if (state is AiApiState.Live.Idle) AiApiState.Live.Requesting else state
        }
        if (newState !is AiApiState.Live.Requesting) return Result.failure(AiApiHolder.ApiIsBusyException())

        val token = token.value
        if (token == null) {
            _currentState.compareAndSet(AiApiState.Live.Requesting, AiApiState.Stopped.CriticalError("No token"))
            return Result.failure(AiApiHolder.TokenIsNullException())
        }

        val result = send(token, apiMessages, temperature)
        val errorMessage = result.exceptionOrNull()?.message
        _currentState.compareAndSet(AiApiState.Live.Requesting, AiApiState.Live.Idle(errorMessage))
        return result.extractContent()
    }

    open suspend fun getToken(): Result<OAuthResponse> = Result.failure(NotImplementedError())
    open suspend fun send(
        token: String,
        apiMessages: List<ApiMessage>,
        temperature: Float,
    ): Result<AiResponse> = Result.failure(NotImplementedError())

    private fun Result<AiResponse>.extractContent() = if (isFailure) {
        Result.failure(exceptionOrNull()!!)
    } else {
        Result.success(getOrNull()?.choices?.firstOrNull()?.message?.content.orEmpty())
    }
}