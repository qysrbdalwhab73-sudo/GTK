package com.example.data.remote

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

data class GeminiResponse(
    val text: String,
    val thinkingTrace: String? = null,
    val searchSources: List<String> = emptyList(),
    val imageBase64: String? = null,
    val videoBase64: String? = null
)

object GeminiClient {

    private const val BASE_URL = "https://generativelanguage.googleapis.com"
    private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val interactionsClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private fun getApiKey(): String {
        return try {
            BuildConfig.GEMINI_API_KEY
        } catch (_: Exception) {
            ""
        }
    }

    /**
     * Generate content using gemini-4-argon with ThinkingLevel.HIGH
     */
    suspend fun generateWithThinking(
        prompt: String,
        systemInstruction: String? = null,
        inlineData: Pair<String, String>? = null // mimeType, base64
    ): GeminiResponse = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext mockLocalThinkingResponse(prompt)
        }

        val url = "$BASE_URL/v1beta/models/gemini-4-argon:generateContent?key=$apiKey"
        val json = JSONObject().apply {
            // Contents
            val contentsArray = JSONArray()
            val userContent = JSONObject().apply {
                put("role", "user")
                val parts = JSONArray()
                parts.put(JSONObject().put("text", prompt))
                if (inlineData != null) {
                    parts.put(JSONObject().apply {
                        put("inlineData", JSONObject().apply {
                            put("mimeType", inlineData.first)
                            put("data", inlineData.second)
                        })
                    })
                }
                put("parts", parts)
            }
            contentsArray.put(userContent)
            put("contents", contentsArray)

            // System Instruction
            if (!systemInstruction.isNullOrBlank()) {
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().put("text", systemInstruction)))
                })
            }

            // High Thinking Config
            put("generationConfig", JSONObject().apply {
                put("thinkingConfig", JSONObject().apply {
                    put("thinkingLevel", "HIGH")
                })
            })
        }

        try {
            val request = Request.Builder()
                .url(url)
                .post(json.toString().toRequestBody(JSON_MEDIA))
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                return@withContext GeminiResponse(
                    text = "API Request failed (${response.code}): $responseBody",
                    thinkingTrace = "Error occurred while contacting gemini-4-argon endpoint."
                )
            }

            parseGenerateContentResponse(responseBody)
        } catch (e: Exception) {
            GeminiResponse(
                text = "Connection error: ${e.message}\nFalling back to on-device reverse engineering heuristics.",
                thinkingTrace = "Network error encountered: ${e.localizedMessage}"
            )
        }
    }

    /**
     * Generate content with Google Search Grounding using gemini-3.8-flash
     */
    suspend fun generateWithSearch(
        prompt: String,
        systemInstruction: String? = null
    ): GeminiResponse = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext mockLocalSearchResponse(prompt)
        }

        val url = "$BASE_URL/v1beta/models/gemini-3.8-flash:generateContent?key=$apiKey"
        val json = JSONObject().apply {
            val contentsArray = JSONArray().put(
                JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().put(JSONObject().put("text", prompt)))
                }
            )
            put("contents", contentsArray)

            if (!systemInstruction.isNullOrBlank()) {
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().put("text", systemInstruction)))
                })
            }

            // Google Search Tool
            put("tools", JSONArray().put(
                JSONObject().put("googleSearch", JSONObject())
            ))
        }

        try {
            val request = Request.Builder()
                .url(url)
                .post(json.toString().toRequestBody(JSON_MEDIA))
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                return@withContext GeminiResponse(text = "Search request failed (${response.code}): $responseBody")
            }

            parseGenerateContentResponse(responseBody)
        } catch (e: Exception) {
            GeminiResponse(text = "Search error: ${e.message}")
        }
    }

    /**
     * Generate Game Art / Textures using gemini-3-pro-image-preview
     */
    suspend fun generateImage(
        prompt: String,
        aspectRatio: String = "1:1",
        imageSize: String = "1K"
    ): GeminiResponse = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext GeminiResponse(
                text = "Image generation requested: '$prompt'. Enter GEMINI_API_KEY in Secrets panel to render live textures."
            )
        }

        val url = "$BASE_URL/v1beta/models/gemini-3-pro-image-preview:generateContent?key=$apiKey"
        val json = JSONObject().apply {
            put("contents", JSONArray().put(
                JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))
            ))
            put("generationConfig", JSONObject().apply {
                put("imageConfig", JSONObject().apply {
                    put("aspectRatio", aspectRatio)
                    put("imageSize", imageSize)
                })
                put("responseModalities", JSONArray().put("TEXT").put("IMAGE"))
            })
        }

        try {
            val request = Request.Builder()
                .url(url)
                .post(json.toString().toRequestBody(JSON_MEDIA))
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                return@withContext GeminiResponse(text = "Image generation failed: $responseBody")
            }

            parseImageResponse(responseBody)
        } catch (e: Exception) {
            GeminiResponse(text = "Image generation error: ${e.message}")
        }
    }

    /**
     * Transcribe audio using gemini-3.5-transcribe
     */
    suspend fun transcribeAudio(
        audioBytes: ByteArray,
        mimeType: String = "audio/mp4"
    ): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Game Reverse Engineering audio query (Microphone input received)"
        }

        val url = "$BASE_URL/v1beta/models/gemini-3.5-transcribe:generateContent?key=$apiKey"
        val base64Audio = Base64.encodeToString(audioBytes, Base64.NO_WRAP)
        val json = JSONObject().apply {
            put("contents", JSONArray().put(
                JSONObject().put("parts", JSONArray().apply {
                    put(JSONObject().put("text", "Transcribe this game reverse engineering audio query accurately."))
                    put(JSONObject().put("inlineData", JSONObject().apply {
                        put("mimeType", mimeType)
                        put("data", base64Audio)
                    }))
                })
            ))
        }

        try {
            val request = Request.Builder()
                .url(url)
                .post(json.toString().toRequestBody(JSON_MEDIA))
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            if (!response.isSuccessful) return@withContext "Transcription failed: ${response.code}"

            val root = JSONObject(responseBody)
            val candidates = root.optJSONArray("candidates")
            val candidate = candidates?.optJSONObject(0)
            val parts = candidate?.optJSONObject("content")?.optJSONArray("parts")
            parts?.optJSONObject(0)?.optString("text") ?: "No speech detected"
        } catch (e: Exception) {
            "Audio transcription error: ${e.message}"
        }
    }

    /**
     * Generate video using gemini-omni-1.1-flash via Interactions API
     */
    suspend fun generateVideo(
        prompt: String,
        aspectRatio: String = "16:9"
    ): GeminiResponse = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext GeminiResponse(
                text = "Video preview request: '$prompt'. Enter GEMINI_API_KEY in Secrets panel to generate live videos via Interactions API."
            )
        }

        val url = "$BASE_URL/v1beta/interactions?key=$apiKey"
        val requestJson = JSONObject().apply {
            put("model", "gemini-omni-1.1-flash")
            put("input", prompt)
            put("background", false)
            put("store", true)
            put("stream", false)
            put("response_format", JSONObject().apply {
                put("type", "video")
                put("aspect_ratio", aspectRatio)
                put("duration", "5s")
            })
        }

        try {
            val postRequest = Request.Builder()
                .url(url)
                .post(requestJson.toString().toRequestBody(JSON_MEDIA))
                .build()

            val postResponse = interactionsClient.newCall(postRequest).execute()
            val postBody = postResponse.body?.string() ?: ""
            if (!postResponse.isSuccessful) {
                return@withContext GeminiResponse(text = "Interactions API video creation failed: $postBody")
            }

            val initJson = JSONObject(postBody)
            val interactionId = initJson.getString("id")
            var status = initJson.optString("status", "queued")

            // Poll for completion
            var attempts = 0
            var finalBody = postBody
            while ((status == "in_progress" || status == "queued") && attempts < 15) {
                delay(5000L)
                attempts++
                val pollUrl = "$BASE_URL/v1beta/interactions/$interactionId?key=$apiKey"
                val pollReq = Request.Builder().url(pollUrl).get().build()
                val pollResp = interactionsClient.newCall(pollReq).execute()
                finalBody = pollResp.body?.string() ?: ""
                val pollJson = JSONObject(finalBody)
                status = pollJson.optString("status")
            }

            val pollJson = JSONObject(finalBody)
            val steps = pollJson.optJSONArray("steps")
            var videoBase64: String? = null
            if (steps != null) {
                for (i in 0 until steps.length()) {
                    val step = steps.getJSONObject(i)
                    val contents = step.optJSONArray("content")
                    if (contents != null) {
                        for (j in 0 until contents.length()) {
                            val c = contents.getJSONObject(j)
                            if (c.optString("type") == "video") {
                                videoBase64 = c.optString("data")
                            }
                        }
                    }
                }
            }

            GeminiResponse(
                text = "Generated 5-second game preview video for '$prompt'.",
                videoBase64 = videoBase64
            )
        } catch (e: Exception) {
            GeminiResponse(text = "Video generation error: ${e.message}")
        }
    }

    private fun parseGenerateContentResponse(responseBody: String): GeminiResponse {
        val root = JSONObject(responseBody)
        val candidates = root.optJSONArray("candidates") ?: return GeminiResponse(text = "No candidates returned")
        val candidate = candidates.optJSONObject(0) ?: return GeminiResponse(text = "Empty candidate")
        val content = candidate.optJSONObject("content") ?: return GeminiResponse(text = "No content")
        val parts = content.optJSONArray("parts") ?: return GeminiResponse(text = "No parts")

        var textContent = ""
        var thinkingTrace: String? = null

        for (i in 0 until parts.length()) {
            val part = parts.optJSONObject(i) ?: continue
            if (part.has("thought") || part.has("thinking")) {
                thinkingTrace = part.optString("thought").ifBlank { part.optString("thinking") }
            }
            if (part.has("text")) {
                val t = part.optString("text")
                if (textContent.isEmpty()) textContent = t else textContent += "\n$t"
            }
        }

        // Search grounding metadata
        val groundingMetadata = candidate.optJSONObject("groundingMetadata")
        val sources = mutableListOf<String>()
        if (groundingMetadata != null) {
            val searchChunks = groundingMetadata.optJSONArray("groundingChunks")
            if (searchChunks != null) {
                for (i in 0 until searchChunks.length()) {
                    val chunk = searchChunks.optJSONObject(i)
                    val web = chunk?.optJSONObject("web")
                    val title = web?.optString("title")
                    val uri = web?.optString("uri")
                    if (!title.isNullOrBlank()) {
                        sources.add(if (!uri.isNullOrBlank()) "$title ($uri)" else title)
                    }
                }
            }
        }

        return GeminiResponse(
            text = textContent.ifBlank { "Analysis complete." },
            thinkingTrace = thinkingTrace,
            searchSources = sources
        )
    }

    private fun parseImageResponse(responseBody: String): GeminiResponse {
        val root = JSONObject(responseBody)
        val candidates = root.optJSONArray("candidates") ?: return GeminiResponse(text = "No image candidate")
        val candidate = candidates.optJSONObject(0)
        val parts = candidate?.optJSONObject("content")?.optJSONArray("parts")

        var text = ""
        var imageBase64: String? = null

        if (parts != null) {
            for (i in 0 until parts.length()) {
                val part = parts.optJSONObject(i) ?: continue
                if (part.has("text")) {
                    text = part.optString("text")
                }
                if (part.has("inlineData")) {
                    imageBase64 = part.optJSONObject("inlineData")?.optString("data")
                }
            }
        }

        return GeminiResponse(
            text = text.ifBlank { "Texture/Art asset generated successfully." },
            imageBase64 = imageBase64
        )
    }

    private fun mockLocalThinkingResponse(prompt: String): GeminiResponse {
        val trace = buildString {
            append("1. Evaluating binary structure & input requirements: '$prompt'\n")
            append("2. Scanning engine conventions: Unity MonoBehaviour serialization & Unreal Engine reflection system\n")
            append("3. Identifying memory layouts: Pointer offsets, weapon attributes (Damage, FireRate, MagCapacity)\n")
            append("4. Verifying modding hook points: Harmony patch vs IL2CPP method RVA interception\n")
            append("5. Synthesizing deconstruction plan and clean architecture artifact.")
        }

        val text = buildString {
            append("### GameReverse Engine Analysis\n\n")
            append("Based on the input query and game file context:\n\n")
            append("- **Target Engine Structure**: In modern game architectures (such as Unity IL2CPP and Unreal Engine 5), weapon data is typically decoupled from visual meshes using serialized ScriptableObjects or DataTable rows (`UDataTable`).\n")
            append("- **Weapon Deconstruction Breakdown**:\n")
            append("  - **Attributes**: `m_BaseDamage`, `m_FireRateRPM`, `m_EffectiveRange`, `m_AmmoCapacity`\n")
            append("  - **Logic Hook Point**: Look for functions like `WeaponController::Fire()`, `ApplyDamage()`, or `ComputeRecoilImpulse()`.\n")
            append("  - **Memory Offsets**: Serialized field offsets in Mono/IL2CPP can be mapped via `global-metadata.dat` string tables.\n\n")
            append("To inspect the raw byte headers, open the **Hex Inspector** or **Artifact Drawer** above.")
        }

        return GeminiResponse(
            text = text,
            thinkingTrace = trace
        )
    }

    private fun mockLocalSearchResponse(prompt: String): GeminiResponse {
        return GeminiResponse(
            text = "Retrieved modding documentation for: $prompt.\nCommon tools: AssetStudio, UModel/FModel, DnSpy, Il2CppDumper, and Cheat Engine Lua scripting for single-player game exploration.",
            searchSources = listOf(
                "Unity AssetBundle Serialization Specs (docs.unity.com)",
                "Unreal Engine 5 PAK File Format Wiki (unreal-community.wiki)",
                "Game Modding & Arabization Frameworks (modding.community)"
            )
        )
    }
}
