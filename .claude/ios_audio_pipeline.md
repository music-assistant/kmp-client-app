# iOS Audio Pipeline & MPV Integration

## Overview
High-Performance Audio Pipeline for iOS using `Libmpv` (via `MPVKit`) for FLAC/Opus/PCM streaming playback via the Sendspin protocol.

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         Server                                   │
│  Sends FLAC/Opus/PCM chunks + codec_header via WebSocket        │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                   AudioStreamManager (Kotlin)                    │
│  • Receives chunks with timestamps                               │
│  • Buffers/reorders (TimestampOrderedBuffer, AdaptiveBuffer)    │
│  • PassthroughDecoder for iOS (no decoding - MPV does it)       │
│  • Calls writeRawPcm(data) to push encoded bytes                │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│            MediaPlayerController.ios.kt → PlatformPlayerProvider │
│  Delegates to Swift via PlatformAudioPlayer interface           │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                  MPVController.swift                             │
│  • prepareStream(codec, sampleRate, channels, bitDepth, header) │
│  • Decodes base64 codecHeader (FLAC STREAMINFO block)           │
│  • Delays loadfile until first data arrives                      │
│  • Writes header + audio to RingBuffer                          │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    RingBuffer.swift                              │
│  • Thread-safe circular buffer (4MB capacity)                    │
│  • Blocking read using NSCondition (MPV requires this)          │
│  • close() method signals EOF to blocked reads                  │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                   Libmpv (C-API via MPVKit)                      │
│  • Custom stream protocol: sendspin://stream                     │
│  • demuxer=lavf for FLAC/Opus (FFmpeg auto-detection)           │
│  • demuxer=rawaudio for PCM                                      │
│  • ao=audiounit for iOS audio output                            │
└─────────────────────────────────────────────────────────────────┘
```

## Key Implementation Details

### Codec Support
| Codec | Demuxer Setting | Notes |
|-------|-----------------|-------|
| FLAC  | `lavf` | Requires codecHeader prepended to stream |
| Opus  | `lavf` | Requires codecHeader prepended to stream |
| PCM   | `rawaudio` | + demuxer-rawaudio-rate/channels/format |

### Critical Implementation Notes

1. **Codec Header Prepending**: The server sends `codec_header` (base64 FLAC/Opus header) separately in `stream/start`. This MUST be decoded and written to the RingBuffer BEFORE any audio data.

2. **Delayed loadfile**: MPV's `loadfile` must be called AFTER data is in the buffer, otherwise the demuxer times out and closes the stream.

3. **Blocking RingBuffer Read**: MPV's stream callback expects blocking reads. The `RingBuffer.read()` uses `NSCondition` to wait for data.

4. **Demuxer Configuration**: Do NOT use `demuxer=auto` (invalid). Use `lavf` (FFmpeg) or omit for default behavior.

### MPV Configuration Options
```swift
// Audio output for iOS
setOptionString("vid", "no")
setOptionString("ao", "audiounit")

// Streaming optimizations
setOptionString("cache-pause", "no")
setOptionString("demuxer-readahead-secs", "0.5")
setOptionString("audio-buffer", "0.1")

// Verbose logging (for debugging)
setOptionString("terminal", "yes")
setOptionString("msg-level", "all=v")
```

## Files

| File | Purpose |
|------|---------|
| `iosApp/MPVController.swift` | Main MPV integration, stream callbacks |
| `iosApp/RingBuffer.swift` | Thread-safe circular buffer with blocking read |
| `iosApp/iOSApp.swift` | Registers MPVController via PlatformPlayerProvider |
| `composeApp/.../PlatformAudioPlayer.kt` | Kotlin interface for Swift implementation |
| `composeApp/.../MediaPlayerController.ios.kt` | Delegates to PlatformPlayerProvider |
| `composeApp/.../PlatformCodecSupport.kt` | isOpusPlaybackSupported=true, isFlacPlaybackSupported=true |

## Status: ✅ Working (2026-01-14)

- [x] FLAC streaming playback
- [x] Opus streaming playback (expected)
- [x] PCM streaming playback
- [x] Proper codec header handling
- [x] MPV demuxer configuration

## Known Issues

1. **Seek/Scrub Crash**: App crashes with `ArrayIndexOutOfBoundsException` in `ServiceClient.sendRequest` when scrubbing. This is a separate issue in the API layer, not the audio pipeline.

## Future Work

1. **Sample-Accurate Sync**: Current sync is approximate (~50-100ms tolerance). The Kotlin layer schedules chunks at the right time, but once data enters the RingBuffer→MPV pipeline, we lose precise timing control. For sub-10ms sync, consider:
   - Using `AudioUnit` directly instead of MPV for timing control
   - Implementing MPV's `ao_push` timing callbacks
   - Passing timestamps through to the audio output layer
