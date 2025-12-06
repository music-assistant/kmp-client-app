package io.music_assistant.client.remote

import co.touchlab.kermit.Logger
import io.music_assistant.client.api.IceServer
import io.music_assistant.client.api.RemoteConfig
import io.music_assistant.client.api.RemoteConnectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

/**
 * WebRTC transport for remote connection to Music Assistant.
 * Uses WebRTC DataChannel to send/receive messages.
 */
class WebRTCTransport {
    private val log = Logger.withTag("WebRTCTransport")
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val signalingClient = SignalingClient()
    private var peerConnection: PeerConnection? = null
    private var dataChannel: DataChannel? = null
    private var peerConnectionFactory: PeerConnectionFactory? = null

    private val _connectionState = MutableStateFlow<RemoteConnectionState>(RemoteConnectionState.Disconnected)
    val connectionState = _connectionState.asStateFlow()

    // Channel for receiving messages from the remote server
    val messageChannel = Channel<String>(Channel.BUFFERED)

    private var pendingIceCandidates = mutableListOf<IceCandidate>()
    private var isRemoteDescriptionSet = false

    /**
     * Connect to a remote Music Assistant instance via WebRTC.
     */
    suspend fun connect(remoteId: String) {
        if (_connectionState.value is RemoteConnectionState.Connected ||
            _connectionState.value is RemoteConnectionState.Connecting) {
            return
        }

        _connectionState.value = RemoteConnectionState.Connecting
        log.i { "Connecting to remote: $remoteId" }

        try {
            // Initialize WebRTC
            initWebRTC()

            // Connect to signaling server
            signalingClient.connect()

            // Start listening for signaling events
            startSignalingListeners()

            // Request connection to remote
            signalingClient.requestConnection(remoteId)

            // Wait for connected message
            val (actualRemoteId, iceServers) = signalingClient.connectedChannel.receive()
            log.i { "Connected to signaling for remote: $actualRemoteId" }

            // Create peer connection with ICE servers
            createPeerConnection(iceServers ?: RemoteConfig.DEFAULT_ICE_SERVERS)

            // Create and send offer
            createAndSendOffer()

        } catch (e: Exception) {
            log.e(e) { "Failed to connect to remote" }
            _connectionState.value = RemoteConnectionState.Error(e.message ?: "Connection failed")
            disconnect()
        }
    }

    /**
     * Disconnect from the remote server.
     */
    suspend fun disconnect() {
        dataChannel?.close()
        dataChannel = null
        peerConnection?.close()
        peerConnection = null
        signalingClient.disconnect()
        _connectionState.value = RemoteConnectionState.Disconnected
        log.i { "Disconnected from remote" }
    }

    /**
     * Send a message to the remote server.
     */
    fun sendMessage(message: String) {
        val buffer = ByteBuffer.wrap(message.toByteArray(StandardCharsets.UTF_8))
        dataChannel?.send(DataChannel.Buffer(buffer, false))
    }

    private fun initWebRTC() {
        val options = PeerConnectionFactory.InitializationOptions.builder(
            io.music_assistant.client.MyApplication.appContext
        )
            .setEnableInternalTracer(false)
            .createInitializationOptions()

        PeerConnectionFactory.initialize(options)

        peerConnectionFactory = PeerConnectionFactory.builder()
            .createPeerConnectionFactory()
    }

    private fun createPeerConnection(iceServers: List<IceServer>) {
        val rtcIceServers = iceServers.map { ice ->
            PeerConnection.IceServer.builder(ice.urls)
                .apply {
                    ice.username?.let { setUsername(it) }
                    ice.credential?.let { setPassword(it) }
                }
                .createIceServer()
        }

        val config = PeerConnection.RTCConfiguration(rtcIceServers)

        peerConnection = peerConnectionFactory?.createPeerConnection(
            config,
            object : PeerConnection.Observer {
                override fun onIceCandidate(candidate: IceCandidate) {
                    log.d { "ICE candidate: ${candidate.sdp}" }
                    scope.launch {
                        val json = buildJsonObject {
                            put("candidate", candidate.sdp)
                            put("sdpMid", candidate.sdpMid)
                            put("sdpMLineIndex", candidate.sdpMLineIndex)
                        }.toString()
                        signalingClient.sendIceCandidate(json)
                    }
                }

                override fun onDataChannel(channel: DataChannel) {
                    log.i { "Data channel received: ${channel.label()}" }
                    setupDataChannel(channel)
                }

                override fun onIceConnectionChange(state: PeerConnection.IceConnectionState) {
                    log.i { "ICE connection state: $state" }
                    when (state) {
                        PeerConnection.IceConnectionState.CONNECTED,
                        PeerConnection.IceConnectionState.COMPLETED -> {
                            // Connection established
                        }
                        PeerConnection.IceConnectionState.DISCONNECTED,
                        PeerConnection.IceConnectionState.FAILED -> {
                            _connectionState.value = RemoteConnectionState.Disconnected
                        }
                        else -> {}
                    }
                }

                override fun onSignalingChange(state: PeerConnection.SignalingState) {
                    log.d { "Signaling state: $state" }
                }

                override fun onIceConnectionReceivingChange(receiving: Boolean) {}
                override fun onIceGatheringChange(state: PeerConnection.IceGatheringState) {}
                override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>) {}
                override fun onAddStream(stream: org.webrtc.MediaStream) {}
                override fun onRemoveStream(stream: org.webrtc.MediaStream) {}
                override fun onRenegotiationNeeded() {}
                override fun onAddTrack(receiver: org.webrtc.RtpReceiver, streams: Array<out org.webrtc.MediaStream>) {}
            }
        )

        // Create data channel
        val dataChannelConfig = DataChannel.Init().apply {
            ordered = true
        }
        dataChannel = peerConnection?.createDataChannel("messaging", dataChannelConfig)
        setupDataChannel(dataChannel!!)
    }

    private fun setupDataChannel(channel: DataChannel) {
        dataChannel = channel
        channel.registerObserver(object : DataChannel.Observer {
            override fun onBufferedAmountChange(previousAmount: Long) {}

            override fun onStateChange() {
                log.i { "Data channel state: ${channel.state()}" }
                when (channel.state()) {
                    DataChannel.State.OPEN -> {
                        _connectionState.value = RemoteConnectionState.Connected(
                            signalingClient.state.value.name
                        )
                        log.i { "Data channel opened - connection ready" }
                    }
                    DataChannel.State.CLOSED -> {
                        _connectionState.value = RemoteConnectionState.Disconnected
                    }
                    else -> {}
                }
            }

            override fun onMessage(buffer: DataChannel.Buffer) {
                val data = ByteArray(buffer.data.remaining())
                buffer.data.get(data)
                val message = String(data, StandardCharsets.UTF_8)
                log.d { "Received message: $message" }
                scope.launch {
                    messageChannel.send(message)
                }
            }
        })
    }

    private suspend fun createAndSendOffer() {
        val constraints = MediaConstraints()

        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription) {
                peerConnection?.setLocalDescription(object : SdpObserver {
                    override fun onSetSuccess() {
                        scope.launch {
                            signalingClient.sendOffer(sdp.description)
                        }
                    }
                    override fun onSetFailure(error: String) {
                        log.e { "Failed to set local description: $error" }
                    }
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onCreateFailure(p0: String?) {}
                }, sdp)
            }

            override fun onCreateFailure(error: String) {
                log.e { "Failed to create offer: $error" }
            }

            override fun onSetSuccess() {}
            override fun onSetFailure(error: String) {}
        }, constraints)
    }

    private fun startSignalingListeners() {
        // Listen for answer
        scope.launch {
            for (sdp in signalingClient.answerChannel) {
                log.i { "Received answer SDP" }
                val sessionDescription = SessionDescription(SessionDescription.Type.ANSWER, sdp)
                peerConnection?.setRemoteDescription(object : SdpObserver {
                    override fun onSetSuccess() {
                        isRemoteDescriptionSet = true
                        // Add pending ICE candidates
                        pendingIceCandidates.forEach {
                            peerConnection?.addIceCandidate(it)
                        }
                        pendingIceCandidates.clear()
                    }
                    override fun onSetFailure(error: String) {
                        log.e { "Failed to set remote description: $error" }
                    }
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onCreateFailure(p0: String?) {}
                }, sessionDescription)
            }
        }

        // Listen for ICE candidates
        scope.launch {
            for (candidateJson in signalingClient.iceCandidateChannel) {
                try {
                    val json = Json.parseToJsonElement(candidateJson) as JsonObject
                    val candidate = IceCandidate(
                        json["sdpMid"]?.jsonPrimitive?.content ?: "",
                        json["sdpMLineIndex"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                        json["candidate"]?.jsonPrimitive?.content ?: ""
                    )

                    if (isRemoteDescriptionSet) {
                        peerConnection?.addIceCandidate(candidate)
                    } else {
                        pendingIceCandidates.add(candidate)
                    }
                } catch (e: Exception) {
                    log.e(e) { "Failed to parse ICE candidate" }
                }
            }
        }

        // Listen for peer disconnection
        scope.launch {
            for (unit in signalingClient.peerDisconnectedChannel) {
                log.i { "Peer disconnected" }
                disconnect()
            }
        }

        // Listen for errors
        scope.launch {
            for (error in signalingClient.errorChannel) {
                log.e { "Signaling error: $error" }
                _connectionState.value = RemoteConnectionState.Error(error)
            }
        }
    }
}
