package io.music_assistant.client.utils

import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.music_assistant.client.api.ConnectionInfo
import io.music_assistant.client.data.model.server.ServerInfo
import io.music_assistant.client.data.model.server.User

sealed class SessionState {
    data class Connected(
        val session: DefaultClientWebSocketSession,
        val connectionInfo: ConnectionInfo,
        val serverInfo: ServerInfo? = null,
        val user: User? = null,
        val authProcessState: AuthProcessState = AuthProcessState.NotStarted,
    ) : SessionState() {

        val dataConnectionState: DataConnectionState = when {
            serverInfo == null -> DataConnectionState.AwaitingServerInfo
            user == null -> DataConnectionState.AwaitingAuth(authProcessState)
            else -> DataConnectionState.Authenticated
        }

    }

    data object Connecting : SessionState()

    sealed class Disconnected : SessionState() {
        data object Initial : Disconnected()
        data object ByUser : Disconnected()
        data object NoServerData : Disconnected()
        data class Error(val reason: Exception?) : Disconnected()
    }
}