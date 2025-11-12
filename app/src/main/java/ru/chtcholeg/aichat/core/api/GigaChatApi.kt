package ru.chtcholeg.aichat.core.api

import ru.chtcholeg.aichat.BuildConfig
import ru.chtcholeg.aichat.http.AiResponse
import ru.chtcholeg.aichat.http.ApiMessage
import ru.chtcholeg.aichat.http.OAuthResponse

class GigaChatApi(private val modelId: String) : AiApiBase() {

    override suspend fun getToken(): Result<OAuthResponse> {
        return GigaChatClient.getToken(CLIENT_ID, CLIENT_SECRET)
    }

    override suspend fun send(
        token: String,
        apiMessages: List<ApiMessage>,
        temperature: Float,
    ): Result<AiResponse> {
        return GigaChatClient.send(token, modelId, temperature, apiMessages)
    }

    companion object {
        private const val CLIENT_ID = BuildConfig.GIGACHAT_CLIENT_ID
        private const val CLIENT_SECRET = BuildConfig.GIGACHAT_CLIENT_SECRET
    }
}