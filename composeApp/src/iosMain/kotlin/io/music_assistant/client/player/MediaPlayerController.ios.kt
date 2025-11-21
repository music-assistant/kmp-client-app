@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package io.music_assistant.client.player

actual class MediaPlayerController actual constructor(
    platformContext: PlatformContext,
) {
    private var isPrepared: Boolean = false
    private var isPlayingInternal: Boolean = false
    private var currentPositionMs: Long = 0
    private var durationMs: Long? = null
    private var callback: MediaPlayerListener? = null

    actual fun prepare(
        pathSource: String,
        listener: MediaPlayerListener,
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

    actual fun getCurrentPosition(): Long? = currentPositionMs

    actual fun getDuration(): Long? = durationMs

    actual fun seekTo(seconds: Long) {
        currentPositionMs = seconds * 1000
    }

    actual fun isPlaying(): Boolean = isPlayingInternal

    actual fun release() {
        isPrepared = false
        isPlayingInternal = false
        callback = null
    }
}

actual class PlatformContext
