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
import ru.chtcholeg.aichat.http.AiRequest
import ru.chtcholeg.aichat.http.AiResponse
import ru.chtcholeg.aichat.http.ApiMessage
import ru.chtcholeg.aichat.http.OAuthResponse

open class AiApiBase(
    private val model: String,
) {

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

    suspend fun processUserRequest(
        apiMessages: List<ApiMessage>,
        temperature: Float,
        maxTokens: Int?
    ): Result<Response> {
        val newState = _currentState.updateAndGet { state ->
            if (state is AiApiState.Live.Idle) AiApiState.Live.Requesting else state
        }
        if (newState !is AiApiState.Live.Requesting) return Result.failure(AiApiHolder.ApiIsBusyException())

        val token = token.value
        if (token == null) {
            _currentState.compareAndSet(AiApiState.Live.Requesting, AiApiState.Stopped.CriticalError("No token"))
            return Result.failure(AiApiHolder.TokenIsNullException())
        }

        val aiRequest = AiRequest(
            model = model, messages = apiMessages, temperature = temperature, maxTokens = maxTokens
        )
        val (aiResponse, requestCompletionTimeMs) = measuredSend(token, aiRequest)

        return processAiResponse(aiResponse, requestCompletionTimeMs)
    }

    private suspend inline fun measuredSend(token: String, aiRequest: AiRequest): Pair<Result<AiResponse>, Long> {
        val start = System.currentTimeMillis()
        val response = send(token, aiRequest)
        return response to (System.currentTimeMillis() - start)
    }

    private fun processAiResponse(result: Result<AiResponse>, requestCompletionTimeMs: Long): Result<Response> {
        val error = result.exceptionOrNull()
        _currentState.compareAndSet(AiApiState.Live.Requesting, AiApiState.Live.Idle(error?.message))
        if (error != null) return Result.failure(error)
        val content = result.extractContent()
        val contentError = content.exceptionOrNull()
        if (contentError != null) return Result.failure(contentError)
        return Result.success(
            Response(
                content = content.getOrNull().orEmpty(),
                originalAiResponse = result.getOrNull()!!,
                requestCompletionTimeMs = requestCompletionTimeMs,
            )
        )
    }

    private fun Result<AiResponse>.extractContent() = if (isFailure) {
        Result.failure(exceptionOrNull()!!)
    } else {
        Result.success(getOrNull()?.choices?.firstOrNull()?.message?.content.orEmpty())
    }

    open suspend fun getToken(): Result<OAuthResponse> =
        Result.failure(exception = NotImplementedError())

    open suspend fun send(token: String, aiRequest: AiRequest): Result<AiResponse> =
        Result.failure(exception = NotImplementedError())
}