package io.music_assistant.client.utils

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors

actual val mainDispatcher: CoroutineDispatcher = Dispatchers.Main

/**
 * High-priority audio dispatcher for Desktop.
 * Uses a single dedicated thread with maximum thread priority for the playback loop.
 *
 * ONLY used for the playback loop - reading PCM from buffer and writing to audio output.
 * This is the only operation that needs real-time priority.
 *
 * All other operations use normal priority:
 * - Binary message processing (Dispatchers.Default) - decodes and buffers with headroom
 * - Monitoring, commands, metadata (Dispatchers.Default) - not time-critical
 *
 * This ensures smooth playback while minimizing the number of high-priority threads.
 */
actual val audioDispatcher: CoroutineDispatcher = Executors.newSingleThreadExecutor { runnable ->
    Thread(runnable, "AudioThread-${System.currentTimeMillis()}").apply {
        priority = Thread.MAX_PRIORITY
        isDaemon = false
    }
}.asCoroutineDispatcher()
