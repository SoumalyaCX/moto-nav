package com.motonav.data.local

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.motonav.MainActivity
import com.motonav.MotoNavApp
import com.motonav.data.model.MapRegion
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineStateNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mapFileManager: MapFileManager
) {
    companion object {
        private const val TAG = "OfflineStateNotifier"
        const val NOTIFICATION_ID = 7001
        const val AUTO_DISMISS_DURATION_MS = 5000L
        const val EXTRA_NAVIGATE_TO = "navigate_to"
        const val DEST_DOWNLOADS = "downloads"
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val _pendingAlert = MutableStateFlow<MapRegion?>(null)
    val pendingAlert: StateFlow<MapRegion?> = _pendingAlert.asStateFlow()

    private var autoDismissJob: Job? = null
    private var lastNotifiedRegionId: String? = null

    /**
     * Checks if current user location is within an Indian state/region that has not yet been downloaded.
     * If un-downloaded, triggers a system push notification and an in-app banner.
     * Both can be dismissed by the user or will automatically dismiss after 5 seconds.
     */
    fun checkLocationAndNotify(lat: Double, lng: Double, scope: CoroutineScope) {
        val region = mapFileManager.findRegionForLocation(lat, lng) ?: return

        val isDownloaded = mapFileManager.isRegionDownloaded(region.id)
        if (isDownloaded) {
            // User is in a downloaded region; reset notified tracking
            if (lastNotifiedRegionId != null) {
                lastNotifiedRegionId = null
            }
            return
        }

        // Avoid continuous spamming on every GPS tick while staying in the same un-downloaded state
        if (lastNotifiedRegionId == region.id) {
            return
        }

        lastNotifiedRegionId = region.id
        Log.i(TAG, "Location is in un-downloaded state: ${region.displayName}. Triggering 5s auto-dismiss notification.")

        // 1. Post Android System Notification
        postSystemNotification(region)

        // 2. Set in-app heads-up alert state
        _pendingAlert.value = region

        // 3. Schedule auto-dismiss after exactly 5 seconds
        autoDismissJob?.cancel()
        autoDismissJob = scope.launch {
            delay(AUTO_DISMISS_DURATION_MS)
            Log.d(TAG, "Auto-dismissing offline state alert after 5 seconds.")
            dismissAlert()
        }
    }

    private fun postSystemNotification(region: MapRegion) {
        try {
            // Check permission on Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val hasPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (!hasPermission) {
                    Log.w(TAG, "POST_NOTIFICATIONS permission not granted; in-app banner will still display.")
                    return
                }
            }

            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_NAVIGATE_TO, DEST_DOWNLOADS)
                putExtra("region_id", region.id)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                NOTIFICATION_ID,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, MotoNavApp.OFFLINE_MAP_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_map)
                .setContentTitle("🗺️ Offline Map Missing: ${region.displayName}")
                .setContentText("You entered ${region.displayName}. Tap to download map & POIs for 100% offline navigation.")
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText("You entered ${region.displayName}. Download the offline vector map and POIs to enable full search and routing without cellular data.")
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .addAction(
                    android.R.drawable.stat_sys_download,
                    "Download Map",
                    pendingIntent
                )

            notificationManager.notify(NOTIFICATION_ID, builder.build())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to post system notification: ${e.message}", e)
        }
    }

    /**
     * Dismisses both the system push notification and the in-app banner immediately.
     * Can be invoked directly by user interaction (dismiss button / tap) or automatically after 5s.
     */
    fun dismissAlert() {
        autoDismissJob?.cancel()
        autoDismissJob = null
        _pendingAlert.value = null
        try {
            notificationManager.cancel(NOTIFICATION_ID)
        } catch (e: Exception) {
            Log.w(TAG, "Error cancelling notification: ${e.message}")
        }
    }
}
