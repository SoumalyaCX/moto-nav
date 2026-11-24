package com.motonav.integrations.spotify

data class SpotifyPlayerState(
    val trackTitle: String = "No music playing",
    val artistName: String = "Open Spotify",
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isConnected: Boolean = false
)
