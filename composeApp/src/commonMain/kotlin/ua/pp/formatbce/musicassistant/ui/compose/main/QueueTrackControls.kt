package ua.pp.formatbce.musicassistant.ui.compose.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import compose.icons.TablerIcons
import compose.icons.tablericons.CircleDashed
import compose.icons.tablericons.CircleX
import compose.icons.tablericons.PlayerPlay
import ua.pp.formatbce.musicassistant.data.model.server.events.QueueItem
import ua.pp.formatbce.musicassistant.ui.compose.common.ActionIcon

@Composable
fun QueueTrackControls(
    chosenItems: List<QueueItem>,
    queueAction: (QueueAction) -> Unit,
    onChosenItemsClear: () -> Unit
) {

    ActionIcon(
        icon = TablerIcons.CircleDashed,
        size = 24.dp
    ) { onChosenItemsClear() }
    Text(
        text = "${chosenItems.size} selected ${if (chosenItems.size == 1) "track" else "tracks"}:",
        color = MaterialTheme.colors.onSurface,
        style = MaterialTheme.typography.body2,
        fontWeight = FontWeight.Bold
    )
    if (chosenItems.size == 1) {
        ActionIcon(
            icon = TablerIcons.PlayerPlay,
            size = 24.dp
        ) {
            queueAction(
                QueueAction.PlayQueueItem(
                    chosenItems.first().queueId,
                    chosenItems.first().queueItemId
                )
            )
        }
    }
    ActionIcon(
        icon = TablerIcons.CircleX,
        size = 24.dp
    ) {
        queueAction(
            QueueAction.RemoveItems(
                chosenItems.first().queueId,
                chosenItems.map { it.queueItemId })
        )
    }

}


