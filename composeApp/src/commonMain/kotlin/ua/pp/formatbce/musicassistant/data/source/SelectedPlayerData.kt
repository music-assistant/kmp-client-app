package ua.pp.formatbce.musicassistant.data.source

import ua.pp.formatbce.musicassistant.data.model.server.QueueItem

data class SelectedPlayerData(
    val playerId: String,
    val queueItems: List<QueueItem>? = null,
    val chosenItemsIds: Set<String> = emptySet()
)