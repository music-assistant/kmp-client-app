@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package io.music_assistant.client.player

/**
 * MediaPlayerController - iOS stub for Sendspin
 *
 * Handles raw PCM audio streaming for Sendspin protocol.
 * TODO: Implement using AVAudioEngine or AudioQueue
 */
actual class MediaPlayerController actual constructor(platformContext: PlatformContext) {
    private var isPrepared: Boolean = false
    private var callback: MediaPlayerListener? = null

    // Sendspin raw PCM streaming methods (stub)

    actual fun prepareRawPcmStream(
        sampleRate: Int,
        channels: Int,
        bitDepth: Int,
        listener: MediaPlayerListener
    ) {
        // TODO: Implement using AVAudioEngine or AudioQueue
        callback = listener
        isPrepared = true
        callback?.onReady()
    }

    actual fun writeRawPcm(data: ByteArray): Int {
        // TODO: Implement raw PCM playback
        return data.size // Pretend we wrote everything
    }

    actual fun stopRawPcmStream() {
        // TODO: Implement
        isPrepared = false
    }

    actual fun setVolume(volume: Int) {
        // TODO: Implement using AVAudioEngine
    }

    actual fun setMuted(muted: Boolean) {
        // TODO: Implement using AVAudioEngine
    }

    actual fun release() {
        isPrepared = false
        callback = null
    }

    actual fun getCurrentSystemVolume(): Int {
        // TODO: "Not yet implemented"
        return 100
    }
}

actual class PlatformContext
