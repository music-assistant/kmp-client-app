package io.music_assistant.client.remote

import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.receiveDeserialized
import io.ktor.client.plugins.websocket.sendSerialized
import io.ktor.client.plugins.websocket.wss
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.websocket.close
import io.music_assistant.client.api.IceServer
import io.music_assistant.client.api.RemoteConfig
import io.music_assistant.client.utils.myJson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * State of the signaling connection.
 */
enum class SignalingState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

/**
 * Signaling message received from the server.
 */
@Serializable
data class SignalingMessage(
    val type: String,
    val remoteId: String? = null,
    val sessionId: String? = null,
    val data: JsonObject? = null,
    val error: String? = null,
    val iceServers: List<IceServerMessage>? = null
)

@Serializable
data class IceServerMessage(
    val urls: List<String>? = null,
    val username: String? = null,
    val credential: String? = null
)

/**
 * Signaling message to send to the server.
 */
@Serializable
data class SignalingRequest(
    val type: String,
    val remoteId: String? = null,
    val sessionId: String? = null,
    val data: JsonObject? = null
)

/**
 * Signaling client for WebRTC connection establishment.
 * Connects to the signaling server to exchange SDP offers/answers and ICE candidates.
 */
class SignalingClient(
    private val serverUrl: String = RemoteConfig.SIGNALING_SERVER_URL
) {
    private val log = Logger.withTag("SignalingClient")

    private val client = HttpClient(CIO) {
        install(WebSockets) {
            contentConverter = KotlinxWebsocketSerializationConverter(myJson)
        }
    }

    private var session: DefaultClientWebSocketSession? = null
    private var listeningJob: Job? = null

    private val _state = MutableStateFlow(SignalingState.DISCONNECTED)
    val state = _state.asStateFlow()

    private var currentRemoteId: String? = null
    private var currentSessionId: String? = null

    // Channels for receiving different message types
    val connectedChannel = Channel<Pair<String, List<IceServer>?>>(Channel.BUFFERED)
    val offerChannel = Channel<Pair<String, String>>(Channel.BUFFERED) // sessionId, sdp
    val answerChannel = Channel<String>(Channel.BUFFERED) // sdp
    val iceCandidateChannel = Channel<String>(Channel.BUFFERED) // candidate json
    val peerDisconnectedChannel = Channel<Unit>(Channel.BUFFERED)
    val errorChannel = Channel<String>(Channel.BUFFERED)

    /**
     * Connect to the signaling server.
     */
    suspend fun connect() {
        if (_state.value == SignalingState.CONNECTED) return

        _state.value = SignalingState.CONNECTING

        try {
            client.wss(serverUrl) {
                session = this
                _state.value = SignalingState.CONNECTED
                log.i { "Connected to signaling server" }
                listenForMessages()
            }
        } catch (e: Exception) {
            log.e(e) { "Failed to connect to signaling server" }
            _state.value = SignalingState.ERROR
            throw e
        }
    }

    /**
     * Disconnect from the signaling server.
     */
    suspend fun disconnect() {
        listeningJob?.cancel()
        listeningJob = null
        session?.close()
        session = null
        currentRemoteId = null
        currentSessionId = null
        _state.value = SignalingState.DISCONNECTED
        log.i { "Disconnected from signaling server" }
    }

    /**
     * Request connection to a remote Music Assistant instance.
     */
    suspend fun requestConnection(remoteId: String) {
        currentRemoteId = remoteId
        send(SignalingRequest(type = "connect-request", remoteId = remoteId))
        log.i { "Requested connection to remote: $remoteId" }
    }

    /**
     * Send an SDP offer to the remote peer.
     */
    suspend fun sendOffer(sdp: String) {
        val data = myJson.parseToJsonElement("""{"type":"offer","sdp":"$sdp"}""") as JsonObject
        send(SignalingRequest(
            type = "offer",
            remoteId = currentRemoteId,
            sessionId = currentSessionId,
            data = data
        ))
    }

    /**
     * Send an SDP answer to the remote peer.
     */
    suspend fun sendAnswer(sdp: String) {
        val data = myJson.parseToJsonElement("""{"type":"answer","sdp":"$sdp"}""") as JsonObject
        send(SignalingRequest(
            type = "answer",
            remoteId = currentRemoteId,
            sessionId = currentSessionId,
            data = data
        ))
    }

    /**
     * Send an ICE candidate to the remote peer.
     */
    suspend fun sendIceCandidate(candidateJson: String) {
        val data = myJson.parseToJsonElement(candidateJson) as JsonObject
        send(SignalingRequest(
            type = "ice-candidate",
            remoteId = currentRemoteId,
            sessionId = currentSessionId,
            data = data
        ))
    }

    private suspend fun send(message: SignalingRequest) {
        session?.sendSerialized(message) ?: run {
            log.e { "Cannot send message - not connected" }
        }
    }

    private suspend fun listenForMessages() {
        try {
            while (true) {
                val message = session?.receiveDeserialized<SignalingMessage>() ?: break
                handleMessage(message)
            }
        } catch (e: Exception) {
            log.e(e) { "Error receiving signaling message" }
            _state.value = SignalingState.DISCONNECTED
        }
    }

    private suspend fun handleMessage(message: SignalingMessage) {
        log.d { "Received signaling message: ${message.type}" }

        when (message.type) {
            "connected" -> {
                currentSessionId = message.sessionId
                val iceServers = message.iceServers?.map { ice ->
                    IceServer(
                        urls = ice.urls ?: emptyList(),
                        username = ice.username,
                        credential = ice.credential
                    )
                }
                connectedChannel.send(Pair(message.remoteId ?: "", iceServers))
            }

            "offer" -> {
                val sdp = message.data?.get("sdp")?.jsonPrimitive?.content ?: return
                offerChannel.send(Pair(message.sessionId ?: "", sdp))
            }

            "answer" -> {
                val sdp = message.data?.get("sdp")?.jsonPrimitive?.content ?: return
                answerChannel.send(sdp)
            }

            "ice-candidate" -> {
                val candidateJson = message.data?.toString() ?: return
                iceCandidateChannel.send(candidateJson)
            }

            "peer-disconnected" -> {
                peerDisconnectedChannel.send(Unit)
            }

            "error" -> {
                errorChannel.send(message.error ?: "Unknown error")
            }
        }
    }
}
