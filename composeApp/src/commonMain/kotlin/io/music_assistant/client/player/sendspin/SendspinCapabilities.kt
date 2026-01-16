package io.music_assistant.client.player.sendspin

import io.music_assistant.client.player.sendspin.model.*
import io.music_assistant.client.player.sendspin.isOpusPlaybackSupported
import io.music_assistant.client.player.sendspin.isFlacPlaybackSupported

object SendspinCapabilities {
    fun buildClientHello(config: SendspinConfig): ClientHelloPayload {
        return ClientHelloPayload(
            clientId = config.clientId,
            name = config.deviceName,
            deviceInfo = DeviceInfo.current,
            version = 1,
            supportedRoles = listOf(
                VersionedRole.PLAYER_V1,
                VersionedRole.METADATA_V1
            ),
            playerV1Support = PlayerSupport(
                supportedFormats = listOf(
                    // PCM - 48kHz, stereo, 16-bit
                    AudioFormatSpec(
                        codec = AudioCodec.PCM,
                        channels = 2,
                        sampleRate = 48000,
                        bitDepth = 16
                    )
                ).toMutableList().apply {
                    if (isOpusPlaybackSupported) {
                        add(
                            AudioFormatSpec(
                                codec = AudioCodec.OPUS,
                                channels = 2,
                                sampleRate = 48000,
                                bitDepth = 16
                            )
                        )
                        add(
                            AudioFormatSpec(
                                codec = AudioCodec.OPUS,
                                channels = 1,
                                sampleRate = 48000,
                                bitDepth = 16
                            )
                        )
                    }
                    if (isFlacPlaybackSupported) {
                         // Add FLAC if supported
                         add(
                            AudioFormatSpec(
                                codec = AudioCodec.FLAC,
                                channels = 2,
                                sampleRate = 48000,
                                bitDepth = 16 // FLAC can vary but claiming standard support
                            )
                         )
                    }
                },
                bufferCapacity = config.bufferCapacityMicros,
                supportedCommands = listOf()
            ),
            metadataV1Support = MetadataSupport(
                supportedPictureFormats = emptyList()
            ),
            artworkV1Support = null,
            visualizerV1Support = null
        )
    }
}
