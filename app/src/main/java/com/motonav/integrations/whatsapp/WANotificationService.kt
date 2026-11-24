package com.motonav.integrations.whatsapp

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class WANotificationService : NotificationListenerService() {

    companion object {
        private const val TAG = "WANotificationService"
        val WHATSAPP_PACKAGES = setOf("com.whatsapp", "com.whatsapp.w4b")

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
        if (sbn == null || sbn.packageName !in WHATSAPP_PACKAGES) return

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

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
