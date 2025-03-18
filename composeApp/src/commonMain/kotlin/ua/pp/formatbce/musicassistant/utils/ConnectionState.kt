package ua.pp.formatbce.musicassistant.utils

import ua.pp.formatbce.musicassistant.api.ConnectionInfo

sealed class ConnectionState {
    data class Connected(val info: ConnectionInfo) : ConnectionState()
    data object Connecting : ConnectionState()
    data class Disconnected(val exception: Exception?) : ConnectionState()
    data object NoServer : ConnectionState()

    companion object {
        fun disconnected(exception: Exception?, connectionInfoPresent: Boolean) =
            if (exception == null && !connectionInfoPresent) {
                NoServer
            } else {
                Disconnected(exception)
            }
    }
}