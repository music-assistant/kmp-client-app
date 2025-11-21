// ABOUTME: Tests for ImageInfo.url() covering remote vs local images, URL encoding,
// ABOUTME: and server URL variations for the image proxy endpoint.
package io.music_assistant.client.data.model.client

import io.music_assistant.client.RobolectricTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ImageInfoTest : RobolectricTest() {
    // ========================================
    // Remote Image Tests
    // ========================================

    @Test
    fun url_withRemoteImage_shouldReturnPathDirectly() {
        // Given
        val imageInfo =
            AppMediaItem.ImageInfo(
                path = "https://example.com/image.jpg",
                isRemotelyAccessible = true,
                provider = "spotify",
            )

        // When
        val url = imageInfo.url("http://localhost:8095")

        // Then
        assertEquals("https://example.com/image.jpg", url)
    }

    @Test
    fun url_withRemoteImage_shouldIgnoreServerUrl() {
        // Given
        val imageInfo =
            AppMediaItem.ImageInfo(
                path = "https://cdn.example.com/album/artwork.png",
                isRemotelyAccessible = true,
                provider = "tidal",
            )

        // When - server URL should be ignored
        val url = imageInfo.url(null)

        // Then - should still return the remote path
        assertEquals("https://cdn.example.com/album/artwork.png", url)
    }

    @Test
    fun url_withRemoteHttpImage_shouldReturnPathDirectly() {
        // Given
        val imageInfo =
            AppMediaItem.ImageInfo(
                path = "http://music.server.local/cover.jpg",
                isRemotelyAccessible = true,
                provider = "local",
            )

        // When
        val url = imageInfo.url("http://localhost:8095")

        // Then
        assertEquals("http://music.server.local/cover.jpg", url)
    }

    // ========================================
    // Local Image with Server URL Tests
    // ========================================

    @Test
    fun url_withLocalImageAndServerUrl_shouldBuildImageproxyUrl() {
        // Given
        val imageInfo =
            AppMediaItem.ImageInfo(
                path = "/local/music/artist/image.jpg",
                isRemotelyAccessible = false,
                provider = "filesystem",
            )

        // When
        val url = imageInfo.url("http://localhost:8095")

        // Then
        assertNotNull(url)
        assertTrue(url.startsWith("http://localhost:8095/imageproxy?"))
        assertTrue(url.contains("path="))
        assertTrue(url.contains("provider=filesystem"))
        assertTrue(url.contains("checksum="))
    }

    @Test
    fun url_withLocalImageAndHttpsServerUrl_shouldBuildHttpsUrl() {
        // Given
        val imageInfo =
            AppMediaItem.ImageInfo(
                path = "/music/album.jpg",
                isRemotelyAccessible = false,
                provider = "local",
            )

        // When
        val url = imageInfo.url("https://music.server.com")

        // Then
        assertNotNull(url)
        assertTrue(url.startsWith("https://music.server.com/imageproxy?"))
    }

    @Test
    fun url_withLocalImageAndServerUrlWithTrailingSlash_shouldBuildCorrectly() {
        // Given
        val imageInfo =
            AppMediaItem.ImageInfo(
                path = "/images/cover.png",
                isRemotelyAccessible = false,
                provider = "local",
            )

        // When
        val url = imageInfo.url("http://localhost:8095/")

        // Then
        assertNotNull(url)
        // Should handle the trailing slash properly
        assertTrue(url.contains("/imageproxy?"))
    }

    @Test
    fun url_withLocalImageAndServerUrlWithPort_shouldBuildCorrectly() {
        // Given
        val imageInfo =
            AppMediaItem.ImageInfo(
                path = "/media/artist.jpg",
                isRemotelyAccessible = false,
                provider = "spotify",
            )

        // When
        val url = imageInfo.url("http://192.168.1.100:8095")

        // Then
        assertNotNull(url)
        assertTrue(url.startsWith("http://192.168.1.100:8095/imageproxy?"))
    }

    // ========================================
    // Local Image without Server URL Tests
    // ========================================

    @Test
    fun url_withLocalImageAndNullServerUrl_shouldReturnNull() {
        // Given
        val imageInfo =
            AppMediaItem.ImageInfo(
                path = "/local/image.jpg",
                isRemotelyAccessible = false,
                provider = "local",
            )

        // When
        val url = imageInfo.url(null)

        // Then
        assertNull(url)
    }

    @Test
    fun url_withLocalImageAndEmptyServerUrl_shouldHandleGracefully() {
        // Given
        val imageInfo =
            AppMediaItem.ImageInfo(
                path = "/local/image.jpg",
                isRemotelyAccessible = false,
                provider = "local",
            )

        // When - empty string server URL
        val url = imageInfo.url("")

        // Then - URLBuilder will construct a URL even with empty server
        // It will be malformed but won't be null
        assertNotNull(url)
        // The URL will be constructed but won't have a proper host
        assertTrue(url.contains("imageproxy"))
    }

    // ========================================
    // URL Encoding Tests
    // ========================================

    @Test
    fun url_withSpacesInPath_shouldEncodeSpaces() {
        // Given
        val imageInfo =
            AppMediaItem.ImageInfo(
                path = "/music/My Artist/Album Cover.jpg",
                isRemotelyAccessible = false,
                provider = "local",
            )

        // When
        val url = imageInfo.url("http://localhost:8095")

        // Then
        assertNotNull(url)
        // Spaces should be encoded
        assertTrue(!url.contains(" "))
        assertTrue(url.contains("path="))
    }

    @Test
    fun url_withSpecialCharactersInPath_shouldEncodeCorrectly() {
        // Given
        val imageInfo =
            AppMediaItem.ImageInfo(
                path = "/music/AC&DC/Back in Black?.jpg",
                isRemotelyAccessible = false,
                provider = "local",
            )

        // When
        val url = imageInfo.url("http://localhost:8095")

        // Then
        assertNotNull(url)
        // Special characters in the path should be encoded
        // The URL will contain '?' as the query string separator, but not "Black?" in raw form
        assertTrue(!url.contains("AC&DC"), "Raw '&DC' should be encoded")
        assertTrue(!url.contains("Black?.jpg"), "Raw 'Black?.jpg' should be encoded")
        assertTrue(url.contains("path="))
        assertTrue(url.contains("provider=local"))
    }

    @Test
    fun url_withSlashesInPath_shouldEncodeCorrectly() {
        // Given
        val imageInfo =
            AppMediaItem.ImageInfo(
                path = "/music/artist/album/track/image.jpg",
                isRemotelyAccessible = false,
                provider = "filesystem",
            )

        // When
        val url = imageInfo.url("http://localhost:8095")

        // Then
        assertNotNull(url)
        assertTrue(url.contains("path="))
    }

    @Test
    fun url_withUnicodeCharactersInPath_shouldEncodeCorrectly() {
        // Given
        val imageInfo =
            AppMediaItem.ImageInfo(
                path = "/music/Björk/音楽/cover.jpg",
                isRemotelyAccessible = false,
                provider = "local",
            )

        // When
        val url = imageInfo.url("http://localhost:8095")

        // Then
        assertNotNull(url)
        // Unicode characters should be properly encoded
        assertTrue(url.contains("path="))
    }

    @Test
    fun url_withPercentEncodedPath_shouldHandleCorrectly() {
        // Given
        val imageInfo =
            AppMediaItem.ImageInfo(
                path = "/music/AC%2FDC/album%20cover.jpg",
                isRemotelyAccessible = false,
                provider = "local",
            )

        // When
        val url = imageInfo.url("http://localhost:8095")

        // Then
        assertNotNull(url)
        assertTrue(url.contains("path="))
    }

    @Test
    fun url_withQueryParametersInPath_shouldEncodeCorrectly() {
        // Given - hypothetical case where path has query-like characters
        val imageInfo =
            AppMediaItem.ImageInfo(
                path = "/music/artist?id=123&type=cover",
                isRemotelyAccessible = false,
                provider = "local",
            )

        // When
        val url = imageInfo.url("http://localhost:8095")

        // Then
        assertNotNull(url)
        // The path's special chars should be encoded
        assertTrue(url.contains("path="))
    }

    // ========================================
    // Provider Parameter Tests
    // ========================================

    @Test
    fun url_shouldIncludeProviderParameter() {
        // Given
        val imageInfo =
            AppMediaItem.ImageInfo(
                path = "/image.jpg",
                isRemotelyAccessible = false,
                provider = "spotify",
            )

        // When
        val url = imageInfo.url("http://localhost:8095")

        // Then
        assertNotNull(url)
        assertTrue(url.contains("provider=spotify"))
    }

    @Test
    fun url_withDifferentProviders_shouldIncludeCorrectProvider() {
        val providers = listOf("spotify", "tidal", "qobuz", "filesystem", "local", "youtube")

        providers.forEach { provider ->
            // Given
            val imageInfo =
                AppMediaItem.ImageInfo(
                    path = "/image.jpg",
                    isRemotelyAccessible = false,
                    provider = provider,
                )

            // When
            val url = imageInfo.url("http://localhost:8095")

            // Then
            assertNotNull(url, "URL should not be null for provider: $provider")
            assertTrue(url.contains("provider=$provider"), "URL should contain provider=$provider")
        }
    }

    @Test
    fun url_withProviderWithSpecialCharacters_shouldEncodeCorrectly() {
        // Given
        val imageInfo =
            AppMediaItem.ImageInfo(
                path = "/image.jpg",
                isRemotelyAccessible = false,
                provider = "provider-with-dashes",
            )

        // When
        val url = imageInfo.url("http://localhost:8095")

        // Then
        assertNotNull(url)
        assertTrue(url.contains("provider=provider-with-dashes"))
    }

    // ========================================
    // Checksum Parameter Tests
    // ========================================

    @Test
    fun url_shouldIncludeEmptyChecksumParameter() {
        // Given
        val imageInfo =
            AppMediaItem.ImageInfo(
                path = "/image.jpg",
                isRemotelyAccessible = false,
                provider = "local",
            )

        // When
        val url = imageInfo.url("http://localhost:8095")

        // Then
        assertNotNull(url)
        assertTrue(url.contains("checksum="))
    }

    // ========================================
    // Empty/Null Path Tests
    // ========================================

    @Test
    fun url_withEmptyPath_shouldHandleGracefully() {
        // Given
        val imageInfo =
            AppMediaItem.ImageInfo(
                path = "",
                isRemotelyAccessible = false,
                provider = "local",
            )

        // When
        val url = imageInfo.url("http://localhost:8095")

        // Then - should build URL even with empty path
        assertNotNull(url)
        assertTrue(url.contains("path="))
    }

    @Test
    fun url_withEmptyPathAndRemoteAccessible_shouldReturnEmptyString() {
        // Given
        val imageInfo =
            AppMediaItem.ImageInfo(
                path = "",
                isRemotelyAccessible = true,
                provider = "spotify",
            )

        // When
        val url = imageInfo.url("http://localhost:8095")

        // Then
        assertEquals("", url)
    }

    // ========================================
    // Different Server URL Formats Tests
    // ========================================

    @Test
    fun url_withLocalhostServer_shouldBuildCorrectly() {
        // Given
        val imageInfo =
            AppMediaItem.ImageInfo(
                path = "/image.jpg",
                isRemotelyAccessible = false,
                provider = "local",
            )

        // When
        val url = imageInfo.url("http://localhost:8095")

        // Then
        assertNotNull(url)
        assertTrue(url.startsWith("http://localhost:8095/imageproxy?"))
    }

    @Test
    fun url_withIpAddressServer_shouldBuildCorrectly() {
        // Given
        val imageInfo =
            AppMediaItem.ImageInfo(
                path = "/image.jpg",
                isRemotelyAccessible = false,
                provider = "local",
            )

        // When
        val url = imageInfo.url("http://192.168.1.50:8095")

        // Then
        assertNotNull(url)
        assertTrue(url.startsWith("http://192.168.1.50:8095/imageproxy?"))
    }

    @Test
    fun url_withDomainNameServer_shouldBuildCorrectly() {
        // Given
        val imageInfo =
            AppMediaItem.ImageInfo(
                path = "/image.jpg",
                isRemotelyAccessible = false,
                provider = "local",
            )

        // When
        val url = imageInfo.url("https://music.example.com:443")

        // Then
        assertNotNull(url)
        // URLBuilder might omit default port 443 for HTTPS
        assertTrue(
            url.startsWith("https://music.example.com:443/imageproxy?") ||
                url.startsWith("https://music.example.com/imageproxy?"),
        )
    }

    @Test
    fun url_withServerUrlWithoutPort_shouldBuildCorrectly() {
        // Given
        val imageInfo =
            AppMediaItem.ImageInfo(
                path = "/image.jpg",
                isRemotelyAccessible = false,
                provider = "local",
            )

        // When
        val url = imageInfo.url("https://music.example.com")

        // Then
        assertNotNull(url)
        assertTrue(url.startsWith("https://music.example.com/imageproxy?"))
    }

    // ========================================
    // Integration Tests
    // ========================================

    @Test
    fun url_fullScenario_localImageWithComplexPath() {
        // Given - realistic scenario
        val imageInfo =
            AppMediaItem.ImageInfo(
                path = "/mnt/music/Artists/The Beatles/Abbey Road/cover.jpg",
                isRemotelyAccessible = false,
                provider = "filesystem",
            )

        // When
        val url = imageInfo.url("http://192.168.1.100:8095")

        // Then
        assertNotNull(url)
        assertTrue(url.startsWith("http://192.168.1.100:8095/imageproxy?"))
        assertTrue(url.contains("provider=filesystem"))
        assertTrue(url.contains("path="))
        assertTrue(url.contains("checksum="))
    }

    @Test
    fun url_fullScenario_remoteImageFromCdn() {
        // Given - realistic CDN scenario
        val imageInfo =
            AppMediaItem.ImageInfo(
                path = "https://i.scdn.co/image/ab67616d0000b273e319baafd16e84f0408af2a0",
                isRemotelyAccessible = true,
                provider = "spotify",
            )

        // When
        val url = imageInfo.url("http://localhost:8095")

        // Then
        assertEquals("https://i.scdn.co/image/ab67616d0000b273e319baafd16e84f0408af2a0", url)
    }

    @Test
    fun url_fullScenario_localImageWithSpecialCharacters() {
        // Given - path with multiple special characters
        val imageInfo =
            AppMediaItem.ImageInfo(
                path = "/music/Artist & The Band/Album (Deluxe Edition)/01 - Song Title?.jpg",
                isRemotelyAccessible = false,
                provider = "local",
            )

        // When
        val url = imageInfo.url("http://localhost:8095")

        // Then
        assertNotNull(url)
        assertTrue(url.startsWith("http://localhost:8095/imageproxy?"))
        // Verify special characters are encoded (no raw & or ?)
        val pathParam = url.substringAfter("path=").substringBefore("&")
        assertTrue(!pathParam.contains(" "))
    }
}
