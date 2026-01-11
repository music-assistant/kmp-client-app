# Volume Control

## Implementation

**MediaSessionHelper** uses local playback mode with system volume integration:

```kotlin
// Local playback (not remote)
mediaSession.setPlaybackToLocal(AudioManager.STREAM_MUSIC)

// Monitor system volume changes via ContentObserver
// - Tracks lastSystemVolume to detect real changes only
// - Converts system volume to 0-100% for server
// - updatingFromServer flag prevents circular updates

// Server volume updates
fun updateVolumeFromServer(volume: Int) {
    // Only updates if system volume would actually change
    // Prevents rounding errors that cause volume drift
}
```

**MainActivity** binds hardware buttons:
```kotlin
volumeControlStream = AudioManager.STREAM_MUSIC
```

**Services** must cleanup:
```kotlin
override fun onDestroy() {
    mediaSessionHelper.release() // Unregister ContentObserver
}
```

## Result
- Single system media volume slider (no remote slider)
- Bidirectional sync: device ↔ server
- No volume drift or rounding errors
- No circular update loops
