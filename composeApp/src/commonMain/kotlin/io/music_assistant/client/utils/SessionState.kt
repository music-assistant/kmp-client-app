package io.music_assistant.client.utils

import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.music_assistant.client.api.ConnectionInfo

sealed class SessionState {
    data class Connected(
        val session: DefaultClientWebSocketSession,
        val connectionInfo: ConnectionInfo
    ) : SessionState()

    data class Connecting(val connectionInfo: ConnectionInfo) : SessionState()

    sealed class Disconnected : SessionState() {
        data object Initial : Disconnected()
        data object ByUser : Disconnected()
        data object NoServerData : Disconnected()
        /** Disconnected because authentication is required but not provided */
        data object AuthRequired : Disconnected()
        data class Error(val reason: Exception?) : Disconnected()
    }

}