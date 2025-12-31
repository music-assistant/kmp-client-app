package io.music_assistant.client.ui.compose.common.items

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.music_assistant.client.data.model.client.AppMediaItem
import io.music_assistant.client.data.model.server.QueueOption

/**
 * A reusable composable that displays a track item with a dropdown menu for queue actions.
 * When onTrackClick is provided, clicking the item opens a menu with play options.
 * Otherwise, it behaves as a simple clickable track item.
 */
@Composable
fun TrackItemWithMenu(
    item: AppMediaItem.Track,
    serverUrl: String?,
    itemSize: Dp = 96.dp,
    onTrackPlayOption: ((AppMediaItem.Track, QueueOption) -> Unit)? = null,
    onItemClick: ((AppMediaItem.Track) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var expandedTrackId by remember { mutableStateOf<String?>(null) }

    if (onTrackPlayOption != null) {
        Box(modifier = modifier) {
            MediaItemTrack(
                item = item,
                serverUrl = serverUrl,
                onClick = { expandedTrackId = item.itemId },
                itemSize = itemSize
            )
            DropdownMenu(
                expanded = expandedTrackId == item.itemId,
                onDismissRequest = { expandedTrackId = null }
            ) {
                DropdownMenuItem(
                    text = { Text("Play Now") },
                    onClick = {
                        onTrackPlayOption(item, QueueOption.PLAY)
                        expandedTrackId = null
                    }
                )
                DropdownMenuItem(
                    text = { Text("Play Next") },
                    onClick = {
                        onTrackPlayOption(item, QueueOption.NEXT)
                        expandedTrackId = null
                    }
                )
                DropdownMenuItem(
                    text = { Text("Add to Queue") },
                    onClick = {
                        onTrackPlayOption(item, QueueOption.ADD)
                        expandedTrackId = null
                    }
                )
                DropdownMenuItem(
                    text = { Text("Replace Queue") },
                    onClick = {
                        onTrackPlayOption(item, QueueOption.REPLACE)
                        expandedTrackId = null
                    }
                )
            }
        }
    } else {
        MediaItemTrack(
            item = item,
            serverUrl = serverUrl,
            onClick = { onItemClick?.invoke(it) },
            itemSize = itemSize
        )
    }
}
