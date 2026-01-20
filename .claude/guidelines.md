# Development Guidelines

## Code Location Rules

| What | Where |
|------|-------|
| Shared logic | `commonMain/` |
| Android-only | `androidMain/` |
| iOS-only | `iosMain/` |
| Reusable composables | `ui/common/composables/` |
| Feature composables | `ui/{feature}/composables/` |
| ViewModels | Same package as screen |

## Kotlin Conventions

```kotlin
// Prefer StateFlow over LiveData
val state: StateFlow<State> = _state.asStateFlow()

// Use sealed interfaces for navigation/polymorphism
sealed interface Result<out T> {
    data class Success<T>(val data: T) : Result<T>
    data class Error(val message: String) : Result<Nothing>
}

// Minimize expect/actual - keep code in commonMain
```

## Compose Conventions

```kotlin
// Inject ViewModels in composables
@Composable
fun Screen(viewModel: ScreenViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
}

// Use remember for expensive computations
val derived = remember(input) { expensiveCalculation(input) }

// Material3 components only
Button(onClick = {}) { Text("Click") }  // ✓
// Not Material2 equivalents
```

## File Organization

- One primary composable per file
- Split by meaning, not by size
- Name file after main composable: `PlayerControls.kt` → `@Composable fun PlayerControls()`
- No previews unless explicitly requested

## Logging

```kotlin
import co.touchlab.kermit.Logger

Logger.withTag("ServiceClient").d { "Connected to $host" }
Logger.withTag("PlayerVM").e(exception) { "Playback failed" }
```

## State Wrappers

```kotlin
sealed interface DataState<out T> {
    data object Loading : DataState<Nothing>
    data class Data<T>(val value: T) : DataState<T>
    data class Error(val message: String) : DataState<Nothing>
    data object NoData : DataState<Nothing>
}
```

## Maintenance Rules

- Update `.claude/dependencies.md` when adding libraries
- Update `.claude/architecture.md` when changing patterns
- Keep `commonMain` as the default location

## Server Connection

Music Assistant server required. Configure:
- Host (IP/hostname)
- Port
- TLS (on/off)
- Auth: login/pass, OAuth, or long-lived token

## Testing Platforms

**Android Auto:**
1. Enable developer mode in Android Auto app
2. Enable "Unknown sources"
3. VPN config: exclude Android Auto from VPN