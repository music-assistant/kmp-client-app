# Music Assistant KMP Client

Cross-platform music player client for [Music Assistant Server](https://github.com/music-assistant/server).  
Built with Kotlin Multiplatform + Compose Multiplatform for Android, iOS, and Desktop.

## Quick Commands

```bash
# Android
./gradlew :composeApp:assembleDebug
./gradlew :composeApp:installDebug

# Desktop
./gradlew :composeApp:run
./gradlew :composeApp:packageDistributionForCurrentOS

# iOS - open in Xcode
open iosApp/iosApp.xcodeproj

# Tests
./gradlew :composeApp:testDebugUnitTest
./gradlew :composeApp:desktopTest
```

## Features

- Queue management and playback control for Music Assistant players
- Library browsing (Artists, Albums, Tracks, Playlists)
- Authentication: login/pass, OAuth, long-lived access token
- Remote access via WebRTC
- Built-in player with Sendspin protocol support
- Local music playback (platform-specific)
- Android Auto / CarPlay support

## Architecture

@import .claude/architecture.md
@import .claude/project-structure.md
@import .claude/dependencies.md
@import .claude/guidelines.md