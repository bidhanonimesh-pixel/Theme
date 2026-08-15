package com.example.core.telemetry

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.core.sound.SoundEffectManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

class JarvisTelemetryService : Service(), TextToSpeech.OnInitListener {

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var soundManager: SoundEffectManager? = null

    private var hasAlertedFullCharge = false
    private var hasAlertedLowBattery = false
    private var hasAlertedOverheat = false
    private var hasAlertedHighLoad = false

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val tempTenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
                val tempCelsius = tempTenths / 10f

                val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
                val batteryPct = if (level >= 0 && scale > 0) ((level.toFloat() / scale.toFloat()) * 100f).toInt() else 100

                checkBatteryAlarms(batteryPct, isCharging, tempCelsius)
                updateNotification(batteryPct, tempCelsius)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        soundManager = SoundEffectManager(this)
        try {
            tts = TextToSpeech(applicationContext, this)
        } catch (_: Exception) {
        }

        try {
            createNotificationChannel()
            startForeground(NOTIFICATION_ID, buildNotification(100, 30.0f))
        } catch (_: Throwable) {
        }

        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        // Periodic CPU & Memory load monitoring loop
        serviceScope.launch {
            while (isActive) {
                checkSystemLoadAlarms()
                delay(12000)
            }
        }
    }

    private fun checkBatteryAlarms(batteryPct: Int, isCharging: Boolean, tempCelsius: Float) {
        // 1. Full Charge Alert
        if (batteryPct >= 100 && isCharging) {
            if (!hasAlertedFullCharge) {
                hasAlertedFullCharge = true
                triggerVoiceAlarm("Power cells fully saturated at 100 percent capacity, sir. Disconnecting charging line recommended.")
            }
        } else if (batteryPct < 95) {
            hasAlertedFullCharge = false
        }

        // 2. Low Battery Warning
        if (batteryPct <= 15 && !isCharging) {
            if (!hasAlertedLowBattery) {
                hasAlertedLowBattery = true
                triggerVoiceAlarm("Warning. Core power cell depleted to $batteryPct percent. Auxiliary power reserves low.")
            }
        } else if (batteryPct > 20) {
            hasAlertedLowBattery = false
        }

        // 3. Overheat Protection
        if (tempCelsius >= 45.0f) {
            if (!hasAlertedOverheat) {
                hasAlertedOverheat = true
                triggerVoiceAlarm("Thermal alert! Core temperature exceeded 45 degrees Celsius. Thermal throttling engaged.")
            }
        } else if (tempCelsius < 40.0f) {
            hasAlertedOverheat = false
        }
    }

    private fun checkSystemLoadAlarms() {
        val runtime = Runtime.getRuntime()
        val usedMem = runtime.totalMemory() - runtime.freeMemory()
        val maxMem = runtime.maxMemory()
        val memPct = ((usedMem.toDouble() / maxMem.toDouble()) * 100).toInt()

        if (memPct >= 88) {
            if (!hasAlertedHighLoad) {
                hasAlertedHighLoad = true
                triggerVoiceAlarm("Subsystem memory allocation is currently high at $memPct percent. Purging cache recommended.")
            }
        } else if (memPct < 70) {
            hasAlertedHighLoad = false
        }
    }

    private fun triggerVoiceAlarm(text: String) {
        soundManager?.playSciFiBeep(SoundEffectManager.ToneType.ALERT)
        if (isTtsReady && tts != null) {
            tts?.speak(text, TextToSpeech.QUEUE_ADD, null, "JARVIS_ALARM_${System.currentTimeMillis()}")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "JARVIS OS System Telemetry",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Continuous background hardware telemetry and thermal protection"
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(batteryPct: Int, tempC: Float): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("JARVIS OS Core Online")
            .setContentText("Power: $batteryPct% | Thermal: ${tempC}°C | Defense Matrix Nominal")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(batteryPct: Int, tempC: Float) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        nm?.notify(NOTIFICATION_ID, buildNotification(batteryPct, tempC))
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            tts?.setPitch(0.92f)
            tts?.setSpeechRate(1.05f)
            isTtsReady = true
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(batteryReceiver)
            tts?.shutdown()
            soundManager?.release()
        } catch (_: Exception) {
        }
    }

    companion object {
        const val CHANNEL_ID = "jarvis_telemetry_channel"
        const val NOTIFICATION_ID = 2050

        fun startService(context: Context) {
            val intent = Intent(context, JarvisTelemetryService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
