package ru.chtcholeg.aichat.http

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object HuggingFaceClient {
    private const val API_BASE_URL = "https://router.huggingface.co/v1"

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
            })
        }

        install(HttpTimeout) {
            requestTimeoutMillis = 120_000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 30_000
        }

        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    android.util.Log.d("Ktor", message)
                }
            }
            level = LogLevel.ALL
        }

        HttpResponseValidator {
            validateResponse { response ->
                val statusCode = response.status.value
                when (statusCode) {
                    in 400..499 -> {
                        val errorText = response.body<String>()
                        throw ClientRequestException(response, errorText)
                    }

                    in 500..599 -> {
                        val errorText = response.body<String>()
                        throw ServerResponseException(response, errorText)
                    }
                }
            }
        }
    }

    suspend fun send(token: String, aiRequest: AiRequest): Result<AiResponse> = withContext(Dispatchers.IO) {
        val result: Result<AiResponse> = try {
            client.post("${API_BASE_URL}/chat/completions") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                bearerAuth(token)
                setBody(
                    Json.encodeToString(aiRequest)
                )
            }.result()
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }

        result
    }

    private suspend inline fun HttpResponse.result(): Result<AiResponse> {
        val jsonText = bodyAsText()
        return if (status.value in 200..299) {
            val json = Json { ignoreUnknownKeys = true }
            Result.success(json.decodeFromString<AiResponse>(jsonText))
        } else {
            Result.failure(Exception("${status}: $jsonText"))
        }
    }

}