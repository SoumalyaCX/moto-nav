package com.motonav.integrations.spotify

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
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

    private val albumArtCache = mutableMapOf<String, Bitmap>()
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val spotifyBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            if (intent == null) return
            when (intent.action) {
                "com.spotify.music.metadatachanged" -> {
                    val track = intent.getStringExtra("track")
                        ?: intent.getStringExtra("track_name")
                        ?: intent.getStringExtra("title")
                        ?: intent.getCharSequenceExtra("track")?.toString()
                    val artist = intent.getStringExtra("artist")
                        ?: intent.getStringExtra("artist_name")
                        ?: intent.getStringExtra("album_artist")
                        ?: intent.getCharSequenceExtra("artist")?.toString()
                    val isPlaying = intent.getBooleanExtra("playing", _playerState.value.isPlaying)
                    val length = intent.getIntExtra("length", 0) * 1000L
                    val trackId = intent.getStringExtra("id") ?: ""

                    Log.d(TAG, "Spotify metadata broadcast: track=$track, artist=$artist, playing=$isPlaying, id=$trackId")
                    _playerState.update { current ->
                        current.copy(
                            trackTitle = track?.takeIf { it.isNotBlank() } ?: current.trackTitle,
                            artistName = artist?.takeIf { it.isNotBlank() } ?: current.artistName,
                            isPlaying = isPlaying,
                            durationMs = if (length > 0) length else current.durationMs,
                            isConnected = true
                        )
                    }

                    // Fetch album art if not already present
                    if (trackId.isNotBlank()) {
                        fetchAlbumArtAsync(trackId)
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
        isPlaying: Boolean?,
        trackId: String? = null
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

        if (albumArt == null && !trackId.isNullOrBlank()) {
            fetchAlbumArtAsync(trackId)
        }
    }

    /**
     * Fetches public Spotify album artwork and metadata via oEmbed API when not available from notification extras.
     */
    fun fetchAlbumArtAsync(trackUriOrId: String) {
        val cleanId = trackUriOrId
            .substringAfterLast("/")
            .substringAfterLast(":")
            .substringBefore("?")
            .trim()
        if (cleanId.isBlank()) return

        val cached = albumArtCache[cleanId]
        if (cached != null) {
            _playerState.update { it.copy(albumArt = cached) }
            return
        }

        coroutineScope.launch {
            try {
                val endpoint = "https://open.spotify.com/oembed?url=https://open.spotify.com/track/$cleanId"
                val conn = URL(endpoint).openConnection() as HttpURLConnection
                conn.connectTimeout = 4000
                conn.readTimeout = 4000
                conn.requestMethod = "GET"
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:109.0) Gecko/114.0 Firefox/114.0")

                if (conn.responseCode == 200) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(body)
                    val thumbUrl = json.optString("thumbnail_url")
                    val authorName = json.optString("author_name")
                    val oembedTitle = json.optString("title")

                    if (authorName.isNotBlank()) {
                        _playerState.update { current ->
                            if (current.artistName in listOf("Open Spotify", "Spotify Music", "Unknown Artist", "")) {
                                current.copy(artistName = authorName)
                            } else current
                        }
                    }
                    if (oembedTitle.isNotBlank()) {
                        _playerState.update { current ->
                            if (current.trackTitle in listOf("No music playing", "")) {
                                current.copy(trackTitle = oembedTitle)
                            } else current
                        }
                    }

                    if (thumbUrl.isNotBlank()) {
                        val imgConn = URL(thumbUrl).openConnection() as HttpURLConnection
                        imgConn.connectTimeout = 4000
                        imgConn.readTimeout = 4000
                        val bitmap = BitmapFactory.decodeStream(imgConn.inputStream)
                        if (bitmap != null) {
                            albumArtCache[cleanId] = bitmap
                            _playerState.update { it.copy(albumArt = bitmap) }
                            Log.d(TAG, "Loaded Spotify album art thumbnail for $cleanId")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "oEmbed album art fetch failed: ${e.message}")
            }
        }
    }

    /**
     * Loads artwork from local content:// or remote http(s):// URI asynchronously.
     */
    fun loadArtFromUriAsync(artUri: String) {
        if (artUri.isBlank()) return
        val cached = albumArtCache[artUri]
        if (cached != null) {
            _playerState.update { it.copy(albumArt = cached) }
            return
        }

        coroutineScope.launch {
            try {
                val bitmap: Bitmap? = if (artUri.startsWith("content://") || artUri.startsWith("android.resource://")) {
                    val uri = android.net.Uri.parse(artUri)
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                } else if (artUri.startsWith("http://") || artUri.startsWith("https://")) {
                    val conn = URL(artUri).openConnection() as HttpURLConnection
                    conn.connectTimeout = 4000
                    conn.readTimeout = 4000
                    conn.setRequestProperty("User-Agent", "MotoNav/1.0")
                    if (conn.responseCode == 200) {
                        conn.inputStream.use { stream ->
                            BitmapFactory.decodeStream(stream)
                        }
                    } else null
                } else null

                if (bitmap != null) {
                    albumArtCache[artUri] = bitmap
                    _playerState.update { it.copy(albumArt = bitmap) }
                    Log.d(TAG, "Successfully loaded album art from URI: $artUri")
                }
            } catch (e: Exception) {
                Log.d(TAG, "Failed loading art from URI ($artUri): ${e.message}")
            }
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
                    val title = meta.getText(android.media.MediaMetadata.METADATA_KEY_TITLE)?.toString()
                        ?: meta.getText(android.media.MediaMetadata.METADATA_KEY_DISPLAY_TITLE)?.toString()

                    val artist = meta.getText(android.media.MediaMetadata.METADATA_KEY_ARTIST)?.toString()
                        ?: meta.getText(android.media.MediaMetadata.METADATA_KEY_ALBUM_ARTIST)?.toString()
                        ?: meta.getText(android.media.MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)?.toString()
                        ?: meta.getText(android.media.MediaMetadata.METADATA_KEY_AUTHOR)?.toString()
                        ?: meta.getText(android.media.MediaMetadata.METADATA_KEY_COMPOSER)?.toString()
                        ?: meta.getText(android.media.MediaMetadata.METADATA_KEY_ALBUM)?.toString()

                    var art = meta.getBitmap(android.media.MediaMetadata.METADATA_KEY_ALBUM_ART)
                        ?: meta.getBitmap(android.media.MediaMetadata.METADATA_KEY_ART)
                        ?: meta.getBitmap(android.media.MediaMetadata.METADATA_KEY_DISPLAY_ICON)

                    val artUri = meta.getString(android.media.MediaMetadata.METADATA_KEY_ALBUM_ART_URI)
                        ?: meta.getString(android.media.MediaMetadata.METADATA_KEY_ART_URI)
                        ?: meta.getString(android.media.MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI)

                    val mediaId = meta.getString(android.media.MediaMetadata.METADATA_KEY_MEDIA_ID)
                        ?: meta.getString(android.media.MediaMetadata.METADATA_KEY_MEDIA_URI)

                    _playerState.update { current ->
                        current.copy(
                            trackTitle = title?.takeIf { it.isNotBlank() } ?: current.trackTitle,
                            artistName = artist?.takeIf { it.isNotBlank() } ?: current.artistName,
                            albumArt = art ?: current.albumArt
                        )
                    }

                    if (art == null) {
                        if (!artUri.isNullOrBlank()) {
                            loadArtFromUriAsync(artUri)
                        } else if (!mediaId.isNullOrBlank()) {
                            fetchAlbumArtAsync(mediaId)
                        }
                    }
                }
            }
        })
    }

    private fun syncControllerMetadata(controller: MediaController) {
        val metadata = controller.metadata
        val pbState = controller.playbackState

        val title = metadata?.getText(android.media.MediaMetadata.METADATA_KEY_TITLE)?.toString()
            ?: metadata?.getText(android.media.MediaMetadata.METADATA_KEY_DISPLAY_TITLE)?.toString()

        val artist = metadata?.getText(android.media.MediaMetadata.METADATA_KEY_ARTIST)?.toString()
            ?: metadata?.getText(android.media.MediaMetadata.METADATA_KEY_ALBUM_ARTIST)?.toString()
            ?: metadata?.getText(android.media.MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)?.toString()
            ?: metadata?.getText(android.media.MediaMetadata.METADATA_KEY_AUTHOR)?.toString()
            ?: metadata?.getText(android.media.MediaMetadata.METADATA_KEY_COMPOSER)?.toString()
            ?: metadata?.getText(android.media.MediaMetadata.METADATA_KEY_ALBUM)?.toString()

        var art = metadata?.getBitmap(android.media.MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata?.getBitmap(android.media.MediaMetadata.METADATA_KEY_ART)
            ?: metadata?.getBitmap(android.media.MediaMetadata.METADATA_KEY_DISPLAY_ICON)

        val artUri = metadata?.getString(android.media.MediaMetadata.METADATA_KEY_ALBUM_ART_URI)
            ?: metadata?.getString(android.media.MediaMetadata.METADATA_KEY_ART_URI)
            ?: metadata?.getString(android.media.MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI)

        val mediaId = metadata?.getString(android.media.MediaMetadata.METADATA_KEY_MEDIA_ID)
            ?: metadata?.getString(android.media.MediaMetadata.METADATA_KEY_MEDIA_URI)

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

        if (art == null) {
            if (!artUri.isNullOrBlank()) {
                loadArtFromUriAsync(artUri)
            } else if (!mediaId.isNullOrBlank()) {
                fetchAlbumArtAsync(mediaId)
            }
        }
    }

    /**
     * Executes Play or Pause cleanly without rapid double-triggering or command collisions.
     */
    fun togglePlayPause() {
        val isActuallyPlaying = (activeMediaController?.playbackState?.state == PlaybackState.STATE_PLAYING)
            || audioManager.isMusicActive
            || _playerState.value.isPlaying

        var commandExecuted = false

        // Method 1: MediaController transport controls (Exact and authoritative)
        if (activeMediaController != null) {
            try {
                if (isActuallyPlaying) {
                    activeMediaController?.transportControls?.pause()
                } else {
                    activeMediaController?.transportControls?.play()
                }
                commandExecuted = true
            } catch (e: Exception) {
                Log.w(TAG, "Transport controls toggle failed: ${e.message}")
            }
        }

        // Method 2: Notification Action PendingIntent (Only if MediaController wasn't connected)
        if (!commandExecuted && playPausePendingIntent != null) {
            try {
                playPausePendingIntent?.send()
                commandExecuted = true
            } catch (e: Exception) {
                Log.d(TAG, "playPause pending intent send failed: ${e.message}")
            }
        }

        // Method 3: Targeted Media Key or Widget Broadcast
        if (!commandExecuted) {
            val keyCode = if (isActuallyPlaying) KeyEvent.KEYCODE_MEDIA_PAUSE else KeyEvent.KEYCODE_MEDIA_PLAY
            sendSpotifyMediaKey(keyCode)

            val widgetAction = if (isActuallyPlaying) {
                "com.spotify.mobile.android.ui.widget.PAUSE"
            } else {
                "com.spotify.mobile.android.ui.widget.PLAY"
            }
            try {
                val intent = Intent(widgetAction).apply {
                    setPackage(SPOTIFY_PACKAGE_NAME)
                }
                context.sendBroadcast(intent)
            } catch (ignored: Exception) {}
        }

        _playerState.update { it.copy(isPlaying = !isActuallyPlaying) }
    }

    fun skipNext() {
        var commandExecuted = false
        if (activeMediaController != null) {
            try {
                activeMediaController?.transportControls?.skipToNext()
                commandExecuted = true
            } catch (e: Exception) {
                Log.w(TAG, "Transport controls skipToNext failed: ${e.message}")
            }
        }

        if (!commandExecuted && nextPendingIntent != null) {
            try {
                nextPendingIntent?.send()
                commandExecuted = true
            } catch (e: Exception) {
                Log.d(TAG, "next pending intent send failed: ${e.message}")
            }
        }

        if (!commandExecuted) {
            sendSpotifyMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT)
            try {
                val intent = Intent("com.spotify.mobile.android.ui.widget.NEXT").apply {
                    setPackage(SPOTIFY_PACKAGE_NAME)
                }
                context.sendBroadcast(intent)
            } catch (ignored: Exception) {}
        }
    }

    fun skipPrevious() {
        var commandExecuted = false
        if (activeMediaController != null) {
            try {
                activeMediaController?.transportControls?.skipToPrevious()
                commandExecuted = true
            } catch (e: Exception) {
                Log.w(TAG, "Transport controls skipToPrevious failed: ${e.message}")
            }
        }

        if (!commandExecuted && prevPendingIntent != null) {
            try {
                prevPendingIntent?.send()
                commandExecuted = true
            } catch (e: Exception) {
                Log.d(TAG, "prev pending intent send failed: ${e.message}")
            }
        }

        if (!commandExecuted) {
            sendSpotifyMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
            try {
                val intent = Intent("com.spotify.mobile.android.ui.widget.PREVIOUS").apply {
                    setPackage(SPOTIFY_PACKAGE_NAME)
                }
                context.sendBroadcast(intent)
            } catch (ignored: Exception) {}
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
