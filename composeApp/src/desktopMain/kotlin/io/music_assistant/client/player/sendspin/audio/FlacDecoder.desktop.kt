package io.music_assistant.client.player.sendspin.audio

import io.music_assistant.client.player.sendspin.model.AudioFormatSpec

actual class FlacDecoder : AudioDecoder {
    override fun configure(config: AudioFormatSpec, codecHeader: String?) {
        // TODO: Implement FLAC decoder using javax.sound or external library
        throw NotImplementedError("FLAC decoder not yet implemented for Desktop. Use PCM codec for now.")
    }

    override fun decode(encodedData: ByteArray): ByteArray {
        throw NotImplementedError("FLAC decoder not yet implemented for Desktop. Use PCM codec for now.")
    }

    override fun reset() {
        // Nothing to reset
    }

    override fun release() {
        // Nothing to release
    }
}
