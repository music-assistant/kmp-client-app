package io.music_assistant.client.ui.compose.home

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.music_assistant.client.data.model.client.Queue
import io.music_assistant.client.ui.compose.common.DataState
import io.music_assistant.client.ui.compose.library.LibraryArgs
import io.music_assistant.client.utils.NavScreen

@Composable
fun CollapsibleQueue(
    modifier: Modifier = Modifier,
    queue: DataState<Queue>,
    libraryArgs: LibraryArgs,
    isQueueExpanded: Boolean,
    onQueueExpandedSwitch: () -> Unit,
    navigateTo: (NavScreen) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = if (isQueueExpanded) 0.dp else 16.dp)
            .animateContentSize(),
    ) {
        Row(
            modifier = Modifier
                .wrapContentSize()
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(16.dp)
                )
                .clickable(onClick = { onQueueExpandedSwitch() })
                .padding(vertical = 4.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Queue",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Icon(
                imageVector = if (isQueueExpanded) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                contentDescription = "Toggle Queue",
                tint = MaterialTheme.colorScheme.onPrimary,
            )
        }

        if (isQueueExpanded) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                val message: String? = when (queue) {
                    is DataState.Error -> "Error loading queue"
                    is DataState.Loading -> "Loading queue..."
                    is DataState.NoData -> "No items"
                    is DataState.Data -> when (queue.data.items) {
                        is DataState.Error -> "Error loading queue"
                        is DataState.Loading -> "Loading queue..."
                        is DataState.NoData -> "Not loaded"
                        is DataState.Data -> null
                    }
                }

                message?.let {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } ?: run {
                    val queueData = (queue as DataState.Data).data
                    val items = (queueData.items as DataState.Data).data

                    if (items.isEmpty()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Queue is empty",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            OutlinedButton(onClick = { navigateTo(NavScreen.Library(libraryArgs)) }) {
                                Text(text = "Browse Library")
                            }
                        }
                    } else {
                        val currentItemId = queueData.info.currentItem?.id
                        val currentItemIndex = currentItemId?.let { id ->
                            items.indexOfFirst { it.id == id }
                        } ?: -1

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(items.size) { index ->
                                QueueItemRow(
                                    item = items[index],
                                    position = index + 1,
                                    isCurrentItem = index == currentItemIndex,
                                    isPlayedItem = currentItemIndex >= 0 && index < currentItemIndex
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}