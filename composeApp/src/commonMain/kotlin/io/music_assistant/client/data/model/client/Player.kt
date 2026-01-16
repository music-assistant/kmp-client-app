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
    val canGroupWith: List<String>?,
    val groupChildren: List<String>?,
    //val activeGroup: String?,
    val groupVolume: Float?,
) {

    val displayName =
        "$name${groupChildren?.takeIf { it.size > 1 }?.size?.let { " +${it - 1}" } ?: ""}"

    val providerType = provider.substringBefore("--")

    val currentVolume = if (groupChildren?.isNotEmpty() == true) groupVolume else volumeLevel

    fun asBindFor(other: Player): PlayerData.Bind? {
        if (id == other.id) return null
        if (other.canGroupWith?.contains(providerType) != true) return null
        return PlayerData.Bind(
            id = id,
            parentId = other.id,
            name = name,
            volume = volumeLevel,
            isBound = other.groupChildren?.contains(id) == true,
        )
    }

    companion object {
        private const val PLAYER_CONTROL_NONE = "none"

        fun ServerPlayer.toPlayer() = Player(
            id = playerId,
            name = displayName,
            provider = provider,
            type = type,
            shouldBeShown = available && enabled && (hidden != true),
            canSetVolume = supportedFeatures.contains(PlayerFeature.VOLUME_SET),
            volumeLevel = volumeLevel,
            volumeMuted = volumeMuted == true,
            canMute = muteControl != null && muteControl != PLAYER_CONTROL_NONE,
            queueId = currentMedia?.queueId ?: activeSource,
            isPlaying = state == PlayerState.PLAYING,
            isAnnouncing = announcementInProgress == true,
            canGroupWith = canGroupWith,
            groupChildren = groupChilds,
            //activeGroup = activeGroup,
            groupVolume = groupVolume,
        )
    }
}