package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.example.physics.PhysicsPlaygroundScreen

class MainActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            val physicsBodies by viewModel.physicsBodies.collectAsState()
            val snackbarHostState = remember { SnackbarHostState() }
            val context = LocalContext.current

            // Audio & Call Permission Request
            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                // Handled gracefully
            }

            LaunchedEffect(Unit) {
                val permissionsToRequest = mutableListOf(
                    Manifest.permission.RECORD_AUDIO
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                }
                permissionLauncher.launch(permissionsToRequest.toTypedArray())
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
                    uiState.isAppDrawerOpen -> viewModel.toggleAppDrawer(false)
                    uiState.isPhysicsPlaygroundOpen -> viewModel.togglePhysicsPlayground(false)
                    uiState.isDialerOpen -> viewModel.toggleDialer(false)
                    uiState.isAdminPanelOpen -> viewModel.toggleAdminPanel(false)
                    uiState.isVoiceOverlayOpen -> viewModel.toggleVoiceOverlay(false)
                    uiState.isAppLockPromptOpen -> viewModel.dismissAppLockPrompt()
                    else -> {
                        // Home Launcher base screen: do nothing on back
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

                        // 2. Action Toolbar (Settings, Torch, Radar)
                        HudActionToolbar(
                            palette = palette,
                            glowIntensity = glow,
                            isTorchOn = uiState.telemetry.isFlashlightOn,
                            onSettingsClick = { viewModel.toggleAdminPanel(true) },
                            onFlashlightClick = { viewModel.onToggleFlashlight() },
                            onPhysicsClick = { viewModel.togglePhysicsPlayground(true) },
                            onAiMicClick = { viewModel.toggleVoiceOverlay(true) }
                        )

                        // 3. Central Animated Holographic Arc Reactor Core with 6 Orbiting Apps
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            JarvisArcReactorCore(
                                palette = palette,
                                glowIntensity = glow,
                                rotationSpeedMultiplier = uiState.settings.arcRotationSpeedMultiplier,
                                orbitSlots = uiState.orbitSlots,
                                allApps = uiState.allInstalledApps,
                                onSlotClick = { slot ->
                                    val assignedPkg = slot.assignedPackage
                                    val app = assignedPkg?.let { pkg ->
                                        uiState.allInstalledApps.firstOrNull { it.packageName == pkg }
                                    }
                                    if (app != null) {
                                        viewModel.onAppClicked(app)
                                    } else {
                                        viewModel.toggleAppDrawer(true)
                                    }
                                },
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

                    // 9. Admin & Theme Lockdown Control Center
                    if (uiState.isAdminPanelOpen) {
                        AdminLockdownDialog(
                            currentSettings = uiState.settings,
                            hardwareController = viewModel.hardwareController,
                            onSaveSettings = { updated -> viewModel.updateSettings(updated) },
                            onDismiss = { viewModel.toggleAdminPanel(false) }
                        )
                    }

                    // 10. App PIN Lock Prompt Dialog
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
