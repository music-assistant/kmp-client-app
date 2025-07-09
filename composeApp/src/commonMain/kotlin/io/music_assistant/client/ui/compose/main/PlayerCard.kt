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
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
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
import io.music_assistant.client.data.model.client.Player
import io.music_assistant.client.data.model.client.PlayerData
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun PlayerCard(
    modifier: Modifier = Modifier,
    playerData: PlayerData,
    isSelected: Boolean,
    playerAction: (PlayerData, PlayerAction) -> Unit,
    settingsAction: (String) -> Unit,
    dspSettingsAction: (String) -> Unit,
) {
    val player = playerData.player
    val queue = playerData.queue
    val currentProgress = queue?.currentItem?.track?.duration
        ?.let { (queue.elapsedTime?.toFloat() ?: 0f) / it.toFloat() }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(size = 16.dp))
            .alpha(if (isSelected) 1f else 0.4f)
            .background(
                color = MaterialTheme.colors.secondary,
                shape = RoundedCornerShape(size = 8.dp)
            )
    ) {
        OverflowMenu(
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
        queue?.currentItem?.track?.imageUrl?.let {
            AsyncImage(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(0.3f)
                    .clip(RoundedCornerShape(size = 16.dp)),
                model = it,
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
                color = MaterialTheme.colors.onPrimary,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.h5
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
                color = MaterialTheme.colors.onPrimary,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.h6
            )
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth(),
                progress = currentProgress ?: 0f,
                color = MaterialTheme.colors.onPrimary,
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

@Preview
@Composable
fun PlayerCardPreview() {
    MaterialTheme {
        PlayerCard(
            modifier = Modifier.fillMaxWidth(),
            playerData = PlayerData(
                player = Player(
                    id = "1",
                    name = "Test Player",
                    isPlaying = true,
                    isAnnouncing = false,
                    canSetVolume = true,
                    isBuiltin = false,
                    shouldBeShown = true,
                    queueId = null
                )
            ),
            isSelected = true,
            playerAction = { _, _ -> },
            settingsAction = {},
            dspSettingsAction = {}
        )
    }
}