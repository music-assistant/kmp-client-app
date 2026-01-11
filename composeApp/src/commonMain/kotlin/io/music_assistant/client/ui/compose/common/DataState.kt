package io.music_assistant.client.ui.compose.common

import io.music_assistant.client.utils.AppError

sealed class DataState<T> {
    class Loading<T> : DataState<T>()
    data class Error<T>(val error: AppError? = null) : DataState<T>()
    class NoData<T> : DataState<T>()
    data class Data<T>(val data: T) : DataState<T>()
}