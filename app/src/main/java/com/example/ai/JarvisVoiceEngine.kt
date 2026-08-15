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
import java.util.Locale

sealed class JarvisVoiceState {
    object Idle : JarvisVoiceState()
    object Listening : JarvisVoiceState()
    object Processing : JarvisVoiceState()
    data class Speaking(val text: String, val tierUsed: String? = null) : JarvisVoiceState()
    data class Error(val message: String) : JarvisVoiceState()
}

class JarvisVoiceEngine(
    private val context: Context,
    private val hardwareController: DeviceHardwareController,
    private val aiRepository: JarvisAiRepository
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

    private val _lastResponse = MutableStateFlow("JARVIS OS Core standing by. Touch Arc Reactor or speak a directive.")
    val lastResponse: StateFlow<String> = _lastResponse.asStateFlow()

    private val _lastTierUsed = MutableStateFlow("Multi-Tier AI Ready")
    val lastTierUsed: StateFlow<String> = _lastTierUsed.asStateFlow()

    private var installedAppsProvider: (() -> List<AppItem>)? = null
    private var telemetryProvider: (() -> SystemTelemetryState)? = null
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
        onTorch: (Boolean) -> Unit
    ) {
        installedAppsProvider = apps
        telemetryProvider = telemetry
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

        val telemetry = telemetryProvider?.invoke() ?: SystemTelemetryState()
        val apps = installedAppsProvider?.invoke() ?: emptyList()

        scope.launch {
            val result = aiRepository.executeAiDirective(query, telemetry, apps)
            when (result) {
                is AiResponseResult.Success -> {
                    _lastTierUsed.value = result.tierUsed
                    speak(result.reply, result.tierUsed)
                }
                is AiResponseResult.OfflineAction -> {
                    _lastTierUsed.value = "Hardware Action Matrix"
                    executeAction(result.actionCommand)
                    speak(result.reply, "Hardware Action Matrix")
                }
                is AiResponseResult.Error -> {
                    _voiceState.value = JarvisVoiceState.Error(result.message)
                }
            }
        }
    }

    private fun executeAction(command: String?) {
        if (command == null) return
        when {
            command == "CMD_TORCH_ON" -> hardwareController.toggleFlashlight { onFlashlightToggled?.invoke(true) }
            command == "CMD_TORCH_OFF" -> hardwareController.toggleFlashlight { onFlashlightToggled?.invoke(false) }
            command == "CMD_TORCH_TOGGLE" -> hardwareController.toggleFlashlight { onFlashlightToggled?.invoke(it) }
            command == "CMD_OPEN_WIFI" -> hardwareController.openWifiSettings()
            command == "CMD_OPEN_BT" -> hardwareController.openBluetoothSettings()
            command == "CMD_AUDIO_SILENT" || command == "CMD_AUDIO_VIBRATE" || command == "CMD_AUDIO_NORMAL" -> {
                hardwareController.cycleAudioMode()
            }
            command.startsWith("CMD_LAUNCH_APP:") -> {
                val pkg = command.removePrefix("CMD_LAUNCH_APP:")
                hardwareController.launchApp(pkg)
            }
            command.startsWith("CMD_DIAL:") -> {
                val num = command.removePrefix("CMD_DIAL:")
                hardwareController.dialPhoneNumber(num)
            }
        }
    }

    fun speak(text: String, tierUsed: String? = null) {
        _lastResponse.value = text
        _voiceState.value = JarvisVoiceState.Speaking(text, tierUsed)
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
            tts?.setPitch(0.92f)
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
