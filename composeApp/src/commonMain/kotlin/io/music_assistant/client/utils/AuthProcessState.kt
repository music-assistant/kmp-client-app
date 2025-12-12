package io.music_assistant.client.utils

sealed interface AuthProcessState {
    object Idle : AuthProcessState
    object InProgress : AuthProcessState
    data class Failed(val reason: String) : AuthProcessState
}