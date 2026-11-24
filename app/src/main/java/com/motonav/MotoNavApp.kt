package com.motonav

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.motonav.data.local.MapFileManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class MotoNavApp : Application() {

    companion object {
        const val NAVIGATION_NOTIFICATION_CHANNEL_ID = "motonav_active_navigation"
        const val LOCATION_ALERTS_CHANNEL_ID = "motonav_location_alerts"
    }

    @Inject
    lateinit var mapFileManager: MapFileManager

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()

        // Asynchronously initialize prepackaged map data (West Bengal)
        applicationScope.launch {
            mapFileManager.ensurePrepackagedRegionExtracted()
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            // High priority channel for navigation guidance
            val navChannel = NotificationChannel(
                NAVIGATION_NOTIFICATION_CHANNEL_ID,
                getString(R.string.navigation_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.navigation_channel_desc)
                setShowBadge(false)
            }

            // WhatsApp peek alert channel
            val alertChannel = NotificationChannel(
                LOCATION_ALERTS_CHANNEL_ID,
                "Location Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Alerts for incoming location links from WhatsApp"
            }

            notificationManager.createNotificationChannel(navChannel)
            notificationManager.createNotificationChannel(alertChannel)
        }
    }
}
