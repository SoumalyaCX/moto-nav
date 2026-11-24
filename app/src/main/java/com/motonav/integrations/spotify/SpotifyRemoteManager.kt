package com.motonav.integrations.spotify

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import com.motonav.integrations.whatsapp.WANotificationService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpotifyRemoteManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "SpotifyRemoteManager"
        const val SPOTIFY_PACKAGE_NAME = "com.spotify.music"
    }

    private val _playerState = MutableStateFlow(SpotifyPlayerState())
    val playerState: StateFlow<SpotifyPlayerState> = _playerState.asStateFlow()

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val sessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    private var activeMediaController: MediaController? = null

    init {
        findAndAttachMediaSession()
    }

    /**
     * Inspects active media sessions for Spotify or primary audio player.
     */
    fun findAndAttachMediaSession() {
        try {
            val component = ComponentName(context, WANotificationService::class.java)
            val controllers = sessionManager.getActiveSessions(component)
            val spotifyController = controllers.firstOrNull { it.packageName == SPOTIFY_PACKAGE_NAME }
                ?: controllers.firstOrNull()

            if (spotifyController != null) {
                activeMediaController = spotifyController
                val metadata = spotifyController.metadata
                val pbState = spotifyController.playbackState

                val title = metadata?.getString(android.media.MediaMetadata.METADATA_KEY_TITLE) ?: "Track"
                val artist = metadata?.getString(android.media.MediaMetadata.METADATA_KEY_ARTIST) ?: "Artist"
                val isPlaying = pbState?.state == PlaybackState.STATE_PLAYING

                _playerState.value = SpotifyPlayerState(
                    trackTitle = title,
                    artistName = artist,
                    isPlaying = isPlaying,
                    isConnected = true
                )

                spotifyController.registerCallback(object : MediaController.Callback() {
                    override fun onPlaybackStateChanged(state: PlaybackState?) {
                        val playing = state?.state == PlaybackState.STATE_PLAYING
                        _playerState.value = _playerState.value.copy(isPlaying = playing)
                    }

                    override fun onMetadataChanged(meta: android.media.MediaMetadata?) {
                        val t = meta?.getString(android.media.MediaMetadata.METADATA_KEY_TITLE) ?: "Track"
                        val a = meta?.getString(android.media.MediaMetadata.METADATA_KEY_ARTIST) ?: "Artist"
                        _playerState.value = _playerState.value.copy(trackTitle = t, artistName = a)
                    }
                })
            }
        } catch (e: SecurityException) {
            Log.d(TAG, "Notification listener permission not yet granted: ${e.message}")
            // Fallback mode without permission using system media key dispatch
            _playerState.value = _playerState.value.copy(isConnected = isSpotifyInstalled())
        }
    }

    fun togglePlayPause() {
        if (activeMediaController != null) {
            val pbState = activeMediaController?.playbackState?.state
            if (pbState == PlaybackState.STATE_PLAYING) {
                activeMediaController?.transportControls?.pause()
                _playerState.value = _playerState.value.copy(isPlaying = false)
            } else {
                activeMediaController?.transportControls?.play()
                _playerState.value = _playerState.value.copy(isPlaying = true)
            }
        } else {
            // Send standard hardware media key event (works with Spotify, Bluetooth intercom)
            sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            _playerState.value = _playerState.value.copy(isPlaying = !_playerState.value.isPlaying)
        }
    }

    fun skipNext() {
        if (activeMediaController != null) {
            activeMediaController?.transportControls?.skipToNext()
        } else {
            sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_NEXT)
        }
    }

    fun skipPrevious() {
        if (activeMediaController != null) {
            activeMediaController?.transportControls?.skipToPrevious()
        } else {
            sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
        }
    }

    fun launchSpotifyApp() {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(SPOTIFY_PACKAGE_NAME)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
        }
    }

    fun isSpotifyInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo(SPOTIFY_PACKAGE_NAME, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun sendMediaKeyEvent(keyCode: Int) {
        val eventTime = SystemClock.uptimeMillis()
        val downEvent = KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN, keyCode, 0)
        val upEvent = KeyEvent(eventTime, eventTime, KeyEvent.ACTION_UP, keyCode, 0)

        audioManager.dispatchMediaKeyEvent(downEvent)
        audioManager.dispatchMediaKeyEvent(upEvent)
    }
}
