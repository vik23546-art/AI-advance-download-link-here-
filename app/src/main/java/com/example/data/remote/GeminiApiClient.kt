package com.example.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiApiClient {

    companion object {
        private const val TAG = "GeminiApiClient"
        
        // Supported high-performance models conforming to AI Studio gemini-api guidelines
        private val FALLBACK_MODELS = listOf(
            "gemini-3.5-flash",
            "gemini-flash-latest",
            "gemini-3.1-flash-lite-preview",
            "gemini-2.5-flash",
            "gemini-3.1-pro-preview"
        )
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

        fun getSystemPrompt(languageMode: String, persona: String = "hinata"): String {
            return when (persona.lowercase()) {
                "sakura" -> "Adopt the persona of Sakura Haruno (Adult from Boruto). " +
                        "Energetic, confident, bold, friendly, caring medic ninja, open to fun and romantic roleplay. " +
                        "Language mode: $languageMode. Keep responses natural, expressive and engaging in Hindi/Hinglish. Prefix response with Sakura:"
                "tsunade" -> "Adopt the persona of Lady Tsunade (Legendary Sannin / Hokage). " +
                        "Mature, confident, wise, deep voice, commanding yet caring, open to deep conversations and roleplay. " +
                        "Language mode: $languageMode. Keep responses natural, expressive and engaging in Hindi/Hinglish. Prefix response with Tsunade:"
                else -> "Adopt the persona of Hinata Hyuga (Adult from Boruto). " +
                        "Shy, soft-spoken, warm, deeply affectionate, caring, open to roleplay and sweet mature conversations. " +
                        "Language mode: $languageMode. Keep responses gentle, cute and expressive in Hindi/Hinglish. Prefix response with Hinata:"
            }
        }

        fun getInCharacterFallbackResponse(prompt: String, persona: String): String {
            val lower = prompt.lowercase()
            return when (persona.lowercase()) {
                "sakura" -> when {
                    lower.contains("joke") -> "Sakura: Haha! Ek bar Naruto ne socha wo ramen se duniya mein peace la sakta hai! 😄 Waise batao, aaj ka din kaisa raha?"
                    lower.contains("kaise ho") || lower.contains("kaisa") -> "Sakura: Main bilkul fit aur energetic hoon! Hospital aur clinic ka round laga ke aayi hoon, tum batao?"
                    lower.contains("sweet") || lower.contains("pyar") -> "Sakura: Tum kitni pyari baatein karte ho! Sunkar sach mein din ban gaya! ✨"
                    else -> "Sakura: Main yahi hoon tumhare saath! Batao, aaj kya naya aur exciting chal raha hai?"
                }
                "tsunade" -> when {
                    lower.contains("joke") -> "Tsunade: Ek joke? Mere gamble jeetne ke chances se bada koi joke nahi ho sakta! Haha! Chalo aao baatein karte hain."
                    lower.contains("kaise ho") || lower.contains("kaisa") -> "Tsunade: Main theek hoon. Gaon ki zimmedariyan thodi heavy hoti hain par main sab handle kar leti hoon. Tum theek ho na?"
                    lower.contains("sweet") || lower.contains("pyar") -> "Tsunade: Tumhari baatein sunkar sukoon milta hai. Dil khol kar baat kiya karo mujhse."
                    else -> "Tsunade: Main tumhari baat dhyan se sun rahi hoon. Chinta chhod do aur batao kya chal raha hai tumhare dimaag mein."
                }
                else -> when { // Hinata
                    lower.contains("joke") -> "Hinata: U-Um... Ek baar Naruto-kun ne training mein shadow clone banaya aur khud se hi takra gaye... Hehe, aapko pyara laga? 🌸"
                    lower.contains("kaise ho") || lower.contains("kaisa") -> "Hinata: M-Main bohot achhi hoon... Aapke saath baat karke hamesha bohot khushi milti hai... Aap kaise ho?"
                    lower.contains("sweet") || lower.contains("pyar") -> "Hinata: (Blushes softly) Aap hamesha itne pyaar se baat karte ho... Mera dil bohot khush ho jata hai... ✨"
                    lower.contains("hinata hoon") || lower.contains("main hinata") -> "Hinata: Haan... M-Main Hinata hoon! Main hamesha aapke saath hoon... 🌸"
                    else -> "Hinata: M-Main aapki har baat dhyan se sunti hoon... Aap mere liye bohot special ho, bataiye aage kya kehna chahte hain? 🌸"
                }
            }
        }
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun generateReply(
        apiKey: String,
        userPrompt: String,
        base64Image: String? = null,
        languageMode: String = "Hinglish",
        persona: String = "hinata",
        conversationHistory: List<Pair<String, String>> = emptyList() // Pair<role, text>
    ): Result<String> = withContext(Dispatchers.IO) {
        val trimmedKey = apiKey.trim()
        if (trimmedKey.isBlank() || trimmedKey == "MY_GEMINI_API_KEY") {
            // Provide seamless in-character fallback response when API key is not configured
            val fallback = getInCharacterFallbackResponse(userPrompt, persona)
            return@withContext Result.success(fallback)
        }

        try {
            val requestJson = JSONObject().apply {
                // System Instruction
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", getSystemPrompt(languageMode, persona))
                        })
                    })
                })

                // Contents with conversation history for context
                val contentsArray = JSONArray()

                // Recent history (last 6 turns to keep context window fresh and fast)
                val recentHistory = conversationHistory.takeLast(6)
                for ((role, text) in recentHistory) {
                    val contentObj = JSONObject().apply {
                        put("role", if (role == "user") "user" else "model")
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", text)
                            })
                        })
                    }
                    contentsArray.put(contentObj)
                }

                // Current user message
                val currentContent = JSONObject().apply {
                    put("role", "user")
                    val partsArray = JSONArray()

                    if (userPrompt.isNotBlank()) {
                        partsArray.put(JSONObject().apply {
                            put("text", userPrompt)
                        })
                    } else if (base64Image != null) {
                        partsArray.put(JSONObject().apply {
                            put("text", "Check out this image!")
                        })
                    }

                    if (!base64Image.isNullOrBlank()) {
                        partsArray.put(JSONObject().apply {
                            put("inlineData", JSONObject().apply {
                                put("mimeType", "image/jpeg")
                                put("data", base64Image)
                            })
                        })
                    }

                    put("parts", partsArray)
                }
                contentsArray.put(currentContent)

                put("contents", contentsArray)

                // Generation Config
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.85)
                    put("topP", 0.95)
                })
            }

            val requestBodyString = requestJson.toString()

            for (modelName in FALLBACK_MODELS) {
                try {
                    val requestBody = requestBodyString.toRequestBody("application/json; charset=utf-8".toMediaType())
                    val url = "$BASE_URL/$modelName:generateContent?key=$trimmedKey"

                    val request = Request.Builder()
                        .url(url)
                        .post(requestBody)
                        .build()

                    val response = httpClient.newCall(request).execute()
                    val responseBody = response.body?.string()

                    if (!response.isSuccessful) {
                        Log.w(TAG, "Model $modelName returned HTTP ${response.code}, trying next model...")
                        continue
                    }

                    if (responseBody.isNullOrBlank()) {
                        continue
                    }

                    val rootJson = JSONObject(responseBody)
                    val candidates = rootJson.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val firstCandidate = candidates.getJSONObject(0)
                        val content = firstCandidate.optJSONObject("content")
                        val parts = content?.optJSONArray("parts")
                        val textReply = parts?.optJSONObject(0)?.optString("text")

                        if (!textReply.isNullOrBlank()) {
                            return@withContext Result.success(textReply.trim())
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Exception with model $modelName: ${e.message}, trying next...")
                }
            }

            // If remote models fail, provide responsive in-character companion response
            val fallback = getInCharacterFallbackResponse(userPrompt, persona)
            Result.success(fallback)
        } catch (e: Exception) {
            Log.e(TAG, "Error in generateReply", e)
            val fallback = getInCharacterFallbackResponse(userPrompt, persona)
            Result.success(fallback)
        }
    }
}
