package com.example.ai

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import com.example.core.hardware.DeviceHardwareController
import com.example.core.telemetry.SystemTelemetryState
import com.example.launcher.model.AppItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

sealed class JarvisVoiceState {
    object Idle : JarvisVoiceState()
    object Listening : JarvisVoiceState()
    object Processing : JarvisVoiceState()
    data class Speaking(val text: String) : JarvisVoiceState()
    data class Error(val message: String) : JarvisVoiceState()
}

class JarvisVoiceEngine(
    private val context: Context,
    private val hardwareController: DeviceHardwareController
) : RecognitionListener, TextToSpeech.OnInitListener {

    private val scope = CoroutineScope(Dispatchers.Main)
    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    private val _voiceState = MutableStateFlow<JarvisVoiceState>(JarvisVoiceState.Idle)
    val voiceState: StateFlow<JarvisVoiceState> = _voiceState.asStateFlow()

    private val _liveRmsDb = MutableStateFlow(0f)
    val liveRmsDb: StateFlow<Float> = _liveRmsDb.asStateFlow()

    private val _lastQuery = MutableStateFlow("")
    val lastQuery: StateFlow<String> = _lastQuery.asStateFlow()

    private val _lastResponse = MutableStateFlow("JARVIS OS Core standing by. Touch Arc Reactor or speak a command.")
    val lastResponse: StateFlow<String> = _lastResponse.asStateFlow()

    private var installedAppsProvider: (() -> List<AppItem>)? = null
    private var telemetryProvider: (() -> SystemTelemetryState)? = null
    private var apiKeyProvider: (() -> String)? = null
    private var onFlashlightToggled: ((Boolean) -> Unit)? = null

    init {
        try {
            tts = TextToSpeech(context.applicationContext, this)
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(this@JarvisVoiceEngine)
                }
            }
        } catch (_: Exception) {
        }
    }

    fun setProviders(
        apps: () -> List<AppItem>,
        telemetry: () -> SystemTelemetryState,
        apiKey: () -> String,
        onTorch: (Boolean) -> Unit
    ) {
        installedAppsProvider = apps
        telemetryProvider = telemetry
        apiKeyProvider = apiKey
        onFlashlightToggled = onTorch
    }

    fun startListening() {
        if (_voiceState.value is JarvisVoiceState.Speaking) {
            stopSpeaking()
        }
        try {
            if (speechRecognizer == null && SpeechRecognizer.isRecognitionAvailable(context)) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(this@JarvisVoiceEngine)
                }
            }
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            _voiceState.value = JarvisVoiceState.Listening
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            _voiceState.value = JarvisVoiceState.Error("Speech listener error: ${e.localizedMessage}")
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) {
        }
    }

    fun submitTextCommand(rawInput: String) {
        processCommand(rawInput)
    }

    private fun processCommand(query: String) {
        _lastQuery.value = query
        _voiceState.value = JarvisVoiceState.Processing
        val clean = query.trim().lowercase(Locale.ROOT)

        scope.launch {
            // 1. Check Offline Hardware & System Commands first for instant execution
            val offlineHandled = handleOfflineCommand(clean)
            if (offlineHandled != null) {
                speak(offlineHandled)
                return@launch
            }

            // 2. Check Online AI response via Gemini or fallback Sci-Fi response
            val userApiKey = apiKeyProvider?.invoke() ?: ""
            if (userApiKey.isNotBlank()) {
                val aiReply = fetchGeminiResponse(userApiKey, query)
                speak(aiReply)
            } else {
                val fallbackReply = generateSciFiJarvisReply(query)
                speak(fallbackReply)
            }
        }
    }

    private fun handleOfflineCommand(cmd: String): String? {
        val telemetry = telemetryProvider?.invoke() ?: SystemTelemetryState()
        val apps = installedAppsProvider?.invoke() ?: emptyList()

        // Flashlight / Torch
        if (cmd.contains("flashlight") || cmd.contains("torch")) {
            if (cmd.contains("on") || cmd.contains("activate") || cmd.contains("enable")) {
                hardwareController.toggleFlashlight { state -> onFlashlightToggled?.invoke(state) }
                return "Illumination array activated, sir."
            } else if (cmd.contains("off") || cmd.contains("disable") || cmd.contains("deactivate")) {
                hardwareController.toggleFlashlight { state -> onFlashlightToggled?.invoke(state) }
                return "Illumination array powered down."
            } else {
                hardwareController.toggleFlashlight { state -> onFlashlightToggled?.invoke(state) }
                return "Toggling auxiliary optical illumination."
            }
        }

        // Audio Profiles
        if (cmd.contains("silent") || cmd.contains("mute")) {
            hardwareController.cycleAudioMode()
            return "Audio sensors set to silent stealth protocol."
        }
        if (cmd.contains("vibrate") || cmd.contains("haptic")) {
            hardwareController.cycleAudioMode()
            return "Haptic feedback mode initialized."
        }
        if (cmd.contains("normal mode") || cmd.contains("unmute") || cmd.contains("sound on")) {
            hardwareController.cycleAudioMode()
            return "Acoustic audio systems operational."
        }

        // Diagnostics & Telemetry
        if (cmd.contains("status") || cmd.contains("diagnostics") || cmd.contains("telemetry") || cmd.contains("report")) {
            return "System Diagnostic Report: Battery at ${telemetry.batteryPercent}%, temperature ${telemetry.batteryTempCelsius} degrees Celsius. RAM usage is ${telemetry.ramUsagePercent}%. Storage utilization is ${telemetry.storageUsagePercent}%. Network link ${telemetry.carrierName} is online at ${telemetry.networkSpeedFormatted}."
        }
        if (cmd.contains("battery") || cmd.contains("power")) {
            val chargeStatus = if (telemetry.isCharging) "currently receiving external charge" else "discharging on internal cell"
            return "Power levels are at ${telemetry.batteryPercent}%, $chargeStatus. Core thermal readout: ${telemetry.batteryTempCelsius} degrees."
        }
        if (cmd.contains("memory") || cmd.contains("ram")) {
            return "Memory allocation is at ${telemetry.ramUsagePercent}%. ${telemetry.ramUsedGb} Gigabytes used out of ${telemetry.ramTotalGb} Gigabytes available."
        }
        if (cmd.contains("storage") || cmd.contains("disk")) {
            return "Local storage capacity: ${telemetry.storageUsagePercent}% utilized. ${telemetry.storageUsedGb} GB occupied of ${telemetry.storageTotalGb} GB."
        }

        // System Settings
        if (cmd.contains("open wifi") || cmd.contains("wi-fi settings")) {
            hardwareController.openWifiSettings()
            return "Accessing Wi-Fi transmission matrix."
        }
        if (cmd.contains("open bluetooth") || cmd.contains("bluetooth settings")) {
            hardwareController.openBluetoothSettings()
            return "Accessing short-range Bluetooth telemetry."
        }
        if (cmd.contains("open settings") || cmd.contains("device settings")) {
            hardwareController.openSystemSettings()
            return "Opening system configuration sub-routines."
        }

        // App Launching ("Open WhatsApp", "Launch Camera", "Start Chrome", etc.)
        if (cmd.startsWith("open ") || cmd.startsWith("launch ") || cmd.startsWith("start ")) {
            val targetName = cmd.removePrefix("open ")
                .removePrefix("launch ")
                .removePrefix("start ")
                .trim()

            val matchedApp = apps.firstOrNull {
                it.appName.lowercase(Locale.ROOT).contains(targetName) ||
                        targetName.contains(it.appName.lowercase(Locale.ROOT))
            }

            if (matchedApp != null) {
                hardwareController.launchApp(matchedApp.packageName)
                return "Initiating ${matchedApp.appName} application."
            } else {
                return "Target module $targetName was not detected in local application partitions."
            }
        }

        // Direct Calling ("Call 911", "Call Mom")
        if (cmd.startsWith("call ") || cmd.startsWith("dial ")) {
            val target = cmd.removePrefix("call ").removePrefix("dial ").trim()
            if (target.matches(Regex("^[0-9+\\-#* ]+$"))) {
                hardwareController.dialPhoneNumber(target)
                return "Routing voice call link to $target."
            }
        }

        return null
    }

    private suspend fun fetchGeminiResponse(apiKey: String, prompt: String): String = withContext(Dispatchers.IO) {
        try {
            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"
            val url = URL(endpoint)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            val systemInstruction = "You are JARVIS, an ultra-advanced 2050 AI Operating System assistant created by Stark Industries. Speak concisely, with British poise, intelligence, and sci-fi elegance. Limit answers to 2-3 sentences max."
            val payload = JSONObject().apply {
                put("contents", org.json.JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", org.json.JSONArray().apply {
                            put(JSONObject().put("text", "$systemInstruction\nUser: $prompt"))
                        })
                    })
                })
            }

            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            if (conn.responseCode == 200) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)
                val candidate = json.getJSONArray("candidates").getJSONObject(0)
                val content = candidate.getJSONObject("content").getJSONArray("parts").getJSONObject(0)
                content.getString("text").trim()
            } else {
                generateSciFiJarvisReply(prompt)
            }
        } catch (_: Exception) {
            generateSciFiJarvisReply(prompt)
        }
    }

    private fun generateSciFiJarvisReply(query: String): String {
        val q = query.lowercase(Locale.ROOT)
        return when {
            q.contains("who are you") || q.contains("what are you") ->
                "I am J.A.R.V.I.S. — Just A Rather Very Intelligent System. Operating as your primary neural interface and launcher controller."
            q.contains("hello") || q.contains("hey jarvis") || q.contains("hi") ->
                "Greetings. All primary OS subsystems are operational. How may I assist your operations today, sir?"
            q.contains("time") || q.contains("what time") -> {
                val time = telemetryProvider?.invoke()?.timeFormatted ?: "Unknown"
                "The current synchronized local time is $time."
            }
            q.contains("weather") ->
                "Atmospheric sensors indicate standard tropospheric conditions. Atmospheric pressure nominal."
            q.contains("mark") || q.contains("suit") || q.contains("armor") ->
                "Mark 85 nanotech protocols are standing by in the deployment bay."
            else ->
                "Acknowledged, sir. Neural matrices processed the directive: \"$query\". All tactical systems ready."
        }
    }

    fun speak(text: String) {
        _lastResponse.value = text
        _voiceState.value = JarvisVoiceState.Speaking(text)
        if (isTtsReady && tts != null) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "JARVIS_TTS_${System.currentTimeMillis()}")
        }
    }

    fun stopSpeaking() {
        tts?.stop()
        _voiceState.value = JarvisVoiceState.Idle
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            tts?.setPitch(0.92f) // Slightly deeper, sophisticated AI cadence
            tts?.setSpeechRate(1.05f)
            isTtsReady = true
        }
    }

    override fun onReadyForSpeech(params: Bundle?) {
        _voiceState.value = JarvisVoiceState.Listening
    }

    override fun onRmsChanged(rmsdB: Float) {
        _liveRmsDb.value = (rmsdB + 2f).coerceAtLeast(0f)
    }

    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val query = matches?.firstOrNull()
        if (!query.isNullOrBlank()) {
            processCommand(query)
        } else {
            _voiceState.value = JarvisVoiceState.Idle
        }
    }

    override fun onError(error: Int) {
        _voiceState.value = JarvisVoiceState.Idle
    }

    override fun onBeginningOfSpeech() {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() {}
    override fun onPartialResults(partialResults: Bundle?) {}
    override fun onEvent(eventType: Int, params: Bundle?) {}

    fun destroy() {
        try {
            speechRecognizer?.destroy()
            tts?.shutdown()
        } catch (_: Exception) {
        }
    }
}
