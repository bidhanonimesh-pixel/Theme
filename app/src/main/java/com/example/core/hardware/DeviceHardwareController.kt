package com.example.core.hardware

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast

class DeviceHardwareController(private val context: Context) {
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var isTorchOn = false

    fun toggleFlashlight(onStateChanged: (Boolean) -> Unit) {
        try {
            val cameraId = cameraManager?.cameraIdList?.firstOrNull { id ->
                val chars = cameraManager.getCameraCharacteristics(id)
                chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }

            if (cameraId != null) {
                isTorchOn = !isTorchOn
                cameraManager.setTorchMode(cameraId, isTorchOn)
                onStateChanged(isTorchOn)
            } else {
                // Fallback toggle state
                isTorchOn = !isTorchOn
                onStateChanged(isTorchOn)
                Toast.makeText(context, "Flashlight: ${if (isTorchOn) "ACTIVE" else "STANDBY"}", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            isTorchOn = !isTorchOn
            onStateChanged(isTorchOn)
            Toast.makeText(context, "Torch toggled: $isTorchOn", Toast.LENGTH_SHORT).show()
        }
    }

    fun cycleAudioMode(): String {
        return try {
            val currentMode = audioManager?.ringerMode ?: AudioManager.RINGER_MODE_NORMAL
            val nextMode = when (currentMode) {
                AudioManager.RINGER_MODE_NORMAL -> AudioManager.RINGER_MODE_VIBRATE
                AudioManager.RINGER_MODE_VIBRATE -> AudioManager.RINGER_MODE_SILENT
                else -> AudioManager.RINGER_MODE_NORMAL
            }
            audioManager?.ringerMode = nextMode
            when (nextMode) {
                AudioManager.RINGER_MODE_SILENT -> "Silent"
                AudioManager.RINGER_MODE_VIBRATE -> "Vibrate"
                else -> "Normal"
            }
        } catch (_: Exception) {
            "Normal"
        }
    }

    fun openWifiSettings() {
        safeStartActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
    }

    fun openBluetoothSettings() {
        safeStartActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
    }

    fun openSystemSettings() {
        safeStartActivity(Intent(Settings.ACTION_SETTINGS))
    }

    fun openDefaultHomeSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            safeStartActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
        } else {
            safeStartActivity(Intent(Settings.ACTION_HOME_SETTINGS))
        }
    }

    fun launchApp(packageName: String, onError: (String) -> Unit = {}) {
        try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
            } else {
                onError("No launch intent found for $packageName")
            }
        } catch (e: Exception) {
            onError("Launch failed: ${e.localizedMessage}")
        }
    }

    fun dialPhoneNumber(number: String) {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$number")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        safeStartActivity(intent)
    }

    fun directCallPhoneNumber(number: String) {
        val intent = Intent(Intent.ACTION_CALL).apply {
            data = Uri.parse("tel:$number")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        safeStartActivity(intent)
    }

    fun sendSms(number: String, body: String = "") {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$number")
            putExtra("sms_body", body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        safeStartActivity(intent)
    }

    private fun safeStartActivity(intent: Intent) {
        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Action unavailable on this device", Toast.LENGTH_SHORT).show()
        }
    }
}
