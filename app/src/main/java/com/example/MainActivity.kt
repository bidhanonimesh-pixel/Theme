package com.example

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.admin.AdminLockdownDialog
import com.example.ai.JarvisOverlayDialog
import com.example.core.theme.CyberTypography
import com.example.core.theme.sciFiHudBackground
import com.example.dialer.SciFiDialerScreen
import com.example.launcher.LauncherViewModel
import com.example.launcher.components.AppDrawerDialog
import com.example.launcher.components.AppLockPromptDialog
import com.example.launcher.components.HudActionToolbar
import com.example.launcher.components.HudBottomDock
import com.example.launcher.components.HudTopTelemetry
import com.example.launcher.components.JarvisArcReactorCore
import com.example.launcher.components.JarvisCircularAppWheel
import com.example.launcher.components.WheelAppPickerDialog
import com.example.physics.PhysicsPlaygroundScreen
import com.example.vision.JarvisVisionScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            val physicsBodies by viewModel.physicsBodies.collectAsState()
            val snackbarHostState = remember { SnackbarHostState() }
            val coroutineScope = rememberCoroutineScope()
            val context = LocalContext.current

            var showPermissionRationaleBanner by remember { mutableStateOf(false) }

            // Safe Multi-Permission Request Launcher
            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { permissionsMap ->
                val allEssentialGranted = permissionsMap.entries.all { it.value }
                if (!allEssentialGranted) {
                    val denied = permissionsMap.filter { !it.value }.keys.map { it.substringAfterLast(".") }
                    if (denied.isNotEmpty()) {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("JARVIS Core active (Sensors restricted: ${denied.joinToString(", ")})")
                        }
                    }
                }
            }

            // Launch runtime permissions on startup safely with try-catch
            LaunchedEffect(Unit) {
                try {
                    val permissionsList = mutableListOf(
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.CAMERA,
                        Manifest.permission.READ_CONTACTS,
                        Manifest.permission.CALL_PHONE
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionsList.add(Manifest.permission.POST_NOTIFICATIONS)
                    }

                    val ungranted = permissionsList.filter {
                        ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
                    }

                    if (ungranted.isNotEmpty()) {
                        permissionLauncher.launch(ungranted.toTypedArray())
                    }
                } catch (e: Throwable) {
                    // Prevent any permission request exception from crashing the activity
                }
            }

            LaunchedEffect(uiState.statusMessage) {
                uiState.statusMessage?.let { msg ->
                    snackbarHostState.showSnackbar(msg)
                    viewModel.clearStatusMessage()
                }
            }

            // Android Launcher Back button handling
            BackHandler {
                when {
                    uiState.isVisionScreenOpen -> viewModel.toggleVisionScreen(false)
                    uiState.isWheelAppPickerOpen -> viewModel.closeWheelAppPicker()
                    uiState.isAppDrawerOpen -> viewModel.toggleAppDrawer(false)
                    uiState.isPhysicsPlaygroundOpen -> viewModel.togglePhysicsPlayground(false)
                    uiState.isDialerOpen -> viewModel.toggleDialer(false)
                    uiState.isAdminPanelOpen -> viewModel.toggleAdminPanel(false)
                    uiState.isVoiceOverlayOpen -> viewModel.toggleVoiceOverlay(false)
                    uiState.isAppLockPromptOpen -> viewModel.dismissAppLockPrompt()
                    else -> {
                        // Home Launcher base screen: persistent root
                    }
                }
            }

            val palette = uiState.settings.palette
            val glow = uiState.settings.glowIntensity

            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("main_launcher_scaffold"),
                snackbarHost = { SnackbarHost(snackbarHostState) },
                containerColor = palette.backgroundDark
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .sciFiHudBackground(palette, glow)
                        .pointerInput(Unit) {
                            var verticalDragTotal = 0f
                            detectVerticalDragGestures(
                                onDragStart = { verticalDragTotal = 0f },
                                onDragEnd = {
                                    if (verticalDragTotal > 60f) {
                                        // Swipe Down: Open JARVIS Voice Assistant
                                        viewModel.toggleVoiceOverlay(true)
                                    } else if (verticalDragTotal < -60f) {
                                        // Swipe Up: Open All Apps Drawer
                                        viewModel.toggleAppDrawer(true)
                                    }
                                    verticalDragTotal = 0f
                                },
                                onDragCancel = { verticalDragTotal = 0f },
                                onVerticalDrag = { _, dragAmount ->
                                    verticalDragTotal += dragAmount
                                }
                            )
                        }
                ) {
                    // Main Launcher Home HUD Content
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 1. Top Telemetry & Clock Wings
                        HudTopTelemetry(
                            telemetry = uiState.telemetry,
                            palette = palette,
                            glowIntensity = glow,
                            onWifiClick = { viewModel.hardwareController.openWifiSettings() },
                            onAudioClick = { viewModel.onCycleAudioProfile() }
                        )

                        // 2. Action Toolbar (Settings, Torch, Radar, Vision)
                        HudActionToolbar(
                            palette = palette,
                            glowIntensity = glow,
                            isTorchOn = uiState.telemetry.isFlashlightOn,
                            onSettingsClick = { viewModel.toggleAdminPanel(true) },
                            onFlashlightClick = { viewModel.onToggleFlashlight() },
                            onPhysicsClick = { viewModel.togglePhysicsPlayground(true) },
                            onVisionClick = { viewModel.toggleVisionScreen(true) },
                            onAiMicClick = { viewModel.toggleVoiceOverlay(true) }
                        )

                        // 3. Central Dynamic Canvas Arc Reactor & Rotating 5-App Orbital Dock
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            // Layer A: Central Core Reactor Canvas & Diagnostics
                            JarvisArcReactorCore(
                                palette = palette,
                                glowIntensity = glow,
                                rotationSpeedMultiplier = uiState.settings.arcRotationSpeedMultiplier,
                                onCenterCoreClick = {
                                    viewModel.toggleVoiceOverlay(true)
                                },
                                onDialerShortcutClick = {
                                    viewModel.toggleDialer(true)
                                },
                                onTorchShortcutClick = {
                                    viewModel.onToggleFlashlight()
                                }
                            )

                            // Layer B: Interactive 5-App Circular Wheel Dock
                            JarvisCircularAppWheel(
                                palette = palette,
                                glowIntensity = glow,
                                rotationSpeedMultiplier = uiState.settings.arcRotationSpeedMultiplier,
                                favoriteAppPackages = uiState.wheelAppPackages,
                                allApps = uiState.allInstalledApps,
                                onAppClick = { app -> viewModel.onAppClicked(app) },
                                onSlotLongClick = { slotIdx -> viewModel.openWheelAppPicker(slotIdx) }
                            )
                        }

                        // 4. Bottom Dock (Camera, Chrome, Dialer, Hub) & Drawer Trigger
                        HudBottomDock(
                            palette = palette,
                            glowIntensity = glow,
                            allApps = uiState.allInstalledApps,
                            onAppClick = { app -> viewModel.onAppClicked(app) },
                            onDialerClick = { viewModel.toggleDialer(true) },
                            onDrawerClick = { viewModel.toggleAppDrawer(true) }
                        )
                    }

                    // --- Modals & Overlays ---

                    // 5. Fullscreen App Drawer Modal
                    if (uiState.isAppDrawerOpen) {
                        AppDrawerDialog(
                            palette = palette,
                            glowIntensity = glow,
                            allApps = uiState.allInstalledApps,
                            visibleApps = uiState.visibleApps,
                            favoriteApps = uiState.favoriteApps,
                            hiddenApps = uiState.hiddenApps,
                            searchQuery = uiState.drawerSearchQuery,
                            selectedCategory = uiState.selectedDrawerCategory,
                            onSearchChange = { viewModel.setDrawerSearchQuery(it) },
                            onCategoryChange = { viewModel.setDrawerCategory(it) },
                            onAppClick = { app -> viewModel.onAppClicked(app) },
                            onUpdateAppCustomization = { pkg, name, hide, lock, fav, cat ->
                                viewModel.updateAppCustomization(pkg, name, hide, lock, fav, cat)
                            },
                            onDismiss = { viewModel.toggleAppDrawer(false) }
                        )
                    }

                    // 6. Interactive 2D/3D Physics Playground Screen
                    if (uiState.isPhysicsPlaygroundOpen) {
                        PhysicsPlaygroundScreen(
                            palette = palette,
                            glowIntensity = glow,
                            physicsEngine = viewModel.physicsEngine,
                            physicsBodies = physicsBodies,
                            currentMode = uiState.settings.physicsMode,
                            onModeChange = { mode -> viewModel.setPhysicsMode(mode) },
                            onExplosionClick = { viewModel.triggerExplosionBurst() },
                            onAppClick = { app -> viewModel.onAppClicked(app) },
                            onClose = { viewModel.togglePhysicsPlayground(false) }
                        )
                    }

                    // 7. Futuristic Sci-Fi Dialer Screen
                    if (uiState.isDialerOpen) {
                        SciFiDialerScreen(
                            palette = palette,
                            glowIntensity = glow,
                            dialerInput = uiState.dialerInputText,
                            quickContacts = uiState.quickContacts,
                            onDigitPress = { digit -> viewModel.onDialerDigitPressed(digit) },
                            onBackspace = { viewModel.onDialerBackspace() },
                            onClear = { viewModel.onDialerClear() },
                            onCallInitiate = { number -> viewModel.onInitiateCall(number) },
                            onSendSms = { number -> viewModel.hardwareController.sendSms(number) },
                            onClose = { viewModel.toggleDialer(false) }
                        )
                    }

                    // 8. JARVIS AI Voice Overlay Dialog
                    if (uiState.isVoiceOverlayOpen) {
                        JarvisOverlayDialog(
                            palette = palette,
                            glowIntensity = glow,
                            voiceEngine = viewModel.voiceEngine,
                            onDismiss = { viewModel.toggleVoiceOverlay(false) }
                        )
                    }

                    // 9. AI Vision & Tactical Optical Scanner
                    if (uiState.isVisionScreenOpen) {
                        JarvisVisionScreen(
                            palette = palette,
                            glowIntensity = glow,
                            onAnalyzeTarget = { result ->
                                viewModel.voiceEngine.speak("Optical reconnaissance complete. $result")
                            },
                            onClose = { viewModel.toggleVisionScreen(false) }
                        )
                    }

                    // 10. Admin & Theme Lockdown Control Center
                    if (uiState.isAdminPanelOpen) {
                        AdminLockdownDialog(
                            currentSettings = uiState.settings,
                            openRouterApiKey = uiState.openRouterApiKey,
                            openRouterModel = uiState.openRouterModel,
                            hardwareController = viewModel.hardwareController,
                            onSaveSettings = { updatedSettings, orKey, orModel ->
                                viewModel.updateSettings(updatedSettings, orKey, orModel)
                            },
                            onDismiss = { viewModel.toggleAdminPanel(false) }
                        )
                    }

                    // 11. 5-App Circular Wheel Slot Customizer Picker
                    if (uiState.isWheelAppPickerOpen) {
                        WheelAppPickerDialog(
                            slotIndex = uiState.selectedWheelSlotIndex,
                            palette = palette,
                            glowIntensity = glow,
                            allApps = uiState.allInstalledApps,
                            onAppSelected = { app ->
                                viewModel.assignAppToWheelSlot(uiState.selectedWheelSlotIndex, app.packageName)
                            },
                            onDismiss = { viewModel.closeWheelAppPicker() }
                        )
                    }

                    // 12. App PIN Lock Prompt Dialog
                    if (uiState.isAppLockPromptOpen && uiState.pendingLaunchApp != null) {
                        AppLockPromptDialog(
                            app = uiState.pendingLaunchApp!!,
                            palette = palette,
                            onVerifyPin = { pin -> viewModel.verifyAppLockPin(pin) },
                            onDismiss = { viewModel.dismissAppLockPrompt() }
                        )
                    }
                }
            }
        }
    }
}
