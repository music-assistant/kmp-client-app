package io.music_assistant.client.api

import kotlinx.serialization.Serializable

/**
 * OAuth/Login flow configuration and state.
 */
object OAuthConfig {
    /** Callback scheme for the app to receive auth tokens */
    const val CALLBACK_SCHEME = "musicassistant"
    const val CALLBACK_HOST = "auth"
    const val CALLBACK_PATH = "/callback"

    /** Full callback URI for OAuth redirects */
    val CALLBACK_URI = "$CALLBACK_SCHEME://$CALLBACK_HOST$CALLBACK_PATH"

    /** Build the login URL for the server */
    fun buildLoginUrl(serverUrl: String, deviceName: String = "Music Assistant App"): String {
        val baseUrl = serverUrl.trimEnd('/')
        val encodedCallback = java.net.URLEncoder.encode(CALLBACK_URI, "UTF-8")
        val encodedDeviceName = java.net.URLEncoder.encode(deviceName, "UTF-8")
        return "$baseUrl/login?redirect_uri=$encodedCallback&device_name=$encodedDeviceName"
    }
}

/**
 * Remote connection configuration.
 */
object RemoteConfig {
    /** The signaling server URL for WebRTC connections */
    const val SIGNALING_SERVER_URL = "wss://signaling.music-assistant.io/ws"

    /** ICE server configurations for WebRTC */
    val DEFAULT_ICE_SERVERS = listOf(
        IceServer(urls = listOf("stun:stun.l.google.com:19302")),
        IceServer(urls = listOf("stun:stun1.l.google.com:19302"))
    )
}

/**
 * ICE server configuration for WebRTC.
 */
@Serializable
data class IceServer(
    val urls: List<String>,
    val username: String? = null,
    val credential: String? = null
)

/**
 * Signaling message types for WebRTC connection establishment.
 */
@Serializable
data class SignalingMessage(
    val type: String,
    val remoteId: String? = null,
    val sessionId: String? = null,
    val data: String? = null, // JSON string for SDP or ICE candidate
    val error: String? = null,
    val iceServers: List<IceServer>? = null
)

/**
 * State of the remote connection.
 */
sealed class RemoteConnectionState {
    data object Disconnected : RemoteConnectionState()
    data object Connecting : RemoteConnectionState()
    data class Connected(val remoteId: String) : RemoteConnectionState()
    data class Error(val message: String) : RemoteConnectionState()
}
