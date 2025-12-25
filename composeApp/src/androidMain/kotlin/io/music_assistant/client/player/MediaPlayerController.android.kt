@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package io.music_assistant.client.player

import android.content.Context
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Player.STATE_ENDED
import androidx.media3.common.Player.STATE_READY
import androidx.media3.exoplayer.ExoPlayer
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

actual class MediaPlayerController actual constructor(platformContext: PlatformContext) {
    private val logger = Logger.withTag("MediaPlayerController")

    private val player = ExoPlayer.Builder(platformContext.applicationContext).build().apply {
        setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build(),
            true
        )
    }

    // Raw PCM streaming
    private var audioTrack: AudioTrack? = null
    private var isPcmStreamMode = false

    actual fun prepare(pathSource: String, listener: MediaPlayerListener) {
        val mediaItem = MediaItem.fromUri(pathSource)
        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                super.onPlayerError(error)
                listener.onError(error)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                when (playbackState) {
                    STATE_READY -> listener.onReady()
                    STATE_ENDED -> listener.onAudioCompleted()
                    Player.STATE_BUFFERING,
                    Player.STATE_IDLE -> Unit
                }
            }

            override fun onPlayerErrorChanged(error: PlaybackException?) {
                super.onPlayerErrorChanged(error)
                listener.onError(error)
            }
        })
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
    }

    actual fun start() {
        player.play()
    }

    actual fun pause() {
        if (player.isPlaying)
            player.pause()
    }

    actual fun seekTo(seconds: Long) {
        if (player.isPlaying)
            player.seekTo(seconds)
    }

    actual fun getCurrentPosition(): Long? {
        return player.currentPosition
    }

    actual fun getDuration(): Long? {
        return player.duration
    }

    actual fun stop() {
        player.stop()
    }

    actual fun release() {
        player.release()
    }

    actual fun isPlaying(): Boolean {
        return player.isPlaying
    }

    // Raw PCM streaming methods for Sendspin

    actual fun prepareRawPcmStream(
        sampleRate: Int,
        channels: Int,
        bitDepth: Int,
        listener: MediaPlayerListener
    ) {
        logger.i { "Preparing raw PCM stream: ${sampleRate}Hz, ${channels}ch, ${bitDepth}bit" }

        isPcmStreamMode = true

        // Stop ExoPlayer if running
        runBlocking {
            withContext(Dispatchers.Main) {
                if (player.isPlaying) {
                    player.stop()
                }
            }
        }

        // Release existing AudioTrack
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

        val encoding = when (bitDepth) {
            8 -> AudioFormat.ENCODING_PCM_8BIT
            16 -> AudioFormat.ENCODING_PCM_16BIT
            24 -> AudioFormat.ENCODING_PCM_24BIT_PACKED
            32 -> AudioFormat.ENCODING_PCM_32BIT
            else -> {
                logger.w { "Unsupported bit depth: $bitDepth, using 16-bit" }
                AudioFormat.ENCODING_PCM_16BIT
            }
        }

        // Calculate buffer size
        val minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, encoding)
        val bufferSize = minBufferSize * 4 // Use 4x min buffer for smoother playback

        logger.d { "AudioTrack buffer size: $bufferSize bytes (min: $minBufferSize)" }

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

            audioTrack?.play()
            logger.i { "AudioTrack started successfully" }
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

        if (!isPcmStreamMode) {
            logger.w { "Not in PCM stream mode" }
            return 0
        }

        return try {
            val written = track.write(data, 0, data.size)
            if (written < 0) {
                logger.w { "AudioTrack write error: $written" }
                0
            } else {
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
        isPcmStreamMode = false
    }
}

actual class PlatformContext(val applicationContext: Context)