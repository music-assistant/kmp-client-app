@file:OptIn(ExperimentalMaterial3Api::class)

package io.music_assistant.client.ui.compose.home

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import io.music_assistant.client.data.model.client.AppMediaItem
import io.music_assistant.client.data.model.client.PlayerData
import io.music_assistant.client.ui.compose.common.action.PlayerAction
import io.music_assistant.client.ui.compose.common.painters.rememberPlaceholderPainter
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.roundToInt

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
    serverUrl: String?,
    playerAction: (PlayerData, PlayerAction) -> Unit,
    onFavoriteClick: (AppMediaItem) -> Unit, // FIXME inconsistent stuff happening
) {
    val track = item.queueInfo?.currentItem?.track
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer

    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
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
            track?.subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } ?: run {
                if (item.queueInfo?.currentItem?.isPlayable == false) {
                    Text(
                        text = "Cannot play this item",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        val duration = track?.duration?.takeIf { it > 0 }?.toFloat()
        val serverElapsed = item.queueInfo?.elapsedTime?.toFloat()

        // Track progress locally for smooth updates while playing
        var localElapsed by remember(item.queueInfo?.currentItem?.track?.uri) {
            mutableStateOf(serverElapsed ?: 0f)
        }

        // Sync with server updates
        LaunchedEffect(serverElapsed) {
            serverElapsed?.let { localElapsed = it }
        }

        // Update progress every second while playing
        LaunchedEffect(item.player.isPlaying, item.queueInfo?.currentItem?.track?.uri) {
            while (isActive && item.player.isPlaying && duration != null && duration > 0f) {
                delay(1000L)
                localElapsed = (localElapsed + 1f).coerceAtMost(duration)
            }
        }

        Column {// Progress bar
            Slider(
                value = localElapsed,
                valueRange = duration?.let { 0f..it } ?: 0f..1f,
                enabled = localElapsed.takeIf { duration != null } != null,
                onValueChange = {
                    localElapsed = it
                    playerAction(item, PlayerAction.SeekTo(it.toLong()))
                },
                modifier = Modifier.fillMaxWidth(),
                thumb = {
                    localElapsed.takeIf { duration != null }?.let {
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
                        enabled = track != null,
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
                    text = formatDuration(localElapsed.takeIf { track != null }),
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
        "$hours:${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}"
    } else {
        "$minutes:${secs.toString().padStart(2, '0')}"
    }
}
