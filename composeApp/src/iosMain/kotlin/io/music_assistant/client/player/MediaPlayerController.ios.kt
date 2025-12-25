@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package io.music_assistant.client.player

actual class MediaPlayerController actual constructor(platformContext: PlatformContext) {
    private var isPrepared: Boolean = false
    private var isPlayingInternal: Boolean = false
    private var currentPositionMs: Long = 0
    private var durationMs: Long? = null
    private var callback: MediaPlayerListener? = null

    actual fun prepare(
        pathSource: String,
        listener: MediaPlayerListener
    ) {
        // Minimal no-op implementation to avoid crashes on iOS until AVPlayer is wired.
        callback = listener
        isPrepared = true
        isPlayingInternal = false
        currentPositionMs = 0
        durationMs = null
        callback?.onReady()
    }

    actual fun start() {
        if (isPrepared) {
            isPlayingInternal = true
        }
    }

    actual fun pause() {
        isPlayingInternal = false
    }

    actual fun stop() {
        isPlayingInternal = false
        currentPositionMs = 0
    }

    actual fun getCurrentPosition(): Long? {
        return currentPositionMs
    }

    actual fun getDuration(): Long? {
        return durationMs
    }

    actual fun seekTo(seconds: Long) {
        currentPositionMs = seconds * 1000
    }

    actual fun isPlaying(): Boolean {
        return isPlayingInternal
    }

    actual fun release() {
        isPrepared = false
        isPlayingInternal = false
        callback = null
    }

    // Raw PCM streaming methods for Sendspin (stub)

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
}

actual class PlatformContext