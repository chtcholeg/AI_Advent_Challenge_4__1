package ru.chtcholeg.aichat.core.api

import ru.chtcholeg.aichat.BuildConfig
import ru.chtcholeg.aichat.http.AiResponse
import ru.chtcholeg.aichat.http.ApiMessage
import ru.chtcholeg.aichat.http.OAuthResponse

class GigaChatApi : AiApiBase() {

    override suspend fun getToken(): Result<OAuthResponse> {
        return GigachatKtorClient.getToken(CLIENT_ID, CLIENT_SECRET)
    }

    override suspend fun send(
        token: String,
        apiMessages: List<ApiMessage>,
        temperature: Float,
    ): Result<AiResponse> {
        return GigachatKtorClient.send(token, AI_MODEL, apiMessages, temperature)
    }

    companion object {
        private const val CLIENT_ID = BuildConfig.GIGACHAT_CLIENT_ID
        private const val CLIENT_SECRET = BuildConfig.GIGACHAT_CLIENT_SECRET
        private const val AI_MODEL = "GigaChat"
    }
}