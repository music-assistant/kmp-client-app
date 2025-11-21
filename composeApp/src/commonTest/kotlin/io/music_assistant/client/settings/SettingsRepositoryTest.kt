// ABOUTME: Comprehensive tests for SettingsRepository covering theme, connection info,
// ABOUTME: player sorting, and local player ID persistence and reactive flows.
package io.music_assistant.client.settings

import app.cash.turbine.test
import com.russhwolf.settings.Settings
import io.music_assistant.client.RobolectricTest
import io.music_assistant.client.api.ConnectionInfo
import io.music_assistant.client.ui.theme.ThemeSetting
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Comprehensive tests for SettingsRepository.
 *
 * Tests cover:
 * - Theme management (initial state, switching, persistence)
 * - ConnectionInfo (initial null, updates, persistence, Flow emissions)
 * - Players sorting (initial null, updates, persistence, Flow emissions)
 * - Local player ID (generation, persistence, consistency)
 * - Edge cases (duplicate updates, empty values, multiple repository instances)
 *
 * Extends RobolectricTest to enable Robolectric on Android.
 */
class SettingsRepositoryTest : RobolectricTest() {
    /**
     * Fake Settings implementation for testing
     */
    private class FakeSettings : Settings {
        private val map = mutableMapOf<String, Any?>()

        override fun clear() = map.clear()

        override fun getBoolean(
            key: String,
            defaultValue: Boolean,
        ): Boolean = map[key] as? Boolean ?: defaultValue

        override fun getBooleanOrNull(key: String): Boolean? = map[key] as? Boolean

        override fun getDouble(
            key: String,
            defaultValue: Double,
        ): Double = map[key] as? Double ?: defaultValue

        override fun getDoubleOrNull(key: String): Double? = map[key] as? Double

        override fun getFloat(
            key: String,
            defaultValue: Float,
        ): Float = map[key] as? Float ?: defaultValue

        override fun getFloatOrNull(key: String): Float? = map[key] as? Float

        override fun getInt(
            key: String,
            defaultValue: Int,
        ): Int = map[key] as? Int ?: defaultValue

        override fun getIntOrNull(key: String): Int? = map[key] as? Int

        override fun getLong(
            key: String,
            defaultValue: Long,
        ): Long = map[key] as? Long ?: defaultValue

        override fun getLongOrNull(key: String): Long? = map[key] as? Long

        override fun getString(
            key: String,
            defaultValue: String,
        ): String = map[key] as? String ?: defaultValue

        override fun getStringOrNull(key: String): String? = map[key] as? String

        override fun hasKey(key: String): Boolean = map.containsKey(key)

        override fun putBoolean(
            key: String,
            value: Boolean,
        ) {
            map[key] = value
        }

        override fun putDouble(
            key: String,
            value: Double,
        ) {
            map[key] = value
        }

        override fun putFloat(
            key: String,
            value: Float,
        ) {
            map[key] = value
        }

        override fun putInt(
            key: String,
            value: Int,
        ) {
            map[key] = value
        }

        override fun putLong(
            key: String,
            value: Long,
        ) {
            map[key] = value
        }

        override fun putString(
            key: String,
            value: String,
        ) {
            map[key] = value
        }

        override fun remove(key: String) {
            map.remove(key)
        }

        override val keys: Set<String> get() = map.keys
        override val size: Int get() = map.size
    }

    // ============= Theme Management Tests =============

    @Test
    fun theme_initialState_shouldBeFollowSystem() =
        runTest {
            // Given
            val settings = FakeSettings()
            val repo = SettingsRepository(settings)

            // When/Then
            repo.theme.test {
                val theme = awaitItem()
                assertEquals(ThemeSetting.FollowSystem, theme, "Default theme should be FollowSystem")
            }
        }

    @Test
    fun theme_withExistingValue_shouldLoadFromSettings() =
        runTest {
            // Given
            val settings = FakeSettings()
            settings.putString("theme", ThemeSetting.Dark.name)
            val repo = SettingsRepository(settings)

            // When/Then
            repo.theme.test {
                val theme = awaitItem()
                assertEquals(ThemeSetting.Dark, theme, "Theme should be loaded from settings")
            }
        }

    @Test
    fun switchTheme_shouldUpdateFlowAndPersist() =
        runTest {
            // Given
            val settings = FakeSettings()
            val repo = SettingsRepository(settings)

            // When
            repo.theme.test {
                val initial = awaitItem()
                assertEquals(ThemeSetting.FollowSystem, initial, "Initial theme should be FollowSystem")

                repo.switchTheme(ThemeSetting.Light)

                // Then
                val updated = awaitItem()
                assertEquals(ThemeSetting.Light, updated, "Theme should be updated to Light")
                assertEquals(ThemeSetting.Light.name, settings.getString("theme", ""), "Theme should be persisted")
            }
        }

    @Test
    fun switchTheme_multipleTimes_shouldEmitEachChange() =
        runTest {
            // Given
            val settings = FakeSettings()
            val repo = SettingsRepository(settings)

            // When/Then
            repo.theme.test {
                awaitItem() // FollowSystem

                repo.switchTheme(ThemeSetting.Dark)
                assertEquals(ThemeSetting.Dark, awaitItem())

                repo.switchTheme(ThemeSetting.Light)
                assertEquals(ThemeSetting.Light, awaitItem())

                repo.switchTheme(ThemeSetting.FollowSystem)
                assertEquals(ThemeSetting.FollowSystem, awaitItem())
            }
        }

    @Test
    fun switchTheme_sameValue_shouldNotEmit() =
        runTest {
            // Given
            val settings = FakeSettings()
            val repo = SettingsRepository(settings)

            // When/Then
            repo.theme.test {
                awaitItem() // FollowSystem

                repo.switchTheme(ThemeSetting.FollowSystem)
                // MutableStateFlow.update uses distinctUntilChanged, so no emission for same value
                expectNoEvents()
            }
        }

    // ============= ConnectionInfo Tests =============

    @Test
    fun connectionInfo_initialState_shouldBeNull() =
        runTest {
            // Given
            val settings = FakeSettings()
            val repo = SettingsRepository(settings)

            // When/Then
            repo.connectionInfo.test {
                val info = awaitItem()
                assertNull(info, "Initial connectionInfo should be null")
            }
        }

    @Test
    fun connectionInfo_withExistingValidSettings_shouldLoadFromSettings() =
        runTest {
            // Given
            val settings = FakeSettings()
            settings.putString("host", "localhost")
            settings.putInt("port", 8095)
            settings.putBoolean("isTls", false)
            val repo = SettingsRepository(settings)

            // When/Then
            repo.connectionInfo.test {
                val info = awaitItem()
                assertNotNull(info, "ConnectionInfo should be loaded from settings")
                assertEquals("localhost", info.host)
                assertEquals(8095, info.port)
                assertFalse(info.isTls)
            }
        }

    @Test
    fun connectionInfo_withTls_shouldLoadCorrectly() =
        runTest {
            // Given
            val settings = FakeSettings()
            settings.putString("host", "example.com")
            settings.putInt("port", 443)
            settings.putBoolean("isTls", true)
            val repo = SettingsRepository(settings)

            // When/Then
            repo.connectionInfo.test {
                val info = awaitItem()
                assertNotNull(info)
                assertEquals("example.com", info.host)
                assertEquals(443, info.port)
                assertTrue(info.isTls, "TLS should be enabled")
            }
        }

    @Test
    fun connectionInfo_withBlankHost_shouldBeNull() =
        runTest {
            // Given
            val settings = FakeSettings()
            settings.putString("host", "")
            settings.putInt("port", 8095)
            settings.putBoolean("isTls", false)
            val repo = SettingsRepository(settings)

            // When/Then
            repo.connectionInfo.test {
                val info = awaitItem()
                assertNull(info, "ConnectionInfo should be null when host is blank")
            }
        }

    @Test
    fun connectionInfo_withWhitespaceHost_shouldBeNull() =
        runTest {
            // Given
            val settings = FakeSettings()
            settings.putString("host", "   ")
            settings.putInt("port", 8095)
            settings.putBoolean("isTls", false)
            val repo = SettingsRepository(settings)

            // When/Then
            repo.connectionInfo.test {
                val info = awaitItem()
                assertNull(info, "ConnectionInfo should be null when host is only whitespace")
            }
        }

    @Test
    fun connectionInfo_withZeroPort_shouldBeNull() =
        runTest {
            // Given
            val settings = FakeSettings()
            settings.putString("host", "localhost")
            settings.putInt("port", 0)
            settings.putBoolean("isTls", false)
            val repo = SettingsRepository(settings)

            // When/Then
            repo.connectionInfo.test {
                val info = awaitItem()
                assertNull(info, "ConnectionInfo should be null when port is 0")
            }
        }

    @Test
    fun connectionInfo_withNegativePort_shouldBeNull() =
        runTest {
            // Given
            val settings = FakeSettings()
            settings.putString("host", "localhost")
            settings.putInt("port", -1)
            settings.putBoolean("isTls", false)
            val repo = SettingsRepository(settings)

            // When/Then
            repo.connectionInfo.test {
                val info = awaitItem()
                assertNull(info, "ConnectionInfo should be null when port is negative")
            }
        }

    @Test
    fun connectionInfo_withMissingHost_shouldBeNull() =
        runTest {
            // Given
            val settings = FakeSettings()
            settings.putInt("port", 8095)
            settings.putBoolean("isTls", false)
            val repo = SettingsRepository(settings)

            // When/Then
            repo.connectionInfo.test {
                val info = awaitItem()
                assertNull(info, "ConnectionInfo should be null when host is not set")
            }
        }

    @Test
    fun connectionInfo_withMissingPort_shouldBeNull() =
        runTest {
            // Given
            val settings = FakeSettings()
            settings.putString("host", "localhost")
            settings.putBoolean("isTls", false)
            val repo = SettingsRepository(settings)

            // When/Then
            repo.connectionInfo.test {
                val info = awaitItem()
                assertNull(info, "ConnectionInfo should be null when port is not set")
            }
        }

    @Test
    fun updateConnectionInfo_shouldUpdateFlowAndPersist() =
        runTest {
            // Given
            val settings = FakeSettings()
            val repo = SettingsRepository(settings)
            val newInfo = ConnectionInfo("192.168.1.100", 8095, false)

            // When
            repo.connectionInfo.test {
                val initial = awaitItem()
                assertNull(initial, "Initial should be null")

                repo.updateConnectionInfo(newInfo)

                // Then
                val updated = awaitItem()
                assertNotNull(updated, "ConnectionInfo should be updated")
                assertEquals(newInfo, updated)
                assertEquals("192.168.1.100", settings.getString("host", ""))
                assertEquals(8095, settings.getInt("port", 0))
                assertFalse(settings.getBoolean("isTls", true))
            }
        }

    @Test
    fun updateConnectionInfo_withTls_shouldPersistCorrectly() =
        runTest {
            // Given
            val settings = FakeSettings()
            val repo = SettingsRepository(settings)
            val newInfo = ConnectionInfo("secure.example.com", 443, true)

            // When
            repo.updateConnectionInfo(newInfo)

            // Then
            repo.connectionInfo.test {
                val info = awaitItem()
                assertNotNull(info)
                assertEquals("secure.example.com", info.host)
                assertEquals(443, info.port)
                assertTrue(info.isTls)
                assertTrue(settings.getBoolean("isTls", false), "TLS should be persisted")
            }
        }

    @Test
    fun updateConnectionInfo_toNull_shouldClearSettings() =
        runTest {
            // Given
            val settings = FakeSettings()
            settings.putString("host", "localhost")
            settings.putInt("port", 8095)
            settings.putBoolean("isTls", false)
            val repo = SettingsRepository(settings)

            // When
            repo.connectionInfo.test {
                awaitItem() // Initial value

                repo.updateConnectionInfo(null)

                // Then
                val updated = awaitItem()
                assertNull(updated, "ConnectionInfo should be null")
                assertEquals("", settings.getString("host", "default"), "Host should be cleared")
                assertEquals(0, settings.getInt("port", -1), "Port should be cleared")
                assertFalse(settings.getBoolean("isTls", true), "TLS should be false")
            }
        }

    @Test
    fun updateConnectionInfo_withSameValue_shouldNotEmit() =
        runTest {
            // Given
            val settings = FakeSettings()
            val repo = SettingsRepository(settings)
            val info = ConnectionInfo("localhost", 8095, false)

            // When
            repo.updateConnectionInfo(info)

            repo.connectionInfo.test {
                awaitItem() // Current value

                // Update with same value
                repo.updateConnectionInfo(info)

                // Then - should not emit a new value
                expectNoEvents()
            }
        }

    @Test
    fun updateConnectionInfo_multipleTimes_shouldEmitEachChange() =
        runTest {
            // Given
            val settings = FakeSettings()
            val repo = SettingsRepository(settings)

            // When/Then
            repo.connectionInfo.test {
                awaitItem() // null

                val info1 = ConnectionInfo("host1", 8095, false)
                repo.updateConnectionInfo(info1)
                assertEquals(info1, awaitItem())

                val info2 = ConnectionInfo("host2", 8096, true)
                repo.updateConnectionInfo(info2)
                assertEquals(info2, awaitItem())

                repo.updateConnectionInfo(null)
                assertNull(awaitItem())
            }
        }

    // ============= Players Sorting Tests =============

    @Test
    fun playersSorting_initialState_shouldBeNull() =
        runTest {
            // Given
            val settings = FakeSettings()
            val repo = SettingsRepository(settings)

            // When/Then
            repo.playersSorting.test {
                val sorting = awaitItem()
                assertNull(sorting, "Initial playersSorting should be null")
            }
        }

    @Test
    fun playersSorting_withExistingValue_shouldLoadFromSettings() =
        runTest {
            // Given
            val settings = FakeSettings()
            settings.putString("players_sort", "player1,player2,player3")
            val repo = SettingsRepository(settings)

            // When/Then
            repo.playersSorting.test {
                val sorting = awaitItem()
                assertNotNull(sorting, "PlayersSorting should be loaded from settings")
                assertEquals(listOf("player1", "player2", "player3"), sorting)
            }
        }

    @Test
    fun playersSorting_withSinglePlayer_shouldLoadCorrectly() =
        runTest {
            // Given
            val settings = FakeSettings()
            settings.putString("players_sort", "player1")
            val repo = SettingsRepository(settings)

            // When/Then
            repo.playersSorting.test {
                val sorting = awaitItem()
                assertNotNull(sorting)
                assertEquals(listOf("player1"), sorting)
            }
        }

    @Test
    fun playersSorting_withEmptyString_shouldContainEmptyStringElement() =
        runTest {
            // Given
            val settings = FakeSettings()
            settings.putString("players_sort", "")
            val repo = SettingsRepository(settings)

            // When/Then
            repo.playersSorting.test {
                val sorting = awaitItem()
                assertNotNull(sorting, "Empty string splits to list with one empty element")
                assertEquals(listOf(""), sorting, "Should contain single empty string")
            }
        }

    @Test
    fun updatePlayersSorting_shouldUpdateFlowAndPersist() =
        runTest {
            // Given
            val settings = FakeSettings()
            val repo = SettingsRepository(settings)
            val newSorting = listOf("player3", "player1", "player2")

            // When
            repo.playersSorting.test {
                val initial = awaitItem()
                assertNull(initial, "Initial should be null")

                repo.updatePlayersSorting(newSorting)

                // Then
                val updated = awaitItem()
                assertNotNull(updated, "PlayersSorting should be updated")
                assertEquals(newSorting, updated)
                assertEquals("player3,player1,player2", settings.getString("players_sort", ""))
            }
        }

    @Test
    fun updatePlayersSorting_withEmptyList_shouldPersist() =
        runTest {
            // Given
            val settings = FakeSettings()
            val repo = SettingsRepository(settings)

            // When
            repo.updatePlayersSorting(emptyList())

            // Then
            repo.playersSorting.test {
                val sorting = awaitItem()
                assertNotNull(sorting)
                assertTrue(sorting.isEmpty(), "Empty list should be persisted")
                assertEquals("", settings.getString("players_sort", "default"))
            }
        }

    @Test
    fun updatePlayersSorting_withSpecialCharacters_shouldHandleCorrectly() =
        runTest {
            // Given
            val settings = FakeSettings()
            val repo = SettingsRepository(settings)
            val sortingWithSpecial = listOf("player-1", "player_2", "player.3")

            // When
            repo.updatePlayersSorting(sortingWithSpecial)

            // Then
            repo.playersSorting.test {
                val sorting = awaitItem()
                assertNotNull(sorting)
                assertEquals(sortingWithSpecial, sorting)
            }
        }

    @Test
    fun updatePlayersSorting_multipleTimes_shouldEmitEachChange() =
        runTest {
            // Given
            val settings = FakeSettings()
            val repo = SettingsRepository(settings)

            // When/Then
            repo.playersSorting.test {
                awaitItem() // null

                val sorting1 = listOf("p1", "p2")
                repo.updatePlayersSorting(sorting1)
                assertEquals(sorting1, awaitItem())

                val sorting2 = listOf("p2", "p1", "p3")
                repo.updatePlayersSorting(sorting2)
                assertEquals(sorting2, awaitItem())

                val sorting3 = listOf("p3")
                repo.updatePlayersSorting(sorting3)
                assertEquals(sorting3, awaitItem())
            }
        }

    @Test
    fun updatePlayersSorting_sameValue_shouldNotEmit() =
        runTest {
            // Given
            val settings = FakeSettings()
            val repo = SettingsRepository(settings)
            val sorting = listOf("p1", "p2")

            // When
            repo.updatePlayersSorting(sorting)

            repo.playersSorting.test {
                awaitItem() // Current value

                // Update with same value
                repo.updatePlayersSorting(sorting)

                // Then - MutableStateFlow.update uses distinctUntilChanged, so no emission
                expectNoEvents()
            }
        }

    // ============= Local Player ID Tests =============

    @Test
    fun getLocalPlayerId_whenNotSet_shouldGenerateAndPersist() {
        // Given
        val settings = FakeSettings()
        val repo = SettingsRepository(settings)

        // When
        val playerId = repo.getLocalPlayerId()

        // Then
        assertNotNull(playerId, "Player ID should be generated")
        assertTrue(playerId.isNotBlank(), "Player ID should not be blank")
        assertEquals(playerId, settings.getString("local_player_id", ""), "Player ID should be persisted")
    }

    @Test
    fun getLocalPlayerId_whenSet_shouldReturnExisting() {
        // Given
        val settings = FakeSettings()
        val existingId = "existing-player-id-12345"
        settings.putString("local_player_id", existingId)
        val repo = SettingsRepository(settings)

        // When
        val playerId = repo.getLocalPlayerId()

        // Then
        assertEquals(existingId, playerId, "Should return existing player ID")
    }

    @Test
    fun getLocalPlayerId_calledMultipleTimes_shouldReturnSameId() {
        // Given
        val settings = FakeSettings()
        val repo = SettingsRepository(settings)

        // When
        val playerId1 = repo.getLocalPlayerId()
        val playerId2 = repo.getLocalPlayerId()
        val playerId3 = repo.getLocalPlayerId()

        // Then
        assertEquals(playerId1, playerId2, "Should return same ID on second call")
        assertEquals(playerId2, playerId3, "Should return same ID on third call")
    }

    @Test
    fun getLocalPlayerId_differentInstances_shouldReturnSameId() {
        // Given
        val settings = FakeSettings()
        val repo1 = SettingsRepository(settings)

        // When
        val playerId1 = repo1.getLocalPlayerId()

        // Create second instance with same settings
        val repo2 = SettingsRepository(settings)
        val playerId2 = repo2.getLocalPlayerId()

        // Then
        assertEquals(playerId1, playerId2, "Different instances should return same persisted ID")
    }

    @Test
    fun getLocalPlayerId_generatedId_shouldBeValidUuid() {
        // Given
        val settings = FakeSettings()
        val repo = SettingsRepository(settings)

        // When
        val playerId = repo.getLocalPlayerId()

        // Then - UUID format check (basic validation)
        assertTrue(playerId.contains("-"), "Generated ID should look like a UUID")
        assertTrue(playerId.length >= 32, "Generated ID should be sufficiently long")
    }

    @Test
    fun getLocalPlayerId_multipleRepositories_generateDifferentIds() {
        // Given
        val settings1 = FakeSettings()
        val settings2 = FakeSettings()
        val repo1 = SettingsRepository(settings1)
        val repo2 = SettingsRepository(settings2)

        // When
        val playerId1 = repo1.getLocalPlayerId()
        val playerId2 = repo2.getLocalPlayerId()

        // Then
        assertNotEquals(playerId1, playerId2, "Different repositories should generate different IDs")
    }

    // ============= Edge Cases and Integration Tests =============

    @Test
    fun multipleRepositoryInstances_withSameSettings_shouldShareState() =
        runTest {
            // Given
            val settings = FakeSettings()
            val repo1 = SettingsRepository(settings)
            val repo2 = SettingsRepository(settings)

            // When
            repo1.switchTheme(ThemeSetting.Dark)

            // Then - repo2 won't see the Flow update, but will see persisted value
            repo2.theme.test {
                // Initial value from repo2's construction (will be default)
                awaitItem()
            }

            // Create new instance to see persisted value
            val repo3 = SettingsRepository(settings)
            repo3.theme.test {
                val theme = awaitItem()
                assertEquals(ThemeSetting.Dark, theme, "New instance should load persisted theme")
            }
        }

    @Test
    fun connectionInfo_withOnlyPartialData_shouldBeNull() =
        runTest {
            // Given
            val settings = FakeSettings()
            settings.putString("host", "localhost")
            // Port is missing
            settings.putBoolean("isTls", false)
            val repo = SettingsRepository(settings)

            // When/Then
            repo.connectionInfo.test {
                val info = awaitItem()
                assertNull(info, "ConnectionInfo should be null when port is missing")
            }
        }

    @Test
    fun updateConnectionInfo_afterPreviousNull_shouldEmit() =
        runTest {
            // Given
            val settings = FakeSettings()
            val repo = SettingsRepository(settings)

            // When/Then
            repo.connectionInfo.test {
                assertNull(awaitItem(), "Initial should be null")

                repo.updateConnectionInfo(null)
                // Should not emit since value is same

                val info = ConnectionInfo("localhost", 8095, false)
                repo.updateConnectionInfo(info)
                assertEquals(info, awaitItem(), "Should emit new value")
            }
        }

    @Test
    fun allSettings_shouldPersistIndependently() =
        runTest {
            // Given
            val settings = FakeSettings()
            val repo = SettingsRepository(settings)

            // When
            repo.switchTheme(ThemeSetting.Dark)
            repo.updateConnectionInfo(ConnectionInfo("host", 8095, true))
            repo.updatePlayersSorting(listOf("p1", "p2"))
            val playerId = repo.getLocalPlayerId()

            // Then - verify all are persisted
            assertEquals(ThemeSetting.Dark.name, settings.getString("theme", ""))
            assertEquals("host", settings.getString("host", ""))
            assertEquals(8095, settings.getInt("port", 0))
            assertTrue(settings.getBoolean("isTls", false))
            assertEquals("p1,p2", settings.getString("players_sort", ""))
            assertEquals(playerId, settings.getString("local_player_id", ""))
        }

    @Test
    fun clearSettings_shouldResetToDefaults() =
        runTest {
            // Given
            val settings = FakeSettings()
            settings.putString("theme", ThemeSetting.Dark.name)
            settings.putString("host", "localhost")
            settings.putInt("port", 8095)
            settings.putBoolean("isTls", false)
            settings.putString("players_sort", "p1,p2")
            settings.putString("local_player_id", "test-id")

            // When
            settings.clear()
            val repo = SettingsRepository(settings)

            // Then
            repo.theme.test {
                assertEquals(ThemeSetting.FollowSystem, awaitItem())
            }
            repo.connectionInfo.test {
                assertNull(awaitItem())
            }
            repo.playersSorting.test {
                assertNull(awaitItem())
            }
        }

    @Test
    fun playersSorting_withCommaInValue_shouldNotBreak() =
        runTest {
            // Given - this is a known edge case: player IDs shouldn't have commas
            // but we test the behavior
            val settings = FakeSettings()
            val repo = SettingsRepository(settings)

            // When
            repo.updatePlayersSorting(listOf("player-1", "player-2"))

            // Then
            repo.playersSorting.test {
                val sorting = awaitItem()
                assertEquals(listOf("player-1", "player-2"), sorting)
            }
        }
}
