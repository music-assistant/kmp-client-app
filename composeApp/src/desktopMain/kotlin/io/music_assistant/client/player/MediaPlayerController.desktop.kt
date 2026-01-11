@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package io.music_assistant.client.player

/**
 * MediaPlayerController - Desktop stub for Sendspin
 *
 * Handles raw PCM audio streaming for Sendspin protocol.
 * TODO: Implement using javax.sound SourceDataLine or similar
 */
actual class MediaPlayerController actual constructor(val platformContext: PlatformContext) {
    private var listener: MediaPlayerListener? = null

    // Sendspin raw PCM streaming methods (stub)

    actual fun prepareRawPcmStream(
        sampleRate: Int,
        channels: Int,
        bitDepth: Int,
        listener: MediaPlayerListener
    ) {
        // TODO: Implement using javax.sound SourceDataLine or similar
        this.listener = listener
        listener.onReady()
    }

    actual fun writeRawPcm(data: ByteArray): Int {
        // TODO: Implement raw PCM playback
        return data.size // Pretend we wrote everything
    }

    actual fun stopRawPcmStream() {
        // TODO: Implement
    }

    actual fun setVolume(volume: Int) {
        // TODO: Implement using javax.sound
    }

    actual fun setMuted(muted: Boolean) {
        // TODO: Implement using javax.sound
    }

    actual fun getCurrentSystemVolume(): Int {
        // TODO: Implement using javax.sound
        return 100
    }

    actual fun release() {
        listener = null
    }
}

actual class PlatformContext
