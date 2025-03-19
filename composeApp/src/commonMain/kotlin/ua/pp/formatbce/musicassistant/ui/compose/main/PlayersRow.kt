package ua.pp.formatbce.musicassistant.ui.compose.main

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.PhoneVolume
import compose.icons.fontawesomeicons.solid.PlayCircle
import kotlinx.coroutines.launch
import ua.pp.formatbce.musicassistant.data.model.server.MediaType
import ua.pp.formatbce.musicassistant.data.model.server.Player
import ua.pp.formatbce.musicassistant.data.model.server.PlayerState
import ua.pp.formatbce.musicassistant.data.source.PlayerData

@Composable
fun PlayersRow(
    modifier: Modifier = Modifier,
    players: List<PlayerData> = emptyList(),
    selectedPlayerId: String?,
    playerAction: (PlayerData, PlayerAction) -> Unit,
    onItemClick: (Player) -> Unit = {},

    ) {

    val scrollState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    LazyRow(
        state = scrollState,
        modifier = modifier.draggable(
            orientation = Orientation.Horizontal,
            state = rememberDraggableState { delta ->
                coroutineScope.launch {
                    scrollState.scrollBy(-delta)
                }
            },
        ),
        contentPadding = PaddingValues(horizontal = 8.dp),
    ) {
        items(items = players) { playerData ->
            val player = playerData.player
            PlayerCard(
                playerData = playerData,
                isSelected = selectedPlayerId == player.playerId,
                playerAction = playerAction
            ) { onItemClick(player) }
        }
    }
}

@Composable
fun PlayerCard(
    modifier: Modifier = Modifier,
    playerData: PlayerData,
    isSelected: Boolean,
    playerAction: (PlayerData, PlayerAction) -> Unit,
    onClick: () -> Unit,
) {
    val player = playerData.player
    val queue = playerData.queue
    Column(
        modifier = modifier
            .width(260.dp)
            .padding(4.dp)
            .clip(RoundedCornerShape(size = 16.dp))
            .alpha(if (isSelected) 1f else 0.4f)
            .background(
                color = MaterialTheme.colors.primary,
                shape = RoundedCornerShape(size = 8.dp)
            )
            .clickable(enabled = !isSelected) { onClick() }
    ) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .basicMarquee(iterations = 100),
            textAlign = TextAlign.Center,
            text = player.displayName,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colors.onPrimary,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.body1
        )
        Row(
            modifier = Modifier.wrapContentSize(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            queue?.currentItem?.image?.path?.let {
                AsyncImage(
                    modifier = Modifier
                        .padding(top = 4.dp, bottom = 4.dp, start = 4.dp, end = 0.dp)
                        .size(48.dp)
                        .clip(RoundedCornerShape(size = 16.dp)),
                    model = it,
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                )
            } ?: run {
                val isAnnouncing = player.currentMedia?.mediaType == MediaType.ANNOUNCEMENT
                        && player.announcementInProgress == true
                Icon(
                    modifier = Modifier
                        .padding(top = 8.dp, bottom = 8.dp, start = 8.dp, end = 4.dp)
                        .size(40.dp),
                    imageVector = if (isAnnouncing)
                        FontAwesomeIcons.Solid.PhoneVolume
                    else
                        FontAwesomeIcons.Solid.PlayCircle,
                    contentDescription = null,
                    tint = if (isAnnouncing)
                        MaterialTheme.colors.error
                    else
                        MaterialTheme.colors.onPrimary,
                )
            }
            Text(
                modifier = Modifier
                    .padding(start = 4.dp, end = 4.dp)
                    .fillMaxWidth()
                    .basicMarquee(iterations = 100),
                textAlign = TextAlign.Center,
                text = queue?.currentItem
                    ?.takeIf { player.state == PlayerState.PLAYING }?.mediaItem?.trackDescription
                    ?: "idle",
                maxLines = 1,
                color = MaterialTheme.colors.onPrimary,
                fontStyle = FontStyle.Italic,
                style = MaterialTheme.typography.body2
            )
        }
        PlayerControls(
            playerData = playerData,
            playerAction = playerAction
        )
    }
}