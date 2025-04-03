package io.music_assistant.client.data.model.client

import io.music_assistant.client.data.model.server.PlayerFeature
import io.music_assistant.client.data.model.server.PlayerState
import io.music_assistant.client.data.model.server.ServerPlayer

data class Player(
    val id: String,
    val name: String,
    val shouldBeShown: Boolean,
    val canSetVolume: Boolean,
    val queueId: String?,
    val isPlaying: Boolean,
    val isAnnouncing: Boolean,
) {
    companion object {
        const val LOCAL_PLAYER_ID = "local"
        const val LOCAL_PLAYER_NAME = "Local player"
        const val LOCAL_QUEUE_ID = "local"

        fun local(isPlaying: Boolean) = Player(
            id = LOCAL_PLAYER_ID,
            name = LOCAL_PLAYER_NAME,
            shouldBeShown = true,
            canSetVolume = true,
            queueId = LOCAL_QUEUE_ID,
            isPlaying = isPlaying,
            isAnnouncing = false,
        )

        fun ServerPlayer.toPlayer() = Player(
            id = playerId,
            name = displayName,
            shouldBeShown = available && enabled && (hidden != true),
            canSetVolume = supportedFeatures.contains(PlayerFeature.VOLUME_SET),
            queueId = currentMedia?.queueId ?: activeSource,
            isPlaying = state == PlayerState.PLAYING,
            isAnnouncing = announcementInProgress == true,
        )
    }
}