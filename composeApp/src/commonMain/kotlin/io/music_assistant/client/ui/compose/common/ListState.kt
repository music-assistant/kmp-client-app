package io.music_assistant.client.ui.compose.common

import io.music_assistant.client.data.model.client.AppMediaItem

sealed class ListState<T : AppMediaItem> {
    class NoData<T : AppMediaItem> : ListState<T>()
    class Loading<T : AppMediaItem> : ListState<T>()
    class Error<T : AppMediaItem> : ListState<T>()
    data class Data<T : AppMediaItem>(val items: List<T>) : ListState<T>()
}