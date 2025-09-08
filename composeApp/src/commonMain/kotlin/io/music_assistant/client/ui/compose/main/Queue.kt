package io.music_assistant.client.ui.compose.main

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import compose.icons.FontAwesomeIcons
import compose.icons.TablerIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.DotCircle
import compose.icons.fontawesomeicons.solid.Play
import compose.icons.tablericons.ClipboardX
import compose.icons.tablericons.GripVertical
import io.music_assistant.client.data.model.client.PlayerData
import io.music_assistant.client.data.model.client.Queue
import io.music_assistant.client.data.model.client.QueueTrack
import io.music_assistant.client.ui.compose.common.MusicNotePainter
import io.music_assistant.client.utils.conditional
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun QueueSection(
    modifier: Modifier = Modifier,
    nestedScrollConnection: NestedScrollConnection,
    serverUrl: String?,
    playerData: PlayerData,
    queueItems: List<QueueTrack>?,
    chosenItemsIds: Set<String>?,
    queueAction: (QueueAction) -> Unit,
    onItemChosenChanged: (String) -> Unit,
    onChosenItemsClear: () -> Unit
) {
    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        queueItems?.takeIf { it.isNotEmpty() }?.let { items ->
            QueueUI(
                nestedScrollConnection = nestedScrollConnection,
                serverUrl = serverUrl,
                queue = playerData.queue,
                items = items,
                chosenItemsIds = chosenItemsIds,
                enabled = !playerData.player.isAnnouncing,
                queueAction = queueAction,
                onItemChosenChanged = onItemChosenChanged,
                onChosenItemsClear = onChosenItemsClear,
            )
        } ?: run {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Nothing here...",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colors.onSurface,
                    style = MaterialTheme.typography.body2,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QueueUI(
    nestedScrollConnection: NestedScrollConnection,
    serverUrl: String?,
    queue: Queue?,
    items: List<QueueTrack>,
    chosenItemsIds: Set<String>?,
    enabled: Boolean,
    queueAction: (QueueAction) -> Unit,
    onItemChosenChanged: (String) -> Unit,
    onChosenItemsClear: () -> Unit
) {

    var internalItems by remember(items) { mutableStateOf(items) }
    var dragEndIndex by remember { mutableStateOf<Int?>(null) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val currentIndex = internalItems.indexOfFirst {
        it.id == queue?.currentItem?.id
    }
    val reorderableLazyListState = rememberReorderableLazyListState(listState) { from, to ->
        if (to.index <= currentIndex) {
            return@rememberReorderableLazyListState
        }
        internalItems = internalItems.toMutableList().apply {
            add(to.index, removeAt(from.index))
        }
        dragEndIndex = to.index
    }
    val queueInfo = "${currentIndex + 1}/${internalItems.size}"
    val isInChooseMode = (chosenItemsIds?.size ?: 0) > 0
    LaunchedEffect(queue?.currentItem?.id) {
        if (currentIndex != -1) {
            val targetIndex = maxOf(0, currentIndex - 1) // Ensure it doesn't go negative
            listState.animateScrollToItem(targetIndex)
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .padding(horizontal = 12.dp)
            .alpha(if (enabled) 1f else 0.5f),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (chosenItemsIds?.isNotEmpty() == true) {
            val chosenItems = items.filter { chosenItemsIds.contains(it.id) }
            QueueTrackControls(
                queueId = queue?.id,
                chosenItems = chosenItems,
                enabled = enabled,
                queueAction = { queueAction(it) },
                onChosenItemsClear = onChosenItemsClear
            )
        } else {
            Text(
                text = queueInfo,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colors.onSurface,
                style = MaterialTheme.typography.body2,
                fontWeight = FontWeight.Bold
            )
            queue?.let {
                Icon(
                    modifier = Modifier
                        .padding(start = 14.dp)
                        .clickable(enabled = enabled) { queueAction(QueueAction.ClearQueue(it.id)) }
                        .size(24.dp)
                        .padding(all = 2.dp)
                        .align(alignment = Alignment.CenterVertically),
                    imageVector = TablerIcons.ClipboardX,
                    contentDescription = null,
                    tint = MaterialTheme.colors.primary,
                )
            }
        }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize()
            .clip(shape = RoundedCornerShape(16.dp))
            .background(MaterialTheme.colors.onSecondary)
            .nestedScroll(nestedScrollConnection)
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { delta ->
                    coroutineScope.launch {
                        listState.scrollBy(-delta)
                    }
                },
            )
            .alpha(if (enabled) 1f else 0.5f),
        state = listState,
    ) {
        itemsIndexed(items = internalItems, key = { _, item -> item.id }) { index, item ->
            val isCurrent = item.id == queue?.currentItem?.id
            val isChosen = chosenItemsIds?.contains(item.id) == true
            val isPlayed = index < currentIndex
            ReorderableItem(
                state = reorderableLazyListState,
                key = item.id,
                enabled = enabled,
            ) {
                Row(
                    modifier = Modifier
                        .padding(vertical = 1.dp)
                        .alpha(if (isPlayed && !isChosen) 0.5f else 1f)
                        .fillMaxWidth()
                        .clip(shape = RoundedCornerShape(16.dp))
                        .background(
                            when {
                                isChosen -> MaterialTheme.colors.primary
                                else -> Color.Transparent
                            }
                        )
                        .conditional(
                            condition = isPlayed,
                            ifTrue = {
                                clickable(!isInChooseMode) {
                                    queue?.id?.let { queueId ->
                                        queueAction(
                                            QueueAction.PlayQueueItem(
                                                queueId, item.id
                                            )
                                        )
                                    }
                                }
                            },
                            ifFalse = {
                                combinedClickable(
                                    enabled = enabled,
                                    onClick = {
                                        if (isInChooseMode) {
                                            onItemChosenChanged(item.id)
                                        } else if (!isCurrent) {
                                            queue?.id?.let { queueId ->
                                                queueAction(
                                                    QueueAction.PlayQueueItem(
                                                        queueId, item.id
                                                    )
                                                )
                                            }
                                        }
                                    },
                                    onLongClick = {
                                        onItemChosenChanged(item.id)
                                    },
                                )
                            }
                        )
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    val placeholder = MusicNotePainter(
                        backgroundColor = MaterialTheme.colors.background,
                        iconColor = when {
                            isChosen -> MaterialTheme.colors.onPrimary
                            else -> MaterialTheme.colors.secondary
                        }
                    )
                    AsyncImage(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(48.dp)
                            .clip(RoundedCornerShape(size = 4.dp)),
                        placeholder = placeholder,
                        fallback = placeholder,
                        model = item.track.imageInfo?.url(serverUrl),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                    )
                    if (isCurrent) {
                        Icon(
                            modifier = Modifier.padding(end = 8.dp).size(12.dp),
                            imageVector = FontAwesomeIcons.Solid.DotCircle,
                            contentDescription = null,
                            tint = when {
                                isChosen -> MaterialTheme.colors.onPrimary
                                else -> MaterialTheme.colors.secondary
                            },
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f).wrapContentHeight()
                    ) {
                        Text(
                            modifier = Modifier.fillMaxWidth().alpha(0.7f),
                            text = item.track.artists
                                ?.takeIf { it.isNotEmpty() }
                                ?.joinToString(separator = ", ") { it.name }
                                ?: "Unknown",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = when {
                                isChosen -> MaterialTheme.colors.onPrimary
                                else -> MaterialTheme.colors.secondary
                            },
                            style = MaterialTheme.typography.body2,
                        )
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = item.track.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = when {
                                isChosen -> MaterialTheme.colors.onPrimary
                                else -> MaterialTheme.colors.secondary
                            },
                            style = MaterialTheme.typography.body1,
                            fontWeight = when {
                                isCurrent -> FontWeight.Bold
                                else -> FontWeight.Normal
                            }
                        )
                    }
                    if (chosenItemsIds?.isNotEmpty() == false && !isCurrent && !isPlayed) {
                        Icon(
                            modifier = Modifier
                                .draggableHandle(
                                    onDragStopped = {
                                        dragEndIndex?.let { to ->
                                            queue?.id?.let { queueId ->
                                                queueAction(
                                                    QueueAction.MoveItem(
                                                        queueId,
                                                        item.id,
                                                        from = index,
                                                        to = to
                                                    )
                                                )
                                            }
                                        }
                                    }
                                )
                                .size(16.dp),
                            imageVector = TablerIcons.GripVertical,
                            contentDescription = null,
                            tint = MaterialTheme.colors.secondary,
                        )
                    }
                }
            }
        }
    }
}