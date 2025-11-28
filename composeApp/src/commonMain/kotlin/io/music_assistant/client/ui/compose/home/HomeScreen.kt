@file:OptIn(ExperimentalMaterial3Api::class)

package io.music_assistant.client.ui.compose.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import io.music_assistant.client.data.model.client.AppMediaItem
import io.music_assistant.client.ui.compose.common.ListState
import io.music_assistant.client.ui.compose.common.MusicNotePainter
import io.music_assistant.client.utils.SessionState
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomeScreen(
    viewModel: HomeScreenViewModel = koinViewModel()
) {
    val state = viewModel.state.collectAsStateWithLifecycle()
    val serverUrl by viewModel.serverUrl.collectAsStateWithLifecycle(null)
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Your Library") })
        }
    ) { paddingValues ->
        val connectionState = state.value.connectionState
        val listState = state.value.recommendations
        if (connectionState !is SessionState.Connected || listState !is ListState.Data) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(items = listState.items) { row ->
                    CategoryRow(
                        serverUrl = serverUrl,
                        title = row.name,
                        buttonLabel = "All items",
                        onClickAll = { viewModel.onRowButtonClicked(row.itemId) },
                        mediaItems = row.items.orEmpty()
                    )
                }
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
    onClickAll: () -> Unit,
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
                onClick = onClickAll,
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(buttonLabel, style = MaterialTheme.typography.labelLarge)
            }
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items = mediaItems) { item ->
                when (item) {
                    is AppMediaItem.Track -> TrackItem(item, serverUrl)
                    is AppMediaItem.Artist -> ArtistItem(item, serverUrl)
                    is AppMediaItem.Album -> AlbumItem(item, serverUrl)
                    is AppMediaItem.Playlist -> PlaylistItem(item, serverUrl)
                    else -> {}
                }
            }
        }
    }
}

// --- Specific Item Composables ---

@Composable
fun TrackItem(track: AppMediaItem.Track, serverUrl: String?) {
    Column(
        modifier = Modifier
            .width(100.dp) // Define item width
            .clickable { /* Handle click */ }
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primaryContainer) // Placeholder image
        ) {
            Icon(
                Icons.Default.Headset,
                contentDescription = null,
                Modifier.size(50.dp).align(Alignment.Center)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = track.name,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = track.subtitle.orEmpty(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ArtistItem(artist: AppMediaItem.Artist, serverUrl: String?) {
    Column(
        modifier = Modifier
            .width(80.dp) // Define item width
            .clickable { /* Handle click */ },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.tertiaryContainer)
        ) {
            val placeholder =
                MusicNotePainter(
                    backgroundColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                    iconColor = MaterialTheme.colorScheme.secondary
                )
            AsyncImage(
                placeholder = placeholder,
                fallback = placeholder,
                model = artist.imageInfo?.url(serverUrl),
                contentDescription = artist.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = artist.name,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun AlbumItem(album: AppMediaItem.Album, serverUrl: String?) {
    val albumSize = 80.dp
    val stripWidth = 10.dp
    val holeRadius = 10.dp

    Column(
        modifier = Modifier
            .width(albumSize)
            .clickable { /* Handle click */ }
    ) {
        Box(
            modifier = Modifier
                .size(albumSize)
                .clip(RoundedCornerShape(8.dp))
        ) {

            val vinylRecord = VinylRecordPainter(
                recordColor = Color(0xFF202020),
                labelColor = Color(0xFFFF5722),
                holeColor = MaterialTheme.colorScheme.background
            )

            Image(
                painter = vinylRecord,
                contentDescription = "Vinyl Record",
                modifier = Modifier.fillMaxSize().clip(CircleShape)
            )

            AsyncImage(
                placeholder = vinylRecord,
                fallback = vinylRecord,
                model = album.imageInfo?.url(serverUrl),
                contentDescription = album.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CutStripShape(stripWidth))
                    .clip(HoleShape(holeRadius))
            )

            Canvas(modifier = Modifier.size(albumSize)) {
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
            text = album.name,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = album.subtitle.orEmpty(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun PlaylistItem(playlist: AppMediaItem.Playlist, serverUrl: String?) {
    Column(
        modifier = Modifier
            .width(80.dp)
            .clickable { /* Handle click */ },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.errorContainer) // Placeholder background
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            // Using a simple Material Icon for the "Wavy star-like icon"
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(50.dp)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = playlist.name,
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
    private val recordColor: Color = Color(0xFF202020),
    private val labelColor: Color = Color(0xFFFF5722),
    private val holeColor: Color = Color.Black,
    private val holeRadius: Dp = 2.dp
) : Painter() {

    override val intrinsicSize: Size = Size.Unspecified // Let the caller define the size

    override fun DrawScope.onDraw() {
        val diameter = size.minDimension // Use minDimension for perfect square aspect ratio
        val radius = diameter / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        // 1. Draw the main record body (dark circle)
        drawCircle(
            color = recordColor,
            radius = radius,
            center = center,
            style = Fill
        )

        // 2. Draw the record label (smaller colored circle)
        val labelRadius = radius * 0.45f // 45% of the record radius
        drawCircle(
            color = labelColor,
            radius = labelRadius,
            center = center,
            style = Fill
        )

        // 3. Draw the center spindle hole (tiny black circle)
        val holeRadius =
            with(Density(density)) { holeRadius.toPx() } // Use the same constant as in item
        drawCircle(
            color = holeColor,
            radius = holeRadius,
            center = center,
            style = Fill
        )

        // Optional: Add a subtle inner ring for more realism
        val innerRingRadius = labelRadius * 0.9f
        drawCircle(
            color = Color.White.copy(alpha = 0.1f),
            radius = innerRingRadius,
            center = center,
            style = Stroke(width = 0.5.dp.toPx())
        )
    }
}