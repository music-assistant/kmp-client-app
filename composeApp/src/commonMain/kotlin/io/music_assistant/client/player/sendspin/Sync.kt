@file:OptIn(ExperimentalTime::class)

package io.music_assistant.client.player.sendspin

import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

enum class SyncQuality {
    GOOD,
    DEGRADED,
    LOST
}

data class ClockStats(
    val offset: Long,
    val rtt: Long,
    val quality: SyncQuality
)

class ClockSynchronizer {
    // Clock synchronization state
    private var offset: Long = 0 // μs (server - client)
    private var drift: Double = 0.0 // μs/μs
    private var rawOffset: Long = 0
    private var rtt: Long = 0
    private var quality: SyncQuality = SyncQuality.LOST
    private var lastSyncTime: Instant? = null
    private var lastSyncMicros: Long = 0
    private var sampleCount: Int = 0
    private val smoothingRate: Double = 0.1

    // Server loop origin tracking
    private val clientProcessStartAbsolute: Long =
        Clock.System.now().toEpochMilliseconds() * 1000
    private var serverLoopOriginAbsolute: Long = 0

    val currentOffset: Long get() = offset
    val currentQuality: SyncQuality get() = quality

    fun getStats() = ClockStats(offset, rtt, quality)

    fun processServerTime(
        clientTransmitted: Long, // t1
        serverReceived: Long, // t2
        serverTransmitted: Long, // t3
        clientReceived: Long // t4
    ) {
        val (calculatedRtt, measuredOffset) = calculateOffset(
            clientTransmitted, serverReceived,
            serverTransmitted, clientReceived
        )

        rtt = calculatedRtt
        rawOffset = measuredOffset
        lastSyncTime = Clock.System.now()

        // Discard invalid samples
        if (calculatedRtt !in 0..100_000) return

        // First sync: initialize
        if (sampleCount == 0) {
            offset = measuredOffset
            lastSyncMicros = clientReceived
            serverLoopOriginAbsolute = clientProcessStartAbsolute - offset
            sampleCount++
            quality = SyncQuality.GOOD
            return
        }

        // Second sync: calculate initial drift
        if (sampleCount == 1) {
            val deltaTime = (clientReceived - lastSyncMicros).toDouble()
            if (deltaTime > 0) {
                drift = (measuredOffset - offset).toDouble() / deltaTime
            }
            offset = measuredOffset
            lastSyncMicros = clientReceived
            serverLoopOriginAbsolute = clientProcessStartAbsolute - offset
            sampleCount++
            quality = SyncQuality.GOOD
            return
        }

        // Subsequent syncs: Kalman filter update
        val deltaTime = (clientReceived - lastSyncMicros).toDouble()
        if (deltaTime <= 0) return

        val predictedOffset = offset + (drift * deltaTime).toLong()
        val residual = measuredOffset - predictedOffset

        // Reject outliers
        if (kotlin.math.abs(residual) > 50_000) return

        // Update offset and drift
        offset = predictedOffset + (smoothingRate * residual).toLong()
        val driftCorrection = residual.toDouble() / deltaTime
        drift += smoothingRate * driftCorrection

        lastSyncMicros = clientReceived
        sampleCount++
        serverLoopOriginAbsolute = clientProcessStartAbsolute - offset

        quality = if (calculatedRtt < 50_000) SyncQuality.GOOD else SyncQuality.DEGRADED
    }

    private fun calculateOffset(
        clientTx: Long,
        serverRx: Long,
        serverTx: Long,
        clientRx: Long
    ): Pair<Long, Long> {
        val rtt = (clientRx - clientTx) - (serverTx - serverRx)
        val offset = ((serverRx - clientTx) + (serverTx - clientRx)) / 2
        return rtt to offset
    }

    fun serverTimeToLocal(serverTime: Long): Long {
        if (sampleCount == 0) {
            return clientProcessStartAbsolute + serverTime
        }
        return serverLoopOriginAbsolute + serverTime
    }

    fun localTimeToServer(localTime: Long): Long {
        if (sampleCount == 0) {
            return localTime - clientProcessStartAbsolute
        }
        return localTime - serverLoopOriginAbsolute
    }

    fun checkQuality(): SyncQuality {
        lastSyncTime?.let { lastSync ->
            val elapsed = (Clock.System.now() - lastSync).inWholeMilliseconds
            if (elapsed > 5000) {
                quality = SyncQuality.LOST
            }
        }
        return quality
    }

    fun reset() {
        offset = 0
        drift = 0.0
        rawOffset = 0
        rtt = 0
        quality = SyncQuality.LOST
        lastSyncTime = null
        lastSyncMicros = 0
        serverLoopOriginAbsolute = 0
        sampleCount = 0
    }
}