package io.music_assistant.client.ui.compose.common

sealed class DataState<T> {
    class Loading<T> : DataState<T>()
    class Error<T> : DataState<T>()
    class NoData<T> : DataState<T>()
    data class Data<T>(val data: T) : DataState<T>()
}