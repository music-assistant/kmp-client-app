package io.music_assistant.client.remote

import co.touchlab.kermit.Logger
import io.music_assistant.client.api.RemoteConnectionState
import io.music_assistant.client.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Manages remote connections via WebRTC.
 * Handles storing/retrieving remote IDs and managing the connection lifecycle.
 */
class RemoteConnectionManager(
    private val settings: SettingsRepository
) {
    private val log = Logger.withTag("RemoteConnectionManager")
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var webRTCTransport: WebRTCTransport? = null

    private val _connectionState = MutableStateFlow<RemoteConnectionState>(RemoteConnectionState.Disconnected)
    val connectionState = _connectionState.asStateFlow()

    private val _storedRemoteId = MutableStateFlow<String?>(null)
    val storedRemoteId = _storedRemoteId.asStateFlow()

    init {
        // Load stored remote ID
        _storedRemoteId.value = settings.getRemoteId()
    }

    /**
     * Connect to a remote Music Assistant instance.
     */
    fun connectRemote(remoteId: String) {
        scope.launch {
            try {
                _connectionState.value = RemoteConnectionState.Connecting

                // Store the remote ID for future use
                settings.setRemoteId(remoteId)
                _storedRemoteId.value = remoteId

                // Create and connect WebRTC transport
                webRTCTransport = WebRTCTransport()
                webRTCTransport?.connect(remoteId)

                // Forward connection state
                scope.launch {
                    webRTCTransport?.connectionState?.collect { state ->
                        _connectionState.value = state
                    }
                }
            } catch (e: Exception) {
                log.e(e) { "Failed to connect to remote" }
                _connectionState.value = RemoteConnectionState.Error(e.message ?: "Connection failed")
            }
        }
    }

    /**
     * Disconnect from the remote server.
     */
    fun disconnect() {
        scope.launch {
            webRTCTransport?.disconnect()
            webRTCTransport = null
            _connectionState.value = RemoteConnectionState.Disconnected
        }
    }

    /**
     * Clear the stored remote ID.
     */
    fun clearStoredRemoteId() {
        settings.clearRemoteId()
        _storedRemoteId.value = null
    }

    /**
     * Get the WebRTC transport for sending/receiving messages.
     * Returns null if not connected.
     */
    fun getTransport(): WebRTCTransport? = webRTCTransport
}
