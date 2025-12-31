package io.music_assistant.client.ui.compose.common.items

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FeaturedPlayList
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.music_assistant.client.data.model.client.AppMediaItem
import io.music_assistant.client.ui.compose.common.painters.VinylRecordPainter
import io.music_assistant.client.ui.compose.common.painters.WaveformPainter
import io.music_assistant.client.ui.compose.common.painters.rememberPlaceholderPainter

/**
 * Common wrapper for media items with click handling.
 */
@Composable
private fun MediaItemWrapper(
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .wrapContentSize()
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        content()
    }
}

/**
 * Track media item with waveform overlay.
 *
 * @param item The track item to display
 * @param serverUrl Server URL for image loading
 * @param onClick Click handler
 * @param onLongClick Long click handler
 * @param itemSize Size of the item (default 96.dp)
 * @param showSubtitle Whether to show subtitle (default true)
 */
@Composable
fun MediaItemTrack(
    item: AppMediaItem.Track,
    serverUrl: String?,
    onClick: (AppMediaItem.Track) -> Unit,
    onLongClick: (AppMediaItem.Track) -> Unit,
    itemSize: Dp = 96.dp,
    showSubtitle: Boolean = true,
) {
    MediaItemWrapper(
        onClick = { onClick(item) },
        onLongClick = { onLongClick(item) }
    ) {
        TrackImage(itemSize, item, serverUrl)
        Spacer(Modifier.height(4.dp))
        Text(
            text = item.name,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(itemSize),
            textAlign = TextAlign.Center,
        )
        if (showSubtitle) {
            Text(
                text = item.subtitle.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.width(itemSize),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun TrackImage(
    itemSize: Dp,
    item: AppMediaItem.Track,
    serverUrl: String?,
) {
    val primary = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer
    Box(
        modifier = Modifier
            .size(itemSize)
            .clip(RoundedCornerShape(8.dp))
            .background(primaryContainer)
    ) {
        val placeholder = rememberPlaceholderPainter(
            backgroundColor = primaryContainer,
            iconColor = onPrimaryContainer,
            icon = Icons.Default.MusicNote
        )
        AsyncImage(
            placeholder = placeholder,
            fallback = placeholder,
            model = item.imageInfo?.url(serverUrl),
            contentDescription = item.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Draw waveform overlay at the bottom
        val waveformPainter = remember(onPrimaryContainer) {
            WaveformPainter(
                waveColor = primary,
                thickness = 3f
            )
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
                .align(Alignment.BottomCenter)
        ) {
            with(waveformPainter) {
                draw(size)
            }
        }
    }
}

/**
 * Artist media item with circular image.
 *
 * @param item The artist item to display
 * @param serverUrl Server URL for image loading
 * @param onClick Click handler
 * @param onLongClick Long click handler
 * @param itemSize Size of the item (default 96.dp)
 * @param showSubtitle Whether to show subtitle (default true)
 */
@Composable
fun MediaItemArtist(
    item: AppMediaItem.Artist,
    serverUrl: String?,
    onClick: (AppMediaItem.Artist) -> Unit,
    onLongClick: (AppMediaItem.Artist) -> Unit,
    itemSize: Dp = 96.dp,
    showSubtitle: Boolean = true,
) {
    MediaItemWrapper(
        onClick = { onClick(item) },
        onLongClick = { onLongClick(item) }
    ) {
        ArtistImage(itemSize, item, serverUrl)
        Spacer(Modifier.height(4.dp))
        Text(
            modifier = Modifier.width(itemSize),
            text = item.name,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (showSubtitle) {
            Text(
                modifier = Modifier.width(itemSize),
                text = item.subtitle.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun ArtistImage(
    itemSize: Dp,
    item: AppMediaItem.Artist,
    serverUrl: String?
) {
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer
    Box(
        modifier = Modifier
            .size(itemSize)
            .clip(CircleShape)
            .background(primaryContainer)
    ) {
        val placeholder = rememberPlaceholderPainter(
            backgroundColor = primaryContainer,
            iconColor = onPrimaryContainer,
            icon = Icons.Default.Mic
        )
        AsyncImage(
            placeholder = placeholder,
            fallback = placeholder,
            model = item.imageInfo?.url(serverUrl),
            contentDescription = item.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * Album media item with vinyl record design.
 *
 * @param item The album item to display
 * @param serverUrl Server URL for image loading
 * @param onClick Click handler
 * @param onLongClick Long click handler
 * @param itemSize Size of the item (default 96.dp)
 * @param showSubtitle Whether to show subtitle (default true)
 */
@Composable
fun MediaItemAlbum(
    item: AppMediaItem.Album,
    serverUrl: String?,
    onClick: (AppMediaItem.Album) -> Unit,
    onLongClick: (AppMediaItem.Album) -> Unit,
    itemSize: Dp = 96.dp,
    showSubtitle: Boolean = true,
) {
    MediaItemWrapper(
        onClick = { onClick(item) },
        onLongClick = { onLongClick(item) }
    ) {
        AlbumImage(itemSize, item, serverUrl)
        Spacer(Modifier.height(4.dp))
        Text(
            modifier = Modifier.width(itemSize),
            text = item.name,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        if (showSubtitle) {
            Text(
                modifier = Modifier.width(itemSize),
                text = item.subtitle.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun AlbumImage(
    itemSize: Dp,
    item: AppMediaItem.Album,
    serverUrl: String?
) {
    val primaryContainer = MaterialTheme.colorScheme.primary
    val background = MaterialTheme.colorScheme.background
    Box(
        modifier = Modifier
            .size(itemSize)
            .clip(RoundedCornerShape(8.dp))
    ) {
        val vinylRecord = remember(primaryContainer, background) {
            VinylRecordPainter(
                recordColor = Color.DarkGray,
                labelColor = primaryContainer,
                holeColor = background
            )
        }
        val stripWidth = 10.dp
        val holeRadius = 16.dp

        val cutStripShape = remember(stripWidth) { CutStripShape(stripWidth) }
        val holeShape = remember(holeRadius) { HoleShape(holeRadius) }

        Image(
            painter = vinylRecord,
            contentDescription = "Vinyl Record",
            modifier = Modifier.fillMaxSize().clip(CircleShape)
        )

        AsyncImage(
            placeholder = vinylRecord,
            fallback = vinylRecord,
            model = item.imageInfo?.url(serverUrl),
            contentDescription = item.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(cutStripShape)
                .clip(holeShape)
        )
    }
}

/**
 * Playlist media item.
 *
 * @param item The playlist item to display
 * @param serverUrl Server URL for image loading
 * @param onClick Click handler
 * @param onLongClick Long click handler
 * @param itemSize Size of the item (default 96.dp)
 * @param showSubtitle Whether to show subtitle (default true)
 */
@Composable
fun MediaItemPlaylist(
    item: AppMediaItem.Playlist,
    serverUrl: String?,
    onClick: (AppMediaItem.Playlist) -> Unit,
    onLongClick: (AppMediaItem.Playlist) -> Unit,
    itemSize: Dp = 96.dp,
    showSubtitle: Boolean = true,
) {
    MediaItemWrapper(
        onClick = { onClick(item) },
        onLongClick = { onLongClick(item) }
    ) {
        PlaylistImage(itemSize, item, serverUrl)
        Spacer(Modifier.height(4.dp))
        Text(
            modifier = Modifier.width(itemSize),
            text = item.name,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        if (showSubtitle) {
            Text(
                modifier = Modifier.width(itemSize),
                text = item.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun PlaylistImage(
    itemSize: Dp,
    item: AppMediaItem.Playlist,
    serverUrl: String?
) {
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer
    Box(
        modifier = Modifier
            .size(itemSize)
            .clip(RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        val placeholder = rememberPlaceholderPainter(
            backgroundColor = primaryContainer,
            iconColor = onPrimaryContainer,
            icon = Icons.AutoMirrored.Filled.FeaturedPlayList
        )
        AsyncImage(
            placeholder = placeholder,
            fallback = placeholder,
            model = item.imageInfo?.url(serverUrl),
            contentDescription = item.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}
