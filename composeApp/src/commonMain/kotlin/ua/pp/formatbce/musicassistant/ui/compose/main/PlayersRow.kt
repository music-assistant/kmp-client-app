package ua.pp.formatbce.musicassistant.ui.compose.main

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch
import ua.pp.formatbce.musicassistant.data.model.server.Player
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
        modifier = modifier.draggable(
            orientation = Orientation.Horizontal,
            state = rememberDraggableState { delta ->
                coroutineScope.launch {
                    scrollState.scrollBy(-delta)
                }
            },
        ),
        state = scrollState,
        flingBehavior = rememberSnapFlingBehavior(lazyListState = scrollState),
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
    val currentProgress = queue?.currentItem?.duration
        ?.let { (queue.elapsedTime?.toFloat() ?: 0f) / it.toFloat() }
    Box(
        modifier = modifier
            .padding(4.dp)
            .width(320.dp)
            .height(180.dp)
            .clip(RoundedCornerShape(size = 16.dp))
            .alpha(if (isSelected) 1f else 0.4f)
            .background(
                color = MaterialTheme.colors.primary,
                shape = RoundedCornerShape(size = 8.dp)
            )
            .clickable(enabled = !isSelected) { onClick() }
    ) {
        queue?.currentItem?.image?.path?.let {
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
                text = player.displayName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colors.onPrimary,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.h6
            )
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .basicMarquee(iterations = 100),
                textAlign = TextAlign.Center,
                text =
                if (player.announcementInProgress == true) "ANNOUNCING"
                else queue?.currentItem?.mediaItem?.trackDescription ?: "idle",
                maxLines = 1,
                color = MaterialTheme.colors.onPrimary,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.body2
            )
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                progress = currentProgress ?: 0f,
                color = MaterialTheme.colors.onPrimary,
                strokeCap = StrokeCap.Round
            )
            PlayerControls(
                playerData = playerData,
                playerAction = playerAction,
                enabled = player.announcementInProgress != true
            )
        }
    }
}