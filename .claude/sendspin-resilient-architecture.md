# Sendspin Resilient Streaming Architecture

## Core Principle: Decouple Playback from WebSocket State

### Current Problem
```
WebSocket disconnects
  ↓
disconnectFromServer() called
  ↓
audioStreamManager.stopStream() → Buffer cleared
  ↓
Playback stops (even though buffer had 5 seconds of audio!)
```

### New Architecture
```
WebSocket disconnects
  ↓
WebSocketHandler auto-reconnects (DON'T call disconnectFromServer)
  ↓
AudioStreamManager keeps playing from buffer
  ↓
If reconnect succeeds before buffer empty:
  - Send Resume message with current position
  - Server resumes streaming
  - Gapless continuation! ✅
  ↓
If buffer runs dry before reconnect:
  - Playback underrun (natural pause)
  - When reconnect succeeds, server sends StreamStart
  - Resume with small gap (unavoidable)
```

## Key Insight from VPN Testing

**Why VPN prevents WebSocket disconnection:**
- VPN tunnel maintains persistent connection to VPN server
- Network change (WiFi ↔ Cellular) happens at lower layer
- VPN client handles transition transparently
- App's WebSocket (over VPN tunnel) never breaks

**Can we replicate without VPN?**
Yes! Two strategies:

### Strategy 1: More Resilient WebSocket
```kotlin
// Ktor WebSocket configuration:
HttpClient(CIO) {
    install(WebSockets) {
        pingInterval = 5.seconds  // More aggressive (current: 30s)
        maxFrameSize = Long.MAX_VALUE
    }

    engine {
        // TCP socket options for keepalive
        endpoint {
            keepAliveTime = 5000  // 5 seconds
            connectTimeout = 10000
            socketTimeout = 10000
        }
    }
}
```

Benefits:
- Keeps connection alive during brief network transitions
- TCP keepalive prevents router/NAT from dropping idle connections
- May survive WiFi ↔ Cellular transition (like VPN does)

### Strategy 2: Fast Transparent Reconnection
Even if WebSocket does disconnect:
- Don't tear down playback pipeline
- Keep playing from buffer
- Auto-reconnect in background
- Resume streaming before buffer empties

## Architecture Changes

### 1. AudioStreamManager - Independent Lifecycle

**Current:**
```kotlin
suspend fun stopStream() {
    isStreaming = false
    audioBuffer.clear()  // ❌ Loses buffered data!
    // ...
}
```

**New: Add `suspend()` method (like Android AudioTrack.pause()):**
```kotlin
private var isSuspended = false

suspend fun suspendStream() {
    logger.i { "Suspending stream (WebSocket reconnecting, buffer preserved)" }
    isSuspended = true
    // Don't clear buffer!
    // Don't stop playback thread!
    // Just stop accepting new chunks temporarily
}

suspend fun resumeStream() {
    logger.i { "Resuming stream (WebSocket reconnected)" }
    isSuspended = true
    // Continue playing from buffer
}

// stopStream() only called on explicit StreamEnd or user stop
suspend fun stopStream() {
    logger.i { "Stopping stream (permanent)" }
    isStreaming = false
    isSuspended = false
    audioBuffer.clear()
    // ... existing cleanup
}
```

**Buffer handling during suspension:**
```kotlin
suspend fun processBinaryMessage(data: ByteArray) {
    if (isSuspended) {
        logger.d { "Stream suspended, buffering chunk for later" }
        // Still buffer chunks during reconnection!
        // This allows seamless continuation
    }

    if (!isStreaming && !isSuspended) {
        logger.d { "Not streaming (ignoring)" }
        return
    }

    // ... existing processing
}
```

### 2. WebSocketHandler - Auto-Reconnect WITHOUT Teardown

**Add auto-reconnect logic:**
```kotlin
private var explicitDisconnect = false
private var reconnectAttempts = 0
private val maxReconnectAttempts = 10
private var reconnectJob: Job? = null

suspend fun disconnect() {
    explicitDisconnect = true
    reconnectJob?.cancel()
    // ... existing disconnect logic
}

private fun startListening(wsSession: DefaultClientWebSocketSession) {
    listenerJob = launch {
        try {
            for (frame in wsSession.incoming) {
                // ... existing frame handling
            }
        } catch (e: Exception) {
            if (explicitDisconnect) {
                logger.i { "Explicit disconnect, not reconnecting" }
                handleDisconnection()
                return@launch
            }

            // Network error - auto-reconnect!
            logger.w(e) { "WebSocket error, attempting auto-reconnect" }
            _connectionState.value = WebSocketState.Reconnecting(reconnectAttempts)

            attemptReconnect()
        }
    }
}

private fun attemptReconnect() {
    reconnectJob = launch {
        while (reconnectAttempts < maxReconnectAttempts && !explicitDisconnect) {
            val delay = calculateBackoff()
            logger.i { "Reconnect attempt ${reconnectAttempts + 1} in ${delay}ms" }
            delay(delay)

            try {
                reconnectAttempts++

                // Try to reconnect
                val wsSession = client.webSocketSession(serverUrl)
                session = wsSession

                // Success!
                logger.i { "Reconnected successfully after $reconnectAttempts attempts" }
                reconnectAttempts = 0
                _connectionState.value = WebSocketState.Connected

                // Resume listening
                startListening(wsSession)
                return@launch

            } catch (e: Exception) {
                logger.w(e) { "Reconnect attempt $reconnectAttempts failed" }
                if (reconnectAttempts >= maxReconnectAttempts) {
                    logger.e { "Max reconnect attempts reached, giving up" }
                    _connectionState.value = WebSocketState.Error(
                        Exception("Failed to reconnect after $maxReconnectAttempts attempts")
                    )
                    handleDisconnection()
                }
            }
        }
    }
}

private fun calculateBackoff(): Long {
    // Exponential backoff: 500ms, 1s, 2s, 5s, 10s
    return when (reconnectAttempts) {
        0 -> 500L
        1 -> 1000L
        2 -> 2000L
        3 -> 5000L
        else -> 10000L
    }
}
```

### 3. SendspinClient - Coordinate Reconnection

**Monitor WebSocket state and coordinate with AudioStreamManager:**

```kotlin
private fun monitorWebSocketState() {
    launch {
        webSocketHandler?.connectionState?.collect { wsState ->
            when (wsState) {
                WebSocketState.Connected -> {
                    // Check if we were streaming before
                    if (wasStreamingBeforeDisconnect) {
                        logger.i { "Reconnected while streaming - sending resume request" }
                        sendResumeRequest()
                    }
                }

                is WebSocketState.Reconnecting -> {
                    // Remember we're streaming (don't stop!)
                    wasStreamingBeforeDisconnect = isCurrentlyStreaming()

                    if (wasStreamingBeforeDisconnect) {
                        logger.i { "Connection lost during streaming - preserving playback" }
                        // DON'T call stopStream()!
                        // AudioStreamManager will keep playing from buffer
                    }
                }

                is WebSocketState.Error -> {
                    // Only stop if max reconnect attempts exceeded
                    if (!isReconnecting()) {
                        logger.e { "Connection failed permanently" }
                        audioStreamManager.stopStream()
                        _playbackState.value = SendspinPlaybackState.Idle
                    }
                }

                WebSocketState.Disconnected -> {
                    // Only if explicit disconnect
                    if (explicitDisconnect) {
                        audioStreamManager.stopStream()
                        _connectionState.value = SendspinConnectionState.Idle
                    }
                }
            }
        }
    }
}

private fun isCurrentlyStreaming(): Boolean {
    return _playbackState.value in listOf(
        SendspinPlaybackState.Buffering,
        SendspinPlaybackState.Synchronized
    )
}

private suspend fun sendResumeRequest() {
    val currentPosition = audioStreamManager.playbackPosition.value
    val bufferedDuration = audioStreamManager.bufferState.value.bufferedDuration

    logger.i { "Sending resume: position=$currentPosition, buffered=${bufferedDuration}μs" }

    // Send custom message to server indicating current playback state
    messageDispatcher?.sendCommand("resume", CommandValue(
        position = currentPosition,
        buffered = bufferedDuration
    ))
}
```

### 4. WebSocketState - Add Reconnecting State

```kotlin
sealed class WebSocketState {
    data object Disconnected : WebSocketState()
    data object Connecting : WebSocketState()
    data class Reconnecting(val attempt: Int) : WebSocketState()  // NEW!
    data object Connected : WebSocketState()
    data class Error(val error: Throwable) : WebSocketState()
}
```

## 3. Resume Protocol

When reconnecting during active streaming, client should inform server of current state.

**Option A: Extend Hello message**
```kotlin
// In MessageDispatcher.sendHello():
val helloPayload = buildJsonObject {
    put("client", clientCapabilities)

    // If resuming playback:
    if (isResuming) {
        putJsonObject("resume") {
            put("position", currentPosition)
            put("buffered_duration", bufferedDuration)
        }
    }
}
```

**Option B: New Resume message**
```kotlin
// After reconnecting, send resume message:
messageDispatcher?.sendMessage(
    MessageType.PLAYER_COMMAND,
    buildJsonObject {
        put("command", "resume")
        put("position", currentPosition)
        put("buffered_duration", bufferedDuration)
    }
)
```

Server can then:
1. Check if this player is still in its active players list
2. Check current queue position
3. Resume streaming from appropriate position
4. If queue moved on, send new StreamStart with updated metadata

## 4. ConnectionService - Android Holder Only

**Purpose**: Just keep foreground service running, hold references

```kotlin
// composeApp/src/androidMain/kotlin/.../services/ConnectionService.kt
class ConnectionService : Service() {

    // Injected via Koin
    private val serviceClient: ServiceClient by inject()
    private val sendspinClient: SendspinClient by inject()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Show notification
        startForeground(NOTIFICATION_ID, createNotification())

        // Monitor connection states (just for notification updates)
        monitorConnectionStates()

        return START_STICKY
    }

    private fun monitorConnectionStates() {
        scope.launch {
            combine(
                serviceClient.sessionState,
                sendspinClient.connectionState
            ) { sessionState, sendspinState ->
                // Update notification based on states
                updateNotification(sessionState, sendspinState)
            }.collect()
        }
    }

    // NO business logic here!
    // All logic stays in ServiceClient and SendspinClient (commonMain)
}
```

## Testing Strategy

### Test 1: Buffered Playback During Network Transition
1. Start Sendspin playback
2. Verify buffer has ~5 seconds of audio
3. Enable airplane mode (force disconnect)
4. Verify: Playback continues from buffer
5. Disable airplane mode within 5 seconds
6. Verify: Reconnection happens, streaming resumes, NO gap heard

### Test 2: Long Disconnection (Buffer Exhaustion)
1. Start Sendspin playback
2. Enable airplane mode
3. Wait 10 seconds (buffer empties)
4. Verify: Playback underrun (pauses naturally)
5. Disable airplane mode
6. Verify: Reconnection, StreamStart received, playback resumes
7. Small gap is acceptable (buffer was empty)

### Test 3: WiFi ↔ Cellular Transition
1. Start Sendspin playback on WiFi
2. Disable WiFi (force cellular)
3. Verify: Either WebSocket survives OR reconnects within 2s
4. Verify: No audible gap (buffer bridged the transition)

### Test 4: OAuth Flow (Original Problem)
1. Connect to Music Assistant
2. ConnectionService starts
3. Trigger OAuth → Chrome Custom Tab opens
4. Verify: ServiceClient WebSocket stays connected
5. Complete OAuth
6. Verify: Still connected, no reconnection needed

## Expected Outcomes

✅ **VPN-like resilience**: Aggressive keepalive may prevent disconnection entirely
✅ **Gapless network transitions**: 5s buffer bridges 1-2s reconnection time
✅ **Graceful degradation**: If reconnect takes longer, buffer empties naturally (pause)
✅ **Stream restoration**: Server can resume from last known position
✅ **No playback disruption**: User doesn't notice brief network issues
✅ **All logic in commonMain**: ConnectionService is just Android lifecycle wrapper

## Implementation Order

1. ✅ **Revise WebSocket keepalive** (quick win, may solve problem entirely)
2. ✅ **Add WebSocketHandler auto-reconnect** (preserves AudioStreamManager)
3. ✅ **Add AudioStreamManager suspend/resume** (buffer preservation)
4. ✅ **SendspinClient reconnection coordination** (resume protocol)
5. ✅ **ConnectionService** (Android foreground wrapper)
6. ✅ **Testing** (verify gapless transitions work)

## Key Differences from Previous Design

| Previous Design | New Design |
|----------------|-----------|
| Focus on ConnectionService | Focus on decoupling playback from WebSocket |
| Reconnect but clear buffer | Reconnect while preserving buffer |
| Accept gaps as inevitable | Eliminate gaps via buffer bridging |
| Separate concerns via services | Separate concerns via state decoupling |
| Android-specific solution | CommonMain architecture + Android wrapper |

This architecture should achieve **gapless playback during network transitions** by leveraging the existing 5-second buffer!
