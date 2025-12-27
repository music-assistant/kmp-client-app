package io.music_assistant.client.ui.compose.main

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.music_assistant.client.data.model.client.PlayerData
import io.music_assistant.client.ui.compose.common.OverflowMenuOption
import io.music_assistant.client.ui.compose.common.OverflowMenuThreeDots
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun PlayerCard(
    modifier: Modifier = Modifier,
    serverUrl: String?,
    playerData: PlayerData,
    isSelected: Boolean,
    playerAction: (PlayerData, PlayerAction) -> Unit,
    settingsAction: (String) -> Unit,
    dspSettingsAction: (String) -> Unit,
) {
    val player = playerData.player
    val queue = playerData.queueInfo
    val duration = queue?.currentItem?.track?.duration?.toFloat()
    val serverElapsed = queue?.elapsedTime?.toFloat()

    // Track progress locally for smooth updates while playing
    var localElapsed by remember(queue?.currentItem?.track?.uri) {
        mutableFloatStateOf(serverElapsed ?: 0f)
    }

    // Sync with server updates
    LaunchedEffect(serverElapsed) {
        serverElapsed?.let { localElapsed = it }
    }

    // Update progress every second while playing
    LaunchedEffect(player.isPlaying, queue?.currentItem?.track?.uri) {
        while (isActive && player.isPlaying && duration != null && duration > 0f) {
            delay(1000L)
            localElapsed = (localElapsed + 1f).coerceAtMost(duration)
        }
    }

    val currentProgress = duration?.let { localElapsed / it }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(size = 16.dp))
            .alpha(if (isSelected) 1f else 0.4f)
            .background(
                color = MaterialTheme.colorScheme.secondary,
                shape = RoundedCornerShape(size = 8.dp)
            )
    ) {
        OverflowMenuThreeDots(
            modifier = Modifier.align(Alignment.TopEnd),
            options = listOf(
                OverflowMenuOption(
                    title = "Settings",
                    onClick = { settingsAction(player.id) }
                ),
                OverflowMenuOption(
                    title = "DSP settings",
                    onClick = { dspSettingsAction(player.id) }
                ),
            )
        )
        queue?.currentItem?.track?.imageInfo?.let {
            AsyncImage(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(0.3f)
                    .clip(RoundedCornerShape(size = 16.dp)),
                model = it.url(serverUrl),
                contentDescription = null,
                contentScale = ContentScale.Crop,
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 8.dp, horizontal = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .basicMarquee(iterations = 100),
                textAlign = TextAlign.Center,
                text = player.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .basicMarquee(iterations = 100),
                textAlign = TextAlign.Center,
                text =
                    if (player.isAnnouncing) "ANNOUNCING"
                    else queue?.currentItem?.track?.description ?: "idle",
                maxLines = 1,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineSmall
            )
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth(),
                progress = { currentProgress ?: 0f },
                color = MaterialTheme.colorScheme.onPrimary,
                strokeCap = StrokeCap.Round
            )
            PlayerControls(
                playerData = playerData,
                playerAction = playerAction,
                enabled = !player.isAnnouncing
            )
        }
    }

}