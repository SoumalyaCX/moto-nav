package com.motonav.integrations.spotify

import android.graphics.Bitmap

data class SpotifyPlayerState(
    val trackTitle: String = "No music playing",
    val artistName: String = "Open Spotify",
    val albumArt: Bitmap? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isConnected: Boolean = false
)
