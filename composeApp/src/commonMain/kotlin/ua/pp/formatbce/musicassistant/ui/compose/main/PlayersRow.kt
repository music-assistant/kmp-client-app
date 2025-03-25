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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import compose.icons.TablerIcons
import compose.icons.tablericons.GripVertical
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableCollectionItemScope
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import ua.pp.formatbce.musicassistant.data.model.client.Player
import ua.pp.formatbce.musicassistant.data.model.client.PlayerData

@Composable
fun PlayersRow(
    modifier: Modifier = Modifier,
    players: List<PlayerData> = emptyList(),
    selectedPlayerId: String?,
    playerAction: (PlayerData, PlayerAction) -> Unit,
    onListReordered: (List<String>) -> Unit,
    onItemClick: (Player) -> Unit,
) {
    var internalItems by remember(players) { mutableStateOf(players) }
    val scrollState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val reorderableLazyListState = rememberReorderableLazyListState(scrollState) { from, to ->
        internalItems = internalItems.toMutableList().apply {
            add(to.index, removeAt(from.index))
        }
    }
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
        items(items = internalItems, key = { it.player.id }) { playerData ->
            val player = playerData.player
            ReorderableItem(
                state = reorderableLazyListState,
                key = player.id,
            ) {
                PlayerCard(
                    playerData = playerData,
                    isSelected = selectedPlayerId == player.id,
                    playerAction = playerAction,
                    reorderScope = this,
                    onItemMoved = {
                        if (internalItems != players)
                            onListReordered(internalItems.map { it.player.id })
                    }
                ) { onItemClick(player) }
            }
        }
    }
}

@Composable
fun PlayerCard(
    modifier: Modifier = Modifier,
    playerData: PlayerData,
    isSelected: Boolean,
    playerAction: (PlayerData, PlayerAction) -> Unit,
    reorderScope: ReorderableCollectionItemScope,
    onItemMoved: () -> Unit,
    onClick: () -> Unit,
) {
    val player = playerData.player
    val queue = playerData.queue
    val currentProgress = queue?.currentItem?.track?.duration
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
                style = MaterialTheme.typography.h6
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
                enabled = !player.isAnnouncing
            )
        }
        Icon(
            modifier = with(reorderScope) {
                Modifier
                    .draggableHandle(
                        onDragStopped = {
                            onItemMoved()
                            println("Moving ${player.id}")
                        },
                        onDragStarted = { println("Moving ${player.id}") }
                    )
                    .padding(vertical = 12.dp, horizontal = 8.dp)
                    .size(16.dp)
                    .align(Alignment.TopEnd)
            },
            imageVector = TablerIcons.GripVertical,
            contentDescription = null,
            tint = MaterialTheme.colors.onPrimary,
        )
    }
}