package ru.chtcholeg.aichat.core.api

sealed interface AiApiState {
    val isLoading: Boolean get() = false
    val error: String? get() = null

    sealed interface Stopped : AiApiState {
        data object Default : Stopped
        data object Starting : Stopped {
            override val isLoading = true
        }

        data class CriticalError(override val error: String) : Stopped
    }

    sealed interface Live : AiApiState {
        data class Idle(override val error: String? = null) : Live

        data object Requesting : Live {
            override val isLoading = true
        }
    }
}