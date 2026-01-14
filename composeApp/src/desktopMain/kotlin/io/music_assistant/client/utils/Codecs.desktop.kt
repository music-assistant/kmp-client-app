package io.music_assistant.client.utils

import io.music_assistant.client.player.sendspin.audio.Codec

actual object Codecs {
    actual val list: List<Codec> = listOf(Codec.OPUS, Codec.PCM)
}
