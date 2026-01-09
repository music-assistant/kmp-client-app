package io.music_assistant.client.utils

import kotlinx.coroutines.CoroutineDispatcher

/**
 * Platform-specific main dispatcher.
 * On Android/iOS this is Dispatchers.Main, on desktop it's Dispatchers.Default.
 */
expect val mainDispatcher: CoroutineDispatcher
