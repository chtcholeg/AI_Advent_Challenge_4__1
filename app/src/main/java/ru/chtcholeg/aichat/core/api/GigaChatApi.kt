package ru.chtcholeg.aichat.core.api

import GigaChatClient
import ru.chtcholeg.aichat.BuildConfig
import ru.chtcholeg.aichat.http.AiRequest
import ru.chtcholeg.aichat.http.AiResponse
import ru.chtcholeg.aichat.http.OAuthResponse

class GigaChatApi(model: String) : AiApiBase(model) {

    override suspend fun getToken(): Result<OAuthResponse> {
        return GigaChatClient.getToken(CLIENT_ID, CLIENT_SECRET)
    }

    override suspend fun send(token: String, aiRequest: AiRequest): Result<AiResponse> =
        GigaChatClient.send(token, aiRequest)

    companion object {
        private const val CLIENT_ID = BuildConfig.GIGACHAT_CLIENT_ID
        private const val CLIENT_SECRET = BuildConfig.GIGACHAT_CLIENT_SECRET
    }
}