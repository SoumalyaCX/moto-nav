package com.motonav.integrations.whatsapp

import android.app.Notification
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.session.MediaSession
import android.os.Build
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.graphics.drawable.toBitmap
import com.motonav.integrations.spotify.SpotifyRemoteManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class WANotificationService : NotificationListenerService() {

    companion object {
        private const val TAG = "WANotificationService"
        val WHATSAPP_PACKAGES = setOf("com.whatsapp", "com.whatsapp.w4b")
        const val SPOTIFY_PACKAGE = SpotifyRemoteManager.SPOTIFY_PACKAGE_NAME

        private val _receivedLocationMessages = MutableStateFlow<List<WALocationMessage>>(emptyList())
        val receivedLocationMessages: StateFlow<List<WALocationMessage>> = _receivedLocationMessages.asStateFlow()

        fun clearMessage(id: String) {
            _receivedLocationMessages.update { list ->
                list.filterNot { it.id == id }
            }
        }

        fun clearAll() {
            _receivedLocationMessages.value = emptyList()
        }

        /**
         * Checks if the user has enabled notification listener permission for MotoNav.
         */
        fun isNotificationAccessGranted(context: Context): Boolean {
            val packageName = context.packageName
            val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            if (!flat.isNullOrEmpty()) {
                val names = flat.split(":")
                for (name in names) {
                    val cn = ComponentName.unflattenFromString(name)
                    if (cn != null && cn.packageName == packageName) {
                        return true
                    }
                }
            }
            return false
        }

        /**
         * Intent to redirect user to Android Notification Access settings.
         */
        fun getNotificationSettingsIntent(): Intent {
            return Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        // 1. Check for Spotify Music Player notification
        if (sbn.packageName == SPOTIFY_PACKAGE) {
            handleSpotifyNotification(notification, extras)
            return
        }

        // 2. Check for WhatsApp Location Sharing
        if (sbn.packageName in WHATSAPP_PACKAGES) {
            handleWhatsAppNotification(notification, extras)
        }
    }

    private fun handleSpotifyNotification(notification: Notification, extras: android.os.Bundle) {
        try {
            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            val artist = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
                ?: extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()

            val albumArt: Bitmap? = try {
                val direct = extras.get(Notification.EXTRA_LARGE_ICON)
                when (direct) {
                    is Bitmap -> direct
                    is android.graphics.drawable.Icon -> direct.loadDrawable(this)?.toBitmap()
                    else -> {
                        val pic = extras.get(Notification.EXTRA_PICTURE)
                        when (pic) {
                            is Bitmap -> pic
                            is android.graphics.drawable.Icon -> pic.loadDrawable(this)?.toBitmap()
                            else -> notification.getLargeIcon()?.loadDrawable(this)?.toBitmap()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed extracting album art: ${e.message}")
                null
            }

            val sessionToken: MediaSession.Token? = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    extras.getParcelable(Notification.EXTRA_MEDIA_SESSION, MediaSession.Token::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    extras.getParcelable(Notification.EXTRA_MEDIA_SESSION)
                }
            } catch (e: Exception) {
                null
            }

            val trackId = extras.getString("com.spotify.music.track_id")
                ?: extras.getString("track_id")

            var playPauseIntent: PendingIntent? = null
            var nextIntent: PendingIntent? = null
            var prevIntent: PendingIntent? = null
            var isPlaying: Boolean? = null

            val actions = notification.actions
            if (actions != null) {
                for (action in actions) {
                    val actionTitle = action.title?.toString()?.lowercase() ?: ""
                    if (actionTitle.contains("pause")) {
                        isPlaying = true
                        playPauseIntent = action.actionIntent
                    } else if (actionTitle.contains("play")) {
                        isPlaying = false
                        playPauseIntent = action.actionIntent
                    } else if (actionTitle.contains("next") || actionTitle.contains("forward")) {
                        nextIntent = action.actionIntent
                    } else if (actionTitle.contains("prev") || actionTitle.contains("rewind") || actionTitle.contains("back")) {
                        prevIntent = action.actionIntent
                    }
                }

                // Positional fallback for compact notifications without textual action titles
                if (actions.size == 3) {
                    if (prevIntent == null) prevIntent = actions[0].actionIntent
                    if (playPauseIntent == null) playPauseIntent = actions[1].actionIntent
                    if (nextIntent == null) nextIntent = actions[2].actionIntent
                } else if (actions.size >= 5) {
                    if (prevIntent == null) prevIntent = actions[1].actionIntent
                    if (playPauseIntent == null) playPauseIntent = actions[2].actionIntent
                    if (nextIntent == null) nextIntent = actions[3].actionIntent
                }
            }

            SpotifyRemoteManager.getInstance()?.onSpotifyNotification(
                title = title,
                artist = artist,
                albumArt = albumArt,
                sessionToken = sessionToken,
                playPauseAction = playPauseIntent,
                nextAction = nextIntent,
                prevAction = prevIntent,
                isPlaying = isPlaying,
                trackId = trackId
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error processing Spotify notification: ${e.message}", e)
        }
    }

    private fun handleWhatsAppNotification(notification: Notification, extras: android.os.Bundle) {
        val sender = extras.getString(Notification.EXTRA_TITLE) ?: "WhatsApp Contact"
        val textSeq = extras.getCharSequence(Notification.EXTRA_TEXT) ?: return
        val messageText = textSeq.toString()

        val locationLink = LocationLinkParser.extractLocationLink(messageText)
        if (locationLink != null) {
            Log.i(TAG, "Location link detected from $sender: $locationLink")
            val coords = LocationLinkParser.parseCoordinates(locationLink)

            val locationMsg = WALocationMessage(
                sender = sender,
                rawText = messageText,
                locationLink = locationLink,
                parsedCoordinate = coords
            )

            _receivedLocationMessages.update { current ->
                // Keep the most recent 5 location messages
                listOf(locationMsg) + current.filterNot { it.locationLink == locationLink }.take(4)
            }
        }
    }
}
