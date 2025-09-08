package io.music_assistant.client.data.model.client

import io.music_assistant.client.data.model.server.PlayerFeature
import io.music_assistant.client.data.model.server.PlayerState
import io.music_assistant.client.data.model.server.ServerPlayer
import kotlinx.serialization.Serializable

@Serializable
data class Player(
    val id: String,
    val name: String,
    val shouldBeShown: Boolean,
    val canSetVolume: Boolean,
    val queueId: String?,
    val isPlaying: Boolean,
    val isAnnouncing: Boolean,
    val isBuiltin: Boolean,
) {
    companion object {
        const val LOCAL_PLAYER_NAME = "This device"
        private const val BUILTIN_PLAYER = "builtin_player"

        fun ServerPlayer.toPlayer() = Player(
            id = playerId,
            name = displayName,
            shouldBeShown = available && enabled && (hidden != true),
            canSetVolume = supportedFeatures.contains(PlayerFeature.VOLUME_SET) && provider != BUILTIN_PLAYER,
            queueId = currentMedia?.queueId ?: activeSource,
            isPlaying = state == PlayerState.PLAYING,
            isAnnouncing = announcementInProgress == true,
            isBuiltin = provider == BUILTIN_PLAYER
        )
    }
}