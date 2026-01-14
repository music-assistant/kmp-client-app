package io.music_assistant.client.utils

import io.music_assistant.client.player.sendspin.audio.Codec

/**
 * Platform types supported by the application.
 */
enum class PlatformType {
    ANDROID,
    DESKTOP,
    IOS
}

/**
 * Platform detection for conditional feature availability.
 */
expect object Codecs {
    /**
     * Returns the current platform type.
     */
    val list: List<Codec>
}
