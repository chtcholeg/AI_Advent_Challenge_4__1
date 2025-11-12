package ru.chtcholeg.aichat.core.api

import ru.chtcholeg.aichat.BuildConfig
import ru.chtcholeg.aichat.http.AiResponse
import ru.chtcholeg.aichat.http.ApiMessage
import ru.chtcholeg.aichat.http.HuggingFaceClient
import ru.chtcholeg.aichat.http.OAuthResponse

class HuggingFaceApi(private val modelId: String) : AiApiBase() {

    override suspend fun getToken(): Result<OAuthResponse> =
        Result.success(
            OAuthResponse(token = BuildConfig.HUGGINGFACE_API_TOKEN, expiresAt = 0)
        )

    override suspend fun send(
        token: String,
        apiMessages: List<ApiMessage>,
        temperature: Float,
    ): Result<AiResponse> {

        return HuggingFaceClient.send(modelId, temperature, apiMessages)
    }

}