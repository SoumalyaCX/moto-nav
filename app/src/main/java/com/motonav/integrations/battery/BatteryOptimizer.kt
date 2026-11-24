package com.motonav.integrations.battery

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.BatteryManager
import android.os.PowerManager
import android.provider.Settings
import android.view.WindowManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class BatteryRideStatus(
    val percentage: Int = 100,
    val isCharging: Boolean = false,
    val isPowerSaveMode: Boolean = false,
    val isIgnoringOptimizations: Boolean = false
)

@Singleton
class BatteryOptimizer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val WHITELISTED_PACKAGES = setOf(
            "com.spotify.music",
            "com.whatsapp",
            "com.whatsapp.w4b"
        )
    }

    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val _batteryStatus = MutableStateFlow(BatteryRideStatus())
    val batteryStatus: StateFlow<BatteryRideStatus> = _batteryStatus.asStateFlow()

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            intent?.let { updateBatteryState(it) }
        }
    }

    init {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val initialIntent = context.registerReceiver(batteryReceiver, filter)
        initialIntent?.let { updateBatteryState(it) }
    }

    private fun updateBatteryState(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val pct = if (level >= 0 && scale > 0) (level * 100) / scale else -1

        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        val isIgnoring = powerManager.isIgnoringBatteryOptimizations(context.packageName)

        _batteryStatus.value = BatteryRideStatus(
            percentage = pct,
            isCharging = isCharging,
            isPowerSaveMode = powerManager.isPowerSaveMode,
            isIgnoringOptimizations = isIgnoring
        )
    }

    /**
     * Activates Ride Mode for motorcycle navigation.
     * Keeps screen awake and locks optimal 70% brightness.
     */
    fun activateRideMode(activity: Activity) {
        // Keep screen on during motorcycle ride
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Calibrate brightness to 75% for outdoor readability with battery balance
        val layoutParams = activity.window.attributes
        layoutParams.screenBrightness = 0.75f
        activity.window.attributes = layoutParams

        // Prompt for battery optimization exemption if not already granted
        requestBatteryExemptionIfRequired(activity)
    }

    fun deactivateRideMode(activity: Activity) {
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Restore system default brightness
        val layoutParams = activity.window.attributes
        layoutParams.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        activity.window.attributes = layoutParams
    }

    fun requestBatteryExemptionIfRequired(activity: Activity) {
        if (!powerManager.isIgnoringBatteryOptimizations(context.packageName)) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                activity.startActivity(intent)
            } catch (e: Exception) {
                // Ignore if device restricted
            }
        }
    }
}
