package io.music_assistant.client.ui.compose.library2.items

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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.music_assistant.client.data.model.client.AppMediaItem
import io.music_assistant.client.ui.compose.common.painters.VinylRecordPainter
import io.music_assistant.client.ui.compose.common.painters.WaveformPainter
import io.music_assistant.client.ui.compose.common.painters.rememberPlaceholderPainter

@Composable
private fun GridItemWrapper(
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

@Composable
fun GridTrackItem(
    item: AppMediaItem.Track,
    serverUrl: String?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val itemSize = 96.dp
    val primary = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer

    GridItemWrapper(onClick, onLongClick) {
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
        Spacer(Modifier.height(4.dp))
        Text(
            text = item.name,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(itemSize),
            textAlign = TextAlign.Center,
        )
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

@Composable
fun GridArtistItem(
    item: AppMediaItem.Artist,
    serverUrl: String?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val itemSize = 96.dp
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer

    GridItemWrapper(onClick, onLongClick) {
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
        Spacer(Modifier.height(4.dp))
        Text(
            modifier = Modifier.width(itemSize),
            text = item.name,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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

@Composable
fun GridAlbumItem(
    item: AppMediaItem.Album,
    serverUrl: String?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val itemSize = 96.dp
    val primaryContainer = MaterialTheme.colorScheme.primary
    val background = MaterialTheme.colorScheme.background

    GridItemWrapper(onClick, onLongClick) {
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
        Spacer(Modifier.height(4.dp))
        Text(
            modifier = Modifier.width(itemSize),
            text = item.name,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
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

@Composable
fun GridPlaylistItem(
    item: AppMediaItem.Playlist,
    serverUrl: String?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val itemSize = 96.dp
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer

    GridItemWrapper(onClick, onLongClick) {
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
        Spacer(Modifier.height(4.dp))
        Text(
            modifier = Modifier.width(itemSize),
            text = item.name,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
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

// Custom shapes for album vinyl record effect
class CutStripShape(private val stripWidth: Dp) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        return Outline.Generic(Path().apply {
            val stripPx = with(density) { stripWidth.toPx() }

            // Defines the album cover area, excluding the rightmost strip
            moveTo(0f, 0f)
            lineTo(size.width - stripPx, 0f)
            lineTo(size.width - stripPx, size.height)
            lineTo(0f, size.height)
            close()
        })
    }
}

class HoleShape(private val holeRadius: Dp) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val radiusPx = with(density) { holeRadius.toPx() }
        val center = size.width / 2f

        // Define the full cover path
        val coverPath = Path().apply {
            addRect(Rect(Offset.Zero, size))
        }

        // Define the hole path
        val holePath = Path().apply {
            val rect = Rect(
                left = center - radiusPx,
                top = center - radiusPx,
                right = center + radiusPx,
                bottom = center + radiusPx
            )
            addOval(oval = rect)
        }

        // Subtract hole from cover
        val finalPath = Path.combine(
            operation = PathOperation.Difference,
            path1 = coverPath,
            path2 = holePath
        )

        return Outline.Generic(finalPath)
    }
}
