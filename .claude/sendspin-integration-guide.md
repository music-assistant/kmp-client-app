# Sendspin Integration Guide - Music Assistant Client

## Overview

The Sendspin protocol integration has been simplified for the Music Assistant Client use case:
- **No mDNS discovery needed** - reuses existing MA server IP from settings
- **Direct WebSocket connection** - connects to `ws://{ma-server-ip}:{port}/sendspin`
- **Simple settings integration** - just add Sendspin port/path to existing settings

**Status:** ✅ **PRODUCTION-READY** - Android with PCM and Opus support (2026-01-05)
- ✅ Playback, pause, resume, seek
- ✅ Next/previous track
- ✅ Metadata display
- ✅ Opus codec (90%+ bandwidth savings)
- ✅ Adaptive buffering (network-aware)
- ⚠️ Volume/mute (receives commands, no UI yet)

---

## ⚠️ Important: Implementation Prerequisites

Before using this guide, ensure you understand the critical fixes documented in `sendspin-integration-design.md`:

1. **Clock sync MUST use monotonic time** (`System.nanoTime()`), not epoch time
2. **State reporting MUST be periodic** (every 2 seconds during playback)

See `sendspin-status.md` for full details on current implementation status and known issues.

---

## Architecture

```
┌──────────────────────────────────────────────────────────┐
│                  Music Assistant Client                   │
├──────────────────────────────────────────────────────────┤
│                                                            │
│  Settings:                                                │
│  • MA Server IP: 192.168.1.100 (existing)                │
│  • Sendspin Port: 8927                                    │
│  • Sendspin Path: /sendspin                               │
│  • Sendspin Enabled: true                                 │
│  • Device Name: "My Phone"                                │
│                                                            │
│  ┌──────────────────────────────────────────────────┐    │
│  │              SendspinClient                       │    │
│  │  • Build URL from settings                        │    │
│  │  • Connect to MA server via WebSocket             │    │
│  │  • Handle protocol messages                       │    │
│  │  • Stream audio via AudioTrack                    │    │
│  └──────────────────────────────────────────────────┘    │
│                                                            │
└──────────────────────────────────────────────────────────┘
                             │
                             │ ws://192.168.1.100:8927/sendspin
                             ▼
┌──────────────────────────────────────────────────────────┐
│              Music Assistant Server                       │
│              (with Sendspin support)                      │
└──────────────────────────────────────────────────────────┘
```

---

## Step 1: Update SettingsRepository

Add Sendspin configuration to your existing settings:

```kotlin
// In your data model
@Serializable
data class AppSettings(
    // Existing MA settings
    val serverUrl: String = "",
    val token: String? = null,

    // New Sendspin settings
    val sendspinEnabled: Boolean = false,
    val sendspinPort: Int = 8927,
    val sendspinPath: String = "/sendspin",
    val sendspinDeviceName: String = "My Phone",
    val sendspinClientId: String = Uuid.random().toString()
)

class SettingsRepository {
    val settings: StateFlow<AppSettings>

    fun buildSendspinConfig(): SendspinConfig {
        val currentSettings = settings.value
        val serverHost = extractHost(currentSettings.serverUrl) // e.g., "192.168.1.100"

        return SendspinConfig(
            clientId = currentSettings.sendspinClientId,
            deviceName = currentSettings.sendspinDeviceName,
            enabled = currentSettings.sendspinEnabled,
            serverHost = serverHost,
            serverPort = currentSettings.sendspinPort,
            serverPath = currentSettings.sendspinPath
        )
    }

    private fun extractHost(url: String): String {
        // Parse "http://192.168.1.100:8123" -> "192.168.1.100"
        return try {
            val uri = URI(url)
            uri.host ?: ""
        } catch (e: Exception) {
            ""
        }
    }
}
```

---

## Step 2: Initialize SendspinClient in MainViewModel

```kotlin
class MainViewModel(
    private val serviceClient: ServiceClient,
    private val settings: SettingsRepository,
    private val platformContext: PlatformContext
) : ViewModel() {

    private var sendspinClient: SendspinClient? = null

    // Expose Sendspin state to UI
    val sendspinConnectionState = MutableStateFlow<SendspinConnectionState>(SendspinConnectionState.Idle)
    val sendspinPlaybackState = MutableStateFlow<SendspinPlaybackState>(SendspinPlaybackState.Idle)
    val sendspinMetadata = MutableStateFlow<StreamMetadataPayload?>(null)

    init {
        // Monitor settings changes
        viewModelScope.launch {
            settings.settings.collect { appSettings ->
                if (appSettings.sendspinEnabled) {
                    startSendspinClient()
                } else {
                    stopSendspinClient()
                }
            }
        }
    }

    private suspend fun startSendspinClient() {
        logger.i { "Starting Sendspin client" }

        // Stop existing client
        sendspinClient?.stop()
        sendspinClient?.close()

        // Build config from settings
        val config = settings.buildSendspinConfig()

        if (!config.isValid) {
            logger.w { "Sendspin config invalid" }
            return
        }

        // Create MediaPlayerController
        val mediaPlayer = MediaPlayerController(platformContext)

        // Create and start Sendspin client
        val client = SendspinClient(config, mediaPlayer)
        sendspinClient = client

        // Collect state updates
        viewModelScope.launch {
            client.connectionState.collect { sendspinConnectionState.value = it }
        }
        viewModelScope.launch {
            client.playbackState.collect { sendspinPlaybackState.value = it }
        }
        viewModelScope.launch {
            client.metadata.collect { sendspinMetadata.value = it }
        }

        // Start connection
        client.start()
    }

    private suspend fun stopSendspinClient() {
        logger.i { "Stopping Sendspin client" }
        sendspinClient?.stop()
        sendspinClient?.close()
        sendspinClient = null

        sendspinConnectionState.value = SendspinConnectionState.Idle
        sendspinPlaybackState.value = SendspinPlaybackState.Idle
        sendspinMetadata.value = null
    }

    override fun onCleared() {
        super.onCleared()
        runBlocking {
            stopSendspinClient()
        }
    }
}
```

---

## Step 3: Update Settings UI

Add Sendspin settings section to your settings screen:

```kotlin
@Composable
fun SettingsScreen(
    settings: AppSettings,
    onUpdateSettings: (AppSettings) -> Unit
) {
    Column {
        // Existing MA server settings
        OutlinedTextField(
            value = settings.serverUrl,
            onValueChange = { onUpdateSettings(settings.copy(serverUrl = it)) },
            label = { Text("Music Assistant Server URL") }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Sendspin settings section
        Text("Sendspin Player", style = MaterialTheme.typography.titleMedium)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Enable Sendspin Player")
            Switch(
                checked = settings.sendspinEnabled,
                onCheckedChange = { onUpdateSettings(settings.copy(sendspinEnabled = it)) }
            )
        }

        if (settings.sendspinEnabled) {
            OutlinedTextField(
                value = settings.sendspinDeviceName,
                onValueChange = { onUpdateSettings(settings.copy(sendspinDeviceName = it)) },
                label = { Text("Device Name") },
                placeholder = { Text("My Phone") }
            )

            OutlinedTextField(
                value = settings.sendspinPort.toString(),
                onValueChange = {
                    it.toIntOrNull()?.let { port ->
                        onUpdateSettings(settings.copy(sendspinPort = port))
                    }
                },
                label = { Text("Sendspin Port") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            OutlinedTextField(
                value = settings.sendspinPath,
                onValueChange = { onUpdateSettings(settings.copy(sendspinPath = it)) },
                label = { Text("Sendspin Path") },
                placeholder = { Text("/sendspin") }
            )

            Text(
                text = "Server: ${extractHost(settings.serverUrl)}:${settings.sendspinPort}${settings.sendspinPath}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
```

---

## Step 4: Display Sendspin Status in Player UI

```kotlin
@Composable
fun PlayerControls(
    viewModel: MainViewModel
) {
    val sendspinState by viewModel.sendspinConnectionState.collectAsState()
    val sendspinMetadata by viewModel.sendspinMetadata.collectAsState()
    val sendspinPlayback by viewModel.sendspinPlaybackState.collectAsState()

    Column {
        // Sendspin connection indicator
        when (val state = sendspinState) {
            is SendspinConnectionState.Connected -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = "Connected",
                        tint = Color.Green
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sendspin: ${state.serverName}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            is SendspinConnectionState.Error -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "Error",
                        tint = Color.Red
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sendspin Error",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Red
                    )
                }
            }
            SendspinConnectionState.Idle -> {
                // Not connected - don't show anything
            }
            SendspinConnectionState.Advertising -> {
                // Shouldn't happen in this mode
            }
        }

        // Show playback state
        when (sendspinPlayback) {
            is SendspinPlaybackState.Synchronized -> {
                Text(
                    text = "♪ Playing via Sendspin",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(8.dp)
                )
            }
            is SendspinPlaybackState.Buffering -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp).padding(8.dp)
                )
            }
            else -> {}
        }

        // Show metadata if available
        sendspinMetadata?.let { metadata ->
            Column(modifier = Modifier.padding(8.dp)) {
                metadata.title?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
                metadata.artist?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                metadata.album?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}
```

---

## Step 5: Android Initialization

In your Android Application class, initialize the MdnsAdvertiser (optional - only if you want mDNS in the future):

```kotlin
class MusicAssistantApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Optional: Initialize mDNS if you want it for future features
        // MdnsAdvertiser.initialize(this)
    }
}
```

---

## How It Works

1. **User opens app** → Settings screen shows Sendspin toggle (disabled by default)

2. **User enables Sendspin** →
   - MainViewModel creates `SendspinClient` with MA server IP from existing settings
   - `SendspinClient.start()` connects to `ws://{ma-server-ip}:8927/sendspin`

3. **Connection established** →
   - Client sends `client/hello` with device capabilities
   - Server responds with `server/hello`
   - Clock synchronization starts (1s intervals)
   - Connection state updates to `Connected`

4. **Music starts playing** →
   - Server sends `stream/start` with PCM format
   - Binary audio chunks arrive via WebSocket
   - AudioStreamManager buffers and decodes chunks
   - AudioTrack plays synchronized audio
   - Playback state updates to `Synchronized`

5. **UI shows** →
   - ✅ "Sendspin: Music Assistant" (connection indicator)
   - ♪ "Playing via Sendspin" (playback state)
   - Track metadata (title, artist, album)

6. **User disables Sendspin** →
   - `SendspinClient.stop()` sends goodbye and disconnects
   - States reset to `Idle`

---

## Configuration Options

### Default Values

```kotlin
SendspinConfig(
    clientId = "auto-generated-uuid",
    deviceName = "My Phone",
    enabled = false,
    bufferCapacityMicros = 500_000, // 500ms buffer
    serverHost = "192.168.1.100",  // From MA server URL
    serverPort = 8927,              // Music Assistant Sendspin port
    serverPath = "/sendspin"        // WebSocket endpoint
)
```

### Settings Screen Fields

| Setting | Description | Default       |
|---------|-------------|---------------|
| **Enable Sendspin** | Toggle on/off | `false`       |
| **Device Name** | How device appears in MA | `"My Phone"`  |
| **Port** | Sendspin server port | `8927`        |
| **Path** | WebSocket endpoint path | `"/sendspin"` |

**Note:** Server IP is automatically extracted from the existing MA server URL setting.

---

## Troubleshooting

### Connection fails

**Check:**
1. Music Assistant server has Sendspin enabled
2. Sendspin port (8927) is accessible
3. Server IP is correct (same as MA connection)
4. Firewall allows WebSocket connections

**Logs to check:**
```
SendspinClient: Connecting to Sendspin server: ws://192.168.1.100:8927/sendspin
WebSocketHandler: Connected to ws://192.168.1.100:8927/sendspin
MessageDispatcher: Received server/hello from Music Assistant
```

### Audio doesn't play

**Check:**
1. Connection state is `Connected`
2. Playback state reaches `Synchronized`
3. Clock sync quality is `GOOD`
4. Buffer is filling (check `bufferState`)

**Logs to check:**
```
AudioStreamManager: Starting stream: pcm, 48000Hz, 2ch, 16bit
MediaPlayerController: Preparing raw PCM stream: 48000Hz, 2ch, 16bit
AudioStreamManager: Prebuffer complete, starting playback
```

### Audio is choppy

**Possible causes:**
1. Network latency too high
2. Buffer too small (increase `bufferCapacityMicros`)
3. Clock sync quality degraded
4. CPU overload (check for dropped chunks)

**Solutions:**
- Increase buffer: `bufferCapacityMicros = 1_000_000` (1 second)
- Check dropped chunks: `bufferState.droppedChunks`
- Monitor sync quality: `ClockSynchronizer.currentQuality`

---

## Advanced: Manual Connection Control

If you want manual control instead of automatic connection:

```kotlin
// Disable auto-connect in init
val sendspinClient = SendspinClient(config, mediaPlayer)

// Manually connect
viewModelScope.launch {
    sendspinClient.start() // Connects to server from config
}

// Manually disconnect
viewModelScope.launch {
    sendspinClient.stop()
}
```

---

## Next Steps

1. **Test with Music Assistant server** - Verify connection and playback
2. **Add FLAC/OPUS support** - Implement decoders for better compression
3. **Add volume/mute UI** - Control playback from app
4. **Add buffer statistics** - Display buffer health in debug UI
5. **Add reconnection logic** - Auto-reconnect on network changes

---

## Protocol Support Status

| Feature | Status | Notes |
|---------|--------|-------|
| WebSocket connection | ✅ Working | Ktor client |
| Protocol handshake | ✅ Working | client/hello ↔ server/hello |
| Clock synchronization | ✅ Working | NTP-style with Kalman filter, **FIXED: monotonic time** |
| PCM audio streaming | ✅ Working | AudioTrack playback |
| OPUS codec | ✅ Working | **Android only (Concentus library) - 2026-01-05** |
| Adaptive buffering | ✅ Working | **Network-aware dynamic buffer sizing - 2026-01-05** |
| Timestamp buffering | ✅ Working | Priority queue, sync'd playback |
| Metadata display | ✅ Working | Title, artist, album |
| State reporting | ✅ Working | **FIXED: Periodic updates every 2s** |
| Playback control | ✅ Working | Play, pause, seek, next/prev |
| Volume control | ⚠️ Partial | Receives commands, needs UI |
| Mute control | ⚠️ Partial | Receives commands, needs UI |
| Error recovery | ⚠️ Partial | Basic error handling only |
| Auto-reconnect | ❌ TODO | Manual reconnect only |
| FLAC codec | ❌ TODO | Decoder stub exists, not implemented |
| OPUS iOS/Desktop | ❌ TODO | Android implementation complete, iOS/Desktop stubs |
| Artwork display | ❌ TODO | Not implemented |
| Visualizer | ❌ TODO | Not implemented |

---

## Files Modified/Created

### Core Implementation (commonMain)
- `SendspinConfig.kt` - Configuration with server settings
- `SendspinClient.kt` - Main orchestrator (simplified, no mDNS)
- `SendspinStates.kt` - State definitions (extended with adaptive metrics - 2026-01-05)
- `SendspinCapabilities.kt` - Client capability builder (added Opus support - 2026-01-05)
- `WebSocketHandler.kt` - WebSocket connection
- `MessageDispatcher.kt` - Protocol message handling
- `AudioStreamManager.kt` - Audio buffering and playback (adaptive integration - 2026-01-05)
- `AdaptiveBufferManager.kt` - Network-aware buffering (NEW - 2026-01-05)
- `ClockSynchronizer.kt` - Time synchronization
- `TimestampOrderedBuffer.kt` - Priority queue for chunks
- `AudioDecoder.kt` - Decoder interfaces
- `MediaPlayerController.kt` - Extended with raw PCM methods

### Android Implementation
- `MediaPlayerController.android.kt` - AudioTrack integration
- `MdnsAdvertiser.android.kt` - NSD service (optional)
- `FlacDecoder.android.kt` - Placeholder (not implemented)
- `OpusDecoder.android.kt` - **Full implementation with Concentus (2026-01-05)**

### Gradle Dependencies (added 2026-01-05)
- `gradle/libs.versions.toml` - Concentus library v1.0.2
- `composeApp/build.gradle.kts` - Concentus dependency for Android

### iOS/Desktop Stubs
- Platform-specific stubs for future implementation

---

## Summary

The Sendspin integration is **production-ready for Android** with PCM and Opus codec support:

### ✅ Working
- Simple configuration - Reuses MA server IP, just add port/path
- Automatic connection - Connects when enabled in settings
- Full protocol support - Handshake, clock sync, streaming
- **Opus codec - 90%+ bandwidth savings over PCM (Android)**
- **Adaptive buffering - Network-aware dynamic buffer sizing**
- Low-latency playback - AudioTrack with synchronized timing
- Rich UI integration - Connection status, metadata, playback state
- Playback controls - Play, pause, seek, next/previous
- State reporting - Periodic updates keep server in sync

### 🎯 Recent Additions (2026-01-05)
- **Opus Decoder** - Concentus library, 48kHz stereo/mono, Android only
- **Adaptive Buffering** - WebRTC NetEQ-inspired, RTT/jitter tracking, dynamic thresholds
- **Buffer Metrics** - Extended BufferState with network statistics

### ⚠️ Known Issues
- No volume/mute UI controls (receives commands but can't send)
- No auto-reconnect on network failures
- Opus only on Android (iOS/Desktop need implementation)
- FLAC codec not implemented

### 📋 Next Steps
1. Add volume/mute UI controls
2. Implement auto-reconnect
3. Add comprehensive error handling
4. Implement Opus for iOS/Desktop
5. Implement FLAC codec

---

**See also:**
- `sendspin-integration-design.md` - Full technical design
- `sendspin-status.md` - Current implementation status and known issues
