@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package io.music_assistant.client.player

import co.touchlab.kermit.Logger
import javax.sound.sampled.*

/**
 * MediaPlayerController - Desktop implementation for Sendspin
 *
 * Handles raw PCM audio streaming for Sendspin protocol using javax.sound.sampled API.
 */
actual class MediaPlayerController actual constructor(val platformContext: PlatformContext) {
    private val logger = Logger.withTag("MediaPlayerController")

    private var sourceDataLine: SourceDataLine? = null
    private var listener: MediaPlayerListener? = null

    // Volume state (0-100)
    private var currentVolume: Int = 100
    private var isMuted: Boolean = false

    actual fun prepareRawPcmStream(
        sampleRate: Int,
        channels: Int,
        bitDepth: Int,
        listener: MediaPlayerListener
    ) {
        logger.i { "Preparing raw PCM stream: ${sampleRate}Hz, ${channels}ch, ${bitDepth}bit" }

        this.listener = listener

        // Release existing line if any
        sourceDataLine?.let {
            if (it.isOpen) {
                it.stop()
                it.close()
            }
        }

        try {
            // Convert parameters to javax.sound AudioFormat
            val encoding = when (bitDepth) {
                8 -> AudioFormat.Encoding.PCM_UNSIGNED
                16 -> AudioFormat.Encoding.PCM_SIGNED
                24 -> AudioFormat.Encoding.PCM_SIGNED
                32 -> AudioFormat.Encoding.PCM_SIGNED
                else -> {
                    logger.w { "Unsupported bit depth: $bitDepth, using 16-bit" }
                    AudioFormat.Encoding.PCM_SIGNED
                }
            }

            val frameSize = (bitDepth / 8) * channels
            val frameRate = sampleRate.toFloat()
            val bigEndian = false // Use little-endian to match most systems

            val audioFormat = AudioFormat(
                encoding,
                frameRate,
                bitDepth,
                channels,
                frameSize,
                frameRate,
                bigEndian
            )

            logger.i { "AudioFormat: $audioFormat" }

            // Get the data line info
            val info = DataLine.Info(SourceDataLine::class.java, audioFormat)

            if (!AudioSystem.isLineSupported(info)) {
                val error = "Audio line not supported for format: $audioFormat"
                logger.e { error }
                listener.onError(Exception(error))
                return
            }

            // Get and open the line
            val line = AudioSystem.getLine(info) as SourceDataLine

            // Calculate buffer size (use 500ms buffer like Sendspin config)
            val bufferSize = (sampleRate * channels * (bitDepth / 8) * 0.5).toInt()
            logger.i { "Opening line with buffer size: $bufferSize bytes" }

            line.open(audioFormat, bufferSize)
            sourceDataLine = line

            // Apply current volume and mute state
            applyVolume()

            // Start the line
            line.start()

            logger.i { "SourceDataLine opened and started successfully" }
            listener.onReady()

        } catch (e: LineUnavailableException) {
            logger.e(e) { "Failed to open audio line" }
            listener.onError(e)
        } catch (e: Exception) {
            logger.e(e) { "Failed to prepare PCM stream" }
            listener.onError(e)
        }
    }

    actual fun writeRawPcm(data: ByteArray): Int {
        val line = sourceDataLine
        if (line == null || !line.isOpen) {
            logger.w { "SourceDataLine not initialized or not open" }
            return 0
        }

        return try {
            if (!line.isRunning) {
                logger.w { "SourceDataLine not running" }
            }

            val written = line.write(data, 0, data.size)
            logger.d { "SourceDataLine wrote $written/${data.size} bytes" }
            written

        } catch (e: Exception) {
            logger.e(e) { "Error writing PCM data" }
            0
        }
    }

    actual fun stopRawPcmStream() {
        logger.i { "Stopping raw PCM stream" }

        listener = null

        sourceDataLine?.let { line ->
            try {
                if (line.isRunning) {
                    line.stop()
                }
                line.flush()
                line.close()
                logger.i { "SourceDataLine stopped and closed" }
            } catch (e: Exception) {
                logger.e(e) { "Error stopping SourceDataLine" }
            }
        }

        sourceDataLine = null
    }

    actual fun setVolume(volume: Int) {
        currentVolume = volume.coerceIn(0, 100)
        logger.i { "Setting volume to $currentVolume" }
        applyVolume()
    }

    actual fun setMuted(muted: Boolean) {
        isMuted = muted
        logger.i { "Setting muted to $muted" }
        applyVolume()
    }

    actual fun getCurrentSystemVolume(): Int {
        // Desktop system volume is platform-specific and complex to retrieve
        // Return current app volume instead
        logger.d { "getCurrentSystemVolume() returning app volume: $currentVolume" }
        return currentVolume
    }

    private fun applyVolume() {
        val line = sourceDataLine ?: return

        try {
            // Try to get MUTE control
            if (line.isControlSupported(BooleanControl.Type.MUTE)) {
                val muteControl = line.getControl(BooleanControl.Type.MUTE) as BooleanControl
                muteControl.value = isMuted
                logger.d { "Applied mute: $isMuted" }
            }

            // Try to get MASTER_GAIN control for volume
            if (line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                val gainControl = line.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl

                // Convert volume (0-100) to decibels
                // Gain ranges from min (typically -80dB) to max (typically 6dB)
                val minGain = gainControl.minimum
                val maxGain = gainControl.maximum

                val gainValue = if (isMuted || currentVolume == 0) {
                    minGain
                } else {
                    // Linear scale: 0-100 -> minGain to maxGain
                    val volumeRatio = currentVolume / 100.0
                    val gainRange = maxGain - minGain
                    minGain + (gainRange * volumeRatio).toFloat()
                }

                gainControl.value = gainValue.coerceIn(minGain, maxGain)
                logger.d { "Applied gain: ${gainControl.value} dB (volume=$currentVolume, muted=$isMuted, range=$minGain to $maxGain)" }
            } else {
                logger.w { "MASTER_GAIN control not supported on this audio line" }
            }

        } catch (e: Exception) {
            logger.e(e) { "Error applying volume/mute" }
        }
    }

    actual fun release() {
        logger.i { "Releasing MediaPlayerController" }
        stopRawPcmStream()
    }
}

actual class PlatformContext
