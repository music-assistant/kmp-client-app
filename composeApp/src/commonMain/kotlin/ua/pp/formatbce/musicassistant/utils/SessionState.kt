package ua.pp.formatbce.musicassistant.utils

import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import ua.pp.formatbce.musicassistant.api.ConnectionInfo

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
        data class Error(val reason: Exception?) : Disconnected()
    }

}