package ua.pp.formatbce.musicassistant.ui.compose.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import ua.pp.formatbce.musicassistant.data.model.server.QueueItem
import ua.pp.formatbce.musicassistant.data.source.PlayerData

@Composable
fun PlayerDetails(
    modifier: Modifier = Modifier,
    nestedScrollConnection: NestedScrollConnection,
    playerData: PlayerData,
    queueItems: List<QueueItem>?,
    chosenItemsIds: Set<String>?,
    queueAction: (QueueAction) -> Unit,
    onItemChosenChanged: (String) -> Unit,
    onChosenItemsClear: () -> Unit
) {
    Column(
        modifier = modifier,
    ) {
        queueItems?.takeIf { it.isNotEmpty() }?.let { items ->
            QueueUI(
                nestedScrollConnection = nestedScrollConnection,
                queue = playerData.queue,
                items = items,
                chosenItemsIds = chosenItemsIds,
                enabled = !playerData.player.isAnnouncing,
                queueAction = queueAction,
                onItemChosenChanged = onItemChosenChanged,
                onChosenItemsClear = onChosenItemsClear,
            )
        } ?: run {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Nothing here...",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colors.onSurface,
                    style = MaterialTheme.typography.body2,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}