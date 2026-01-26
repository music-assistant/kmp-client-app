package io.music_assistant.client.player.sendspin.audio

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.PriorityQueue

data class AudioChunk(
    val timestamp: Long, // server microseconds
    val data: ByteArray,
    val localTimestamp: Long // converted to local time
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AudioChunk) return false
        return timestamp == other.timestamp &&
                localTimestamp == other.localTimestamp &&
                data.contentEquals(other.data)
    }

    override fun hashCode(): Int {
        var result = timestamp.hashCode()
        result = 31 * result + localTimestamp.hashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }
}

class TimestampOrderedBuffer {
    private val queue = PriorityQueue<AudioChunk>(compareBy { it.localTimestamp })
    private val mutex = Mutex()
    private var maxTimestamp: Long = 0L

    suspend fun add(chunk: AudioChunk) = mutex.withLock {
        queue.add(chunk)
        if (chunk.localTimestamp > maxTimestamp) {
            maxTimestamp = chunk.localTimestamp
        }
    }

    suspend fun peek(): AudioChunk? = mutex.withLock {
        queue.peek()
    }

    suspend fun poll(): AudioChunk? = mutex.withLock {
        val polled = queue.poll()
        // Recalculate max if queue is now empty
        if (queue.isEmpty()) {
            maxTimestamp = 0L
        }
        polled
    }

    suspend fun clear() = mutex.withLock {
        queue.clear()
        maxTimestamp = 0L
    }

    suspend fun size(): Int = mutex.withLock {
        queue.size
    }

    suspend fun isEmpty(): Boolean = mutex.withLock {
        queue.isEmpty()
    }

    suspend fun isNotEmpty(): Boolean = mutex.withLock {
        queue.isNotEmpty()
    }

    suspend fun getBufferedDuration(): Long = mutex.withLock {
        if (queue.isEmpty()) {
            return@withLock 0L
        }
        val first = queue.peek()?.localTimestamp ?: return@withLock 0L
        maxTimestamp - first
    }
}
