# ConnectionService Design Document

## Purpose
Maintain Music Assistant WebSocket connections alive when app is backgrounded (e.g., during OAuth flow) by running a foreground service.

## Architecture

### Service Lifecycle
```
App Launch
  ↓
ServiceClient.connect() → SessionState.Connected
  ↓
Start ConnectionService (500ms debounce)
  ↓
Show "Connected to [ServerName]" notification
  ↓
Service keeps WebSocket connections alive
  ↓
User disconnects → SessionState.Disconnected.ByUser
  ↓
Stop ConnectionService
```

### Dual Connection Monitoring

ConnectionService observes **two** WebSocket connections:

1. **ServiceClient** (Primary - Music Assistant API)
   - Required for service to run
   - Service lifecycle tied to this connection

2. **SendspinClient** (Secondary - Audio Streaming)
   - Optional (only when Sendspin player selected)
   - If connected, enhances notification: "Connected + Streaming ready"

### State Flow

```kotlin
// ConnectionService monitors:
combine(
    serviceClient.sessionState,
    sendspinClient.connectionState
) { sessionState, sendspinState ->
    when {
        sessionState is SessionState.Connected -> {
            val hasSendspin = sendspinState is SendspinConnectionState.Connected
            ConnectionServiceState.Active(
                serverName = sessionState.serverInfo?.serverName ?: "Music Assistant",
                hasStreaming = hasSendspin
            )
        }
        else -> ConnectionServiceState.Inactive
    }
}
```

### Notification States

**Active (Connected)**
```
🎵 Music Assistant
Connected to [ServerName]
[Disconnect] button
```

**Active (Connected + Streaming)**
```
🎵 Music Assistant
Connected to [ServerName] • Streaming ready
[Disconnect] button
```

**Reconnecting**
```
🎵 Music Assistant
Reconnecting to [ServerName]...
(No action buttons)
```

## Anti-Flapping Measures

### 1. Debounced Service Start (500ms)
```kotlin
sessionState
    .debounce(500)
    .filter { it is SessionState.Connected }
    .collect { startService() }
```

### 2. Minimum Service Lifetime (30s)
```kotlin
private var serviceStartTime: Long? = null

fun maybeStopService() {
    val startTime = serviceStartTime ?: return
    val lifetime = System.currentTimeMillis() - startTime
    if (lifetime < 30_000) {
        logger.d { "Service running for ${lifetime}ms, waiting for 30s minimum" }
        return
    }
    stopSelf()
}
```

### 3. Connection Attempt Throttling
```kotlin
// In ServiceClient auto-reconnect:
private var reconnectAttempts = 0
private var lastReconnectTime = 0L

private fun getReconnectDelay(): Long {
    val now = System.currentTimeMillis()
    val timeSinceLastAttempt = now - lastReconnectTime

    return when {
        timeSinceLastAttempt < 5000 -> 5000L  // Max once per 5s
        reconnectAttempts < 3 -> 1000L        // Quick retry: 1s
        reconnectAttempts < 10 -> 5000L       // Medium retry: 5s
        else -> 30000L                        // Slow retry: 30s
    }
}
```

## Network Change Handling

Listen for network transitions and proactively reconnect:

```kotlin
// In ConnectionService:
private val networkCallback = object : ConnectivityManager.NetworkCallback() {
    override fun onAvailable(network: Network) {
        logger.i { "Network available: $network" }
        // Don't auto-reconnect yet, wait for stability
    }

    override fun onLost(network: Network) {
        logger.i { "Network lost: $network - will reconnect when new network available" }
    }

    override fun onCapabilitiesChanged(
        network: Network,
        capabilities: NetworkCapabilities
    ) {
        if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            logger.i { "Network has internet - triggering reconnection" }
            scope.launch {
                delay(1000) // Brief delay for network stability
                reconnectIfNeeded()
            }
        }
    }
}

fun registerNetworkCallback() {
    val connectivityManager = getSystemService<ConnectivityManager>()
    val request = NetworkRequest.Builder()
        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        .build()
    connectivityManager?.registerNetworkCallback(request, networkCallback)
}
```

## Sendspin Auto-Reconnect

### Pattern Matching ServiceClient

Add auto-reconnect to `WebSocketHandler`:

```kotlin
// WebSocketHandler.kt:
private var explicitDisconnect = false
private var reconnectAttempts = 0

suspend fun disconnect() {
    explicitDisconnect = true
    // ... existing disconnect logic
}

private fun startListening(wsSession: DefaultClientWebSocketSession) {
    listenerJob = launch {
        try {
            // ... existing frame handling
        } catch (e: Exception) {
            if (explicitDisconnect) {
                logger.i { "Explicit disconnect, not reconnecting" }
                return@launch
            }

            logger.w(e) { "WebSocket error, will auto-reconnect" }
            _connectionState.value = WebSocketState.Error(e)

            // Auto-reconnect with exponential backoff
            val delay = calculateReconnectDelay()
            logger.i { "Reconnecting in ${delay}ms" }
            delay(delay)

            reconnectAttempts++
            connect() // Recursive reconnect
        } finally {
            handleDisconnection()
        }
    }
}

private fun calculateReconnectDelay(): Long {
    return when (reconnectAttempts) {
        0 -> 1000L
        1 -> 2000L
        2 -> 5000L
        else -> 10000L
    }.coerceAtMost(30000L)
}

// Reset on successful connection:
suspend fun connect() {
    // ... existing connect logic
    reconnectAttempts = 0 // Reset on success
    explicitDisconnect = false
}
```

### Stream Restoration

Track streaming state for intelligent restoration:

```kotlin
// SendspinClient.kt:
private var wasStreamingBeforeDisconnect = false
private var lastStreamConfig: StreamStartPlayer? = null

private fun monitorProtocolState() {
    launch {
        messageDispatcher?.protocolState?.collect { state ->
            when (state) {
                is ProtocolState.Ready -> {
                    // ... existing connected logic

                    if (wasStreamingBeforeDisconnect) {
                        logger.i { "Was streaming before disconnect, waiting for StreamStart to resume..." }
                        // Server should auto-send StreamStart when player reconnects
                        // Monitor with timeout:
                        launch {
                            delay(5000)
                            if (_playbackState.value == SendspinPlaybackState.Idle) {
                                logger.w { "Stream restoration timed out - server didn't resume playback" }
                                wasStreamingBeforeDisconnect = false
                            }
                        }
                    }
                }

                ProtocolState.Disconnected -> {
                    // Remember if we were streaming
                    wasStreamingBeforeDisconnect =
                        _playbackState.value in listOf(
                            SendspinPlaybackState.Buffering,
                            SendspinPlaybackState.Synchronized
                        )

                    if (wasStreamingBeforeDisconnect) {
                        lastStreamConfig = audioStreamManager.currentStreamConfig
                        logger.i { "Disconnected while streaming - will attempt restoration" }
                    }
                }
            }
        }
    }
}
```

**Note**: Stream restoration has inherent limitations:
- Audio gap during reconnection is unavoidable (buffer cleared)
- Server queue position advances during disconnect
- Best outcome: Resume within 2-5 seconds with minimal gap

## Implementation Files

### New Files
1. `composeApp/src/androidMain/kotlin/io/music_assistant/client/services/ConnectionService.kt`
   - Foreground service
   - Monitors ServiceClient + SendspinClient states
   - Shows connection notification
   - Registers network change callback

2. `composeApp/src/androidMain/kotlin/io/music_assistant/client/services/ConnectionNotificationManager.kt`
   - Creates connection notifications
   - Handles notification updates
   - "Disconnect" action handler

### Modified Files
1. `composeApp/src/commonMain/kotlin/io/music_assistant/client/player/sendspin/connection/WebSocketHandler.kt`
   - Add auto-reconnect logic
   - Add `explicitDisconnect` flag
   - Add reconnect delay calculation

2. `composeApp/src/commonMain/kotlin/io/music_assistant/client/player/sendspin/SendspinClient.kt`
   - Add stream restoration tracking
   - Monitor disconnection during streaming
   - Attempt restoration on reconnect

3. `composeApp/src/commonMain/kotlin/io/music_assistant/client/api/ServiceClient.kt`
   - Improve reconnect throttling
   - Add reconnect attempt counter reset on success

4. `composeApp/src/androidMain/kotlin/io/music_assistant/client/MainActivity.kt`
   - Remove playback-based service start (lines 38-50)
   - Start ConnectionService on app launch or connection

5. `composeApp/src/androidMain/AndroidManifest.xml`
   - Add ConnectionService declaration
   - Add FOREGROUND_SERVICE permission
   - Add POST_NOTIFICATIONS permission (Android 13+)

## Testing Strategy

### OAuth Flow Test
1. Connect to Music Assistant → ConnectionService starts
2. Trigger OAuth flow → Chrome Custom Tab opens
3. Verify: ConnectionService keeps running (check notification)
4. Verify: WebSocket stays connected (check logs)
5. Complete OAuth → Return to app
6. Verify: Still connected, no reconnection needed

### Network Transition Test
1. Connect on WiFi
2. Toggle WiFi off (force cellular)
3. Verify: Auto-reconnect triggers within 2-3s
4. Verify: Notification shows "Reconnecting..."
5. Verify: Connection restored
6. Toggle WiFi back on
7. Verify: Connection maintained or quickly restored

### Sendspin Streaming Test
1. Start Sendspin playback
2. Force disconnect (airplane mode on/off)
3. Verify: Auto-reconnect triggers
4. Verify: Server resends StreamStart
5. Verify: Playback resumes (with expected gap)

### Service Flapping Prevention Test
1. Repeatedly toggle connection on/off rapidly
2. Verify: Service doesn't restart more than once per 500ms
3. Verify: Service stays alive for minimum 30s
4. Verify: Notification doesn't spam

## Benefits

✅ **OAuth Flow**: No more WebSocket disconnection during auth
✅ **Network Transitions**: Fast recovery on WiFi ↔ Cellular switch
✅ **Background Stability**: Foreground service prevents Android from killing connections
✅ **Stream Restoration**: Audio playback resumes after brief disconnection
✅ **Clean Architecture**: Separation of concerns (connection service vs playback service)
✅ **User Experience**: Single "Connected" notification, minimal disruption

## Limitations

⚠️ **Audio Gaps**: Inevitable during reconnection (buffer cleared)
⚠️ **Battery**: Foreground service uses more battery (acceptable tradeoff)
⚠️ **Persistent Notification**: Android requires it for foreground services
⚠️ **Server State**: Can't restore server-side queue position perfectly
