package io.music_assistant.client.ui.compose.main

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import compose.icons.TablerIcons
import compose.icons.tablericons.CircleDashed
import compose.icons.tablericons.CircleX
import compose.icons.tablericons.PlayerPlay
import io.music_assistant.client.data.model.client.QueueTrack
import io.music_assistant.client.ui.compose.common.ActionIcon

@Composable
fun QueueTrackControls(
    queueId: String?,
    chosenItems: List<QueueTrack>,
    enabled: Boolean,
    queueAction: (QueueAction) -> Unit,
    onChosenItemsClear: () -> Unit,
) {
    ActionIcon(
        icon = TablerIcons.CircleDashed,
        size = 24.dp,
        enabled = enabled,
    ) { onChosenItemsClear() }
    Text(
        text = "${chosenItems.size} selected ${if (chosenItems.size == 1) "track" else "tracks"}:",
        color = MaterialTheme.colors.onSurface,
        style = MaterialTheme.typography.body2,
        fontWeight = FontWeight.Bold,
    )
    queueId?.let {
        if (chosenItems.size == 1) {
            ActionIcon(
                icon = TablerIcons.PlayerPlay,
                size = 24.dp,
                enabled = enabled,
            ) {
                queueAction(
                    QueueAction.PlayQueueItem(it, chosenItems.first().id),
                )
            }
        }
        ActionIcon(
            icon = TablerIcons.CircleX,
            size = 24.dp,
            enabled = enabled,
        ) {
            queueAction(
                QueueAction.RemoveItems(it, chosenItems.map { it.id }),
            )
        }
    }
}
