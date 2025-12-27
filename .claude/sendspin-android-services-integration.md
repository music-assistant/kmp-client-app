# Sendspin Android Services Integration

## Problem

Current Sendspin implementation lacks:
1. **MediaSession** - Required for Android Auto, notifications, lock screen controls
2. **Foreground Service** - Needed for background playback
3. **Lifecycle Management** - WebSocket must survive app backgrounding

## Solution Architecture

```
┌─────────────────────────────────────────────────────────┐
│                  SendspinService                        │
│              (Foreground Service)                       │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  ┌────────────────────┐      ┌──────────────────────┐  │
│  │  SendspinClient    │      │   MediaSession       │  │
│  │  • WebSocket       │─────>│   • Metadata         │  │
│  │  • Audio streaming │      │   • Playback state   │  │
│  │  • State mgmt      │      │   • Callbacks        │  │
│  └────────────────────┘      └──────────────────────┘  │
│           │                            │                │
│           │                            ▼                │
│           │                  ┌──────────────────────┐  │
│           │                  │   Notification       │  │
│           │                  │   • Now Playing      │  │
│           │                  │   • Controls         │  │
│           │                  └──────────────────────┘  │
│           ▼                            │                │
│  ┌────────────────────┐               │                │
│  │  AudioTrack        │               ▼                │
│  │  • PCM playback    │      ┌──────────────────────┐  │
│  └────────────────────┘      │   Android Auto       │  │
│                               │   • Car display      │  │
│                               └──────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

## Implementation

### Step 1: Create SendspinService

```kotlin
package io.music_assistant.client.player.sendspin

import android.app.*
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import co.touchlab.kermit.Logger
import io.music_assistant.client.player.MediaPlayerController
import io.music_assistant.client.player.PlatformContext
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class SendspinService : LifecycleService() {

    private val logger = Logger.withTag("SendspinService")
    private val binder = SendspinBinder()

    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var sendspinClient: SendspinClient
    private lateinit var mediaPlayerController: MediaPlayerController

    private var config: SendspinConfig? = null

    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "sendspin_playback"

        const val ACTION_START = "io.music_assistant.sendspin.START"
        const val ACTION_STOP = "io.music_assistant.sendspin.STOP"
        const val EXTRA_CONFIG = "config"
    }

    inner class SendspinBinder : Binder() {
        fun getService(): SendspinService = this@SendspinService
    }

    override fun onCreate() {
        super.onCreate()
        logger.i { "SendspinService onCreate" }

        createNotificationChannel()
        initMediaSession()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        when (intent?.action) {
            ACTION_START -> {
                val config = intent.getParcelableExtra<SendspinConfig>(EXTRA_CONFIG)
                if (config != null) {
                    startSendspin(config)
                }
            }
            ACTION_STOP -> {
                stopSendspin()
            }
        }

        return START_STICKY
    }

    private fun startSendspin(newConfig: SendspinConfig) {
        logger.i { "Starting Sendspin with config: $newConfig" }

        config = newConfig

        // Start foreground
        val notification = createNotification("Connecting to ${newConfig.serverHost}...")
        startForeground(NOTIFICATION_ID, notification)

        // Initialize MediaPlayerController
        mediaPlayerController = MediaPlayerController(PlatformContext(applicationContext))

        // Initialize SendspinClient
        sendspinClient = SendspinClient(newConfig, mediaPlayerController)

        // Monitor states and update MediaSession
        lifecycleScope.launch {
            sendspinClient.connectionState.collect { state ->
                handleConnectionState(state)
            }
        }

        lifecycleScope.launch {
            sendspinClient.playbackState.collect { state ->
                handlePlaybackState(state)
            }
        }

        lifecycleScope.launch {
            sendspinClient.metadata.collect { metadata ->
                handleMetadata(metadata)
            }
        }

        // Start connection
        lifecycleScope.launch {
            sendspinClient.start()
        }
    }

    private fun stopSendspin() {
        logger.i { "Stopping Sendspin" }

        lifecycleScope.launch {
            sendspinClient.stop()
            sendspinClient.close()
        }

        mediaSession.isActive = false
        stopForeground(true)
        stopSelf()
    }

    private fun initMediaSession() {
        mediaSession = MediaSessionCompat(this, "SendspinSession").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    // Sendspin is server-controlled, can't initiate play
                    logger.w { "Play requested but Sendspin is server-controlled" }
                }

                override fun onPause() {
                    // Sendspin is server-controlled, can't initiate pause
                    logger.w { "Pause requested but Sendspin is server-controlled" }
                }

                override fun onStop() {
                    stopSendspin()
                }

                override fun onSetRating(rating: RatingCompat?) {
                    // Could implement volume control here
                }
            })

            isActive = true
        }
    }

    private fun handleConnectionState(state: SendspinConnectionState) {
        when (state) {
            is SendspinConnectionState.Connected -> {
                logger.i { "Connected to ${state.serverName}" }
                updateNotification("Connected to ${state.serverName}")

                // Update MediaSession
                val playbackState = PlaybackStateCompat.Builder()
                    .setState(PlaybackStateCompat.STATE_CONNECTING, 0L, 1.0f)
                    .build()
                mediaSession.setPlaybackState(playbackState)
            }
            is SendspinConnectionState.Error -> {
                logger.e { "Connection error: ${state.error.message}" }
                updateNotification("Connection Error", isError = true)
            }
            SendspinConnectionState.Idle -> {
                updateNotification("Disconnected")
            }
            else -> {}
        }
    }

    private fun handlePlaybackState(state: SendspinPlaybackState) {
        val stateCode = when (state) {
            is SendspinPlaybackState.Synchronized -> PlaybackStateCompat.STATE_PLAYING
            is SendspinPlaybackState.Buffering -> PlaybackStateCompat.STATE_BUFFERING
            is SendspinPlaybackState.Playing -> PlaybackStateCompat.STATE_PLAYING
            SendspinPlaybackState.Idle -> PlaybackStateCompat.STATE_STOPPED
            is SendspinPlaybackState.Error -> PlaybackStateCompat.STATE_ERROR
        }

        val playbackState = PlaybackStateCompat.Builder()
            .setState(stateCode, 0L, 1.0f)
            .build()
        mediaSession.setPlaybackState(playbackState)

        // Update notification based on state
        if (state is SendspinPlaybackState.Synchronized) {
            updateNotification("Playing")
        }
    }

    private fun handleMetadata(metadata: StreamMetadataPayload?) {
        if (metadata == null) return

        logger.d { "Metadata updated: ${metadata.title}" }

        val mediaMetadata = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, metadata.title ?: "Unknown")
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, metadata.artist ?: "Unknown")
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, metadata.album ?: "Unknown")
            // TODO: Load artwork from URL
            // .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bitmap)
            .build()

        mediaSession.setMetadata(mediaMetadata)

        updateNotification(
            "${metadata.title ?: "Unknown"} - ${metadata.artist ?: "Unknown"}"
        )
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Sendspin Playback",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows now playing information"
            setShowBadge(false)
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(contentText: String, isError: Boolean = false): Notification {
        val stopIntent = Intent(this, SendspinService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_music_note) // TODO: Add icon
            .setContentTitle("Sendspin Player")
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(!isError)
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(mediaSession.sessionToken)
                .setShowActionsInCompactView(0))
            .addAction(
                R.drawable.ic_stop,
                "Stop",
                stopPendingIntent
            )
            .build()
    }

    private fun updateNotification(contentText: String, isError: Boolean = false) {
        val notification = createNotification(contentText, isError)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    override fun onDestroy() {
        logger.i { "SendspinService onDestroy" }
        lifecycleScope.launch {
            sendspinClient.stop()
            sendspinClient.close()
        }
        mediaSession.release()
        super.onDestroy()
    }

    // Public API for ViewModel
    fun getSendspinClient(): SendspinClient = sendspinClient
}
```

### Step 2: Update AndroidManifest.xml

```xml
<manifest>
    <application>
        <!-- Sendspin Service -->
        <service
            android:name=".player.sendspin.SendspinService"
            android:enabled="true"
            android:exported="false"
            android:foregroundServiceType="mediaPlayback">
            <intent-filter>
                <action android:name="android.media.browse.MediaBrowserService" />
            </intent-filter>
        </service>
    </application>

    <!-- Permissions -->
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
</manifest>
```

### Step 3: Make SendspinConfig Parcelable

```kotlin
@OptIn(ExperimentalUuidApi::class)
@Parcelize
data class SendspinConfig(
    val clientId: String = Uuid.random().toString(),
    val deviceName: String,
    val enabled: Boolean = true,
    val bufferCapacityMicros: Int = 500_000,
    val serverHost: String = "",
    val serverPort: Int = 8927,
    val serverPath: String = "/sendspin"
) : Parcelable {
    // ... existing code
}
```

### Step 4: Update MainViewModel to Use Service

```kotlin
class MainViewModel(
    private val context: Context,
    private val settings: SettingsRepository
) : ViewModel() {

    private var serviceBound = false
    private var sendspinService: SendspinService? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val service = (binder as? SendspinService.SendspinBinder)?.getService()
            sendspinService = service
            serviceBound = true

            // Collect states from service's SendspinClient
            service?.getSendspinClient()?.let { client ->
                viewModelScope.launch {
                    client.connectionState.collect { /* update UI */ }
                }
                viewModelScope.launch {
                    client.playbackState.collect { /* update UI */ }
                }
                viewModelScope.launch {
                    client.metadata.collect { /* update UI */ }
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            sendspinService = null
            serviceBound = false
        }
    }

    fun startSendspin(config: SendspinConfig) {
        val intent = Intent(context, SendspinService::class.java).apply {
            action = SendspinService.ACTION_START
            putExtra(SendspinService.EXTRA_CONFIG, config)
        }

        // Start foreground service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }

        // Bind to service
        context.bindService(
            Intent(context, SendspinService::class.java),
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    fun stopSendspin() {
        val intent = Intent(context, SendspinService::class.java).apply {
            action = SendspinService.ACTION_STOP
        }
        context.startService(intent)

        if (serviceBound) {
            context.unbindService(serviceConnection)
            serviceBound = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (serviceBound) {
            context.unbindService(serviceConnection)
        }
    }
}
```

---

## Android Auto Integration

With MediaSession in place, Android Auto works automatically:

1. **Car detects MediaSession** → Shows app in Android Auto
2. **Now Playing displays** → Title, artist, album from Sendspin metadata
3. **Controls show** → Limited to Stop (since Sendspin is server-controlled)
4. **Audio routes to car** → AudioTrack output goes to car speakers

**Limitations:**
- Play/Pause buttons won't work (Sendspin is server-controlled)
- Seek won't work (no seek support in protocol)
- Only Stop and volume work

---

## Background Playback

With the Foreground Service:

✅ **WebSocket stays alive** when app is backgrounded
✅ **Audio continues playing** via AudioTrack
✅ **Notification shows** with now playing info
✅ **Lock screen controls** work via MediaSession
✅ **Service survives** app being killed (START_STICKY)

---

## Summary of Changes Needed

1. **Create SendspinService** (foreground service with MediaSession)
2. **Make SendspinConfig Parcelable** (to pass to service)
3. **Update MainViewModel** (start/bind service instead of direct client)
4. **Add AndroidManifest entries** (service declaration + permissions)
5. **Add notification icon** (res/drawable/ic_music_note.xml)

This makes Sendspin work with:
- ✅ Android Auto
- ✅ Background playback
- ✅ Lock screen controls
- ✅ Notification controls
- ✅ System media controls

Would you like me to implement any of these files?

---

## Android Auto Integration (Actual Implementation)

The AndroidAutoPlaybackService integrates with Sendspin using the same architectural pattern as MainMediaPlaybackService:

### Architecture

- **Sendspin Management**: Handled by MainDataSource singleton (not by AndroidAutoPlaybackService directly)
- **Player Access**: Via `dataSource.playersData` StateFlow
- **Player Filter**: Shows first player with active playback (`queueInfo?.currentItem != null`)
- **Queue Access**: Uses `playerData.queue` instead of deprecated `builtinPlayerQueue`
- **Actions**: All go through `dataSource.playerAction()` and `dataSource.queueAction()` (server API calls)

### Key Implementation Details

1. **Player Selection** (AndroidAutoPlaybackService.kt:47-51)
   ```kotlin
   private val currentPlayerData =
       dataSource.playersData.map { players ->
           // Show first player with active playback (Sendspin when playing locally)
           players.firstOrNull { it.queueInfo?.currentItem != null }
       }.stateIn(scope, SharingStarted.Eagerly, null)
   ```
   - Filters for any player with current playback
   - When Sendspin is playing locally, it will be shown
   - Follows MainMediaPlaybackService pattern exactly

2. **Queue Handling** (AndroidAutoPlaybackService.kt:84-104)
   ```kotlin
   scope.launch {
       currentPlayerData.filterNotNull().collect { playerData ->
           when (val queueData = playerData.queue) {
               is DataState.Data -> {
                   when (val queueItems = queueData.data.items) {
                       is DataState.Data -> {
                           val baseUrl = (dataSource.apiClient.sessionState.value as? SessionState.Connected)?.serverInfo?.baseUrl
                           mediaSessionHelper.updateQueue(queueItems.data.map { queueTrack ->
                               QueueItem(
                                   queueTrack.track.toMediaDescription(baseUrl, defaultIconUri),
                                   queueTrack.track.longId
                               )
                           })
                       }
                       else -> mediaSessionHelper.updateQueue(emptyList())
                   }
               }
               else -> mediaSessionHelper.updateQueue(emptyList())
           }
       }
   }
   ```
   - Accesses queue from `playerData.queue.items`
   - Handles DataState properly (Data, Error, NoData)
   - Updates MediaSession queue for Android Auto display

3. **No Direct Sendspin Integration**
   - AndroidAutoPlaybackService does NOT create/manage SendspinClient
   - Sendspin lifecycle managed by MainDataSource
   - Service simply displays active player's state via playersData

### Android Auto Behavior

With Sendspin enabled and playing:
- ✅ Shows current track in Android Auto
- ✅ Displays queue for browsing
- ✅ Play/pause/next/previous controls work
- ✅ Seek and volume controls work
- ✅ Audio plays through car speakers (local playback via Sendspin)
- ✅ Metadata and artwork display correctly

Without active playback:
- Service shows no player (currentPlayerData = null)
- Android Auto shows empty/no media state

### Removed Deprecated Code

- **Removed**: `isBuiltin` player filter (builtin players are deprecated)
- **Removed**: `builtinPlayerQueue` usage (replaced with `playerData.queue`)
- **Updated**: All queue access now uses DataState pattern

### Differences from Design Document

The design document (earlier in this file) proposed creating a separate SendspinService. This was NOT implemented. Instead:

**What we actually did**: Follow MainMediaPlaybackService pattern
- Sendspin managed centrally by MainDataSource
- Services access via playersData StateFlow
- No service-specific Sendspin management

**Why this is better**:
- Single source of truth (MainDataSource)
- No lifecycle synchronization issues
- Simpler architecture
- Consistent with rest of app
- Works with any player, not just Sendspin
