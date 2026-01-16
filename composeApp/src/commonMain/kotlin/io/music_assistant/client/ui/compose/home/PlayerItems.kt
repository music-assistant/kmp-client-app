@file:OptIn(ExperimentalMaterial3Api::class)

package io.music_assistant.client.ui.compose.home

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import io.music_assistant.client.data.model.client.AppMediaItem
import io.music_assistant.client.data.model.client.AppMediaItem.Companion.description
import io.music_assistant.client.data.model.client.PlayerData
import io.music_assistant.client.ui.compose.common.action.PlayerAction
import io.music_assistant.client.ui.compose.common.painters.rememberPlaceholderPainter
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
fun CompactPlayerItem(
    item: PlayerData,
    serverUrl: String?,
    playerAction: (PlayerData, PlayerAction) -> Unit,
) {
    val track = item.queueInfo?.currentItem?.track
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Album cover on the far left
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(primaryContainer.copy(alpha = track?.let { 1f } ?: 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                if (track != null) {
                    val placeholder = rememberPlaceholderPainter(
                        backgroundColor = primaryContainer,
                        iconColor = onPrimaryContainer,
                        icon = Icons.Default.MusicNote
                    )
                    AsyncImage(
                        placeholder = placeholder,
                        fallback = placeholder,
                        model = track.imageInfo?.url(serverUrl),
                        contentDescription = track.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Album,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = onPrimaryContainer.copy(alpha = 0.4f)
                    )
                }
            }

            // Track info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = track?.name ?: "--idle--",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                track?.subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } ?: run {
                    if (item.queueInfo?.currentItem?.isPlayable == false) {
                        Text(
                            text = "Cannot play this item",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            PlayerControls(
                playerData = item,
                playerAction = playerAction,
                enabled = !item.player.isAnnouncing,
                showVolumeButtons = false,
                showAdditionalButtons = false,
            )
        }
    }
}

@Composable
fun FullPlayerItem(
    modifier: Modifier,
    item: PlayerData,
    isLocal: Boolean,
    serverUrl: String?,
    simplePlayerAction: (String, PlayerAction) -> Unit,
    playerAction: (PlayerData, PlayerAction) -> Unit,
    onFavoriteClick: (AppMediaItem) -> Unit, // FIXME inconsistent stuff happening
) {
    val track = item.queueInfo?.currentItem?.track
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer

    var showGroupDialog by remember { mutableStateOf(false) }

    // Group dialog
    if (showGroupDialog) {
        val coroutineScope = rememberCoroutineScope()
        val listState = rememberLazyListState()

        AlertDialog(
            onDismissRequest = { showGroupDialog = false },
            title = { Text("Join players") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Non-scrollable Done button at top
                    OutlinedButton(
                        onClick = { showGroupDialog = false },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        Text("Done")
                    }

                    // Scrollable list of players
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)
                            .draggable(
                                orientation = Orientation.Vertical,
                                state = rememberDraggableState { delta ->
                                    coroutineScope.launch {
                                        listState.scrollBy(-delta)
                                    }
                                },
                            ),
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Current player at the very top
                        item {
                            GroupPlayerVolumeItem(
                                playerId = item.player.id,
                                playerName = item.player.name,
                                volume = item.player.volumeLevel,
                                simplePlayerAction = simplePlayerAction
                            )
                        }

                        // Bound players
                        val boundChildren = item.groupChildren.filter { it.isBound }
                        items(boundChildren, key = { "${it.id}_${it.volume}" }) { child ->
                            GroupPlayerVolumeItem(
                                playerId = child.id,
                                playerName = child.name,
                                volume = child.volume,
                                simplePlayerAction = simplePlayerAction,
                                bindItem = child,
                            )
                        }

                        // Unbound players
                        val unboundChildren = item.groupChildren.filter { !it.isBound }
                        items(unboundChildren, key = { it.id }) { child ->
                            GroupPlayerVolumeItem(
                                playerId = child.id,
                                playerName = child.name,
                                volume = child.volume,
                                simplePlayerAction = simplePlayerAction,
                                bindItem = child,
                            )
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = item.player.providerType.takeIf { !isLocal } ?: "",
                fontSize = 12.sp,
            )
            when {
                item.groupChildren.isEmpty() ->
                    OutlinedButton(
                        enabled = false,
                        onClick = {}) {
                        Text("Cannot group player")
                    }

                item.groupChildren.none { it.isBound } ->
                    OutlinedButton(
                        enabled = true,
                        onClick = { showGroupDialog = true }
                    ) {
                        Text("Group with others")
                    }

                else ->
                    Button(enabled = true, onClick = { showGroupDialog = true }) {
                        Text("Manage group")
                    }
            }
        }
        Box(
            modifier = Modifier
                .weight(1f, fill = false)
                .aspectRatio(1f)
                .heightIn(max = 500.dp)
                .padding(16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(primaryContainer.copy(alpha = track?.let { 1f } ?: 0.4f)),
            contentAlignment = Alignment.Center
        ) {
            if (track != null) {
                val placeholder = rememberPlaceholderPainter(
                    backgroundColor = primaryContainer,
                    iconColor = onPrimaryContainer,
                    icon = Icons.Default.MusicNote
                )
                AsyncImage(
                    placeholder = placeholder,
                    fallback = placeholder,
                    model = track.imageInfo?.url(serverUrl),
                    contentDescription = track.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Album,
                    contentDescription = null,
                    modifier = Modifier.size(120.dp),
                    tint = onPrimaryContainer.copy(alpha = 0.4f)
                )
            }
        }


        // Track info
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = track?.name ?: "--idle--",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (item.queueInfo?.currentItem?.isPlayable == false) {
                Text(
                    text = "Cannot play this item",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Text(
                    text = track?.subtitle ?: "",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = item.queueInfo?.currentItem?.audioFormat(item.playerId)?.description ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        val duration = track?.duration?.takeIf { it > 0 }?.toFloat()

        // Position is calculated in MainDataSource and updated twice per second
        val displayPosition = item.queueInfo?.elapsedTime?.toFloat() ?: 0f

        // Track user drag state separately
        var userDragPosition by remember { mutableStateOf<Float?>(null) }

        // Use user drag position if dragging, otherwise use calculated position
        val sliderPosition = userDragPosition ?: displayPosition

        Column {// Progress bar
            Slider(
                value = sliderPosition,
                valueRange = duration?.let { 0f..it } ?: 0f..1f,
                enabled = displayPosition.takeIf { duration != null } != null,
                onValueChange = {
                    userDragPosition = it  // Track drag position locally
                },
                onValueChangeFinished = {
                    userDragPosition?.let { seekPos ->
                        playerAction(item, PlayerAction.SeekTo(seekPos.toLong()))
                        userDragPosition = null  // Clear drag state
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                thumb = {
                    sliderPosition.takeIf { duration != null }?.let {
                        SliderDefaults.Thumb(
                            interactionSource = remember { MutableInteractionSource() },
                            thumbSize = DpSize(16.dp, 16.dp),
                            colors = SliderDefaults.colors()
                                .copy(thumbColor = MaterialTheme.colorScheme.secondary),
                        )
                    }
                },
                track = { sliderState ->
                    SliderDefaults.Track(
                        sliderState = sliderState,
                        thumbTrackGapSize = 0.dp,
                        trackInsideCornerSize = 0.dp,
                        drawStopIndicator = null,
                        enabled = track != null && !item.player.isAnnouncing,
                        modifier = Modifier.height(8.dp)
                    )
                }
            )

            // Duration labels
            Row(
                modifier = Modifier.fillMaxWidth().offset(y = (-8).dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatDuration(sliderPosition.takeIf { track != null }),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatDuration(duration),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        PlayerControls(
            playerData = item,
            playerAction = playerAction,
            enabled = !item.player.isAnnouncing,
            showVolumeButtons = false,
            mainButtonSize = 64.dp
        )
    }
}

/**
 * Formats duration in seconds to MM:SS or HH:MM:SS format
 */
private fun formatDuration(seconds: Float?): String {
    if (seconds == null || seconds <= 0f) return "--:--"

    val totalSeconds = seconds.roundToInt()
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val secs = totalSeconds % 60

    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format("%d:%02d", minutes, secs)
    }
}

/**
 * Group player volume item with name and volume slider
 */
@Composable
private fun GroupPlayerVolumeItem(
    playerId: String,
    playerName: String,
    volume: Float?,
    simplePlayerAction: (String, PlayerAction) -> Unit,
    bindItem: PlayerData.Bind? = null,
) {
    var currentVolume by remember(volume) {
        mutableStateOf(volume ?: 0f)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy((-4).dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                modifier = Modifier.alpha(if (bindItem?.isBound != false) 1f else 0.4f).weight(1f),
                text = playerName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Show button only for non-current players (when bindItem is provided)
            bindItem?.let { bind ->
                val itemId = listOf(playerId)
                IconButton(
                    onClick = {
                        simplePlayerAction(
                            bind.parentId,
                            PlayerAction.GroupManage(
                                toAdd = itemId.takeIf { !bind.isBound },
                                toRemove = itemId.takeIf { bind.isBound }
                            )
                        )
                    }
                ) {
                    Icon(
                        imageVector = if (bindItem.isBound) Icons.Default.Remove else Icons.Default.Add,
                        contentDescription = if (bindItem.isBound) "Remove from group" else "Add to group",
                        tint = if (bindItem.isBound)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        val sliderEnabled = volume != null && bindItem?.isBound != false
        Slider(
            modifier = Modifier.fillMaxWidth().alpha(if (sliderEnabled) 1f else 0.4f),
            value = currentVolume,
            valueRange = 0f..100f,
            enabled = sliderEnabled,
            onValueChange = {
                currentVolume = it
            },
            onValueChangeFinished = {
                simplePlayerAction(
                    playerId,
                    PlayerAction.VolumeSet(currentVolume.toDouble())
                )
            },
            thumb = {
                SliderDefaults.Thumb(
                    interactionSource = remember { MutableInteractionSource() },
                    thumbSize = DpSize(16.dp, 16.dp),
                    colors = SliderDefaults.colors()
                        .copy(thumbColor = MaterialTheme.colorScheme.secondary),
                )
            },
            track = { sliderState ->
                SliderDefaults.Track(
                    sliderState = sliderState,
                    thumbTrackGapSize = 0.dp,
                    trackInsideCornerSize = 0.dp,
                    drawStopIndicator = null,
                    modifier = Modifier.height(4.dp)
                )
            }
        )
    }
}
