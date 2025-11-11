package ru.chtcholeg.aichat.core.api

sealed interface Model {
    val api: Api get() = Api.HUGGINGFACE
    val id: String

    data object GigaChat : Model {
        override val id: String = "GigaChat"
        override val api: Api = Api.GIGACHAT
    }

    data object Llama323BInstruct : Model {
        override val id: String = "meta-llama/Llama-3.2-3B-Instruct"
    }

    data object MetaLlama370BInstruct : Model {
        override val id: String = "meta-llama/Meta-Llama-3-70B-Instruct"
    }

    data object DeepSeekV3 : Model {
        override val id: String = "deepseek-ai/DeepSeek-V3"
    }

    enum class Api { GIGACHAT, HUGGINGFACE }
}