# Project Structure

## Source Sets

```
composeApp/src/
├── commonMain/     # 95%+ of code lives here
├── androidMain/    # ExoPlayer, MediaService, Android Auto
├── iosMain/        # AVPlayer, CarPlay
├── desktopMain/    # VLC player
└── appleMain/      # Shared Apple code (iOS + macOS)
```

## Package Organization

```
commonMain/kotlin/
├── api/              # WebSocket client, API interfaces
├── data/
│   ├── model/
│   │   ├── server/   # Server DTOs (raw API responses)
│   │   └── client/   # Domain models (Player, Queue, AppMediaItem)
│   └── repository/   # Data sources, repositories
├── di/               # Koin modules
├── ui/
│   ├── common/
│   │   └── composables/  # Reusable UI components
│   ├── home/             # Home screen + HomeViewModel
│   │   └── composables/  # Home-specific components
│   ├── library/          # Library browser
│   ├── player/           # Player controls
│   ├── queue/            # Queue management
│   └── settings/         # Settings screen
├── utils/            # Navigation, extensions, helpers
└── theme/            # Material3 theme, colors
```

## Feature Module Pattern

```
ui/{feature}/
├── {Feature}Screen.kt      # Main composable
├── {Feature}ViewModel.kt   # State + logic
├── {Feature}State.kt       # State data class (if complex)
└── composables/            # Feature-specific components
    ├── {Component}A.kt
    └── {Component}B.kt
```

## Platform-Specific Code

| Feature | Android | iOS | Desktop |
|---------|---------|-----|---------|
| Local Player | ExoPlayer (Media3) | AVPlayer | VLCJ |
| Background Playback | MediaService | - | - |
| Car Integration | Android Auto | CarPlay | - |
| Settings Storage | SharedPreferences | NSUserDefaults | Properties |

## Key Files

- `api/ServiceClient.kt` - WebSocket connection to MA server
- `data/repository/MainDataSource.kt` - Central state management
- `di/SharedModule.kt` - Common DI definitions
- `utils/Navigation.kt` - Navigation destinations