package io.music_assistant.client.utils

import android.os.Process
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors

actual val mainDispatcher: CoroutineDispatcher = Dispatchers.Main

/**
 * High-priority audio dispatcher for Android.
 * Uses a single dedicated thread with THREAD_PRIORITY_AUDIO for the playback loop.
 *
 * ONLY used for the playback loop - reading PCM from buffer and writing to AudioTrack.
 * This is the only operation that needs real-time priority.
 *
 * All other operations use normal priority:
 * - Binary message processing (Dispatchers.Default) - decodes and buffers with headroom
 * - Monitoring, commands, metadata (Dispatchers.Default) - not time-critical
 *
 * This ensures smooth playback even when app is backgrounded while minimizing
 * the number of high-priority threads.
 */
actual val audioDispatcher: CoroutineDispatcher = Executors.newSingleThreadExecutor { runnable ->
    Thread({
        // Set Android process priority for this thread to AUDIO priority
        // THREAD_PRIORITY_AUDIO = -16 (higher priority than default 0)
        // This ensures audio streaming continues smoothly even when app is backgrounded
        Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
        runnable.run()
    }, "AudioThread-${System.currentTimeMillis()}").apply {
        priority = Thread.MAX_PRIORITY
        setUncaughtExceptionHandler { thread, exception ->
            android.util.Log.e("AudioDispatcher", "Uncaught exception in $thread", exception)
        }
    }
}.asCoroutineDispatcher()
