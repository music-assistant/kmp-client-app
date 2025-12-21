package io.music_assistant.client.data.model.client

import io.music_assistant.client.ui.compose.common.DataState
import io.music_assistant.client.ui.compose.library.LibraryArgs

data class PlayerData(
    val player: Player,
    val queue: DataState<Queue>,
) {
    val playerId = player.id
    val queueInfo = (queue as? DataState.Data)?.data?.info
    val queueItems = ((queue as? DataState.Data)?.data?.items as? DataState.Data)?.data
    val queueId = queueInfo?.id
    val libraryArgs = LibraryArgs(
        name = player.name,
        queueOrPlayerId = queueId ?: player.id
    )

    fun updateFrom(other: PlayerData): PlayerData {
        return PlayerData(
            player = other.player,
            queue = when (queue) {
                is DataState.Data -> {
                    when (other.queue) {
                        is DataState.Loading -> queue
                        is DataState.Error -> queue
                        is DataState.NoData -> queue
                        is DataState.Data -> other.queue
                    }
                }

                else -> other.queue
            }
        )
    }
}