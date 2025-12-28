@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package io.music_assistant.client.player

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import co.touchlab.kermit.Logger

/**
 * MediaPlayerController - Sendspin audio player
 *
 * Handles raw PCM audio streaming for Sendspin protocol.
 * Built-in player (ExoPlayer) has been removed - Sendspin is now the only playback method.
 */
actual class MediaPlayerController actual constructor(platformContext: PlatformContext) {
    private val logger = Logger.withTag("MediaPlayerController")
    private val context: Context = platformContext.applicationContext
    private val audioManager: AudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    // AudioTrack for raw PCM streaming (Sendspin)
    private var audioTrack: AudioTrack? = null

    // AudioFocus management for Android Auto
    private var audioFocusRequest: AudioFocusRequest? = null
    private var hasAudioFocus = false

    // Volume state (0-100)
    private var currentVolume: Int = 100
    private var isMuted: Boolean = false

    // AudioFocus listener for handling focus changes (Android Auto, phone calls, etc.)
    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                logger.i { "AudioFocus gained" }
                hasAudioFocus = true
                // Resume playback if it was paused
                audioTrack?.let { track ->
                    if (track.playState != AudioTrack.PLAYSTATE_PLAYING) {
                        track.play()
                    }
                }
                // Restore volume if it was ducked
                applyVolume()
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                logger.i { "AudioFocus lost permanently" }
                hasAudioFocus = false
                // Pause playback
                audioTrack?.pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                logger.i { "AudioFocus lost temporarily" }
                hasAudioFocus = false
                // Pause playback
                audioTrack?.pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                logger.i { "AudioFocus lost temporarily (can duck)" }
                // Lower volume (duck)
                audioTrack?.setVolume(0.2f)
            }
        }
    }

    // Request audio focus for playback (critical for Android Auto)
    private fun requestAudioFocus(): Boolean {
        if (hasAudioFocus) {
            logger.d { "Already have audio focus" }
            return true
        }

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(audioAttributes)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()
            audioFocusRequest = request
            audioManager.requestAudioFocus(request)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }

        hasAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        logger.i { "Audio focus request: ${if (hasAudioFocus) "GRANTED" else "DENIED"}" }
        return hasAudioFocus
    }

    // Release audio focus
    private fun releaseAudioFocus() {
        if (!hasAudioFocus) {
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let {
                audioManager.abandonAudioFocusRequest(it)
            }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(audioFocusChangeListener)
        }

        hasAudioFocus = false
        audioFocusRequest = null
        logger.i { "Audio focus released" }
    }

    // Sendspin raw PCM streaming methods

    actual fun prepareRawPcmStream(
        sampleRate: Int,
        channels: Int,
        bitDepth: Int,
        listener: MediaPlayerListener
    ) {
        logger.i { "Preparing raw PCM stream: ${sampleRate}Hz, ${channels}ch, ${bitDepth}bit" }

        // Request audio focus before creating AudioTrack
        if (!requestAudioFocus()) {
            logger.w { "Failed to gain audio focus, but continuing anyway" }
        }

        // Release existing AudioTrack if any
        audioTrack?.release()

        // Convert parameters to Android AudioFormat constants
        val channelConfig = when (channels) {
            1 -> AudioFormat.CHANNEL_OUT_MONO
            2 -> AudioFormat.CHANNEL_OUT_STEREO
            else -> {
                logger.w { "Unsupported channel count: $channels, using stereo" }
                AudioFormat.CHANNEL_OUT_STEREO
            }
        }

        val encoding = when {
            bitDepth == 8 -> AudioFormat.ENCODING_PCM_8BIT
            bitDepth == 16 -> AudioFormat.ENCODING_PCM_16BIT
            bitDepth == 24 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> AudioFormat.ENCODING_PCM_24BIT_PACKED
            bitDepth == 32 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> AudioFormat.ENCODING_PCM_32BIT
            else -> {
                logger.w { "Unsupported bit depth: $bitDepth, using 16-bit" }
                AudioFormat.ENCODING_PCM_16BIT
            }
        }

        // Calculate buffer size
        val minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, encoding)
        val bufferSize = minBufferSize * 4 // Use 4x min buffer for smoother playback

        logger.i { "AudioTrack config: sampleRate=$sampleRate, channels=$channels, bitDepth=$bitDepth" }
        logger.i { "AudioTrack buffer: $bufferSize bytes (min: $minBufferSize)" }

        try {
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelConfig)
                        .setEncoding(encoding)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            val state = audioTrack?.state
            val playState = audioTrack?.playState
            logger.i { "AudioTrack created: state=$state (${if (state == AudioTrack.STATE_INITIALIZED) "INITIALIZED" else "UNINITIALIZED"}), playState=$playState" }

            audioTrack?.play()

            val newPlayState = audioTrack?.playState
            logger.i { "AudioTrack started: playState=$newPlayState (${if (newPlayState == AudioTrack.PLAYSTATE_PLAYING) "PLAYING" else "NOT_PLAYING"})" }

            // Apply current volume/mute state
            applyVolume()

            listener.onReady()

        } catch (e: Exception) {
            logger.e(e) { "Failed to create AudioTrack" }
            listener.onError(e)
        }
    }

    actual fun writeRawPcm(data: ByteArray): Int {
        val track = audioTrack
        if (track == null) {
            logger.w { "AudioTrack not initialized" }
            return 0
        }

        return try {
            val playState = track.playState
            val state = track.state

            if (playState != AudioTrack.PLAYSTATE_PLAYING) {
                logger.w { "AudioTrack not playing! playState=$playState, state=$state" }
            }

            val written = track.write(data, 0, data.size)
            if (written < 0) {
                val errorName = when (written) {
                    AudioTrack.ERROR_INVALID_OPERATION -> "ERROR_INVALID_OPERATION"
                    AudioTrack.ERROR_BAD_VALUE -> "ERROR_BAD_VALUE"
                    AudioTrack.ERROR_DEAD_OBJECT -> "ERROR_DEAD_OBJECT"
                    else -> "UNKNOWN_ERROR($written)"
                }
                logger.w { "AudioTrack write error: $errorName" }
                0
            } else {
                logger.d { "AudioTrack wrote $written/${data.size} bytes" }
                written
            }
        } catch (e: Exception) {
            logger.e(e) { "Error writing PCM data" }
            0
        }
    }

    actual fun stopRawPcmStream() {
        logger.i { "Stopping raw PCM stream" }

        audioTrack?.let { track ->
            try {
                if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    track.pause()
                }
                track.flush()
                track.stop()
                track.release()
            } catch (e: Exception) {
                logger.e(e) { "Error stopping AudioTrack" }
            }
        }

        audioTrack = null
        releaseAudioFocus()
    }

    actual fun setVolume(volume: Int) {
        currentVolume = volume.coerceIn(0, 100)
        logger.i { "Setting volume to $currentVolume" }
        applyVolume()
    }

    actual fun setMuted(muted: Boolean) {
        isMuted = muted
        logger.i { "Setting muted to $muted (audioTrack=${if (audioTrack != null) "initialized" else "null"})" }
        applyVolume()
    }

    private fun applyVolume() {
        val track = audioTrack ?: return

        // AudioTrack uses 0.0 to 1.0 scale
        val volumeFloat = if (isMuted) {
            0f
        } else {
            (currentVolume / 100f).coerceIn(0f, 1f)
        }

        try {
            track.setVolume(volumeFloat)
            logger.d { "Applied volume: $volumeFloat (volume=$currentVolume, muted=$isMuted)" }
        } catch (e: Exception) {
            logger.e(e) { "Error setting volume" }
        }
    }

    actual fun release() {
        logger.i { "Releasing MediaPlayerController" }
        stopRawPcmStream()
        releaseAudioFocus()
    }
}

actual class PlatformContext(val applicationContext: Context)