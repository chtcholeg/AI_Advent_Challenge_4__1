package ru.chtcholeg.aichat.core.api

import ru.chtcholeg.aichat.BuildConfig
import ru.chtcholeg.aichat.http.AiRequest
import ru.chtcholeg.aichat.http.AiResponse
import ru.chtcholeg.aichat.http.HuggingFaceClient
import ru.chtcholeg.aichat.http.OAuthResponse

class HuggingFaceApi(model: String) : AiApiBase(model) {

    override suspend fun getToken(): Result<OAuthResponse> =
        Result.success(OAuthResponse(token = BuildConfig.HUGGINGFACE_API_TOKEN, expiresAt = 0))

    override suspend fun send(token: String, aiRequest: AiRequest): Result<AiResponse> =
        HuggingFaceClient.send(token, aiRequest)

}