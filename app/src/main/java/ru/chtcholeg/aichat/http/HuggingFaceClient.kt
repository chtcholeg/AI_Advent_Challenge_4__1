package ru.chtcholeg.aichat.http

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.chtcholeg.aichat.BuildConfig

object HuggingFaceClient {
    private const val API_BASE_URL = "https://router.huggingface.co/v1"

    private const val API_TOKEN = BuildConfig.HUGGINGFACE_API_TOKEN

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
            })
        }

        install(HttpTimeout) {
            requestTimeoutMillis = 120_000
            connectTimeoutMillis = 5_000
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

    suspend fun send(
        model: String,
        temperature: Float,
        apiMessages: List<ApiMessage>,
    ): Result<AiResponse> = withContext(Dispatchers.IO) {

        val result: Result<AiResponse> = try {
            val aiRequest = AIRequest(
                model = model,
                messages = apiMessages,
                temperature = temperature,
            )
            client.post("${API_BASE_URL}/chat/completions") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                bearerAuth(API_TOKEN)
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