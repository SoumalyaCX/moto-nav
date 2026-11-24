package com.motonav.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "motonav_settings")

enum class DarkModePreference {
    AUTO,   // Automatic by local time / sunrise-sunset
    DARK,   // Forced dark (optimal for OLED and night rides)
    LIGHT   // Forced light
}

@Singleton
class PreferencesStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val KEY_DARK_MODE = stringPreferencesKey("dark_mode_pref")
        private val KEY_ACTIVE_REGION = stringPreferencesKey("active_region_id")
        private val KEY_RIDING_STYLE = stringPreferencesKey("riding_style")
        private val KEY_BATTERY_RIDE_MODE = booleanPreferencesKey("battery_ride_mode_enabled")
        private val KEY_BLUETOOTH_AUDIO_DUCKING = booleanPreferencesKey("bluetooth_audio_ducking")
        private val KEY_OFFLINE_ONLY = booleanPreferencesKey("offline_only_mode")
        private val KEY_AVOID_HIGHWAYS = booleanPreferencesKey("avoid_highways")
        private val KEY_AVOID_TOLLS = booleanPreferencesKey("avoid_tolls")
    }

    val darkModePreferenceFlow: Flow<DarkModePreference> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_DARK_MODE] ?: DarkModePreference.AUTO.name
        try {
            DarkModePreference.valueOf(raw)
        } catch (e: Exception) {
            DarkModePreference.AUTO
        }
    }

    val activeRegionIdFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_ACTIVE_REGION] ?: MapFileManager.PREPACKAGED_REGION_ID
    }

    val ridingStyleFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_RIDING_STYLE] ?: "SPORT" // Default to curvy roads
    }

    val isBatteryRideModeEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_BATTERY_RIDE_MODE] ?: true
    }

    val isBluetoothDuckingEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_BLUETOOTH_AUDIO_DUCKING] ?: true
    }

    val isOfflineOnlyModeFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_OFFLINE_ONLY] ?: false
    }

    val avoidHighwaysFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_AVOID_HIGHWAYS] ?: true // Motorcycle default: prefer twisty state highways over interstates
    }

    val avoidTollsFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_AVOID_TOLLS] ?: false
    }

    suspend fun setDarkModePreference(preference: DarkModePreference) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DARK_MODE] = preference.name
        }
    }

    suspend fun setActiveRegionId(regionId: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ACTIVE_REGION] = regionId
        }
    }

    suspend fun setRidingStyle(style: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_RIDING_STYLE] = style
        }
    }

    suspend fun setBatteryRideModeEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_BATTERY_RIDE_MODE] = enabled
        }
    }

    suspend fun setOfflineOnlyMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_OFFLINE_ONLY] = enabled
        }
    }

    suspend fun setAvoidHighways(avoid: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_AVOID_HIGHWAYS] = avoid
        }
    }

    /**
     * Resolves effective theme mode considering Auto setting vs Manual override.
     */
    fun shouldRenderDarkTheme(pref: DarkModePreference): Boolean {
        return when (pref) {
            DarkModePreference.DARK -> true
            DarkModePreference.LIGHT -> false
            DarkModePreference.AUTO -> {
                val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                hour < 6 || hour >= 18 // Dark from 6 PM to 6 AM
            }
        }
    }
}
