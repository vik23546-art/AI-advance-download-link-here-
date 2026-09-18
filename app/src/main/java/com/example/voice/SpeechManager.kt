package com.example.voice

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sin

class SpeechManager(context: Context) : TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "SpeechManager"
        private const val UTTERANCE_ID = "VesperaUtterance"
    }

    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _audioAmplitude = MutableStateFlow(0f)
    val audioAmplitude: StateFlow<Float> = _audioAmplitude.asStateFlow()

    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private var amplitudeJob: Job? = null

    var voicePitch: Float = 1.6f
    var voiceSpeed: Float = 0.95f

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val ttsEngine = tts ?: return

            // Configure Hindi for Hinglish/Hindi accents, fallback to default/English
            val hindi = Locale("hi", "IN")
            val result = ttsEngine.setLanguage(hindi)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                ttsEngine.setLanguage(Locale.getDefault())
            }

            // Find soft/female voice if available (similar to prototype's voice selection)
            try {
                val availableVoices = ttsEngine.voices
                if (availableVoices != null) {
                    val femaleVoice = availableVoices.find { voice ->
                        voice.name.contains("Google hi-IN", ignoreCase = true) ||
                        voice.name.contains("Samantha", ignoreCase = true) ||
                        voice.name.contains("female", ignoreCase = true) ||
                        voice.name.contains("Zira", ignoreCase = true)
                    }
                    if (femaleVoice != null) {
                        ttsEngine.voice = femaleVoice
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "Voice selection exception, using default engine voice")
            }

            ttsEngine.setPitch(voicePitch)
            ttsEngine.setSpeechRate(voiceSpeed)

            ttsEngine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                    startAudioAmplitudeSimulation()
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                    stopAudioAmplitudeSimulation()
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                    stopAudioAmplitudeSimulation()
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    _isSpeaking.value = false
                    stopAudioAmplitudeSimulation()
                }
            })

            isTtsReady = true
            Log.d(TAG, "TTS initialized successfully with Hinata pitch $voicePitch, speed $voiceSpeed")
        } else {
            Log.e(TAG, "Failed to initialize TTS, status: $status")
        }
    }

    fun updateVoiceParameters(pitch: Float, speed: Float) {
        voicePitch = pitch
        voiceSpeed = speed
        tts?.setPitch(pitch)
        tts?.setSpeechRate(speed)
    }

    fun speak(rawText: String, pitch: Float = voicePitch, speed: Float = voiceSpeed) {
        if (!isTtsReady || tts == null) return

        // Clean text: strip emojis, markdown asterisks and URLs for fluent reading
        val cleanedText = sanitizeForSpeech(rawText)
        if (cleanedText.isBlank()) return

        stop()

        tts?.setPitch(pitch)
        tts?.setSpeechRate(speed)

        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, UTTERANCE_ID)
        }

        tts?.speak(cleanedText, TextToSpeech.QUEUE_FLUSH, params, UTTERANCE_ID)
    }

    fun stop() {
        tts?.stop()
        _isSpeaking.value = false
        stopAudioAmplitudeSimulation()
    }

    fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
        isTtsReady = false
    }

    private fun startAudioAmplitudeSimulation() {
        amplitudeJob?.cancel()
        amplitudeJob = coroutineScope.launch {
            var step = 0f
            while (isActive && _isSpeaking.value) {
                step += 0.18f
                // Multi-frequency harmonic envelope simulating human speech audio spectrum
                val rawWave = (sin(step.toDouble()) * 0.5 + sin(step * 2.3) * 0.3 + sin(step * 0.7) * 0.2).toFloat()
                val amp = (abs(rawWave) * 0.85f) + 0.15f
                _audioAmplitude.value = amp.coerceIn(0.1f, 1.0f)
                delay(35) // ~30Hz update rate
            }
            _audioAmplitude.value = 0f
        }
    }

    private fun stopAudioAmplitudeSimulation() {
        amplitudeJob?.cancel()
        amplitudeJob = null
        _audioAmplitude.value = 0f
    }

    private fun sanitizeForSpeech(text: String): String {
        return text
            .replace(Regex("\\*+"), "") // markdown bold/italic
            .replace(Regex("`[^`]*`"), "") // code
            .replace(Regex("https?://\\S+"), "") // urls
            .replace(Regex("[\\p{So}\\p{Cn}]"), "") // symbols/emojis
            .trim()
    }
}
