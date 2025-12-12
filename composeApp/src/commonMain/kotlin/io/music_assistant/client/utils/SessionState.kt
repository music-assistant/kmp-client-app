package io.music_assistant.client.utils

import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.music_assistant.client.api.ConnectionInfo
import io.music_assistant.client.data.model.server.User
import io.music_assistant.client.data.model.server.ServerInfo

sealed class SessionState {
    data class Connected(
        val session: DefaultClientWebSocketSession,
        val connectionInfo: ConnectionInfo,
        val serverInfo: ServerInfo? = null,
        val user: User? = null,
        val authProcessState: AuthProcessState = AuthProcessState.Idle,
    ) : SessionState() {

        val dataConnectionState: DataConnectionState = when {
            serverInfo == null -> DataConnectionState.AwaitingServerInfo

            user == null ->
                if (serverInfo.schemaVersion?.let { it >= AUTH_REQUIRED_SCHEMA_VERSION } == true)
                    DataConnectionState.AwaitingAuth(authProcessState)
                else
                    DataConnectionState.Ready

            else -> DataConnectionState.Ready
        }

    }

    data class Connecting(val connectionInfo: ConnectionInfo) : SessionState()

    sealed class Disconnected : SessionState() {
        data object Initial : Disconnected()
        data object ByUser : Disconnected()
        data object NoServerData : Disconnected()
        data class Error(val reason: Exception?) : Disconnected()
    }

    companion object {
        private const val AUTH_REQUIRED_SCHEMA_VERSION = 28
    }

}