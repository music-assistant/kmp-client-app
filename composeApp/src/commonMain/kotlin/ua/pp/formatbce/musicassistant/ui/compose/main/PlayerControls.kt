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
import ua.pp.formatbce.musicassistant.data.model.server.PlayerFeature
import ua.pp.formatbce.musicassistant.data.model.server.PlayerState
import ua.pp.formatbce.musicassistant.data.model.server.RepeatMode
import ua.pp.formatbce.musicassistant.data.source.PlayerData
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
        if (player.supportedFeatures.contains(PlayerFeature.VOLUME_SET)) {
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
            icon = when (player.state) {
                PlayerState.PLAYING -> FontAwesomeIcons.Solid.Pause
                else -> FontAwesomeIcons.Solid.Play
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
            ActionIcon(
                icon = when (it.repeatMode) {
                    RepeatMode.ONE -> TablerIcons.RepeatOnce
                    RepeatMode.OFF,
                    RepeatMode.ALL -> TablerIcons.Repeat
                },
                tint = when (it.repeatMode) {
                    RepeatMode.OFF -> MaterialTheme.colors.onPrimary.copy(alpha = 0.5f)
                    RepeatMode.ALL,
                    RepeatMode.ONE -> MaterialTheme.colors.onPrimary
                },
                size = 22.dp,
                enabled = enabled && buttonsEnabled,
            ) { playerAction(playerData, PlayerAction.ToggleRepeatMode(current = it.repeatMode)) }
        }

        if (player.supportedFeatures.contains(PlayerFeature.VOLUME_SET)) {
            ActionIcon(
                icon = FontAwesomeIcons.Solid.VolumeUp,
                tint = MaterialTheme.colors.onPrimary,
                size = 22.dp,
                enabled = enabled,
            ) { playerAction(playerData, PlayerAction.VolumeUp) }
        }
    }
}