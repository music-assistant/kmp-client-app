package ua.pp.formatbce.musicassistant.ui.compose.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import compose.icons.FontAwesomeIcons
import compose.icons.TablerIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.FastBackward
import compose.icons.fontawesomeicons.solid.FastForward
import compose.icons.fontawesomeicons.solid.Pause
import compose.icons.fontawesomeicons.solid.Play
import compose.icons.fontawesomeicons.solid.VolumeDown
import compose.icons.fontawesomeicons.solid.VolumeUp
import compose.icons.tablericons.ArrowsRight
import compose.icons.tablericons.Repeat
import compose.icons.tablericons.RepeatOnce
import compose.icons.tablericons.Switch2
import ua.pp.formatbce.musicassistant.data.model.server.RepeatMode
import ua.pp.formatbce.musicassistant.data.model.client.PlayerData
import ua.pp.formatbce.musicassistant.ui.compose.common.ActionIcon

@Composable
fun PlayerControls(
    modifier: Modifier = Modifier,
    playerData: PlayerData,
    enabled: Boolean,
    playerAction: (PlayerData, PlayerAction) -> Unit
) {
    val player = playerData.player
    val queue = playerData.queue
    val buttonsEnabled = queue?.currentItem != null
    Row(
        modifier = modifier
            .padding(bottom = 8.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (player.canSetVolume) {
            ActionIcon(
                icon = FontAwesomeIcons.Solid.VolumeDown,
                tint = MaterialTheme.colors.onPrimary,
                size = 22.dp,
                enabled = enabled,
            ) { playerAction(playerData, PlayerAction.VolumeDown) }
        }

        queue?.let {
            ActionIcon(
                icon = if (it.shuffleEnabled)
                    TablerIcons.Switch2
                else
                    TablerIcons.ArrowsRight,
                tint = MaterialTheme.colors.onPrimary,
                size = 22.dp,
                enabled = enabled && buttonsEnabled,
            ) { playerAction(playerData, PlayerAction.ToggleShuffle(current = it.shuffleEnabled)) }
        }

        ActionIcon(
            icon = FontAwesomeIcons.Solid.FastBackward,
            tint = MaterialTheme.colors.onPrimary,
            size = 22.dp,
            enabled = enabled && buttonsEnabled,
        ) { playerAction(playerData, PlayerAction.Previous) }

        ActionIcon(
            icon = when (player.isPlaying) {
                true -> FontAwesomeIcons.Solid.Pause
                false -> FontAwesomeIcons.Solid.Play
            },
            tint = MaterialTheme.colors.onPrimary,
            size = 26.dp,
            enabled = enabled && buttonsEnabled,
        ) { playerAction(playerData, PlayerAction.TogglePlayPause) }

        ActionIcon(
            icon = FontAwesomeIcons.Solid.FastForward,
            tint = MaterialTheme.colors.onPrimary,
            size = 22.dp,
            enabled = enabled && buttonsEnabled,
        ) { playerAction(playerData, PlayerAction.Next) }

        queue?.let {
            val repeatMode = it.repeatMode
            ActionIcon(
                icon = when (repeatMode) {
                    RepeatMode.ONE -> TablerIcons.RepeatOnce
                    RepeatMode.OFF,
                    RepeatMode.ALL,
                    null -> TablerIcons.Repeat
                },
                tint = when (repeatMode) {
                    RepeatMode.OFF, null -> MaterialTheme.colors.onPrimary.copy(alpha = 0.5f)
                    RepeatMode.ALL,
                    RepeatMode.ONE -> MaterialTheme.colors.onPrimary
                },
                size = 22.dp,
                enabled = enabled && buttonsEnabled && repeatMode != null,
            ) {
                repeatMode?.let {
                    playerAction(playerData, PlayerAction.ToggleRepeatMode(current = repeatMode))
                }
            }
        }

        if (player.canSetVolume) {
            ActionIcon(
                icon = FontAwesomeIcons.Solid.VolumeUp,
                tint = MaterialTheme.colors.onPrimary,
                size = 22.dp,
                enabled = enabled,
            ) { playerAction(playerData, PlayerAction.VolumeUp) }
        }
    }
}