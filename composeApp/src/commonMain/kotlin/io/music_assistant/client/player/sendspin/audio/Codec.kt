package io.music_assistant.client.player.sendspin.audio

import io.music_assistant.client.player.sendspin.model.AudioCodec
import io.music_assistant.client.utils.Codecs

enum class Codec(
    private val title: String,
    val decoderInitializer: () -> AudioDecoder,
    val sendspinAudioCodec: AudioCodec
) {
    PCM("PCM", { PcmDecoder() }, AudioCodec.PCM),
    FLAC("FLAC", { FlacDecoder() }, AudioCodec.FLAC),
    OPUS("Opus", { OpusDecoder() }, AudioCodec.OPUS);

    fun uiTitle() = "${title}${if (this == Codecs.list.getOrNull(0)) " (recommended)" else ""}"
}

fun codecByName(name: String): Codec? =
    try {
        Codec.valueOf(name)
    } catch (_: Exception) {
        null
    }