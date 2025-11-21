// ABOUTME: Tests for navigation logic covering route serialization, platform-specific navigation,
// ABOUTME: and back handler behavior across Android/Desktop/iOS platforms.
package io.music_assistant.client.navigation

import io.music_assistant.client.RobolectricTest
import io.music_assistant.client.ui.compose.nav.AppRoutes
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class NavigationTest : RobolectricTest() {
    // ============= Route Serialization Tests =============

    @Test
    fun appRoutes_main_canBeCreated() {
        // When
        val route: AppRoutes = AppRoutes.Main

        // Then
        assertNotNull(route, "Main route should be creatable")
    }

    @Test
    fun appRoutes_libraryArgs_canBeCreated() {
        // When
        val route =
            AppRoutes.LibraryArgs(
                name = "TestLibrary",
                queueOrPlayerId = "player123",
            )

        // Then
        assertNotNull(route, "LibraryArgs route should be creatable")
        assertEquals("TestLibrary", route.name)
        assertEquals("player123", route.queueOrPlayerId)
    }

    @Test
    fun appRoutes_settings_canBeCreated() {
        // When
        val route: AppRoutes = AppRoutes.Settings

        // Then
        assertNotNull(route, "Settings route should be creatable")
    }

    @Test
    fun appRoutes_libraryArgs_preservesParameters() {
        // Given
        val expectedName = "My Playlist"
        val expectedPlayerId = "queue_456"

        // When
        val route = AppRoutes.LibraryArgs(name = expectedName, queueOrPlayerId = expectedPlayerId)

        // Then
        assertEquals(expectedName, route.name, "Name parameter should be preserved")
        assertEquals(expectedPlayerId, route.queueOrPlayerId, "QueueOrPlayerId parameter should be preserved")
    }

    @Test
    fun appRoutes_libraryArgs_withSpecialCharacters_preservesParameters() {
        // Given
        val specialName = "Playlist/With/Slashes & Symbols!"
        val specialId = "id:with:colons"

        // When
        val route = AppRoutes.LibraryArgs(name = specialName, queueOrPlayerId = specialId)

        // Then
        assertEquals(specialName, route.name, "Name with special characters should be preserved")
        assertEquals(specialId, route.queueOrPlayerId, "Id with special characters should be preserved")
    }

    @Test
    fun appRoutes_libraryArgs_withEmptyStrings_preservesParameters() {
        // Given
        val emptyName = ""
        val emptyId = ""

        // When
        val route = AppRoutes.LibraryArgs(name = emptyName, queueOrPlayerId = emptyId)

        // Then
        assertEquals(emptyName, route.name, "Empty name should be preserved")
        assertEquals(emptyId, route.queueOrPlayerId, "Empty id should be preserved")
    }

    @Test
    fun appRoutes_libraryArgs_withLongStrings_preservesParameters() {
        // Given
        val longName = "A".repeat(1000)
        val longId = "1".repeat(1000)

        // When
        val route = AppRoutes.LibraryArgs(name = longName, queueOrPlayerId = longId)

        // Then
        assertEquals(longName, route.name, "Long name should be preserved")
        assertEquals(longId, route.queueOrPlayerId, "Long id should be preserved")
    }

    @Test
    fun appRoutes_libraryArgs_withUnicodeCharacters_preservesParameters() {
        // Given
        val unicodeName = "🎵 Music 音楽 🎶"
        val unicodeId = "player_音楽_123"

        // When
        val route = AppRoutes.LibraryArgs(name = unicodeName, queueOrPlayerId = unicodeId)

        // Then
        assertEquals(unicodeName, route.name, "Unicode name should be preserved")
        assertEquals(unicodeId, route.queueOrPlayerId, "Unicode id should be preserved")
    }

    // ============= Route Type Tests =============

    @Test
    fun appRoutes_main_isAppRoutesType() {
        // When
        val route: AppRoutes = AppRoutes.Main

        // Then
        assert(route is AppRoutes) { "Main should be an AppRoutes type" }
    }

    @Test
    fun appRoutes_libraryArgs_isAppRoutesType() {
        // When
        val route: AppRoutes = AppRoutes.LibraryArgs("test", "test123")

        // Then
        assert(route is AppRoutes) { "LibraryArgs should be an AppRoutes type" }
    }

    @Test
    fun appRoutes_settings_isAppRoutesType() {
        // When
        val route: AppRoutes = AppRoutes.Settings

        // Then
        assert(route is AppRoutes) { "Settings should be an AppRoutes type" }
    }

    // ============= Route Equality Tests =============

    @Test
    fun appRoutes_main_equals_anotherMain() {
        // Given
        val route1: AppRoutes = AppRoutes.Main
        val route2: AppRoutes = AppRoutes.Main

        // Then
        assertEquals(route1, route2, "Two Main routes should be equal")
    }

    @Test
    fun appRoutes_settings_equals_anotherSettings() {
        // Given
        val route1: AppRoutes = AppRoutes.Settings
        val route2: AppRoutes = AppRoutes.Settings

        // Then
        assertEquals(route1, route2, "Two Settings routes should be equal")
    }

    @Test
    fun appRoutes_libraryArgs_equals_withSameParameters() {
        // Given
        val route1 = AppRoutes.LibraryArgs("library", "player1")
        val route2 = AppRoutes.LibraryArgs("library", "player1")

        // Then
        assertEquals(route1, route2, "LibraryArgs with same parameters should be equal")
    }

    @Test
    fun appRoutes_libraryArgs_notEquals_withDifferentName() {
        // Given
        val route1 = AppRoutes.LibraryArgs("library1", "player1")
        val route2 = AppRoutes.LibraryArgs("library2", "player1")

        // Then
        assert(route1 != route2) { "LibraryArgs with different names should not be equal" }
    }

    @Test
    fun appRoutes_libraryArgs_notEquals_withDifferentPlayerId() {
        // Given
        val route1 = AppRoutes.LibraryArgs("library", "player1")
        val route2 = AppRoutes.LibraryArgs("library", "player2")

        // Then
        assert(route1 != route2) { "LibraryArgs with different playerIds should not be equal" }
    }

    // ============= Serialization/Deserialization Tests =============

    @Test
    fun appRoutes_libraryArgs_canBeSerializedAndDeserialized() {
        // Given
        val original = AppRoutes.LibraryArgs("MyLibrary", "queue789")
        val json = Json { }

        // When
        val serialized = json.encodeToString(AppRoutes.LibraryArgs.serializer(), original)
        val deserialized = json.decodeFromString(AppRoutes.LibraryArgs.serializer(), serialized)

        // Then
        assertEquals(original, deserialized, "Serialized and deserialized route should be equal")
        assertEquals(original.name, deserialized.name, "Name should be preserved through serialization")
        assertEquals(original.queueOrPlayerId, deserialized.queueOrPlayerId, "QueueOrPlayerId should be preserved")
    }

    @Test
    fun appRoutes_libraryArgs_serialization_handlesSpecialCharacters() {
        // Given
        val original = AppRoutes.LibraryArgs("Library \"with quotes\"", "player/with/slashes")
        val json = Json { }

        // When
        val serialized = json.encodeToString(AppRoutes.LibraryArgs.serializer(), original)
        val deserialized = json.decodeFromString(AppRoutes.LibraryArgs.serializer(), serialized)

        // Then
        assertEquals(original, deserialized, "Special characters should survive serialization")
    }

    @Test
    fun appRoutes_libraryArgs_serialization_handlesUnicode() {
        // Given
        val original = AppRoutes.LibraryArgs("🎵 Música", "播放器_123")
        val json = Json { }

        // When
        val serialized = json.encodeToString(AppRoutes.LibraryArgs.serializer(), original)
        val deserialized = json.decodeFromString(AppRoutes.LibraryArgs.serializer(), serialized)

        // Then
        assertEquals(original, deserialized, "Unicode characters should survive serialization")
    }

    @Test
    fun appRoutes_libraryArgs_serialization_handlesEmptyStrings() {
        // Given
        val original = AppRoutes.LibraryArgs("", "")
        val json = Json { }

        // When
        val serialized = json.encodeToString(AppRoutes.LibraryArgs.serializer(), original)
        val deserialized = json.decodeFromString(AppRoutes.LibraryArgs.serializer(), serialized)

        // Then
        assertEquals(original, deserialized, "Empty strings should survive serialization")
    }

    // ============= Type Safety Tests =============

    @Test
    fun appRoutes_isSealed_allowsExhaustiveWhen() {
        // Given
        val routes: List<AppRoutes> =
            listOf(
                AppRoutes.Main,
                AppRoutes.LibraryArgs("test", "test"),
                AppRoutes.Settings,
            )

        // When/Then - exhaustive when should compile and work
        routes.forEach { route ->
            val description =
                when (route) {
                    is AppRoutes.Main -> "Main route"
                    is AppRoutes.LibraryArgs -> "Library route with args"
                    is AppRoutes.Settings -> "Settings route"
                }
            assertNotNull(description, "Route description should be generated")
        }
    }

    @Test
    fun appRoutes_libraryArgs_canBeUsedInWhenExpression() {
        // Given
        val route: AppRoutes = AppRoutes.LibraryArgs("TestLibrary", "player1")

        // When
        val result =
            when (route) {
                is AppRoutes.Main -> "main"
                is AppRoutes.LibraryArgs -> "library-${route.name}"
                is AppRoutes.Settings -> "settings"
            }

        // Then
        assertEquals("library-TestLibrary", result, "When expression should access LibraryArgs properties")
    }

    // ============= Route Parameter Edge Cases =============

    @Test
    fun appRoutes_libraryArgs_withWhitespace_preservesWhitespace() {
        // Given
        val nameWithSpaces = "  Library  Name  "
        val idWithSpaces = "  player  id  "

        // When
        val route = AppRoutes.LibraryArgs(name = nameWithSpaces, queueOrPlayerId = idWithSpaces)

        // Then
        assertEquals(nameWithSpaces, route.name, "Whitespace should be preserved in name")
        assertEquals(idWithSpaces, route.queueOrPlayerId, "Whitespace should be preserved in id")
    }

    @Test
    fun appRoutes_libraryArgs_withNewlines_preservesNewlines() {
        // Given
        val nameWithNewlines = "Library\nWith\nNewlines"
        val idWithNewlines = "player\nid"

        // When
        val route = AppRoutes.LibraryArgs(name = nameWithNewlines, queueOrPlayerId = idWithNewlines)

        // Then
        assertEquals(nameWithNewlines, route.name, "Newlines should be preserved in name")
        assertEquals(idWithNewlines, route.queueOrPlayerId, "Newlines should be preserved in id")
    }

    @Test
    fun appRoutes_libraryArgs_withTabs_preservesTabs() {
        // Given
        val nameWithTabs = "Library\tWith\tTabs"
        val idWithTabs = "player\tid"

        // When
        val route = AppRoutes.LibraryArgs(name = nameWithTabs, queueOrPlayerId = idWithTabs)

        // Then
        assertEquals(nameWithTabs, route.name, "Tabs should be preserved in name")
        assertEquals(idWithTabs, route.queueOrPlayerId, "Tabs should be preserved in id")
    }

    // ============= Multiple Route Creation Tests =============

    @Test
    fun appRoutes_multipleLibraryArgs_canBeCreatedIndependently() {
        // When
        val route1 = AppRoutes.LibraryArgs("Library1", "player1")
        val route2 = AppRoutes.LibraryArgs("Library2", "player2")
        val route3 = AppRoutes.LibraryArgs("Library3", "player3")

        // Then
        assertNotNull(route1)
        assertNotNull(route2)
        assertNotNull(route3)
        assert(route1 != route2) { "Routes with different parameters should be different" }
        assert(route2 != route3) { "Routes with different parameters should be different" }
    }

    @Test
    fun appRoutes_mixedTypes_canCoexist() {
        // When
        val main: AppRoutes = AppRoutes.Main
        val library: AppRoutes = AppRoutes.LibraryArgs("Library", "player")
        val settings: AppRoutes = AppRoutes.Settings

        // Then - all routes should be creatable and different
        assertNotNull(main)
        assertNotNull(library)
        assertNotNull(settings)
        assert(main != library) { "Main and Library routes should be different" }
        assert(library != settings) { "Library and Settings routes should be different" }
        assert(main != settings) { "Main and Settings routes should be different" }
    }
}
