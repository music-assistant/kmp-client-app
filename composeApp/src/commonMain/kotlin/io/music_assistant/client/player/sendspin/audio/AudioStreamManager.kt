package io.music_assistant.client.player.sendspin.audio

import co.touchlab.kermit.Logger
import io.music_assistant.client.player.MediaPlayerController
import io.music_assistant.client.player.MediaPlayerListener
import io.music_assistant.client.player.sendspin.BufferState
import io.music_assistant.client.player.sendspin.ClockSynchronizer
import io.music_assistant.client.player.sendspin.SyncQuality
import io.music_assistant.client.player.sendspin.model.*
import io.music_assistant.client.player.sendspin.isNativeOpusDecodingSupported
import io.music_assistant.client.player.sendspin.isNativeFlacDecodingSupported
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
    private var adaptationJob: Job? = null

    // Adaptive buffering manager
    private val adaptiveBufferManager = AdaptiveBufferManager(clockSynchronizer)

    private val _bufferState = MutableStateFlow(
        BufferState(
            bufferedDuration = 0L,
            isUnderrun = false,
            droppedChunks = 0,
            targetBufferDuration = 0L,
            currentPrebufferThreshold = 0L,
            smoothedRTT = 0.0,
            jitter = 0.0,
            dropRate = 0.0
        )
    )
    val bufferState: StateFlow<BufferState> = _bufferState.asStateFlow()

    private val _playbackPosition = MutableStateFlow(0L)
    val playbackPosition: StateFlow<Long> = _playbackPosition.asStateFlow()

    // Error state - emits when stream encounters an error
    private val _streamError = MutableStateFlow<Throwable?>(null)
    val streamError: StateFlow<Throwable?> = _streamError.asStateFlow()

    private var streamConfig: StreamStartPlayer? = null
    private var isStreaming = false
    private var droppedChunksCount = 0



// ...

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

        // Determine output codec (PCM or Passthrough)
        val outputCodec = if (
            (config.codec.equals("opus", ignoreCase = true) && !isNativeOpusDecodingSupported) ||
            (config.codec.equals("flac", ignoreCase = true) && !isNativeFlacDecodingSupported)
        ) {
            AudioCodec.valueOf(config.codec.uppercase())
        } else {
            AudioCodec.PCM
        }

        // Prepare MediaPlayerController
        mediaPlayerController.prepareStream(
            codec = outputCodec,
            sampleRate = config.sampleRate,
            channels = config.channels,
            bitDepth = config.bitDepth,
            codecHeader = config.codecHeader,
            listener = object : MediaPlayerListener {
                override fun onReady() {
                    logger.i { "MediaPlayer ready for stream ($outputCodec)" }
                }

                override fun onAudioCompleted() {
                    logger.i { "Audio completed" }
                }

                override fun onError(error: Throwable?) {
                    logger.e(error) { "MediaPlayer error - stopping stream" }
                    // When MediaPlayer encounters an error (e.g., audio output disconnected),
                    // emit the error and stop the stream to prevent zombie playback
                    launch {
                        _streamError.update { error }
                        stopStream()
                    }
                }
            }
        )

        // Clear buffer and start playback thread
        audioBuffer.clear()
        startPlaybackThread()
    }

    private fun createDecoder(config: StreamStartPlayer): AudioDecoder {
        val codec = config.codec.lowercase()
        logger.i { "Creating decoder for codec: $codec" }

        return when (codec) {
            "pcm" -> {
                logger.i { "Using PCM decoder (passthrough)" }
                PcmDecoder()
            }

            "flac" -> {
                logger.w { "FLAC decoder not yet implemented, server should send PCM" }
                FlacDecoder()
            }

            "opus" -> {
                logger.w { "Using Opus decoder" }
                OpusDecoder()
            }

            else -> {
                logger.w { "Unknown codec $codec, using PCM decoder" }
                PcmDecoder()
            }
        }
    }

    suspend fun processBinaryMessage(data: ByteArray) {
        if (!isStreaming) {
            // Server is still sending chunks after we stopped - this is normal
            // (server doesn't know we stopped until timeout or explicit notification)
            logger.d { "Received audio chunk but not streaming (ignoring)" }
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

        logger.d { "Received audio chunk: timestamp=${binaryMessage.timestamp}, size=${binaryMessage.data.size} bytes" }

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
        logger.d { "Buffer now has ${audioBuffer.size()} chunks, ${audioBuffer.getBufferedDuration()}μs buffered" }

        // Update buffer state
        updateBufferState()
    }

    private fun startPlaybackThread() {
        playbackJob?.cancel()
        playbackJob = launch {
            logger.i { "Starting playback thread" }

            // Wait for pre-buffer
            waitForPrebuffer()

            // Start adaptation thread
            startAdaptationThread()

            // SYNC FAST-FORWARD: After prebuffer, skip to the first chunk that's "current"
            // This handles the case where pause/next caused a time gap and all buffered
            // chunks have timestamps in the past.
            val syncStartTime = getCurrentTimeMicros()
            var skippedChunks = 0
            while (isActive && isStreaming) {
                val chunk = audioBuffer.peek() ?: break
                val chunkPlaybackTime = chunk.localTimestamp
                val lateThreshold = adaptiveBufferManager.currentLateThreshold
                
                if (chunkPlaybackTime < syncStartTime - lateThreshold) {
                    // This chunk is late - skip it
                    audioBuffer.poll()
                    skippedChunks++
                } else {
                    // Found a chunk that's current or early - start playing from here
                    break
                }
            }
            if (skippedChunks > 0) {
                logger.i { "🔄 Sync fast-forward: skipped $skippedChunks late chunks to catch up" }
                adaptiveBufferManager.reset() // Reset stats after bulk skip
            }

            var chunksPlayed = 0
            var lastLogTime = getCurrentTimeMicros()

            while (isActive && isStreaming) {
                try {
                    val chunk = audioBuffer.peek()

                    if (chunk == null) {
                        // Buffer underrun
                        if (!_bufferState.value.isUnderrun) {
                            logger.w { "Buffer underrun" }
                            adaptiveBufferManager.recordUnderrun(getCurrentTimeMicros())
                            _bufferState.update { it.copy(isUnderrun = true) }
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
                    val timeDiff = chunkPlaybackTime - currentLocalTime

                    // Use adaptive thresholds
                    val lateThreshold = adaptiveBufferManager.currentLateThreshold
                    val earlyThreshold = adaptiveBufferManager.currentEarlyThreshold

                    when {
                        chunkPlaybackTime < currentLocalTime - lateThreshold -> {
                            // Chunk is too late, drop it
                            audioBuffer.poll()
                            droppedChunksCount++
                            adaptiveBufferManager.recordChunkDropped()
                            logger.w { "Dropped late chunk: ${(currentLocalTime - chunkPlaybackTime) / 1000}ms late" }
                            updateBufferState()
                        }

                        chunkPlaybackTime > currentLocalTime + earlyThreshold -> {
                            // Chunk is too early, wait
                            val delayMs =
                                ((chunkPlaybackTime - currentLocalTime) / 1000).coerceAtMost(100)
                            logger.d { "Chunk too early, waiting ${delayMs}ms (diff=${timeDiff / 1000}ms)" }
                            delay(delayMs)
                        }

                        else -> {
                            // Chunk is ready to play
                            playChunk(chunk)
                            audioBuffer.poll()
                            adaptiveBufferManager.recordChunkPlayed()
                            _playbackPosition.update { chunk.timestamp }
                            updateBufferState()
                            chunksPlayed++

                            // Log progress every 5 seconds with adaptive buffer info
                            val now = getCurrentTimeMicros()
                            if (now - lastLogTime > 5_000_000) {
                                val bufferMs = audioBuffer.getBufferedDuration() / 1000
                                val targetMs = adaptiveBufferManager.targetBufferDuration / 1000
                                logger.i { "Playback: $chunksPlayed chunks, buffer=${bufferMs}ms (target=${targetMs}ms)" }
                                lastLogTime = now
                            }
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logger.e(e) { "Error in playback thread" }
                }
            }

            logger.i { "Playback thread stopped, total chunks played: $chunksPlayed" }
        }
    }

    private suspend fun waitForPrebuffer() {
        val threshold = adaptiveBufferManager.currentPrebufferThreshold
        logger.i { "Waiting for prebuffer (threshold=${threshold/1000}ms)..." }
        while (isActive && audioBuffer.getBufferedDuration() < threshold) {
            delay(50)
        }
        val bufferMs = audioBuffer.getBufferedDuration() / 1000
        val thresholdMs = threshold / 1000
        logger.i { "Prebuffer complete: ${bufferMs}ms (threshold=${thresholdMs}ms)" }
    }

    private fun startAdaptationThread() {
        adaptationJob?.cancel()
        adaptationJob = launch {
            logger.i { "Starting adaptation thread" }
            while (isActive && isStreaming) {
                try {
                    // Update network stats from clock synchronizer
                    val stats = clockSynchronizer.getStats()
                    adaptiveBufferManager.updateNetworkStats(stats.rtt, stats.quality)

                    // Run adaptation logic every 5 seconds
                    adaptiveBufferManager.updateThresholds(getCurrentTimeMicros())

                    delay(5000)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logger.e(e) { "Error in adaptation thread" }
                }
            }
            logger.i { "Adaptation thread stopped" }
        }
    }

    private fun playChunk(chunk: AudioChunk) {
        try {
            // Decode audio
            val decoder = audioDecoder ?: return
            val decodedData = decoder.decode(chunk.data)

            logger.d { "Decoded chunk: ${chunk.data.size} -> ${decodedData.size} PCM bytes" }

            // Write to MediaPlayerController
            val written = mediaPlayerController.writeRawPcm(decodedData)
            if (written < decodedData.size) {
                logger.w { "Only wrote $written/${decodedData.size} bytes to AudioTrack" }
            } else {
                logger.d { "Wrote $written bytes to AudioTrack successfully" }
            }

        } catch (e: Exception) {
            logger.e(e) { "Error decoding chunk" }
        }
    }

    private suspend fun updateBufferState() {
        val bufferedDuration = audioBuffer.getBufferedDuration()
        _bufferState.update {
            BufferState(
                bufferedDuration = bufferedDuration,
                isUnderrun = bufferedDuration == 0L && isStreaming,
                droppedChunks = droppedChunksCount,
                // Adaptive metrics
                targetBufferDuration = adaptiveBufferManager.targetBufferDuration,
                currentPrebufferThreshold = adaptiveBufferManager.currentPrebufferThreshold,
                smoothedRTT = adaptiveBufferManager.currentSmoothedRTT,
                jitter = adaptiveBufferManager.currentJitter,
                dropRate = adaptiveBufferManager.getCurrentDropRate()
            )
        }
    }

    suspend fun clearStream() {
        logger.i { "Clearing stream" }
        audioBuffer.clear()
        _playbackPosition.update { 0L }
        droppedChunksCount = 0
        updateBufferState()
    }

    /**
     * Flush audio for track change (next/prev/seek).
     * Clears buffer and stops native audio for immediate responsiveness,
     * but keeps isStreaming=true so the playback thread can receive new chunks.
     */
    suspend fun flushForTrackChange() {
        logger.i { "Flushing for track change (keeping stream active)" }
        // Clear the audio buffer
        audioBuffer.clear()
        // Reset decoder for new track
        audioDecoder?.reset()
        // Stop native audio playback immediately (for responsiveness)
        mediaPlayerController.stopRawPcmStream()
        // Reset playback position
        _playbackPosition.update { 0L }
        droppedChunksCount = 0
        // Reset adaptive buffer for new track
        adaptiveBufferManager.reset()
        updateBufferState()
        // NOTE: isStreaming stays TRUE so we can receive new chunks
    }

    suspend fun stopStream() {
        logger.i { "Stopping stream" }
        isStreaming = false
        playbackJob?.cancel()
        playbackJob = null
        adaptationJob?.cancel()
        adaptationJob = null

        audioBuffer.clear()
        audioDecoder?.reset()

        // Reset adaptive buffer manager
        adaptiveBufferManager.reset()

        // Stop raw PCM stream on MediaPlayerController
        mediaPlayerController.stopRawPcmStream()

        _playbackPosition.update { 0L }
        droppedChunksCount = 0
        _bufferState.update {
            BufferState(
                bufferedDuration = 0L,
                isUnderrun = false,
                droppedChunks = 0,
                targetBufferDuration = 0L,
                currentPrebufferThreshold = 0L,
                smoothedRTT = 0.0,
                jitter = 0.0,
                dropRate = 0.0
            )
        }
        // Clear any error state
        _streamError.update { null }
    }

    // Use monotonic time for playback timing instead of wall clock time
    // This matches the server's relative time base
    // Use monotonic time for playback timing instead of wall clock time
    // This matches the server's relative time base
    private val startMark = kotlin.time.TimeSource.Monotonic.markNow()

    private fun getCurrentTimeMicros(): Long {
        // Use relative time since stream start, not Unix epoch time
        return startMark.elapsedNow().inWholeMicroseconds
    }

    fun close() {
        logger.i { "Closing AudioStreamManager" }
        playbackJob?.cancel()
        audioDecoder?.release()
        supervisorJob.cancel()
    }
}
