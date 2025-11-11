package ru.chtcholeg.aichat.core.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import ru.chtcholeg.aichat.http.ApiMessage
import kotlin.Exception

object AiApiHolder {

    private val logicScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _model = MutableStateFlow<Model>(Model.GigaChat)
    val model = _model.asStateFlow()

    private val aiApi: StateFlow<AiApiBase?> = _model
        .map { model ->
            when (model.api) {
                Model.Api.GIGACHAT -> GigaChatApi(model.id)
                Model.Api.HUGGINGFACE -> HuggingFaceApi(model.id)
            }.apply {
                init()
            }
        }.stateIn(logicScope, SharingStarted.Eagerly, null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentState: Flow<AiApiState> = aiApi.flatMapLatest { aiApi ->
        aiApi?.currentState ?: MutableStateFlow<AiApiState>(AiApiState.Stopped.Default)
    }

    fun setModel(model: Model): Boolean {
        return this._model.getAndUpdate { model } != model
    }

    fun refreshToken() = aiApi.value?.refreshToken()

    suspend fun processUserRequest(apiMessages: List<ApiMessage>, temperature: Float): Result<Response> {
        return aiApi.value?.processUserRequest(apiMessages, temperature) ?: Result.failure(ApiIsNotSelectedException())
    }

    class ApiIsBusyException : Exception("Request can't be completed because of processing of another request")
    class TokenIsNullException : Exception("Valid token wasn't received")
    class ApiIsNotSelectedException : Exception("API is not selected")
}

