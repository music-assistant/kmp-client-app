@file:OptIn(ExperimentalMaterial3Api::class)

package io.music_assistant.client.ui.compose.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import co.touchlab.kermit.Logger
import coil3.compose.AsyncImage
import io.music_assistant.client.data.model.client.AppMediaItem
import io.music_assistant.client.ui.compose.common.DataState
import io.music_assistant.client.ui.compose.common.painters.MusicMicrophonePainter
import io.music_assistant.client.ui.compose.common.painters.MusicNotePainter
import io.music_assistant.client.utils.SessionState
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sin

@Composable
fun LandingPage(
    modifier: Modifier = Modifier,
    connectionState: SessionState,
    dataState: DataState<List<AppMediaItem.RecommendationFolder>>,
    serverUrl: String?,
    onItemClick: (AppMediaItem) -> Unit,
    onLongItemClick: (AppMediaItem) -> Unit,
    onRowActionClick: (String) -> Unit,
) {
    Logger.e("LandingPage recomposition - connectionState: $connectionState, dataState: $dataState")
    if (connectionState !is SessionState.Connected || dataState !is DataState.Data) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(items = dataState.data.filter {
                it.items?.filter { item ->
                    item is AppMediaItem.Track
                            || item is AppMediaItem.Artist
                            || item is AppMediaItem.Album
                            || item is AppMediaItem.Playlist
                }.orEmpty().isNotEmpty()
            }) { row ->
                CategoryRow(
                    serverUrl = serverUrl,
                    title = row.name,
                    buttonLabel = "All items",
                    onItemClick = onItemClick,
                    onLongItemClick = onLongItemClick,
                    onAllClick = {onRowActionClick(row.itemId)},
                    mediaItems = row.items.orEmpty()
                )
            }
        }
    }
}

// --- Common UI Components ---

@Composable
fun CategoryRow(
    serverUrl: String?,
    title: String,
    buttonLabel: String,
    onItemClick: (AppMediaItem) -> Unit,
    onLongItemClick: (AppMediaItem) -> Unit,
    onAllClick: () -> Unit,
    mediaItems: List<AppMediaItem>
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
            TextButton(
                onClick = onAllClick,
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(buttonLabel, style = MaterialTheme.typography.labelLarge)
            }
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(items = mediaItems) { item ->
                when (item) {
                    is AppMediaItem.Track -> TrackItem(
                        item = item,
                        itemSize = 96.dp,
                        onClick = onItemClick,
                        onLongClick = onLongItemClick,
                        serverUrl = serverUrl
                    )

                    is AppMediaItem.Artist -> ArtistItem(
                        item = item,
                        itemSize = 96.dp,
                        onClick = onItemClick,
                        onLongClick = onLongItemClick,
                        serverUrl = serverUrl
                    )

                    is AppMediaItem.Album -> AlbumItem(
                        item = item,
                        itemSize = 96.dp,
                        onClick = onItemClick,
                        onLongClick = onLongItemClick,
                        serverUrl = serverUrl
                    )

                    is AppMediaItem.Playlist -> PlaylistItem(
                        item = item,
                        itemSize = 96.dp,
                        onClick = onItemClick,
                        onLongClick = onLongItemClick,
                        serverUrl = serverUrl
                    )

                    else -> {}
                }
            }
        }
    }
}

// --- Specific Item Composables ---
@Composable
fun LibraryItem(
    item: AppMediaItem,
    onClick: (AppMediaItem) -> Unit,
    onLongClick: (AppMediaItem) -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .wrapContentSize()
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(
                onClick = { onClick(item) },
                onLongClick = { onLongClick(item) }
            )
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        content()
    }
}

@Composable
fun TrackItem(
    item: AppMediaItem.Track,
    itemSize: Dp,
    onClick: (AppMediaItem) -> Unit,
    onLongClick: (AppMediaItem) -> Unit,
    serverUrl: String?,
) {
    LibraryItem(item, onClick, onLongClick) {
        Box(
            modifier = Modifier
                .size(itemSize)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
        ) {
            val placeholder =
                MusicNotePainter(
                    backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                    iconColor = MaterialTheme.colorScheme.onPrimaryContainer
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
            val waveformPainter = WaveformPainter(
                waveColor = MaterialTheme.colorScheme.onPrimaryContainer,
                thickness = 3f
            )
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
            modifier = Modifier.width(itemSize)
        )
        Text(
            text = item.subtitle.orEmpty(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(itemSize)
        )
    }
}

@Composable
fun ArtistItem(
    item: AppMediaItem.Artist,
    itemSize: Dp,
    onClick: (AppMediaItem) -> Unit,
    onLongClick: (AppMediaItem) -> Unit,
    serverUrl: String?,
) {
    LibraryItem(item, onClick, onLongClick) {
        Box(
            modifier = Modifier
                .size(itemSize)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
        ) {
            val placeholder =
                MusicMicrophonePainter(
                    backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                    iconColor = MaterialTheme.colorScheme.onPrimaryContainer
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
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun AlbumItem(
    item: AppMediaItem.Album,
    itemSize: Dp,
    onClick: (AppMediaItem) -> Unit,
    onLongClick: (AppMediaItem) -> Unit,
    serverUrl: String?,
) {
    LibraryItem(item, onClick, onLongClick) {
        Box(
            modifier = Modifier
                .size(itemSize)
                .clip(RoundedCornerShape(8.dp))
        ) {

            val vinylRecord = VinylRecordPainter(
                recordColor = Color(0xFF202020),
                labelColor = MaterialTheme.colorScheme.primaryContainer,
                holeColor = MaterialTheme.colorScheme.background
            )
            val stripWidth = 10.dp
            val holeRadius = 10.dp

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
                    .clip(CutStripShape(stripWidth))
                    .clip(HoleShape(holeRadius))
            )

            Canvas(modifier = Modifier.size(itemSize)) {
                val center = size.width / 2f
                val radiusPx = holeRadius.toPx()
                drawCircle(
                    color = Color.Black,
                    radius = radiusPx + 1.dp.toPx(),
                    center = Offset(center, center),
                    style = Stroke(width = 3.dp.toPx())
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            modifier = Modifier.width(itemSize),
            text = item.name,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            modifier = Modifier.width(itemSize),
            text = item.subtitle.orEmpty(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun PlaylistItem(
    item: AppMediaItem.Playlist,
    itemSize: Dp,
    onClick: (AppMediaItem) -> Unit,
    onLongClick: (AppMediaItem) -> Unit,
    serverUrl: String?,
) {
    LibraryItem(item, onClick, onLongClick) {
        Box(
            modifier = Modifier
                .size(itemSize)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.List,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(50.dp)
            )
            AsyncImage(
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
            overflow = TextOverflow.Ellipsis
        )
    }
}

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

        // 1. Define the full cover path (the shape we want to keep)
        val coverPath = Path().apply {
            addRect(Rect(Offset.Zero, size))
        }

        // 2. Define the hole path (the shape we want to cut out)
        val holePath = Path().apply {
            val rect = Rect(
                left = center - radiusPx,
                top = center - radiusPx,
                right = center + radiusPx,
                bottom = center + radiusPx
            )
            // Use addOval to define the circle
            addOval(oval = rect)
        }

        // 3. Perform the subtraction operation (Difference)
        val finalPath = Path.combine(
            operation = PathOperation.Difference, // <-- This explicitly subtracts the second path
            path1 = coverPath, // The album cover
            path2 = holePath // The hole
        )

        return Outline.Generic(finalPath)
    }
}

class VinylRecordPainter(
    private val recordColor: Color = Color(0xFF202020), // Dark grey/black for vinyl
    private val labelColor: Color = Color(0xFFFF5722), // Bright orange/red for label
    private val holeColor: Color = Color.Black, // Color for the center spindle hole
    private val holeRadius: Dp = 3.dp, // Color for the center spindle hole
    private val grooveColor: Color = labelColor.copy(alpha = 0.4f), // Subtle white for grooves
    private val grooveCount: Int = 6 // Number of grooves to draw
) : Painter() {

    override val intrinsicSize: Size = Size.Unspecified

    override fun DrawScope.onDraw() {
        val diameter = size.minDimension
        val radius = diameter / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        // Draw the main record body (dark circle)
        drawCircle(
            color = recordColor,
            radius = radius,
            center = center,
            style = Fill
        )

        // Draw the record label (smaller colored circle)
        val labelRadius = radius * 0.45f
        drawCircle(
            color = labelColor,
            radius = labelRadius,
            center = center,
            style = Fill
        )

        // Draw the center spindle hole (tiny black circle)
        val holeRadius = with(Density(density)) { holeRadius.toPx() }
        drawCircle(
            color = holeColor,
            radius = holeRadius,
            center = center,
            style = Fill
        )

        // Start grooves from just outside the label, extending to the edge of the record
        val grooveStartRadius = labelRadius + (1.dp.toPx()) // Start slightly after the label
        val grooveEndRadius = radius - (1.dp.toPx()) // End slightly before the outer edge

        if (grooveEndRadius > grooveStartRadius) {
            val grooveSpacing = (grooveEndRadius - grooveStartRadius) / grooveCount
            for (i in 0 until grooveCount) {
                val currentGrooveRadius = grooveStartRadius + (i * grooveSpacing)
                drawCircle(
                    color = grooveColor,
                    radius = currentGrooveRadius,
                    center = center,
                    style = Stroke(width = 0.5.dp.toPx()) // Very thin stroke for grooves
                )
            }
        }
    }
}


class WaveformPainter(
    private val waveColor: Color,
    private val thickness: Float = 4f,
    private val baseFrequency: Float = 16f,
    private val baseAmplitudeFactor: Float = 0.8f,
    private val verticalOffset: Float = 0.3f
) : Painter() {

    override val intrinsicSize: Size = Size.Unspecified

    override fun DrawScope.onDraw() {
        if (size.width <= 0f || size.height <= 0f) return

        val centerLine = size.height * (0.5f + verticalOffset) // Shift the center line down

        var phase = 0f
        var prevX = 0f
        var prevY = centerLine

        // Draw the wave with variable amplitude and frequency
        for (x in 0..size.width.toInt()) {
            val normalizedX = x.toFloat() / size.width

            // Create an envelope that peaks only at the exact center
            val distanceFromCenter = abs(normalizedX - 0.5f) * 2f // 0 at center, 1 at edges
            // Using Gaussian-like curve for sharp peak at center
            val envelopeCurve = exp(-distanceFromCenter * distanceFromCenter * 8f)
            // Scale from 0.2 to 1.0 instead of 0 to 1.0
            val envelope = 0.1f + envelopeCurve * 0.9f

            // Vary amplitude based on position
            val amplitude = size.height * baseAmplitudeFactor * envelope

            // Vary frequency based on position (higher frequency in the center)
            val frequency = baseFrequency * (0.5f + envelope * 0.5f)

            // Accumulate phase for smooth transitions
            if (x > 0) {
                phase += frequency * 2 * PI.toFloat() / size.width
            }

            // Calculate y using accumulated phase, centered on the shifted center line
            val y = centerLine + amplitude * sin(phase)

            // Interpolate color based on envelope (amplitude)
            // envelope ranges from 0.1 to 1.0, we map it to color interpolation
            val colorT = 1 - ((envelope - 0.1f) / 0.9f) // Normalize to 1..0
            val segmentColor = Color(
                red = 1f - (1f - waveColor.red) * colorT,
                green = 1f - (1f - waveColor.green) * colorT,
                blue = 1f - (1f - waveColor.blue) * colorT,
                alpha = waveColor.alpha
            )

            // Draw line segment with current color
            if (x > 0) {
                drawLine(
                    color = segmentColor,
                    start = Offset(prevX, prevY),
                    end = Offset(x.toFloat(), y),
                    strokeWidth = thickness
                )
            }

            prevX = x.toFloat()
            prevY = y
        }
    }
}