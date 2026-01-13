package io.music_assistant.client.data.model.client

import io.music_assistant.client.data.model.server.PlayerFeature
import io.music_assistant.client.data.model.server.PlayerState
import io.music_assistant.client.data.model.server.PlayerType
import io.music_assistant.client.data.model.server.ServerPlayer

data class Player(
    val id: String,
    val name: String,
    val provider: String,
    val type: PlayerType,
    val shouldBeShown: Boolean,
    val canSetVolume: Boolean,
    val volumeLevel: Float?,
    val volumeMuted: Boolean,
    val canMute: Boolean,
    val queueId: String?,
    val isPlaying: Boolean,
    val isAnnouncing: Boolean,
) {
    companion object {
        private const val PLAYER_CONTROL_NONE = "none"

        fun ServerPlayer.toPlayer() = Player(
            id = playerId,
            name = displayName,
            provider = provider,
            type = type,
            shouldBeShown = available && enabled && (hidden != true),
            canSetVolume = supportedFeatures.contains(PlayerFeature.VOLUME_SET),
            volumeLevel = volumeLevel?.toFloat(),
            volumeMuted = volumeMuted == true,
            canMute = muteControl != null && muteControl != PLAYER_CONTROL_NONE,
            queueId = currentMedia?.queueId ?: activeSource,
            isPlaying = state == PlayerState.PLAYING,
            isAnnouncing = announcementInProgress == true,
        )
    }
}