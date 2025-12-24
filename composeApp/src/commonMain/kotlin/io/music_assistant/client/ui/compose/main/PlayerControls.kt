package io.music_assistant.client.ui.compose.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOn
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.ShuffleOn
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.VolumeDown
import compose.icons.fontawesomeicons.solid.VolumeUp
import io.music_assistant.client.data.model.client.PlayerData
import io.music_assistant.client.data.model.server.RepeatMode
import io.music_assistant.client.ui.compose.common.ActionIcon

@Composable
fun PlayerControls(
    modifier: Modifier = Modifier,
    playerData: PlayerData,
    enabled: Boolean,
    playerAction: (PlayerData, PlayerAction) -> Unit,
    showVolumeButtons: Boolean = true,
    showAdditionalButtons: Boolean = true,
    mainButtonSize: Dp = 48.dp
) {
    val player = playerData.player
    val queue = playerData.queueInfo
    val buttonsEnabled = queue?.currentItem != null
    val smallButtonSize = (mainButtonSize.value * 0.6).dp
    val additionalButtonSize = (mainButtonSize.value * 0.4).dp
    Row(
        modifier = modifier
            .wrapContentSize(),
        horizontalArrangement = Arrangement.spacedBy(
            8.dp,
            alignment = Alignment.CenterHorizontally
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (player.canSetVolume && showVolumeButtons) {
            ActionIcon(
                icon = FontAwesomeIcons.Solid.VolumeDown,
                tint = MaterialTheme.colorScheme.primary,
                size = smallButtonSize,
                enabled = enabled,
            ) { playerAction(playerData, PlayerAction.VolumeDown) }
        }

        if (showAdditionalButtons) {
            queue?.let {
                ActionIcon(
                    icon = if (it.shuffleEnabled)
                        Icons.Default.ShuffleOn
                    else
                        Icons.Default.Shuffle,
                    tint = MaterialTheme.colorScheme.primary,
                    size = additionalButtonSize,
                    enabled = enabled && buttonsEnabled,
                ) {
                    playerAction(
                        playerData,
                        PlayerAction.ToggleShuffle(current = it.shuffleEnabled)
                    )
                }
            }
        }

        ActionIcon(
            icon = Icons.Default.SkipPrevious,
            tint = MaterialTheme.colorScheme.primary,
            size = smallButtonSize,
            enabled = enabled && buttonsEnabled,
        ) { playerAction(playerData, PlayerAction.Previous) }

        ActionIcon(
            icon = when (player.isPlaying) {
                true -> Icons.Default.Pause
                false -> Icons.Default.PlayArrow
            },
            tint = MaterialTheme.colorScheme.primary,
            size = mainButtonSize,
            enabled = enabled && buttonsEnabled,
        ) { playerAction(playerData, PlayerAction.TogglePlayPause) }

        ActionIcon(
            icon = Icons.Default.SkipNext,
            tint = MaterialTheme.colorScheme.primary,
            size = smallButtonSize,
            enabled = enabled && buttonsEnabled,
        ) { playerAction(playerData, PlayerAction.Next) }

        if (showAdditionalButtons) {
            queue?.let {
                val repeatMode = it.repeatMode
                ActionIcon(
                    icon = when (repeatMode) {
                        RepeatMode.ONE -> Icons.Default.RepeatOne
                        RepeatMode.ALL -> Icons.Default.RepeatOn
                        RepeatMode.OFF,
                        null -> Icons.Default.Repeat
                    },
                    tint = MaterialTheme.colorScheme.primary,
                    size = additionalButtonSize,
                    enabled = enabled && buttonsEnabled && repeatMode != null,
                ) {
                    repeatMode?.let {
                        playerAction(
                            playerData,
                            PlayerAction.ToggleRepeatMode(current = repeatMode)
                        )
                    }
                }
            }
        }

        if (player.canSetVolume && showVolumeButtons) {
            ActionIcon(
                icon = FontAwesomeIcons.Solid.VolumeUp,
                tint = MaterialTheme.colorScheme.primary,
                size = smallButtonSize,
                enabled = enabled,
            ) { playerAction(playerData, PlayerAction.VolumeUp) }
        }
    }
}