package io.music_assistant.client.auto

import android.content.ContentResolver
import android.net.Uri
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import androidx.media.MediaBrowserServiceCompat
import io.music_assistant.client.data.model.server.ServerMediaItem

class AutoLibrary {

    fun getItems(
        id: String,
        result: MediaBrowserServiceCompat.Result<List<MediaBrowserCompat.MediaItem>>
    ) {
        println("Lib items: $id")
        when (id) {
            MediaIds.ROOT -> {
                result.sendResult(
                    listOf(
                        rootTabItem("Artists", MediaIds.TAB_ARTISTS),
                        rootTabItem("Playlists", MediaIds.TAB_PLAYLISTS)
                    )
                )
            }

            MediaIds.TAB_ARTISTS -> {
                // Load and return list of artists
                result.sendResult(null)
            }

            MediaIds.TAB_PLAYLISTS -> {
                // Load and return list of albums
                result.sendResult(null)
            }

            else -> {
                result.sendResult(null)
            }
        }
    }

    fun search(
        query: String,
        result: MediaBrowserServiceCompat.Result<List<MediaBrowserCompat.MediaItem>>
    ) {
        println("Lib search: $query")
        result.sendResult(null)
    }


    private fun rootTabItem(tabName: String, tabId: String): MediaBrowserCompat.MediaItem =
        MediaBrowserCompat.MediaItem(
            MediaDescriptionCompat.Builder()
                .setTitle(tabName)
                .setMediaId(tabId)
                .build(), MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
        )
}

object MediaIds {
    const val ROOT = "root"
    const val TAB_ARTISTS = "artists"
    const val TAB_PLAYLISTS = "albums"
}

private fun ServerMediaItem.toAutoMediaItem(): MediaBrowserCompat.MediaItem =
    MediaBrowserCompat.MediaItem(
        MediaDescriptionCompat.Builder()
            .setMediaId(itemId)
            .setTitle(name)
            .setSubtitle("Subtitle")
            //.setMediaUri(Uri.parse("uri"))
            //.setIconUri()
            .build(),
        MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
    )