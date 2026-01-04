package io.music_assistant.client.player.sendspin

import io.music_assistant.client.player.sendspin.model.AudioCodec
import io.music_assistant.client.player.sendspin.model.AudioFormatSpec
import io.music_assistant.client.player.sendspin.model.ClientHelloPayload
import io.music_assistant.client.player.sendspin.model.DeviceInfo
import io.music_assistant.client.player.sendspin.model.MetadataSupport
import io.music_assistant.client.player.sendspin.model.PlayerSupport
import io.music_assistant.client.player.sendspin.model.VersionedRole

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
                    // Start with PCM - 48kHz, stereo, 16-bit
                    AudioFormatSpec(
                        codec = AudioCodec.PCM,
                        channels = 2,
                        sampleRate = 48000,
                        bitDepth = 16
                    )
                    // TODO: Add FLAC and OPUS later
                ),
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
