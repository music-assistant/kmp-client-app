package io.music_assistant.client.services

import io.music_assistant.client.data.model.client.PlayerData
import io.music_assistant.client.data.model.server.RepeatMode

data class MediaNotificationData(
    val multiplePlayers: Boolean,
    val longItemId: Long?,
    val name: String?,
    val artist: String?,
    val album: String?,
    val repeatMode: RepeatMode?,
    val shuffleEnabled: Boolean?,
    val isPlaying: Boolean,
    val imageUrl: String?,
    val elapsedTime: Long?,
    val playerName: String,
    val duration: Long?,
) {
    companion object {
        fun from(
            serverUrl: String?,
            playerData: PlayerData,
            multiplePlayers: Boolean,
        ) = MediaNotificationData(
            multiplePlayers = multiplePlayers,
            longItemId =
                playerData.queue
                    ?.currentItem
                    ?.track
                    ?.longId,
            name =
                playerData.queue
                    ?.currentItem
                    ?.track
                    ?.name,
            artist =
                playerData.queue
                    ?.currentItem
                    ?.track
                    ?.subtitle,
            album =
                playerData.queue
                    ?.currentItem
                    ?.track
                    ?.album
                    ?.name,
            repeatMode = playerData.queue?.repeatMode,
            shuffleEnabled = playerData.queue?.shuffleEnabled,
            isPlaying = playerData.player.isPlaying,
            imageUrl =
                playerData.queue
                    ?.currentItem
                    ?.track
                    ?.imageInfo
                    ?.url(serverUrl),
            elapsedTime =
                playerData.queue
                    ?.elapsedTime
                    ?.toLong()
                    ?.let { it * 1000 },
            playerName = playerData.player.name,
            duration =
                playerData.queue
                    ?.currentItem
                    ?.track
                    ?.duration
                    ?.toLong()
                    ?.let { it * 1000 },
        )

        fun areTooSimilarToUpdate(
            old: MediaNotificationData,
            new: MediaNotificationData,
        ): Boolean {
            if (old.copy(elapsedTime = null) != new.copy(elapsedTime = null)) {
                return false
            }
            if (old.elapsedTime == null) {
                return new.elapsedTime == null
            }
            if (new.elapsedTime == null) {
                return false
            }
            if (old.elapsedTime > new.elapsedTime) {
                return false
            }
            if (new.elapsedTime - old.elapsedTime > 10000) {
                return false
            }
            return true
        }
    }
}
