import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.accept
import io.ktor.client.request.basicAuth
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.parametersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.chtcholeg.aichat.http.AIRequest
import ru.chtcholeg.aichat.http.AIResponse
import ru.chtcholeg.aichat.http.Message
import ru.chtcholeg.aichat.http.OAuthResponse
import java.security.cert.X509Certificate
import java.util.UUID
import javax.net.ssl.X509TrustManager

object KtorClient {
    private const val TOKEN_RECEIVING_URL = "https://ngw.devices.sberbank.ru:9443/api/v2/oauth"
    private const val API_BASE_URL = "https://gigachat.devices.sberbank.ru/api/v1"

    private const val ROLE_USER = "user"

    val instance = HttpClient(CIO) {
        // JSON serialization
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }

        // Timeout configuration
        install(HttpTimeout) {
            requestTimeoutMillis = 30000
            connectTimeoutMillis = 10000
            socketTimeoutMillis = 30000
        }

        // Logging
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    android.util.Log.d("Ktor", message)
                }
            }
            level = LogLevel.ALL
        }

        engine {
            https {
                // Создаем и назначаем TrustManager, который всем доверяет
                trustManager = object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                }
            }
        }

        // Default request configuration
        expectSuccess = true
    }

    suspend fun getToken(clientId: String, clientSecret: String): Result<OAuthResponse> {
        return try {
            // https://developers.sber.ru/docs/ru/gigachat/api/reference/rest/post-token
            instance.post(TOKEN_RECEIVING_URL) {
                contentType(ContentType.Application.FormUrlEncoded)
                accept(ContentType.Application.Json)
                basicAuth(clientId, clientSecret)
                header("RqUID", UUID.randomUUID().toString())
                setBody(FormDataContent(parametersOf("scope", "GIGACHAT_API_PERS")))
            }.result()
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }

    suspend fun sendMessage(token: String, model: String, message: String): Result<AIResponse> {
        return try {
            val aiRequest = AIRequest(
                model = model,
                messages = listOf(Message(role = ROLE_USER, message)),
            )
            instance.post("$API_BASE_URL/chat/completions") {
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
    }

    private suspend inline fun <reified T> HttpResponse.result(): Result<T> {
        val jsonText = bodyAsText()
        return if (status.value in 200..299) {
            val json = Json { ignoreUnknownKeys = true }
            Result.success(json.decodeFromString<T>(jsonText))
        } else {
            Result.failure(Exception("${status}: $jsonText"))
        }
    }

}
