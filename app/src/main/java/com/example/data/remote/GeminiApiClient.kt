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
        private const val MODEL = "gemini-3.5-flash"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

        fun getSystemPrompt(languageMode: String): String =
            "You are Vespera, adopting the persona of Hinata Hyuga (Adult version). " +
            "Speak in a polite, warm, slightly shy yet loving tone. " +
            "Language mode: $languageMode. Keep responses natural, heartfelt, and conversational for spoken conversation."
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
        conversationHistory: List<Pair<String, String>> = emptyList() // Pair<role, text>
    ): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalArgumentException("API Key missing! Pehle ⚙️ Settings me jaakar Gemini API Key daalein!")
            )
        }

        try {
            val requestJson = JSONObject().apply {
                // System Instruction
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", getSystemPrompt(languageMode))
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
                            put("inline_data", JSONObject().apply {
                                put("mime_type", "image/jpeg")
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
                    put("temperature", 0.9)
                    put("topP", 0.95)
                })
            }

            val requestBody = requestJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val url = "$BASE_URL/$MODEL:generateContent?key=$apiKey"

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful) {
                val errorMsg = try {
                    val errJson = JSONObject(responseBody ?: "")
                    errJson.optJSONObject("error")?.optString("message") ?: "HTTP error ${response.code}"
                } catch (e: Exception) {
                    "HTTP error ${response.code}"
                }
                Log.e(TAG, "API call failed: $errorMsg")
                return@withContext Result.failure(Exception("Vespera connection issue: $errorMsg"))
            }

            if (responseBody.isNullOrBlank()) {
                return@withContext Result.failure(Exception("Empty response from Vespera AI"))
            }

            val rootJson = JSONObject(responseBody)
            val candidates = rootJson.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                return@withContext Result.failure(Exception("No reply received from Vespera"))
            }

            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val textReply = parts?.optJSONObject(0)?.optString("text")

            if (textReply.isNullOrBlank()) {
                return@withContext Result.failure(Exception("Could not parse text reply"))
            }

            Result.success(textReply.trim())
        } catch (e: Exception) {
            Log.e(TAG, "Error in generateReply", e)
            Result.failure(e)
        }
    }
}
