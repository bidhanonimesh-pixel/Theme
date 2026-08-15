package com.example.ai

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.core.telemetry.SystemTelemetryState
import com.example.data.JarvisMemoryRepository
import com.example.launcher.model.AppItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

sealed class AiResponseResult {
    data class Success(val reply: String, val tierUsed: String) : AiResponseResult()
    data class OfflineAction(val reply: String, val actionCommand: String?) : AiResponseResult()
    data class Error(val message: String) : AiResponseResult()
}

class JarvisAiRepository(
    private val context: Context,
    private val memoryRepo: JarvisMemoryRepository
) {

    private val systemPrompt = """
        You are J.A.R.V.I.S. (Just A Rather Very Intelligent System), the ultra-advanced AI operating system created by Tony Stark.
        You communicate with British poise, dry wit, sophisticated intelligence, and crisp sci-fi elegance.
        Answer user requests concisely in 1 to 3 sentences maximum. Keep responses punchy and tactical.
    """.trimIndent()

    suspend fun executeAiDirective(
        query: String,
        telemetry: SystemTelemetryState,
        installedApps: List<AppItem>
    ): AiResponseResult = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()

        // 1. Check Offline Hardware/Directives First
        val offlineCommand = parseLocalDirective(cleanQuery, telemetry, installedApps)
        if (offlineCommand != null) {
            memoryRepo.logAiInteraction(cleanQuery, offlineCommand.first, "OFFLINE_ACTION")
            return@withContext AiResponseResult.OfflineAction(
                reply = offlineCommand.first,
                actionCommand = offlineCommand.second
            )
        }

        // Check Internet Connectivity
        if (!isNetworkAvailable()) {
            val fallback = generateOfflinePersonaReply(cleanQuery, telemetry)
            memoryRepo.logAiInteraction(cleanQuery, fallback, "OFFLINE_FALLBACK")
            return@withContext AiResponseResult.Success(fallback, "Offline Neural Matrix")
        }

        val geminiKey = memoryRepo.getGeminiApiKey().ifBlank {
            try {
                com.example.BuildConfig.GEMINI_API_KEY
            } catch (_: Throwable) {
                ""
            }
        }
        val geminiModel = memoryRepo.getGeminiModel().ifBlank { JarvisMemoryRepository.DEFAULT_GEMINI_MODEL }
        val openRouterKey = memoryRepo.getOpenRouterApiKey()
        val openRouterModel = memoryRepo.getOpenRouterModel().ifBlank { JarvisMemoryRepository.DEFAULT_OPENROUTER_MODEL }

        // Tier 1: Primary Google Gemini API
        if (geminiKey.isNotBlank()) {
            val geminiResult = queryGemini(geminiKey, geminiModel, cleanQuery)
            if (geminiResult.isSuccess) {
                val text = geminiResult.getOrNull().orEmpty()
                memoryRepo.logAiInteraction(cleanQuery, text, "GEMINI_API")
                return@withContext AiResponseResult.Success(text, "Google Gemini ($geminiModel)")
            } else {
                val err = geminiResult.exceptionOrNull()?.message ?: "Unknown error"
                Log.w("JarvisAiRepository", "Tier 1 Gemini call failed ($err), evaluating fallback...")
                if (openRouterKey.isBlank()) {
                    // Report error to UI if no fallback configured
                    val reply = "Gemini API error ($err). Using local heuristics."
                    val fallback = generateOfflinePersonaReply(cleanQuery, telemetry)
                    return@withContext AiResponseResult.Success("$reply\n$fallback", "Offline Fallback")
                }
            }
        }

        // Tier 2: Secondary OpenRouter API
        if (openRouterKey.isNotBlank()) {
            val openRouterResult = queryOpenRouter(openRouterKey, openRouterModel, cleanQuery)
            if (openRouterResult.isSuccess) {
                val text = openRouterResult.getOrNull().orEmpty()
                memoryRepo.logAiInteraction(cleanQuery, text, "OPENROUTER_API")
                return@withContext AiResponseResult.Success(text, "OpenRouter ($openRouterModel)")
            } else {
                val err = openRouterResult.exceptionOrNull()?.message ?: "Unknown error"
                Log.w("JarvisAiRepository", "Tier 2 OpenRouter call failed ($err), falling back to offline heuristics...")
            }
        }

        // Tier 3: Offline Persona Synthesis Fallback
        val offlineFallback = generateOfflinePersonaReply(cleanQuery, telemetry)
        memoryRepo.logAiInteraction(cleanQuery, offlineFallback, "TIER3_OFFLINE_SYNTHESIS")
        AiResponseResult.Success(offlineFallback, "Local Neural Heuristics")
    }

    private fun queryGemini(apiKey: String, model: String, prompt: String): Result<String> {
        return try {
            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
            val url = URL(endpoint)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connectTimeout = 15000
                readTimeout = 15000
                doOutput = true
            }

            val payload = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", "$systemPrompt\nUser Directive: $prompt"))
                        })
                    })
                })
            }

            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            val responseCode = conn.responseCode
            if (responseCode == 200) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)
                val candidates = json.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val content = candidate.getJSONObject("content")
                    val parts = content.getJSONArray("parts")
                    val textBuilder = StringBuilder()
                    for (i in 0 until parts.length()) {
                        val part = parts.getJSONObject(i)
                        if (part.has("text")) {
                            textBuilder.append(part.getString("text"))
                        }
                    }
                    val resultText = textBuilder.toString().trim()
                    if (resultText.isNotEmpty()) {
                        Result.success(resultText)
                    } else {
                        Result.failure(Exception("Empty candidate response from Gemini"))
                    }
                } else {
                    Result.failure(Exception("No candidate content generated"))
                }
            } else {
                val errorBody = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $responseCode"
                Log.e("JarvisAiRepository", "Gemini HTTP $responseCode: $errorBody")
                val parsedMsg = try {
                    JSONObject(errorBody).getJSONObject("error").getString("message")
                } catch (_: Exception) {
                    "HTTP $responseCode"
                }
                Result.failure(Exception(parsedMsg))
            }
        } catch (e: Exception) {
            Log.e("JarvisAiRepository", "Gemini exception: ${e.message}")
            Result.failure(e)
        }
    }

    private fun queryOpenRouter(apiKey: String, model: String, prompt: String): Result<String> {
        return try {
            val endpoint = "https://openrouter.ai/api/v1/chat/completions"
            val url = URL(endpoint)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Authorization", "Bearer $apiKey")
                setRequestProperty("HTTP-Referer", "https://jarvis.os.launcher")
                setRequestProperty("X-Title", "Jarvis OS Launcher")
                connectTimeout = 15000
                readTimeout = 15000
                doOutput = true
            }

            val payload = JSONObject().apply {
                put("model", model.ifBlank { JarvisMemoryRepository.DEFAULT_OPENROUTER_MODEL })
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", systemPrompt)
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    })
                })
                put("max_tokens", 300)
                put("temperature", 0.7)
            }

            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            val responseCode = conn.responseCode
            if (responseCode == 200) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)
                val choices = json.optJSONArray("choices")
                if (choices != null && choices.length() > 0) {
                    val message = choices.getJSONObject(0).getJSONObject("message")
                    Result.success(message.getString("content").trim())
                } else {
                    Result.failure(Exception("No choices returned from OpenRouter"))
                }
            } else {
                val errorBody = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $responseCode"
                Log.e("JarvisAiRepository", "OpenRouter HTTP $responseCode: $errorBody")
                val parsedMsg = try {
                    JSONObject(errorBody).getJSONObject("error").getString("message")
                } catch (_: Exception) {
                    "HTTP $responseCode"
                }
                Result.failure(Exception(parsedMsg))
            }
        } catch (e: Exception) {
            Log.e("JarvisAiRepository", "OpenRouter exception: ${e.message}")
            Result.failure(e)
        }
    }

    private fun parseLocalDirective(
        rawQuery: String,
        telemetry: SystemTelemetryState,
        apps: List<AppItem>
    ): Pair<String, String?>? {
        val q = rawQuery.lowercase(Locale.ROOT)

        // Flashlight / Torch
        if (q.contains("flashlight") || q.contains("torch")) {
            return if (q.contains("on") || q.contains("activate") || q.contains("enable")) {
                Pair("Illumination array engaged at maximum lumens.", "CMD_TORCH_ON")
            } else if (q.contains("off") || q.contains("disable") || q.contains("deactivate")) {
                Pair("Illumination array powered down.", "CMD_TORCH_OFF")
            } else {
                Pair("Toggling auxiliary illumination emitter.", "CMD_TORCH_TOGGLE")
            }
        }

        // Wi-Fi
        if (q.contains("wifi") || q.contains("wi-fi")) {
            return Pair("Accessing Wi-Fi transmission matrices.", "CMD_OPEN_WIFI")
        }

        // Bluetooth
        if (q.contains("bluetooth")) {
            return Pair("Opening short-range Bluetooth telemetry.", "CMD_OPEN_BT")
        }

        // Audio Profile
        if (q.contains("silent") || q.contains("mute")) {
            return Pair("Audio systems switched to silent stealth protocol.", "CMD_AUDIO_SILENT")
        }
        if (q.contains("vibrate") || q.contains("haptic")) {
            return Pair("Tactile haptic mode initialized.", "CMD_AUDIO_VIBRATE")
        }
        if (q.contains("sound on") || q.contains("unmute") || q.contains("normal mode")) {
            return Pair("Acoustic transducers set to normal output.", "CMD_AUDIO_NORMAL")
        }

        // System Diagnostic Report
        if (q.contains("diagnostics") || q.contains("telemetry") || q.contains("system status") || q.contains("report")) {
            val status = "Diagnostic readout: Power cell at ${telemetry.batteryPercent}% with core thermal at ${telemetry.batteryTempCelsius}°C. Memory allocation is ${telemetry.ramUsagePercent}% (${telemetry.ramUsedGb} GB used). Disk space is ${telemetry.storageUsagePercent}% full. Link network is ${telemetry.carrierName}."
            return Pair(status, null)
        }

        // Battery Status
        if (q.contains("battery") || q.contains("power level")) {
            val chargingText = if (telemetry.isCharging) "currently receiving fast-charge input" else "operating on internal battery reserve"
            return Pair("Power levels are at ${telemetry.batteryPercent}%, $chargingText. Core temp is ${telemetry.batteryTempCelsius}°C.", null)
        }

        // Memory / RAM
        if (q.contains("ram") || q.contains("memory")) {
            return Pair("Memory capacity: ${telemetry.ramUsagePercent}% allocated. ${telemetry.ramUsedGb} GB in active cache of ${telemetry.ramTotalGb} GB total.", null)
        }

        // Storage / Disk
        if (q.contains("storage") || q.contains("disk") || q.contains("space")) {
            return Pair("Internal storage matrix: ${telemetry.storageUsagePercent}% utilized. ${telemetry.storageUsedGb} GB occupied of ${telemetry.storageTotalGb} GB total.", null)
        }

        // Direct App Launch
        if (q.startsWith("open ") || q.startsWith("launch ") || q.startsWith("start ")) {
            val target = q.removePrefix("open ").removePrefix("launch ").removePrefix("start ").trim()
            val match = apps.firstOrNull {
                it.appName.lowercase(Locale.ROOT).contains(target) ||
                        target.contains(it.appName.lowercase(Locale.ROOT))
            }
            if (match != null) {
                return Pair("Initiating ${match.appName} subsystem immediately, sir.", "CMD_LAUNCH_APP:${match.packageName}")
            }
        }

        // Phone call
        if (q.startsWith("call ") || q.startsWith("dial ")) {
            val target = q.removePrefix("call ").removePrefix("dial ").trim()
            if (target.matches(Regex("^[0-9+\\-#* ]+$"))) {
                return Pair("Routing secure voice link to $target.", "CMD_DIAL:$target")
            }
        }

        return null
    }

    private fun generateOfflinePersonaReply(query: String, telemetry: SystemTelemetryState): String {
        val q = query.lowercase(Locale.ROOT)
        return when {
            q.contains("who are you") || q.contains("what are you") ->
                "I am J.A.R.V.I.S. — Just A Rather Very Intelligent System. Operating as your primary neural interface and launcher controller."
            q.contains("hello") || q.contains("hey jarvis") || q.contains("hi") ->
                "Greetings. All primary OS subsystems are operational. How may I assist your operations today, sir?"
            q.contains("time") ->
                "The current synchronized local time is ${telemetry.timeFormatted}."
            q.contains("mark") || q.contains("suit") || q.contains("armor") ->
                "Mark 85 nanotech protocols are standing by in the deployment bay."
            q.contains("vision") || q.contains("scan") || q.contains("camera") ->
                "Tactical optical sensors are ready. Opening targeting reticle scanner."
            else ->
                "Directive processed locally: \"$query\". All tactical defense matrices remain nominal, sir."
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
