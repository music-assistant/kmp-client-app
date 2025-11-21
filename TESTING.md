# Testing Guide

## Running Tests

### All Tests (Desktop + Android, Skip iOS)
```bash
./test-no-ios.sh
```

Or manually:
```bash
./gradlew :composeApp:desktopTest :composeApp:testDebugUnitTest :composeApp:testReleaseUnitTest
```

This runs:
- Desktop tests (JVM)
- Android Debug unit tests
- Android Release unit tests

### Individual Test Suites

**Desktop Tests:**
```bash
./gradlew :composeApp:desktopTest
```

**Android Tests:**
```bash
./gradlew :composeApp:testDebugUnitTest
./gradlew :composeApp:testReleaseUnitTest
```

**All Tests (including iOS - requires Xcode):**
```bash
./gradlew check
```

## Current Test Coverage

### ✅ Completed
- **ServiceClient API Layer** (16 tests)
  - Connection lifecycle management
  - SessionState transitions
  - Request/response correlation
  - WebSocket message handling
  - Error scenarios

- **Utility Functions** (5 tests)
  - Host validation (IPv4 and hostnames)
  - Port validation
  - Time formatting

**Total: 21 tests passing**

### 🔄 Pending (See missing-tests.md)
- Data Model Serialization Tests
- Media Player Controller Tests
- MainDataSource State Management Tests
- ViewModel Tests
- Android Service Integration Tests
- Request Builder Tests
- Navigation Tests

## Test Infrastructure

### Testing Libraries
- `kotlin-test` - Kotlin multiplatform testing framework
- `kotlinx-coroutines-test` (1.10.2) - Coroutine testing utilities
- `turbine` (1.2.0) - Flow testing utilities

### Test Structure
```
composeApp/src/
├── commonTest/       # Cross-platform tests
│   ├── kotlin/
│   │   └── io/music_assistant/client/
│   │       ├── api/ServiceClientTest.kt
│   │       └── utils/ExtTest.kt
├── androidUnitTest/ # Android-specific tests
├── desktopTest/     # Desktop-specific tests
└── iosTest/         # iOS-specific tests
```

## Known Issues

### iOS Tests
iOS tests require full Xcode installation and proper SDK configuration. If you encounter:
```
xcrun: error: SDK "iphonesimulator" cannot be located
```

**Fix:**
```bash
sudo xcode-select --switch /Applications/Xcode.app/Contents/Developer
```

**Or use the checkWithoutIos task instead of check.**

## Writing New Tests

### Example Test Structure
```kotlin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class MyFeatureTest {
    @Test
    fun testMyFeature() = runTest {
        // Arrange
        val input = "test"

        // Act
        val result = myFunction(input)

        // Assert
        assertEquals("expected", result)
    }
}
```

### Testing Flows
```kotlin
import app.cash.turbine.test

@Test
fun testFlowEmissions() = runTest {
    myFlow.test {
        assertEquals(expectedValue1, awaitItem())
        assertEquals(expectedValue2, awaitItem())
        awaitComplete()
    }
}
```

## CI/CD Integration

For CI/CD pipelines, use:
```bash
./test-no-ios.sh
```

Or if you need Gradle-specific flags:
```bash
./gradlew :composeApp:desktopTest :composeApp:testDebugUnitTest :composeApp:testReleaseUnitTest --no-daemon --stacktrace
```

This ensures tests run without iOS dependencies and provides detailed error output.
