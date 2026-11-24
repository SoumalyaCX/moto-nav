package com.motonav.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.motonav.MainActivity
import com.motonav.MotoNavApp
import com.motonav.R
import com.motonav.navigation.FerrostarBridge
import com.motonav.navigation.NavigationStatus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@AndroidEntryPoint
class NavigationService : Service() {

    companion object {
        const val NOTIFICATION_ID = 4001
        const val ACTION_START_NAVIGATION = "com.motonav.action.START_NAV"
        const val ACTION_STOP_NAVIGATION = "com.motonav.action.STOP_NAV"
    }

    @Inject
    lateinit var ferrostarBridge: FerrostarBridge

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()

        // Observe active navigation state and update notification in real-time
        ferrostarBridge.navigationState.onEach { status ->
            when (status) {
                is NavigationStatus.Navigating -> {
                    updateNotification(
                        title = status.currentManeuver.instruction,
                        content = "Next in ${status.currentManeuver.distanceFormatted} · Speed: ${status.currentSpeedKmh.toInt()} km/h"
                    )
                }
                is NavigationStatus.Arrived -> {
                    updateNotification(
                        title = "Destination Reached",
                        content = "You have arrived at your destination."
                    )
                }
                is NavigationStatus.Idle -> {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        }.launchIn(serviceScope)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_NAVIGATION) {
            ferrostarBridge.stopNavigation()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val initialNotification = buildNotification(
            title = "MotoNav Active Ride",
            content = "Tracking motorcycle route and voice guidance..."
        )
        startForeground(NOTIFICATION_ID, initialNotification)

        return START_STICKY
    }

    private fun updateNotification(title: String, content: String) {
        val notification = buildNotification(title, content)
        val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(title: String, content: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, NavigationService::class.java).apply {
            action = ACTION_STOP_NAVIGATION
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, MotoNavApp.NAVIGATION_NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW) // Silent to prevent sound clash with TTS
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "End Ride", stopPendingIntent)
            .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
