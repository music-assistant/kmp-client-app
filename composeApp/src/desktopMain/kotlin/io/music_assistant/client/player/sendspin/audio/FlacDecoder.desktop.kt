package io.music_assistant.client.player.sendspin.audio

import io.music_assistant.client.player.sendspin.model.AudioFormatSpec

/**
 * Desktop FLAC decoder stub.
 *
 * FLAC decoding is not available on desktop due to architectural limitations
 * of available Java FLAC libraries (file-oriented vs frame-level streaming).
 *
 * Use Opus (recommended) or PCM codecs instead on desktop.
 */
actual class FlacDecoder : AudioDecoder {
    override fun configure(config: AudioFormatSpec, codecHeader: String?) {
        throw NotImplementedError(
            "FLAC decoder not available on desktop. " +
            "Please use Opus (recommended) or PCM codec instead in settings."
        )
    }

    override fun decode(encodedData: ByteArray): ByteArray {
        throw NotImplementedError("FLAC decoder not available on desktop")
    }

    override fun reset() {
        // No-op
    }

    override fun release() {
        // No-op
    }
}
