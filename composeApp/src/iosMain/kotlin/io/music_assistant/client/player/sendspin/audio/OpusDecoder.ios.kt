package io.music_assistant.client.player.sendspin.audio

import io.music_assistant.client.player.sendspin.model.AudioFormatSpec

actual class OpusDecoder : AudioDecoder {
    override fun configure(config: AudioFormatSpec, codecHeader: String?) {
        // TODO: Implement OPUS decoder using AudioToolbox
        throw NotImplementedError("OPUS decoder not yet implemented for iOS. Use PCM codec for now.")
    }

    override fun decode(encodedData: ByteArray): ByteArray {
        throw NotImplementedError("OPUS decoder not yet implemented for iOS. Use PCM codec for now.")
    }

    override fun reset() {
        // Nothing to reset
    }

    override fun release() {
        // Nothing to release
    }
}
