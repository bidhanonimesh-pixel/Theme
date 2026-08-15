package com.example.core.telemetry

import android.app.ActivityManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.telephony.TelephonyManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

data class SystemTelemetryState(
    val timeFormatted: String = "12:00 PM",
    val dateFormatted: String = "Sat, Jan 1",
    val stardateCode: String = "2050.228",
    val ramUsagePercent: Int = 54,
    val ramUsedGb: Float = 4.2f,
    val ramTotalGb: Float = 8.0f,
    val storageUsagePercent: Int = 68,
    val storageUsedGb: Float = 86.4f,
    val storageTotalGb: Float = 128.0f,
    val batteryPercent: Int = 85,
    val isCharging: Boolean = false,
    val batteryTempCelsius: Float = 31.5f,
    val audioProfile: String = "Normal", // Normal, Vibrate, Silent
    val isWifiEnabled: Boolean = true,
    val wifiSsid: String = "Connected",
    val isDataEnabled: Boolean = true,
    val carrierName: String = "RYZE 5G",
    val isBluetoothEnabled: Boolean = false,
    val networkSpeedFormatted: String = "77.7 KB/s",
    val isFlashlightOn: Boolean = false
)

class SystemTelemetryManager(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private val _telemetryState = MutableStateFlow(SystemTelemetryState())
    val telemetryState: StateFlow<SystemTelemetryState> = _telemetryState.asStateFlow()

    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    private val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager

    private var batteryReceiver: BroadcastReceiver? = null

    init {
        registerBatteryReceiver()
        startTelemetryLoop()
    }

    private fun registerBatteryReceiver() {
        batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    val temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f

                    val batteryPct = if (level >= 0 && scale > 0) ((level / scale.toFloat()) * 100).toInt() else 75
                    val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                            status == BatteryManager.BATTERY_STATUS_FULL

                    _telemetryState.value = _telemetryState.value.copy(
                        batteryPercent = batteryPct,
                        isCharging = isCharging,
                        batteryTempCelsius = if (temp > 0f) temp else 32.0f
                    )
                }
            }
        }
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        context.registerReceiver(batteryReceiver, filter)
    }

    private fun startTelemetryLoop() {
        scope.launch {
            val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            val dateFormat = SimpleDateFormat("EEE MMMM d", Locale.getDefault())

            while (isActive) {
                val now = Date()
                val timeStr = timeFormat.format(now).uppercase()
                val dateStr = dateFormat.format(now)
                val stardate = "SD-${System.currentTimeMillis() / 1000 % 100000}"

                // RAM Telemetry
                val memInfo = ActivityManager.MemoryInfo()
                activityManager?.getMemoryInfo(memInfo)
                val totalRam = memInfo.totalMem / (1024.0 * 1024.0 * 1024.0)
                val availRam = memInfo.availMem / (1024.0 * 1024.0 * 1024.0)
                val usedRam = (totalRam - availRam).coerceAtLeast(0.0)
                val ramPct = if (totalRam > 0) ((usedRam / totalRam) * 100).toInt() else 50

                // Storage Telemetry
                val statFs = StatFs(Environment.getDataDirectory().path)
                val totalStorageBytes = statFs.blockCountLong * statFs.blockSizeLong
                val availStorageBytes = statFs.availableBlocksLong * statFs.blockSizeLong
                val usedStorageBytes = totalStorageBytes - availStorageBytes
                val totalStorageGb = totalStorageBytes / (1024f * 1024f * 1024f)
                val usedStorageGb = usedStorageBytes / (1024f * 1024f * 1024f)
                val storagePct = if (totalStorageBytes > 0) ((usedStorageBytes.toDouble() / totalStorageBytes.toDouble()) * 100).toInt() else 65

                // Audio Mode
                val ringerMode = audioManager?.ringerMode ?: AudioManager.RINGER_MODE_NORMAL
                val audioProfile = when (ringerMode) {
                    AudioManager.RINGER_MODE_SILENT -> "Silent"
                    AudioManager.RINGER_MODE_VIBRATE -> "Vibrate"
                    else -> "Normal"
                }

                // Wi-Fi
                val isWifi = try {
                    val network = connectivityManager?.activeNetwork
                    val caps = connectivityManager?.getNetworkCapabilities(network)
                    caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true || (wifiManager?.isWifiEnabled == true)
                } catch (_: Exception) {
                    true
                }

                // Bluetooth
                val isBt = try {
                    val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                    btManager?.adapter?.isEnabled == true
                } catch (_: Exception) {
                    false
                }

                // Carrier name
                val carrier = try {
                    val opName = telephonyManager?.networkOperatorName
                    if (!opName.isNullOrBlank()) opName else "RYZE"
                } catch (_: Exception) {
                    "RYZE"
                }

                // Data enabled
                val isData = try {
                    val network = connectivityManager?.activeNetwork
                    val caps = connectivityManager?.getNetworkCapabilities(network)
                    caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
                } catch (_: Exception) {
                    true
                }

                // Live dynamic network throughput speed simulation
                val simSpeed = (Random.nextFloat() * 120 + 20).toInt()
                val speedFormatted = "$simSpeed.${Random.nextInt(10, 99)} KB/s"

                _telemetryState.value = _telemetryState.value.copy(
                    timeFormatted = timeStr,
                    dateFormatted = dateStr,
                    stardateCode = stardate,
                    ramUsagePercent = ramPct,
                    ramUsedGb = usedRam.toFloat(),
                    ramTotalGb = totalRam.toFloat(),
                    storageUsagePercent = storagePct,
                    storageUsedGb = usedStorageGb,
                    storageTotalGb = totalStorageGb,
                    audioProfile = audioProfile,
                    isWifiEnabled = isWifi,
                    isDataEnabled = isData,
                    carrierName = carrier,
                    isBluetoothEnabled = isBt,
                    networkSpeedFormatted = speedFormatted
                )

                delay(1000)
            }
        }
    }

    fun setFlashlightState(isOn: Boolean) {
        _telemetryState.value = _telemetryState.value.copy(isFlashlightOn = isOn)
    }

    fun destroy() {
        try {
            batteryReceiver?.let { context.unregisterReceiver(it) }
        } catch (_: Exception) {
        }
    }
}
