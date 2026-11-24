package com.motonav.navigation

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.motonav.data.local.PreferencesStore
import com.motonav.data.model.RouteManeuver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceGuidanceManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesStore: PreferencesStore,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "VoiceGuidanceManager"
    }

    private var tts: TextToSpeech? = null
    private var isReady = false
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var activeFocusRequest: AudioFocusRequest? = null

    init {
        initializeTts()
    }

    private fun initializeTts() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // Configure Indian English for motorcycle road names in India
                val locale = Locale("en", "IN")
                val langResult = tts?.setLanguage(locale)
                if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    // Fallback to default English
                    tts?.language = Locale.ENGLISH
                }

                // Configure AudioAttributes specifically for Navigation Guidance
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                tts?.setAudioAttributes(audioAttributes)

                // Speech rate: slightly faster (1.1x) to keep instructions concise while riding
                tts?.setSpeechRate(1.1f)
                tts?.setPitch(1.0f)

                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        Log.d(TAG, "Speech started: $utteranceId")
                    }

                    override fun onDone(utteranceId: String?) {
                        releaseAudioFocus()
                    }

                    override fun onError(utteranceId: String?) {
                        releaseAudioFocus()
                    }
                })

                isReady = true
                Log.i(TAG, "TTS initialized successfully.")
            } else {
                Log.e(TAG, "TTS initialization failed with code: $status")
            }
        }
    }

    /**
     * Announces upcoming turn at distance threshold (e.g. 500m, 200m).
     */
    fun announceUpcomingManeuver(maneuver: RouteManeuver, distanceRemainingMeters: Double) {
        val distText = if (distanceRemainingMeters >= 1000) {
            String.format("%.1f kilometers", distanceRemainingMeters / 1000)
        } else {
            "${distanceRemainingMeters.toInt()} meters"
        }
        val text = "In $distText, ${maneuver.instruction}"
        speak(text)
    }

    /**
     * Announces immediate turn action (e.g. at 50m).
     */
    fun announceImmediateAction(maneuver: RouteManeuver) {
        speak(maneuver.instruction)
    }

    /**
     * Announces off-route / recalculating.
     */
    fun announceRerouting() {
        speak("Recalculating route")
    }

    private fun speak(text: String) {
        if (!isReady || tts == null) return

        scope.launch {
            val shouldDuck = preferencesStore.isBluetoothDuckingEnabledFlow.first()
            if (shouldDuck) {
                requestAudioFocusWithDucking()
            }

            val utteranceId = UUID.randomUUID().toString()
            // QUEUE_FLUSH ensures fresh turn guidance immediately overrides any old speech
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        }
    }

    private fun requestAudioFocusWithDucking() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val playbackAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()

            activeFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(playbackAttributes)
                .setAcceptsDelayedFocusGain(false)
                .setOnAudioFocusChangeListener { focusChange ->
                    Log.d(TAG, "Audio focus change: $focusChange")
                }
                .build()

            activeFocusRequest?.let { audioManager.requestAudioFocus(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_NOTIFICATION,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            )
        }
    }

    private fun releaseAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            activeFocusRequest?.let {
                audioManager.abandonAudioFocusRequest(it)
                activeFocusRequest = null
            }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isReady = false
    }
}
