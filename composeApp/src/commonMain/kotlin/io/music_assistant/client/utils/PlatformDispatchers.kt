package io.music_assistant.client.utils

import kotlinx.coroutines.CoroutineDispatcher

/**
 * Platform-specific main dispatcher.
 * On Android/iOS this is Dispatchers.Main.
 */
expect val mainDispatcher: CoroutineDispatcher

/**
 * Platform-specific high-priority dispatcher for audio streaming.
 * This dispatcher uses dedicated threads with elevated priority to ensure
 * smooth audio playback even when the app is backgrounded or UI is busy.
 *
 * - Android: Uses THREAD_PRIORITY_AUDIO for real-time audio processing
 * - iOS: Uses high-priority DispatchQueue
 */
expect val audioDispatcher: CoroutineDispatcher
