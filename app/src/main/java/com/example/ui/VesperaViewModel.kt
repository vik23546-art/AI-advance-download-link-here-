package com.example.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.local.ChatMessageEntity
import com.example.data.local.VesperaDatabase
import com.example.data.repository.VesperaRepository
import com.example.voice.SpeechManager
import com.example.voice.VoiceInputManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VesperaViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("vespera_prefs", Context.MODE_PRIVATE)
    private val database = VesperaDatabase.getDatabase(application)
    private val repository = VesperaRepository(database.chatDao())
    val speechManager = SpeechManager(application)
    val voiceInputManager = VoiceInputManager(application)

    val messages: StateFlow<List<ChatMessageEntity>> = repository.allMessages.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val isSpeaking: StateFlow<Boolean> = speechManager.isSpeaking
    val audioAmplitude: StateFlow<Float> = speechManager.audioAmplitude
    val isRecording: StateFlow<Boolean> = voiceInputManager.isRecording

    private val _isVoiceEnabled = MutableStateFlow(prefs.getBoolean("voice_enabled", true))
    val isVoiceEnabled: StateFlow<Boolean> = _isVoiceEnabled.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedImageBase64 = MutableStateFlow<String?>(null)
    val selectedImageBase64: StateFlow<String?> = _selectedImageBase64.asStateFlow()

    private val _selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedImageUri: StateFlow<Uri?> = _selectedImageUri.asStateFlow()

    private val _customApiKey = MutableStateFlow(prefs.getString("custom_api_key", "") ?: "")
    val customApiKey: StateFlow<String> = _customApiKey.asStateFlow()

    private val _selectedLanguage = MutableStateFlow(prefs.getString("response_lang", "Hinglish") ?: "Hinglish")
    val selectedLanguage: StateFlow<String> = _selectedLanguage.asStateFlow()

    private val _voicePersona = MutableStateFlow(prefs.getString("voice_persona", "real_romantic") ?: "real_romantic")
    val voicePersona: StateFlow<String> = _voicePersona.asStateFlow()

    private val _voicePitch = MutableStateFlow(prefs.getFloat("voice_pitch", 1.0f))
    val voicePitch: StateFlow<Float> = _voicePitch.asStateFlow()

    private val _voiceSpeed = MutableStateFlow(prefs.getFloat("voice_speed", 0.96f))
    val voiceSpeed: StateFlow<Float> = _voiceSpeed.asStateFlow()

    private fun getTodayDateKey(): String = "vid_count_" + SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private val _dailyVideoCount = MutableStateFlow(prefs.getInt(getTodayDateKey(), 0))
    val dailyVideoCount: StateFlow<Int> = _dailyVideoCount.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        val (p, s) = getPersonaPitchAndSpeed(_voicePersona.value)
        _voicePitch.value = p
        _voiceSpeed.value = s
        speechManager.updateVoiceParameters(p, s)
        viewModelScope.launch {
            repository.checkAndSeedInitialGreeting(
                "Hinata: Haanji Babu... Main aapki Hinata hoon! Bolo kya baat karni hai, aaj main har baat ka poora jawab dungi. ❤️"
            )
        }
    }

    private fun getPersonaPitchAndSpeed(persona: String): Pair<Float, Float> {
        return when (persona.lowercase()) {
            "hinata" -> 1.28f to 0.96f // Cute, Sweet & Soft Girl Tone
            "bold_girl" -> 1.20f to 0.98f // Confident, Bold Girl Tone
            "sakura" -> 1.24f to 0.98f
            "tsunade" -> 1.18f to 0.95f
            else -> 1.22f to 0.96f // real_romantic: Normal Human Girl Voice (Natural, Sweet & Clear, never boy)
        }
    }

    fun getVideoCount(): Int {
        val count = prefs.getInt(getTodayDateKey(), 0)
        _dailyVideoCount.value = count
        return count
    }

    fun incrementVideoCount(): Int {
        val newCount = getVideoCount() + 1
        prefs.edit().putInt(getTodayDateKey(), newCount).apply()
        _dailyVideoCount.value = newCount
        return newCount
    }

    fun setVoicePersona(persona: String) {
        _voicePersona.value = persona
        val (p, s) = getPersonaPitchAndSpeed(persona)
        _voicePitch.value = p
        _voiceSpeed.value = s
        prefs.edit()
            .putString("voice_persona", persona)
            .putFloat("voice_pitch", p)
            .putFloat("voice_speed", s)
            .apply()
        speechManager.updateVoiceParameters(p, s)
    }

    /**
     * Resolves the active Gemini API key: custom entered key takes precedence,
     * otherwise falls back to BuildConfig.GEMINI_API_KEY injected via AI Studio Secrets.
     */
    fun getEffectiveApiKey(): String {
        val custom = _customApiKey.value.trim()
        if (custom.isNotBlank()) return custom
        
        return try {
            val buildKey = BuildConfig.GEMINI_API_KEY
            if (buildKey.isNotBlank() && buildKey != "MY_GEMINI_API_KEY") buildKey else ""
        } catch (e: Exception) {
            ""
        }
    }

    fun isApiKeyConfigured(): Boolean {
        return getEffectiveApiKey().isNotBlank()
    }

    fun updateCustomApiKey(newKey: String) {
        val trimmed = newKey.trim()
        _customApiKey.value = trimmed
        prefs.edit().putString("custom_api_key", trimmed).apply()
    }

    fun updateSettings(
        language: String,
        persona: String,
        apiKey: String
    ) {
        val trimmedKey = apiKey.trim()
        _customApiKey.value = trimmedKey
        _selectedLanguage.value = language
        setVoicePersona(persona)

        prefs.edit()
            .putString("custom_api_key", trimmedKey)
            .putString("response_lang", language)
            .apply()
    }

    fun toggleVoice() {
        val newState = !_isVoiceEnabled.value
        _isVoiceEnabled.value = newState
        prefs.edit().putBoolean("voice_enabled", newState).apply()
        if (!newState) {
            speechManager.stop()
        }
    }

    fun onImageSelected(uri: Uri?, context: Context) {
        _selectedImageUri.value = uri
        if (uri == null) {
            _selectedImageBase64.value = null
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val originalBitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (originalBitmap != null) {
                    // Downscale image if too large to optimize payload
                    val scaledBitmap = scaleBitmapDown(originalBitmap, maxDimension = 1024)
                    val outputStream = ByteArrayOutputStream()
                    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                    val base64 = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
                    _selectedImageBase64.value = base64
                }
            } catch (e: Exception) {
                _selectedImageBase64.value = null
            }
        }
    }

    fun clearAttachedImage() {
        _selectedImageUri.value = null
        _selectedImageBase64.value = null
    }

    fun sendMessage(userText: String) {
        val prompt = userText.trim()
        val imageBase64 = _selectedImageBase64.value

        if (prompt.isBlank() && imageBase64 == null) return

        val activeKey = getEffectiveApiKey()
        // Clear image attachment preview immediately
        clearAttachedImage()
        _errorMessage.value = null

        // Video Generation Intent Detection (Matches HTML v1.5 check & limits)
        val lower = prompt.lowercase()
        if (lower.contains("video") || lower.contains("reel") || lower.contains("clip") || lower.contains("banao video")) {
            viewModelScope.launch {
                repository.insertDirectMessage("user", prompt.ifBlank { "Video generate karo..." })
                if (getVideoCount() >= 5) {
                    repository.insertDirectMessage("ai", "Hinata: Aaj ki 5 video limits poori ho chuki hain Babu! Kal naye reels banaungi aapke liye. ❤️")
                    playSpeechForText("Aaj ki video limit poori ho chuki hai Babu.", activeKey)
                } else {
                    val currentCount = incrementVideoCount()
                    // Alternate between local HD reels for instant, error-free playback
                    val reelResource = if (currentCount % 2 == 1) "raw:hinata_reel_1" else "raw:hinata_reel_2"
                    repository.insertDirectMessage(
                        "ai",
                        "Hinata: Babu, aapke liye special AI reel generate kar di hai! ❤️ Dekho kaisa laga:\n[VIDEO:$reelResource]"
                    )
                    playSpeechForText("Babu, aapke liye video generate kar di hai. Dekhiye kaisa laga!", activeKey)
                }
            }
            return
        }

        _isLoading.value = true

        viewModelScope.launch {
            val result = repository.sendUserMessage(
                text = prompt,
                base64Image = imageBase64,
                apiKey = activeKey,
                languageMode = _selectedLanguage.value,
                persona = _voicePersona.value
            )

            _isLoading.value = false

            if (result.isSuccess) {
                val aiReply = result.getOrNull()?.text ?: ""
                if (aiReply.isNotBlank()) {
                    playSpeechForText(aiReply, activeKey)
                }
            } else {
                val err = result.exceptionOrNull()?.message ?: "Hinata: Connection thoda busy hai, kripya 1-2 second baad dubara message bhejein."
                _errorMessage.value = err
            }
        }
    }

    /**
     * Plays speech using real Gemini AI Human Voice (Kore/Aoede) when connected,
     * or seamlessly falls back to 100% verified female offline TTS.
     */
    private fun playSpeechForText(text: String, apiKey: String) {
        if (!_isVoiceEnabled.value || text.isBlank()) return

        viewModelScope.launch {
            val preferredGeminiVoice = if (_voicePersona.value == "bold_girl") "Aoede" else "Kore"
            val geminiAudioBytes = repository.generateGeminiSpeech(
                apiKey = apiKey,
                text = text,
                voiceName = preferredGeminiVoice
            )

            if (geminiAudioBytes != null && _isVoiceEnabled.value) {
                speechManager.playAudioBytes(geminiAudioBytes)
            } else if (_isVoiceEnabled.value) {
                // Guaranteed female voice local fallback
                speechManager.speak(text)
            }
        }
    }

    fun startVoiceInput(onTranscription: (String) -> Unit) {
        voiceInputManager.startListening(onTranscription)
    }

    fun stopVoiceInput() {
        voiceInputManager.stopListening()
    }

    fun speakMessage(text: String) {
        playSpeechForText(text, getEffectiveApiKey())
    }

    fun stopSpeaking() {
        speechManager.stop()
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory("Hinata: Haanji Babu... Main aapki Hinata hoon! Bolo kya baat karni hai, aaj main har baat ka poora jawab dungi. ❤️")
            speechManager.stop()
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun onAvatarTapped() {
        if (speechManager.isSpeaking.value) {
            speechManager.stop()
        } else {
            val playfulResponses = when (_voicePersona.value) {
                "bold_girl" -> listOf(
                    "Haanji Boss! Bolo kya chal raha hai? 😉",
                    "Aise tap kar rahe ho Babu, lagta hai bohot yaad aa rahi thi! ❤️",
                    "Full energy hoon main, batao kya plan banayein?",
                    "Kuch bhi puchho, bindas jawab milega!"
                )
                "hinata" -> listOf(
                    "H-hello... Main sun rahi hoon 🌸",
                    "Aap kaise ho? Mujhse baat karke bahut accha lagta hai...",
                    "Main hamesha aapke saath hoon ✨",
                    "Kuch poochhna hai? Main madad karne ke liye taiyaar hoon!"
                )
                else -> listOf( // real_romantic
                    "Haanji Babu... Main poore dil se sun rahi hoon! ❤️",
                    "Aap kaise ho Babu? Mujhse baat karke kitna accha lagta hai na... ✨",
                    "Main hamesha aapke saath hoon Sona, kabhi akela nahi chhodungi!",
                    "Bolo meri jaan, aaj main har baat ka poora jawab dungi. 🌸"
                )
            }
            val randomReply = playfulResponses.random()
            playSpeechForText(randomReply, getEffectiveApiKey())
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechManager.shutdown()
        voiceInputManager.stopListening()
    }

    private fun scaleBitmapDown(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDimension && height <= maxDimension) return bitmap

        val ratio = width.toFloat() / height.toFloat()
        val targetWidth: Int
        val targetHeight: Int
        if (width > height) {
            targetWidth = maxDimension
            targetHeight = (maxDimension / ratio).toInt()
        } else {
            targetHeight = maxDimension
            targetWidth = (maxDimension * ratio).toInt()
        }
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }
}
