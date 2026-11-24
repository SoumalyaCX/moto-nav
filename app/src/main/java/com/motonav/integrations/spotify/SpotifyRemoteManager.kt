package com.motonav.integrations.spotify

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.media.AudioManager
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import androidx.core.content.ContextCompat
import com.motonav.integrations.whatsapp.WANotificationService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpotifyRemoteManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "SpotifyRemoteManager"
        const val SPOTIFY_PACKAGE_NAME = "com.spotify.music"

        @Volatile
        private var instance: SpotifyRemoteManager? = null

        fun getInstance(): SpotifyRemoteManager? = instance
    }

    private val _playerState = MutableStateFlow(SpotifyPlayerState())
    val playerState: StateFlow<SpotifyPlayerState> = _playerState.asStateFlow()

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val sessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    private var activeMediaController: MediaController? = null

    private var playPausePendingIntent: PendingIntent? = null
    private var nextPendingIntent: PendingIntent? = null
    private var prevPendingIntent: PendingIntent? = null

    private val spotifyBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            if (intent == null) return
            when (intent.action) {
                "com.spotify.music.metadatachanged" -> {
                    val track = intent.getStringExtra("track")
                        ?: intent.getStringExtra("track_name")
                        ?: intent.getCharSequenceExtra("track")?.toString()
                    val artist = intent.getStringExtra("artist")
                        ?: intent.getStringExtra("artist_name")
                        ?: intent.getCharSequenceExtra("artist")?.toString()
                    val isPlaying = intent.getBooleanExtra("playing", _playerState.value.isPlaying)
                    val length = intent.getIntExtra("length", 0) * 1000L

                    Log.d(TAG, "Spotify metadata broadcast: track=$track, artist=$artist, playing=$isPlaying")
                    _playerState.update { current ->
                        current.copy(
                            trackTitle = track?.takeIf { it.isNotBlank() } ?: current.trackTitle,
                            artistName = artist?.takeIf { it.isNotBlank() } ?: current.artistName,
                            isPlaying = isPlaying,
                            durationMs = if (length > 0) length else current.durationMs,
                            isConnected = true
                        )
                    }
                    if (activeMediaController == null) {
                        findAndAttachMediaSession()
                    }
                }

                "com.spotify.music.playbackstatechanged" -> {
                    val isPlaying = intent.getBooleanExtra("playing", false)
                    val position = intent.getLongExtra("playbackPosition", 0L)
                    Log.d(TAG, "Spotify playback broadcast: isPlaying=$isPlaying, position=$position")
                    _playerState.update { current ->
                        current.copy(
                            isPlaying = isPlaying,
                            positionMs = position,
                            isConnected = true
                        )
                    }
                }

                "com.spotify.music.queuechanged" -> {
                    findAndAttachMediaSession()
                }
            }
        }
    }

    init {
        instance = this

        // Register dynamic broadcast receiver for Spotify's standard broadcasts
        try {
            val filter = IntentFilter().apply {
                addAction("com.spotify.music.metadatachanged")
                addAction("com.spotify.music.playbackstatechanged")
                addAction("com.spotify.music.queuechanged")
            }
            ContextCompat.registerReceiver(
                context,
                spotifyBroadcastReceiver,
                filter,
                ContextCompat.RECEIVER_EXPORTED
            )
            Log.d(TAG, "Registered Spotify dynamic broadcast receiver")
        } catch (e: Exception) {
            Log.w(TAG, "Failed registering Spotify dynamic receiver: ${e.message}")
        }

        // Try attaching active media sessions
        findAndAttachMediaSession()

        // Register active sessions changed listener if notification permission is present
        try {
            val component = ComponentName(context, WANotificationService::class.java)
            sessionManager.addOnActiveSessionsChangedListener({ controllers ->
                val spotifyController = controllers?.firstOrNull { it.packageName == SPOTIFY_PACKAGE_NAME }
                    ?: controllers?.firstOrNull()
                if (spotifyController != null) {
                    attachMediaSession(spotifyController)
                }
            }, component)
        } catch (e: SecurityException) {
            Log.d(TAG, "Notification listener permission not yet granted: ${e.message}")
            _playerState.update { it.copy(isConnected = isSpotifyInstalled()) }
        }
    }

    /**
     * Called by WANotificationService when Spotify posts or updates its notification.
     */
    fun onSpotifyNotification(
        title: String?,
        artist: String?,
        albumArt: Bitmap?,
        sessionToken: MediaSession.Token?,
        playPauseAction: PendingIntent?,
        nextAction: PendingIntent?,
        prevAction: PendingIntent?,
        isPlaying: Boolean?
    ) {
        if (playPauseAction != null) playPausePendingIntent = playPauseAction
        if (nextAction != null) nextPendingIntent = nextAction
        if (prevAction != null) prevPendingIntent = prevAction

        if (sessionToken != null && (activeMediaController == null || activeMediaController?.sessionToken != sessionToken)) {
            try {
                val controller = MediaController(context, sessionToken)
                attachMediaSession(controller)
            } catch (e: Exception) {
                Log.w(TAG, "Could not create MediaController from token: ${e.message}")
            }
        }

        _playerState.update { current ->
            current.copy(
                trackTitle = title?.takeIf { it.isNotBlank() } ?: current.trackTitle,
                artistName = artist?.takeIf { it.isNotBlank() } ?: current.artistName,
                albumArt = albumArt ?: current.albumArt,
                isPlaying = isPlaying ?: current.isPlaying,
                isConnected = true
            )
        }
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
                attachMediaSession(spotifyController)
            }
        } catch (e: SecurityException) {
            Log.d(TAG, "Notification listener permission not yet granted: ${e.message}")
            _playerState.update { it.copy(isConnected = isSpotifyInstalled()) }
        }
    }

    private fun attachMediaSession(controller: MediaController) {
        activeMediaController = controller
        syncControllerMetadata(controller)

        controller.registerCallback(object : MediaController.Callback() {
            override fun onPlaybackStateChanged(state: PlaybackState?) {
                val playing = state?.state == PlaybackState.STATE_PLAYING
                _playerState.update { it.copy(isPlaying = playing) }
            }

            override fun onMetadataChanged(meta: android.media.MediaMetadata?) {
                if (meta != null) {
                    val title = meta.getString(android.media.MediaMetadata.METADATA_KEY_TITLE)
                    val artist = meta.getString(android.media.MediaMetadata.METADATA_KEY_ARTIST)
                    val art = meta.getBitmap(android.media.MediaMetadata.METADATA_KEY_ALBUM_ART)
                        ?: meta.getBitmap(android.media.MediaMetadata.METADATA_KEY_ART)
                        ?: meta.getBitmap(android.media.MediaMetadata.METADATA_KEY_DISPLAY_ICON)

                    _playerState.update { current ->
                        current.copy(
                            trackTitle = title?.takeIf { it.isNotBlank() } ?: current.trackTitle,
                            artistName = artist?.takeIf { it.isNotBlank() } ?: current.artistName,
                            albumArt = art ?: current.albumArt
                        )
                    }
                }
            }
        })
    }

    private fun syncControllerMetadata(controller: MediaController) {
        val metadata = controller.metadata
        val pbState = controller.playbackState

        val title = metadata?.getString(android.media.MediaMetadata.METADATA_KEY_TITLE)
        val artist = metadata?.getString(android.media.MediaMetadata.METADATA_KEY_ARTIST)
        val art = metadata?.getBitmap(android.media.MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata?.getBitmap(android.media.MediaMetadata.METADATA_KEY_ART)
            ?: metadata?.getBitmap(android.media.MediaMetadata.METADATA_KEY_DISPLAY_ICON)
        val isPlaying = pbState?.state == PlaybackState.STATE_PLAYING

        _playerState.update { current ->
            current.copy(
                trackTitle = title?.takeIf { it.isNotBlank() } ?: current.trackTitle,
                artistName = artist?.takeIf { it.isNotBlank() } ?: current.artistName,
                albumArt = art ?: current.albumArt,
                isPlaying = isPlaying,
                isConnected = true
            )
        }
    }

    fun togglePlayPause() {
        val currentPlaying = _playerState.value.isPlaying
        val willPlay = !currentPlaying

        // 1. MediaController transport controls
        if (activeMediaController != null) {
            try {
                if (currentPlaying) {
                    activeMediaController?.transportControls?.pause()
                } else {
                    activeMediaController?.transportControls?.play()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Transport controls toggle failed: ${e.message}")
            }
        }

        // 2. Notification Action PendingIntent fallback
        try {
            playPausePendingIntent?.send()
        } catch (e: Exception) {
            Log.d(TAG, "playPause pending intent send failed: ${e.message}")
        }

        // 3. Explicit media button broadcast targeted directly to Spotify
        sendSpotifyMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)

        // 4. Spotify widget broadcast actions
        val widgetAction = if (currentPlaying) {
            "com.spotify.mobile.android.ui.widget.PAUSE"
        } else {
            "com.spotify.mobile.android.ui.widget.PLAY"
        }
        try {
            val intent = Intent(widgetAction).apply {
                setPackage(SPOTIFY_PACKAGE_NAME)
            }
            context.sendBroadcast(intent)
        } catch (e: Exception) {
            Log.d(TAG, "Widget play/pause broadcast failed: ${e.message}")
        }

        // Optimistic UI state update
        _playerState.update { it.copy(isPlaying = willPlay) }
    }

    fun skipNext() {
        if (activeMediaController != null) {
            try {
                activeMediaController?.transportControls?.skipToNext()
            } catch (e: Exception) {
                Log.w(TAG, "Transport controls skipToNext failed: ${e.message}")
            }
        }

        try {
            nextPendingIntent?.send()
        } catch (e: Exception) {
            Log.d(TAG, "next pending intent send failed: ${e.message}")
        }

        sendSpotifyMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT)

        try {
            val intent = Intent("com.spotify.mobile.android.ui.widget.NEXT").apply {
                setPackage(SPOTIFY_PACKAGE_NAME)
            }
            context.sendBroadcast(intent)
        } catch (e: Exception) {
            Log.d(TAG, "Widget next broadcast failed: ${e.message}")
        }
    }

    fun skipPrevious() {
        if (activeMediaController != null) {
            try {
                activeMediaController?.transportControls?.skipToPrevious()
            } catch (e: Exception) {
                Log.w(TAG, "Transport controls skipToPrevious failed: ${e.message}")
            }
        }

        try {
            prevPendingIntent?.send()
        } catch (e: Exception) {
            Log.d(TAG, "prev pending intent send failed: ${e.message}")
        }

        sendSpotifyMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)

        try {
            val intent = Intent("com.spotify.mobile.android.ui.widget.PREVIOUS").apply {
                setPackage(SPOTIFY_PACKAGE_NAME)
            }
            context.sendBroadcast(intent)
        } catch (e: Exception) {
            Log.d(TAG, "Widget previous broadcast failed: ${e.message}")
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

    private fun sendSpotifyMediaKey(keyCode: Int) {
        try {
            val eventTime = SystemClock.uptimeMillis()
            val downEvent = KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN, keyCode, 0)
            val upEvent = KeyEvent(eventTime, eventTime, KeyEvent.ACTION_UP, keyCode, 0)

            val downIntent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
                setPackage(SPOTIFY_PACKAGE_NAME)
                putExtra(Intent.EXTRA_KEY_EVENT, downEvent)
            }
            val upIntent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
                setPackage(SPOTIFY_PACKAGE_NAME)
                putExtra(Intent.EXTRA_KEY_EVENT, upEvent)
            }

            context.sendOrderedBroadcast(downIntent, null)
            context.sendOrderedBroadcast(upIntent, null)

            // System audio manager fallback
            audioManager.dispatchMediaKeyEvent(downEvent)
            audioManager.dispatchMediaKeyEvent(upEvent)
        } catch (e: Exception) {
            Log.w(TAG, "Failed sending media key: ${e.message}")
        }
    }
}
