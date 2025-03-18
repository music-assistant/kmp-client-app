package ua.pp.formatbce.musicassistant.utils

import ua.pp.formatbce.musicassistant.api.ConnectionInfo

sealed class ConnectionState {
    data class Connected(val info: ConnectionInfo) : ConnectionState()
    data object Connecting : ConnectionState()
    data class Disconnected(val exception: Exception?) : ConnectionState()
}