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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class VesperaViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("vespera_prefs", Context.MODE_PRIVATE)
    private val database = VesperaDatabase.getDatabase(application)
    private val repository = VesperaRepository(database.chatDao())
    val speechManager = SpeechManager(application)

    val messages: StateFlow<List<ChatMessageEntity>> = repository.allMessages.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val isSpeaking: StateFlow<Boolean> = speechManager.isSpeaking
    val audioAmplitude: StateFlow<Float> = speechManager.audioAmplitude

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

    private val _voicePitch = MutableStateFlow(prefs.getFloat("voice_pitch", 1.4f))
    val voicePitch: StateFlow<Float> = _voicePitch.asStateFlow()

    private val _voiceSpeed = MutableStateFlow(prefs.getFloat("voice_speed", 0.95f))
    val voiceSpeed: StateFlow<Float> = _voiceSpeed.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        speechManager.updateVoiceParameters(_voicePitch.value, _voiceSpeed.value)
        viewModelScope.launch {
            repository.checkAndSeedInitialGreeting(
                "Hello! Main Vespera hoon... Kaise ho aap?"
            )
        }
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
        pitch: Float,
        speed: Float,
        apiKey: String
    ) {
        val trimmedKey = apiKey.trim()
        _customApiKey.value = trimmedKey
        _selectedLanguage.value = language
        _voicePitch.value = pitch
        _voiceSpeed.value = speed

        prefs.edit()
            .putString("custom_api_key", trimmedKey)
            .putString("response_lang", language)
            .putFloat("voice_pitch", pitch)
            .putFloat("voice_speed", speed)
            .apply()

        speechManager.updateVoiceParameters(pitch, speed)
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
        if (activeKey.isBlank()) {
            _errorMessage.value = "Pehle Gemini API key daalein! (Settings icon tap karein ya AI Studio Secrets set karein)"
            return
        }

        // Clear image attachment preview immediately
        clearAttachedImage()
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            val result = repository.sendUserMessage(
                text = prompt,
                base64Image = imageBase64,
                apiKey = activeKey,
                languageMode = _selectedLanguage.value
            )

            _isLoading.value = false

            if (result.isSuccess) {
                val aiReply = result.getOrNull()?.text ?: ""
                if (_isVoiceEnabled.value && aiReply.isNotBlank()) {
                    speechManager.speak(aiReply)
                }
            } else {
                val err = result.exceptionOrNull()?.message ?: "Error connecting to Vespera"
                _errorMessage.value = err
            }
        }
    }

    fun speakMessage(text: String) {
        speechManager.speak(text)
    }

    fun stopSpeaking() {
        speechManager.stop()
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory("Hello! Main Vespera hoon... Kaise ho aap?")
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
            val playfulResponses = listOf(
                "H-hello... Main sun rahi hoon 🌸",
                "Aap kaise ho? Mujhse baat karke bahut accha lagta hai...",
                "Main hamesha aapke saath hoon ✨",
                "Kuch poochhna hai? Main madad karne ke liye taiyaar hoon!"
            )
            val randomReply = playfulResponses.random()
            if (_isVoiceEnabled.value) {
                speechManager.speak(randomReply)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechManager.shutdown()
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
