package io.music_assistant.client.player.sendspin.audio

import co.touchlab.kermit.Logger
import io.music_assistant.client.player.MediaPlayerController
import io.music_assistant.client.player.MediaPlayerListener
import io.music_assistant.client.player.sendspin.BufferState
import io.music_assistant.client.player.sendspin.ClockSynchronizer
import io.music_assistant.client.player.sendspin.SyncQuality
import io.music_assistant.client.player.sendspin.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.coroutines.CoroutineContext

class AudioStreamManager(
    private val clockSynchronizer: ClockSynchronizer,
    private val mediaPlayerController: MediaPlayerController
) : CoroutineScope {

    private val logger = Logger.withTag("AudioStreamManager")
    private val supervisorJob = SupervisorJob()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + supervisorJob

    private val audioBuffer = TimestampOrderedBuffer()
    private var audioDecoder: AudioDecoder? = null
    private var playbackJob: Job? = null

    private val _bufferState = MutableStateFlow(
        BufferState(
            bufferedDuration = 0L,
            isUnderrun = false,
            droppedChunks = 0
        )
    )
    val bufferState: StateFlow<BufferState> = _bufferState.asStateFlow()

    private val _playbackPosition = MutableStateFlow(0L)
    val playbackPosition: StateFlow<Long> = _playbackPosition.asStateFlow()

    private var streamConfig: StreamStartPlayer? = null
    private var isStreaming = false
    private var droppedChunksCount = 0

    // Thresholds
    companion object {
        const val LATE_THRESHOLD = 100_000L // 100ms in microseconds
        const val EARLY_THRESHOLD = 1_000_000L // 1s buffer
        const val PREBUFFER_THRESHOLD = 200_000L // 200ms prebuffer before starting
    }

    suspend fun startStream(config: StreamStartPlayer) {
        logger.i { "Starting stream: ${config.codec}, ${config.sampleRate}Hz, ${config.channels}ch, ${config.bitDepth}bit" }

        streamConfig = config
        isStreaming = true
        droppedChunksCount = 0

        // Create appropriate decoder
        audioDecoder?.release()
        audioDecoder = createDecoder(config)

        // Configure decoder
        val formatSpec = AudioFormatSpec(
            codec = AudioCodec.valueOf(config.codec.uppercase()),
            channels = config.channels,
            sampleRate = config.sampleRate,
            bitDepth = config.bitDepth
        )
        audioDecoder?.configure(formatSpec, config.codecHeader)

        // Prepare MediaPlayerController for raw PCM streaming
        mediaPlayerController.prepareRawPcmStream(
            sampleRate = config.sampleRate,
            channels = config.channels,
            bitDepth = config.bitDepth,
            listener = object : MediaPlayerListener {
                override fun onReady() {
                    logger.i { "MediaPlayer ready for PCM streaming" }
                }

                override fun onAudioCompleted() {
                    logger.i { "Audio completed" }
                }

                override fun onError(error: Throwable?) {
                    logger.e(error) { "MediaPlayer error" }
                }
            }
        )

        // Clear buffer and start playback thread
        audioBuffer.clear()
        startPlaybackThread()
    }

    private fun createDecoder(config: StreamStartPlayer): AudioDecoder {
        return when (config.codec.lowercase()) {
            "pcm" -> PcmDecoder()
            "flac" -> FlacDecoder()
            "opus" -> OpusDecoder()
            else -> {
                logger.w { "Unknown codec ${config.codec}, using PCM decoder" }
                PcmDecoder()
            }
        }
    }

    suspend fun processBinaryMessage(data: ByteArray) {
        if (!isStreaming) {
            logger.w { "Received audio chunk but not streaming" }
            return
        }

        // Parse binary message (9-byte header + payload)
        val binaryMessage = BinaryMessage.decode(data)
        if (binaryMessage == null) {
            logger.w { "Failed to decode binary message" }
            return
        }

        if (binaryMessage.type != BinaryMessageType.AUDIO_CHUNK) {
            logger.d { "Ignoring non-audio binary message: ${binaryMessage.type}" }
            return
        }

        // Convert server timestamp to local time
        val localTimestamp = clockSynchronizer.serverTimeToLocal(binaryMessage.timestamp)

        // Create audio chunk
        val chunk = AudioChunk(
            timestamp = binaryMessage.timestamp,
            data = binaryMessage.data,
            localTimestamp = localTimestamp
        )

        // Add to buffer
        audioBuffer.add(chunk)

        // Update buffer state
        updateBufferState()
    }

    private fun startPlaybackThread() {
        playbackJob?.cancel()
        playbackJob = launch {
            logger.i { "Starting playback thread" }

            // Wait for pre-buffer
            waitForPrebuffer()

            while (isActive && isStreaming) {
                try {
                    val chunk = audioBuffer.peek()

                    if (chunk == null) {
                        // Buffer underrun
                        if (!_bufferState.value.isUnderrun) {
                            logger.w { "Buffer underrun" }
                            _bufferState.value = _bufferState.value.copy(isUnderrun = true)
                        }
                        delay(10) // Wait for more data
                        continue
                    }

                    // Check sync quality
                    if (clockSynchronizer.currentQuality == SyncQuality.LOST) {
                        logger.w { "Clock sync lost, waiting..." }
                        delay(100)
                        continue
                    }

                    val currentLocalTime = getCurrentTimeMicros()
                    val chunkPlaybackTime = chunk.localTimestamp

                    when {
                        chunkPlaybackTime < currentLocalTime - LATE_THRESHOLD -> {
                            // Chunk is too late, drop it
                            audioBuffer.poll()
                            droppedChunksCount++
                            logger.d { "Dropped late chunk: ${currentLocalTime - chunkPlaybackTime}μs late" }
                            updateBufferState()
                        }
                        chunkPlaybackTime > currentLocalTime + EARLY_THRESHOLD -> {
                            // Chunk is too early, wait
                            val delayMs = ((chunkPlaybackTime - currentLocalTime) / 1000).coerceAtMost(100)
                            delay(delayMs)
                        }
                        else -> {
                            // Chunk is ready to play
                            playChunk(chunk)
                            audioBuffer.poll()
                            _playbackPosition.value = chunk.timestamp
                            updateBufferState()
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logger.e(e) { "Error in playback thread" }
                }
            }

            logger.i { "Playback thread stopped" }
        }
    }

    private suspend fun waitForPrebuffer() {
        logger.i { "Waiting for prebuffer..." }
        while (isActive && audioBuffer.getBufferedDuration() < PREBUFFER_THRESHOLD) {
            delay(50)
        }
        logger.i { "Prebuffer complete, starting playback" }
    }

    private fun playChunk(chunk: AudioChunk) {
        try {
            // Decode audio
            val decoder = audioDecoder ?: return
            val decodedData = decoder.decode(chunk.data)

            // Write to MediaPlayerController
            val written = mediaPlayerController.writeRawPcm(decodedData)
            if (written < decodedData.size) {
                logger.w { "Only wrote $written/${decodedData.size} bytes to AudioTrack" }
            }

        } catch (e: Exception) {
            logger.e(e) { "Error decoding chunk" }
        }
    }

    private suspend fun updateBufferState() {
        val bufferedDuration = audioBuffer.getBufferedDuration()
        _bufferState.value = BufferState(
            bufferedDuration = bufferedDuration,
            isUnderrun = bufferedDuration == 0L && isStreaming,
            droppedChunks = droppedChunksCount
        )
    }

    suspend fun clearStream() {
        logger.i { "Clearing stream" }
        audioBuffer.clear()
        _playbackPosition.value = 0L
        droppedChunksCount = 0
        updateBufferState()
    }

    suspend fun stopStream() {
        logger.i { "Stopping stream" }
        isStreaming = false
        playbackJob?.cancel()
        playbackJob = null

        audioBuffer.clear()
        audioDecoder?.reset()

        // Stop raw PCM stream on MediaPlayerController
        mediaPlayerController.stopRawPcmStream()

        _playbackPosition.value = 0L
        droppedChunksCount = 0
        _bufferState.value = BufferState(
            bufferedDuration = 0L,
            isUnderrun = false,
            droppedChunks = 0
        )
    }

    private fun getCurrentTimeMicros(): Long {
        return System.currentTimeMillis() * 1000
    }

    fun close() {
        logger.i { "Closing AudioStreamManager" }
        playbackJob?.cancel()
        audioDecoder?.release()
        supervisorJob.cancel()
    }
}
