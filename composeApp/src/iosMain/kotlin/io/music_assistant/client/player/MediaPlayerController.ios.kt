@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package io.music_assistant.client.player

actual class MediaPlayerController actual constructor(platformContext: PlatformContext) {
    actual fun prepare(
        pathSource: String,
        listener: MediaPlayerListener
    ) {
    }

    actual fun start() {
    }

    actual fun pause() {
    }

    actual fun stop() {
    }

    actual fun getCurrentPosition(): Long? {
        TODO("Not yet implemented")
    }

    actual fun getDuration(): Long? {
        TODO("Not yet implemented")
    }

    actual fun seekTo(seconds: Long) {
    }

    actual fun isPlaying(): Boolean {
        TODO("Not yet implemented")
    }

    actual fun release() {
    }

}

actual class PlatformContext