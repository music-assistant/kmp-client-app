package ua.pp.formatbce.musicassistant.data.model.common

interface Player {
    val id: String
    val name: String
    val shouldBeShown: Boolean
    val canSetVolume: Boolean
    val currentQueueId: String?
    val isPlaying: Boolean
    val isAnnouncing: Boolean
}