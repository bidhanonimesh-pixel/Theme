package com.example.launcher

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.ai.JarvisVoiceEngine
import com.example.ai.JarvisVoiceState
import com.example.core.hardware.DeviceHardwareController
import com.example.core.model.CyberColorPalette
import com.example.core.model.LauncherSettings
import com.example.core.model.PhysicsMode
import com.example.core.sound.SoundEffectManager
import com.example.core.telemetry.SystemTelemetryManager
import com.example.core.telemetry.SystemTelemetryState
import com.example.data.AppCustomizationEntity
import com.example.data.AppDatabase
import com.example.data.QuickContactEntity
import com.example.launcher.model.AppItem
import com.example.launcher.model.OrbitAppSlot
import com.example.physics.PhysicsBody
import com.example.physics.PhysicsEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class LauncherUiState(
    val settings: LauncherSettings = LauncherSettings(),
    val telemetry: SystemTelemetryState = SystemTelemetryState(),
    val allInstalledApps: List<AppItem> = emptyList(),
    val visibleApps: List<AppItem> = emptyList(),
    val favoriteApps: List<AppItem> = emptyList(),
    val hiddenApps: List<AppItem> = emptyList(),
    val orbitSlots: List<OrbitAppSlot> = emptyList(),
    val quickContacts: List<QuickContactEntity> = emptyList(),
    val isAppDrawerOpen: Boolean = false,
    val isPhysicsPlaygroundOpen: Boolean = false,
    val isDialerOpen: Boolean = false,
    val isAdminPanelOpen: Boolean = false,
    val isVoiceOverlayOpen: Boolean = false,
    val isAppLockPromptOpen: Boolean = false,
    val pendingLaunchApp: AppItem? = null,
    val dialerInputText: String = "",
    val drawerSearchQuery: String = "",
    val selectedDrawerCategory: String = "All",
    val statusMessage: String? = null
)

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val db = Room.databaseBuilder(
        application,
        AppDatabase::class.java,
        "jarvis_launcher_db"
    ).fallbackToDestructiveMigration().build()
    private val dao = db.launcherDao()

    val soundManager = SoundEffectManager(application)
    val hardwareController = DeviceHardwareController(application)
    val telemetryManager = SystemTelemetryManager(application)
    val voiceEngine = JarvisVoiceEngine(application, hardwareController)
    val physicsEngine = PhysicsEngine()

    private val _settings = MutableStateFlow(LauncherSettings())
    private val _isAppDrawerOpen = MutableStateFlow(false)
    private val _isPhysicsPlaygroundOpen = MutableStateFlow(false)
    private val _isDialerOpen = MutableStateFlow(false)
    private val _isAdminPanelOpen = MutableStateFlow(false)
    private val _isVoiceOverlayOpen = MutableStateFlow(false)
    private val _isAppLockPromptOpen = MutableStateFlow(false)
    private val _pendingLaunchApp = MutableStateFlow<AppItem?>(null)
    private val _dialerInputText = MutableStateFlow("")
    private val _drawerSearchQuery = MutableStateFlow("")
    private val _selectedDrawerCategory = MutableStateFlow("All")
    private val _statusMessage = MutableStateFlow<String?>(null)

    private val _rawInstalledApps = MutableStateFlow<List<AppItem>>(emptyList())
    private val _physicsBodies = MutableStateFlow<List<PhysicsBody>>(emptyList())
    val physicsBodies: StateFlow<List<PhysicsBody>> = _physicsBodies.asStateFlow()

    val uiState: StateFlow<LauncherUiState> = combine(
        _settings,
        telemetryManager.telemetryState,
        _rawInstalledApps,
        dao.getAllCustomizationsFlow(),
        dao.getAllQuickContactsFlow(),
        _isAppDrawerOpen,
        _isPhysicsPlaygroundOpen,
        _isDialerOpen,
        _isAdminPanelOpen,
        _isVoiceOverlayOpen,
        _dialerInputText,
        _drawerSearchQuery,
        _selectedDrawerCategory
    ) { args: Array<Any> ->
        val settings = args[0] as LauncherSettings
        val telemetry = args[1] as SystemTelemetryState
        val rawApps = args[2] as List<AppItem>
        val customizations = args[3] as List<AppCustomizationEntity>
        val contacts = args[4] as List<QuickContactEntity>
        val isDrawer = args[5] as Boolean
        val isPhysics = args[6] as Boolean
        val isDialer = args[7] as Boolean
        val isAdmin = args[8] as Boolean
        val isVoice = args[9] as Boolean
        val dialerInput = args[10] as String
        val searchQuery = args[11] as String
        val selectedCategory = args[12] as String

        val custMap = customizations.associateBy { it.packageName }
        val mergedApps = rawApps.map { base ->
            val cust = custMap[base.packageName]
            if (cust != null) {
                base.copy(
                    appName = cust.customName ?: base.appName,
                    isHidden = cust.isHidden,
                    isLocked = cust.isLocked,
                    isFavorite = cust.isFavorite,
                    category = cust.category,
                    clickCount = cust.clickCount,
                    orbitSlotIndex = cust.orbitSlotIndex
                )
            } else {
                base
            }
        }

        val visible = mergedApps.filter { !it.isHidden }
        val hidden = mergedApps.filter { it.isHidden }
        val favorites = mergedApps.filter { it.isFavorite }

        val orbitSlots = buildOrbitSlots(mergedApps)

        LauncherUiState(
            settings = settings,
            telemetry = telemetry,
            allInstalledApps = mergedApps,
            visibleApps = visible,
            favoriteApps = favorites,
            hiddenApps = hidden,
            orbitSlots = orbitSlots,
            quickContacts = if (contacts.isNotEmpty()) contacts else getDefaultContacts(),
            isAppDrawerOpen = isDrawer,
            isPhysicsPlaygroundOpen = isPhysics,
            isDialerOpen = isDialer,
            isAdminPanelOpen = isAdmin,
            isVoiceOverlayOpen = isVoice,
            isAppLockPromptOpen = _isAppLockPromptOpen.value,
            pendingLaunchApp = _pendingLaunchApp.value,
            dialerInputText = dialerInput,
            drawerSearchQuery = searchQuery,
            selectedDrawerCategory = selectedCategory,
            statusMessage = _statusMessage.value
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = LauncherUiState()
    )

    init {
        loadInstalledApps()
        seedInitialQuickContacts()

        voiceEngine.setProviders(
            apps = { _rawInstalledApps.value },
            telemetry = { telemetryManager.telemetryState.value },
            apiKey = { _settings.value.customApiKey },
            onTorch = { state -> telemetryManager.setFlashlightState(state) }
        )
    }

    fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val pm = getApplication<Application>().packageManager
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
            val currentPkg = getApplication<Application>().packageName

            val apps = resolveInfos.mapNotNull { ri ->
                val pkg = ri.activityInfo.packageName
                if (pkg == currentPkg) return@mapNotNull null
                val label = ri.loadLabel(pm).toString()
                val icon = ri.loadIcon(pm)
                val category = categorizeApp(pkg, label)
                AppItem(
                    packageName = pkg,
                    appName = label,
                    icon = icon,
                    category = category
                )
            }.sortedBy { it.appName.lowercase() }

            withContext(Dispatchers.Main) {
                _rawInstalledApps.value = apps
            }
        }
    }

    private fun categorizeApp(pkg: String, name: String): String {
        val p = pkg.lowercase()
        val n = name.lowercase()
        return when {
            p.contains("camera") || p.contains("gallery") || p.contains("photo") || p.contains("video") || p.contains("youtube") || p.contains("music") || p.contains("spotify") || p.contains("netflix") -> "Media"
            p.contains("whatsapp") || p.contains("telegram") || p.contains("facebook") || p.contains("instagram") || p.contains("twitter") || p.contains("threads") || p.contains("tiktok") || p.contains("discord") || p.contains("messenger") -> "Social"
            p.contains("dialer") || p.contains("phone") || p.contains("contact") || p.contains("message") || p.contains("gmail") || p.contains("mail") -> "Communication"
            p.contains("game") || p.contains("play") || p.contains("puzzle") || p.contains("arcade") -> "Games"
            p.contains("setting") || p.contains("calc") || p.contains("clock") || p.contains("file") || p.contains("browser") || p.contains("chrome") -> "Tools"
            else -> "General"
        }
    }

    private fun buildOrbitSlots(apps: List<AppItem>): List<OrbitAppSlot> {
        // 6 Orbit Slots around the Arc Reactor HUD Core matching user screenshot!
        val topSlot = findAppByKeywords(apps, listOf("play", "store", "market", "vending"))
        val rightTopSlot = findAppByKeywords(apps, listOf("gmail", "email", "mail", "outlook"))
        val rightBottomSlot = findAppByKeywords(apps, listOf("google", "search", "chrome", "browser"))
        val bottomSlot = findAppByKeywords(apps, listOf("facebook", "meta", "social", "instagram", "twitter"))
        val leftBottomSlot = findAppByKeywords(apps, listOf("whatsapp", "message", "telegram", "chat"))
        val leftTopSlot = findAppByKeywords(apps, listOf("camera", "gallery", "lens", "photos"))

        return listOf(
            OrbitAppSlot(0, "Play", "PLAY", topSlot?.packageName, angleDegrees = 270f),
            OrbitAppSlot(1, "Mail", "MAIL", rightTopSlot?.packageName, angleDegrees = 330f),
            OrbitAppSlot(2, "Google", "G", rightBottomSlot?.packageName, angleDegrees = 30f),
            OrbitAppSlot(3, "Network", "NET", bottomSlot?.packageName, angleDegrees = 90f),
            OrbitAppSlot(4, "Chat", "CHAT", leftBottomSlot?.packageName, angleDegrees = 150f),
            OrbitAppSlot(5, "Optics", "VISION", leftTopSlot?.packageName, angleDegrees = 210f)
        )
    }

    private fun findAppByKeywords(apps: List<AppItem>, keywords: List<String>): AppItem? {
        return apps.firstOrNull { app ->
            val pkg = app.packageName.lowercase()
            val name = app.appName.lowercase()
            keywords.any { k -> pkg.contains(k) || name.contains(k) }
        }
    }

    private fun seedInitialQuickContacts() {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = dao.getAllQuickContactsFlow()
            // Quick default contacts
            val defaults = getDefaultContacts()
            defaults.forEach { dao.insertContact(it) }
        }
    }

    private fun getDefaultContacts(): List<QuickContactEntity> = listOf(
        QuickContactEntity("1", "Pepper Potts", "+15550198", "PP", true, "#00E5FF"),
        QuickContactEntity("2", "James Rhodes", "+15550244", "JR", true, "#FF6D00"),
        QuickContactEntity("3", "Bruce Banner", "+15550377", "BB", true, "#00E676"),
        QuickContactEntity("4", "Happy Hogan", "+15550412", "HH", true, "#FFD600"),
        QuickContactEntity("5", "Peter Parker", "+15550599", "PP", true, "#FF1744"),
        QuickContactEntity("6", "Nick Fury", "+15550700", "NF", true, "#D500F9")
    )

    fun onAppClicked(app: AppItem) {
        if (_settings.value.soundEffectsEnabled) {
            soundManager.playSciFiBeep(SoundEffectManager.ToneType.CLICK)
        }
        if (_settings.value.hapticFeedbackEnabled) {
            soundManager.performHaptic(20)
        }

        if (app.isLocked) {
            _pendingLaunchApp.value = app
            _isAppLockPromptOpen.value = true
        } else {
            launchAppInternal(app.packageName)
        }
    }

    fun verifyAppLockPin(pin: String): Boolean {
        if (pin == _settings.value.adminPin) {
            val target = _pendingLaunchApp.value
            _isAppLockPromptOpen.value = false
            _pendingLaunchApp.value = null
            if (target != null) {
                launchAppInternal(target.packageName)
            }
            return true
        }
        soundManager.playSciFiBeep(SoundEffectManager.ToneType.ALERT)
        return false
    }

    fun dismissAppLockPrompt() {
        _isAppLockPromptOpen.value = false
        _pendingLaunchApp.value = null
    }

    private fun launchAppInternal(packageName: String) {
        hardwareController.launchApp(packageName) { err ->
            _statusMessage.value = err
        }
        viewModelScope.launch(Dispatchers.IO) {
            dao.incrementClickCount(packageName)
        }
        _isAppDrawerOpen.value = false
        _isPhysicsPlaygroundOpen.value = false
    }

    fun toggleAppDrawer(open: Boolean? = null) {
        val newState = open ?: !_isAppDrawerOpen.value
        _isAppDrawerOpen.value = newState
        if (_settings.value.soundEffectsEnabled) {
            soundManager.playSciFiBeep(if (newState) SoundEffectManager.ToneType.CONFIRM else SoundEffectManager.ToneType.CLICK)
        }
    }

    fun togglePhysicsPlayground(open: Boolean? = null) {
        val newState = open ?: !_isPhysicsPlaygroundOpen.value
        _isPhysicsPlaygroundOpen.value = newState
        if (newState) {
            initPhysicsSimulation()
        }
    }

    fun initPhysicsSimulation(width: Float = 1080f, height: Float = 1920f) {
        val visibleApps = _rawInstalledApps.value.filter { !it.isHidden }
        val sampleApps = if (visibleApps.isNotEmpty()) visibleApps else _rawInstalledApps.value
        physicsEngine.currentMode = _settings.value.physicsMode
        physicsEngine.updateParameters(_settings.value.physicsGravityStrength, _settings.value.physicsBounciness)
        val bodies = physicsEngine.initBodies(sampleApps.take(24), width, height)
        _physicsBodies.value = bodies
    }

    fun triggerExplosionBurst() {
        soundManager.playSciFiBeep(SoundEffectManager.ToneType.ALERT)
        physicsEngine.explodeBurst(_physicsBodies.value)
    }

    fun setPhysicsMode(mode: PhysicsMode) {
        _settings.value = _settings.value.copy(physicsMode = mode)
        physicsEngine.currentMode = mode
    }

    fun toggleDialer(open: Boolean? = null) {
        val newState = open ?: !_isDialerOpen.value
        _isDialerOpen.value = newState
        if (_settings.value.soundEffectsEnabled) {
            soundManager.playSciFiBeep(SoundEffectManager.ToneType.CLICK)
        }
    }

    fun toggleAdminPanel(open: Boolean? = null) {
        _isAdminPanelOpen.value = open ?: !_isAdminPanelOpen.value
        if (_settings.value.soundEffectsEnabled) {
            soundManager.playSciFiBeep(SoundEffectManager.ToneType.CONFIRM)
        }
    }

    fun toggleVoiceOverlay(open: Boolean? = null) {
        val newState = open ?: !_isVoiceOverlayOpen.value
        _isVoiceOverlayOpen.value = newState
        if (newState) {
            voiceEngine.startListening()
        } else {
            voiceEngine.stopListening()
        }
    }

    fun onDialerDigitPressed(digit: Char) {
        _dialerInputText.value = _dialerInputText.value + digit
        if (_settings.value.soundEffectsEnabled) {
            val tone = when (digit) {
                '0' -> SoundEffectManager.ToneType.DIAL_0
                '1' -> SoundEffectManager.ToneType.DIAL_1
                '2' -> SoundEffectManager.ToneType.DIAL_2
                '3' -> SoundEffectManager.ToneType.DIAL_3
                '4' -> SoundEffectManager.ToneType.DIAL_4
                '5' -> SoundEffectManager.ToneType.DIAL_5
                '6' -> SoundEffectManager.ToneType.DIAL_6
                '7' -> SoundEffectManager.ToneType.DIAL_7
                '8' -> SoundEffectManager.ToneType.DIAL_8
                '9' -> SoundEffectManager.ToneType.DIAL_9
                '*' -> SoundEffectManager.ToneType.DIAL_STAR
                '#' -> SoundEffectManager.ToneType.DIAL_POUND
                else -> SoundEffectManager.ToneType.CLICK
            }
            soundManager.playSciFiBeep(tone)
        }
        if (_settings.value.hapticFeedbackEnabled) {
            soundManager.performHaptic(15)
        }
    }

    fun onDialerBackspace() {
        if (_dialerInputText.value.isNotEmpty()) {
            _dialerInputText.value = _dialerInputText.value.dropLast(1)
            soundManager.performHaptic(10)
        }
    }

    fun onDialerClear() {
        _dialerInputText.value = ""
        soundManager.performHaptic(30)
    }

    fun onInitiateCall(number: String = _dialerInputText.value) {
        if (number.isNotBlank()) {
            hardwareController.dialPhoneNumber(number)
            soundManager.playSciFiBeep(SoundEffectManager.ToneType.CONFIRM)
        }
    }

    fun onToggleFlashlight() {
        hardwareController.toggleFlashlight { isOn ->
            telemetryManager.setFlashlightState(isOn)
            soundManager.playSciFiBeep(SoundEffectManager.ToneType.CLICK)
        }
    }

    fun onCycleAudioProfile() {
        hardwareController.cycleAudioMode()
        soundManager.playSciFiBeep(SoundEffectManager.ToneType.CONFIRM)
    }

    fun updateSettings(newSettings: LauncherSettings) {
        _settings.value = newSettings
        physicsEngine.updateParameters(newSettings.physicsGravityStrength, newSettings.physicsBounciness)
        physicsEngine.currentMode = newSettings.physicsMode
    }

    fun updateAppCustomization(
        packageName: String,
        customName: String?,
        isHidden: Boolean,
        isLocked: Boolean,
        isFavorite: Boolean,
        category: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val entity = AppCustomizationEntity(
                packageName = packageName,
                customName = customName,
                isHidden = isHidden,
                isLocked = isLocked,
                isFavorite = isFavorite,
                category = category
            )
            dao.saveCustomization(entity)
        }
    }

    fun setDrawerSearchQuery(q: String) {
        _drawerSearchQuery.value = q
    }

    fun setDrawerCategory(cat: String) {
        _selectedDrawerCategory.value = cat
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        soundManager.release()
        telemetryManager.destroy()
        voiceEngine.destroy()
    }
}
