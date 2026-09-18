package com.example.voice

import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
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
import java.io.File
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sin

class SpeechManager(private val context: Context) : TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "SpeechManager"
        private const val UTTERANCE_ID = "HinataSpeechUtterance"
    }

    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var mediaPlayer: MediaPlayer? = null

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _audioAmplitude = MutableStateFlow(0f)
    val audioAmplitude: StateFlow<Float> = _audioAmplitude.asStateFlow()

    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private var amplitudeJob: Job? = null

    // Natural human female speaking pitch (1.22f base ensures clearly feminine register, never male/boy)
    var voicePitch: Float = 1.22f
    var voiceSpeed: Float = 0.96f

    init {
        initTtsEngine()
    }

    private fun initTtsEngine() {
        try {
            // Check available TTS engines and prioritize Google TTS (com.google.android.tts)
            // for the highest-fidelity natural human neural voice
            val tempTts = TextToSpeech(context.applicationContext, null)
            val engines = tempTts.engines
            val googleEngine = engines?.find { it.name.equals("com.google.android.tts", ignoreCase = true) }?.name
            tempTts.shutdown()

            if (googleEngine != null) {
                Log.d(TAG, "Initializing with Google Speech Services engine: $googleEngine")
                tts = TextToSpeech(context.applicationContext, this, googleEngine)
            } else {
                Log.d(TAG, "Using default platform TTS engine")
                tts = TextToSpeech(context.applicationContext, this)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing custom engine, falling back to default", e)
            tts = TextToSpeech(context.applicationContext, this)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val ttsEngine = tts ?: return

            // Prefer Hindi (hi-IN) or Indian English for natural pronunciation
            val hindi = Locale.forLanguageTag("hi-IN")
            val result = ttsEngine.setLanguage(hindi)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                val inEnglish = Locale.forLanguageTag("en-IN")
                if (ttsEngine.setLanguage(inEnglish) == TextToSpeech.LANG_NOT_SUPPORTED) {
                    ttsEngine.setLanguage(Locale.US)
                }
            }

            // Strictly select sweet, genuine human girl voice (100% filter out male voices)
            selectNaturalGirlVoice(ttsEngine)

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
            Log.d(TAG, "Natural girl TTS ready. Pitch=$voicePitch, Speed=$voiceSpeed")
        } else {
            Log.e(TAG, "Failed to initialize TTS, status: $status")
        }
    }

    /**
     * Determines whether a TTS voice is a male or forbidden voice.
     * Prevents Android's default Indian male voice (hi-in-x-hia) from ever speaking!
     */
    private fun isForbiddenMaleVoice(voice: Voice): Boolean {
        val name = voice.name.lowercase()
        if (name.contains("male") || name.contains("man") || name.contains("boy") || name.contains("guy")) return true
        // Google TTS known Indian & US male identifiers:
        if (name.contains("-hia") || name.contains("-hib") || name.contains("-hif")) return true
        if (name.contains("-sfd") || name.contains("-iol") || name.contains("-gmc") || name.contains("-chm")) return true
        return false
    }

    private fun selectNaturalGirlVoice(ttsEngine: TextToSpeech) {
        try {
            val availableVoices = ttsEngine.voices ?: return

            // Priority 1: Google Neural/WaveNet high-quality Hindi female voices
            val preferredHindiFemale = listOf(
                "hi-in-x-hie-network",
                "hi-in-x-hie-local",
                "hi-in-x-hid-network",
                "hi-in-x-hid-local",
                "hi-in-x-hic-network",
                "hi-in-x-hic-local"
            )

            for (target in preferredHindiFemale) {
                val voice = availableVoices.find { it.name.equals(target, ignoreCase = true) }
                if (voice != null && !isForbiddenMaleVoice(voice)) {
                    ttsEngine.voice = voice
                    Log.d(TAG, "Selected Neural Hindi Female Voice: ${voice.name}")
                    return
                }
            }

            // Priority 2: Indian English / British / US natural female voices (sweet & melodious)
            val preferredEnglishFemale = listOf(
                "en-in-x-ene-network",
                "en-in-x-ene-local",
                "en-in-x-cfl-network",
                "en-in-x-cfl-local",
                "en-in-x-ahp-network",
                "en-in-x-ahp-local",
                "en-in-x-end-network",
                "en-in-x-end-local",
                "en-us-x-sfg-network",
                "en-us-x-sfg-local",
                "en-us-x-tpf-network",
                "en-us-x-tpf-local",
                "en-us-x-iom-network",
                "en-us-x-iom-local",
                "en-gb-x-rjs-network",
                "en-gb-x-rjs-local"
            )

            for (target in preferredEnglishFemale) {
                val voice = availableVoices.find { it.name.equals(target, ignoreCase = true) }
                if (voice != null && !isForbiddenMaleVoice(voice)) {
                    ttsEngine.voice = voice
                    Log.d(TAG, "Selected Natural English Female Voice: ${voice.name}")
                    return
                }
            }

            // Priority 3: Scan all voices for explicit female identifiers (excluding any male)
            val genericFemale = availableVoices.filter { voice ->
                !isForbiddenMaleVoice(voice) &&
                (voice.name.contains("female", ignoreCase = true) ||
                 voice.name.contains("woman", ignoreCase = true) ||
                 voice.name.contains("girl", ignoreCase = true) ||
                 voice.name.contains("hie", ignoreCase = true) ||
                 voice.name.contains("hid", ignoreCase = true) ||
                 voice.name.contains("ene", ignoreCase = true) ||
                 voice.name.contains("sfg", ignoreCase = true))
            }.maxByOrNull { it.quality }

            if (genericFemale != null) {
                ttsEngine.voice = genericFemale
                Log.d(TAG, "Selected Generic Female Voice: ${genericFemale.name}")
                return
            }

            // Priority 4: If the current voice is a male voice, switch immediately to English female default
            val current = ttsEngine.voice
            if (current != null && isForbiddenMaleVoice(current)) {
                Log.w(TAG, "Current default voice is MALE (${current.name}), forcing switch away from male voice")
                val anyNonMale = availableVoices.firstOrNull { !isForbiddenMaleVoice(it) }
                if (anyNonMale != null) {
                    ttsEngine.voice = anyNonMale
                } else {
                    ttsEngine.setLanguage(Locale.US)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Exception selecting natural female voice", e)
        }
    }

    /**
     * Plays high-fidelity real human audio generated directly by Gemini API (Prebuilt Kore / Aoede voice).
     */
    fun playAudioBytes(audioBytes: ByteArray, onComplete: (() -> Unit)? = null) {
        try {
            stop()
            val tempFile = File(context.cacheDir, "gemini_voice_temp.mp3")
            tempFile.outputStream().use { it.write(audioBytes) }

            mediaPlayer = MediaPlayer().apply {
                setDataSource(tempFile.absolutePath)
                setOnPreparedListener { mp ->
                    _isSpeaking.value = true
                    startAudioAmplitudeSimulation()
                    mp.start()
                }
                setOnCompletionListener { mp ->
                    _isSpeaking.value = false
                    stopAudioAmplitudeSimulation()
                    mp.release()
                    mediaPlayer = null
                    tempFile.delete()
                    onComplete?.invoke()
                }
                setOnErrorListener { mp, what, extra ->
                    Log.w(TAG, "MediaPlayer error ($what, $extra)")
                    _isSpeaking.value = false
                    stopAudioAmplitudeSimulation()
                    mp.release()
                    mediaPlayer = null
                    tempFile.delete()
                    false
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing audio bytes", e)
            _isSpeaking.value = false
            stopAudioAmplitudeSimulation()
        }
    }

    fun updateVoiceParameters(pitch: Float, speed: Float) {
        voicePitch = pitch.coerceAtLeast(1.18f)
        voiceSpeed = speed
        tts?.setPitch(voicePitch)
        tts?.setSpeechRate(voiceSpeed)
    }

    fun speak(rawText: String, pitch: Float = voicePitch, speed: Float = voiceSpeed) {
        if (!isTtsReady || tts == null) return

        // Clean text: strip prefixes, video tags, emojis, markdown asterisks and URLs
        val cleanedText = sanitizeForSpeech(rawText)
        if (cleanedText.isBlank()) return

        stop()

        // Strict fail-safe: Enforce minimum 1.18f pitch to guarantee sweet female vocal tone (never deep/boy)
        val safePitch = pitch.coerceAtLeast(1.18f)
        tts?.setPitch(safePitch)
        tts?.setSpeechRate(speed)

        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, UTTERANCE_ID)
        }

        tts?.speak(cleanedText, TextToSpeech.QUEUE_FLUSH, params, UTTERANCE_ID)
    }

    fun stop() {
        try {
            tts?.stop()
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
            mediaPlayer = null
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping audio", e)
        }
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
                val rawWave = (sin(step.toDouble()) * 0.5 + sin(step * 2.3) * 0.3 + sin(step * 0.7) * 0.2).toFloat()
                val amp = (abs(rawWave) * 0.85f) + 0.15f
                _audioAmplitude.value = amp.coerceIn(0.1f, 1.0f)
                delay(35)
            }
            _audioAmplitude.value = 0f
        }
    }

    private fun stopAudioAmplitudeSimulation() {
        amplitudeJob?.cancel()
        amplitudeJob = null
        _audioAmplitude.value = 0f
    }

    fun sanitizeForSpeech(text: String): String {
        return text
            // Strip video tags completely (never read [VIDEO:...] aloud)
            .replace(Regex("\\[VIDEO:[^\\]]*\\]"), "")
            // Strip character / bot prefixes ("Hinata:", "Sakura:", "Tsunade:", etc.)
            .replace(Regex("^(Hinata|Sakura|Tsunade|AI|User):\\s*", RegexOption.IGNORE_CASE), "")
            // Strip stage actions / parentheticals like (blushes softly), *smiles*, (laughs)
            .replace(Regex("\\([^)]*\\)"), "")
            .replace(Regex("\\*[^*]*\\*"), "")
            // Strip URLs
            .replace(Regex("https?://\\S+"), "")
            // Strip code blocks
            .replace(Regex("`[^`]*`"), "")
            // Strip emojis & symbols
            .replace(Regex("[\\p{So}\\p{Cn}]"), "")
            // Replace multi-dots with natural breath pause comma
            .replace(Regex("\\.{2,}"), ", ")
            // Normalize spaces
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
