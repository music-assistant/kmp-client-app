package io.music_assistant.client.player.sendspin.audio

import io.music_assistant.client.player.sendspin.model.AudioFormatSpec

actual class FlacDecoder : AudioDecoder, PassthroughDecoder {
    override fun configure(config: AudioFormatSpec, codecHeader: String?) {
        // Pass-through: No configuration needed for raw stream passing
    }

    override fun decode(encodedData: ByteArray): ByteArray {
        // Pass-through: Return raw flac data to be handled by MPV
        return encodedData
    }

    override fun reset() {
        // Nothing to reset
    }

    override fun release() {
        // Nothing to release
    }
}
