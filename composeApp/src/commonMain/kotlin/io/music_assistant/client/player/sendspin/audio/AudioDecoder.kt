package io.music_assistant.client.player.sendspin.audio

import io.music_assistant.client.player.sendspin.model.AudioFormatSpec

interface AudioDecoder {
    fun configure(config: AudioFormatSpec, codecHeader: String?)
    fun decode(encodedData: ByteArray): ByteArray
    fun reset()
    fun release()
}

class PcmDecoder : AudioDecoder {
    override fun configure(config: AudioFormatSpec, codecHeader: String?) {
        // PCM needs no configuration
    }

    override fun decode(encodedData: ByteArray): ByteArray {
        // PCM is already decoded, just pass through
        return encodedData
    }

    override fun reset() {
        // Nothing to reset
    }

    override fun release() {
        // Nothing to release
    }
}

// Platform-specific decoders for FLAC and OPUS (future implementation)
expect class FlacDecoder() : AudioDecoder {
    override fun configure(config: AudioFormatSpec, codecHeader: String?)
    override fun decode(encodedData: ByteArray): ByteArray
    override fun reset()
    override fun release()
}

expect class OpusDecoder() : AudioDecoder {
    override fun configure(config: AudioFormatSpec, codecHeader: String?)
    override fun decode(encodedData: ByteArray): ByteArray
    override fun reset()
    override fun release()
}
