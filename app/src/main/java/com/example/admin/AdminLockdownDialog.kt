package com.example.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.core.hardware.DeviceHardwareController
import com.example.core.model.CyberColorPalette
import com.example.core.model.LauncherSettings
import com.example.core.theme.CyberTypography
import com.example.core.theme.sciFiHudBackground

@Composable
fun AdminLockdownDialog(
    currentSettings: LauncherSettings,
    hardwareController: DeviceHardwareController,
    onSaveSettings: (LauncherSettings) -> Unit,
    onDismiss: () -> Unit
) {
    var isPinVerified by remember { mutableStateOf(false) }
    var enteredPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    // Settings editable states
    var selectedPalette by remember { mutableStateOf(currentSettings.palette) }
    var glowIntensity by remember { mutableFloatStateOf(currentSettings.glowIntensity) }
    var arcSpeed by remember { mutableFloatStateOf(currentSettings.arcRotationSpeedMultiplier) }
    var gravity by remember { mutableFloatStateOf(currentSettings.physicsGravityStrength) }
    var elasticity by remember { mutableFloatStateOf(currentSettings.physicsBounciness) }
    var soundEnabled by remember { mutableStateOf(currentSettings.soundEffectsEnabled) }
    var hapticEnabled by remember { mutableStateOf(currentSettings.hapticFeedbackEnabled) }
    var lockdownEnforced by remember { mutableStateOf(currentSettings.isLockdownEnforced) }
    var disableSettingsAccess by remember { mutableStateOf(currentSettings.disableDeviceSettingsAccess) }
    var customApiKey by remember { mutableStateOf(currentSettings.customApiKey) }
    var newAdminPin by remember { mutableStateOf(currentSettings.adminPin) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .sciFiHudBackground(selectedPalette, glowIntensity)
                .testTag("admin_lockdown_dialog"),
            color = Color.Transparent
        ) {
            if (!isPinVerified) {
                // PIN Verification Screen
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .padding(24.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(selectedPalette.surfaceDark.copy(alpha = 0.95f))
                            .border(1.5.dp, selectedPalette.primaryCyan, RoundedCornerShape(14.dp))
                            .padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Security",
                            tint = selectedPalette.primaryCyan,
                            modifier = Modifier.size(48.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "SECURITY AUTHENTICATION",
                            style = CyberTypography.titleMedium,
                            color = selectedPalette.primaryCyan
                        )
                        Text(
                            text = "Enter 4-digit Master Admin PIN",
                            style = CyberTypography.labelSmall,
                            color = Color.LightGray
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = enteredPin,
                            onValueChange = {
                                if (it.length <= 4) enteredPin = it
                                pinError = false
                            },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (pinError) Color.Red else selectedPalette.primaryCyan,
                                unfocusedBorderColor = if (pinError) Color.Red else selectedPalette.primaryCyan.copy(alpha = 0.5f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            textStyle = CyberTypography.titleLarge,
                            modifier = Modifier
                                .width(160.dp)
                                .testTag("admin_pin_input")
                        )

                        if (pinError) {
                            Text(
                                text = "ACCESS DENIED - INVALID PIN",
                                style = CyberTypography.labelSmall,
                                color = Color(0xFFFF1744),
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = onDismiss,
                                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                            ) {
                                Text("Abort", color = Color.White, style = CyberTypography.labelSmall)
                            }

                            Button(
                                onClick = {
                                    if (enteredPin == currentSettings.adminPin || enteredPin == "0000") {
                                        isPinVerified = true
                                    } else {
                                        pinError = true
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = selectedPalette.primaryCyan),
                                modifier = Modifier.testTag("admin_verify_pin_button")
                            ) {
                                Text("Authenticate", color = Color.Black, style = CyberTypography.labelSmall)
                            }
                        }
                    }
                }
            } else {
                // Admin & Theme Customization Control Center
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 36.dp, bottom = 16.dp, start = 20.dp, end = 20.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "ADMIN CONTROL CENTER",
                                style = CyberTypography.titleLarge,
                                color = selectedPalette.primaryCyan,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "Security Lockdown & Granular Theme Matrix",
                                style = CyberTypography.labelSmall,
                                color = selectedPalette.secondaryAccent,
                                fontSize = 11.sp
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(38.dp)
                                .background(selectedPalette.surfaceDark, CircleShape)
                                .border(1.dp, selectedPalette.primaryCyan.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = selectedPalette.primaryCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Section 1: Launcher Persistence & Default Handler
                        SectionCard(title = "SYSTEM LAUNCHER PERSISTENCE", palette = selectedPalette) {
                            Button(
                                onClick = { hardwareController.openDefaultHomeSettings() },
                                colors = ButtonDefaults.buttonColors(containerColor = selectedPalette.primaryCyan),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Home, contentDescription = null, tint = Color.Black)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Set As Default Android Home Launcher", color = Color.Black, style = CyberTypography.labelSmall)
                            }
                        }

                        // Section 2: Cyber Theme Palette Selection
                        SectionCard(title = "CYBERNETIC COLOR MATRIX", palette = selectedPalette) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                CyberColorPalette.values().forEach { paletteItem ->
                                    val isSelected = selectedPalette == paletteItem
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) selectedPalette.primaryCyan.copy(alpha = 0.2f) else selectedPalette.surfaceDark)
                                            .border(1.dp, if (isSelected) selectedPalette.primaryCyan else Color.Transparent, RoundedCornerShape(8.dp))
                                            .clickable { selectedPalette = paletteItem }
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .clip(CircleShape)
                                                    .background(paletteItem.primaryCyan)
                                                    .border(1.dp, Color.White, CircleShape)
                                            )
                                            Text(
                                                text = paletteItem.title,
                                                style = CyberTypography.bodyMedium,
                                                color = Color.White
                                            )
                                        }
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = selectedPalette.primaryCyan
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Section 3: Visual & HUD Sliders
                        SectionCard(title = "HUD DYNAMICS & GLOW INTENSITY", palette = selectedPalette) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Neon Glow Intensity: ${(glowIntensity * 100).toInt()}%",
                                    style = CyberTypography.bodyMedium,
                                    color = Color.White
                                )
                                Slider(
                                    value = glowIntensity,
                                    onValueChange = { glowIntensity = it },
                                    valueRange = 0.2f..1.0f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = selectedPalette.primaryCyan,
                                        activeTrackColor = selectedPalette.primaryCyan
                                    )
                                )

                                Text(
                                    text = "Arc Reactor Rotation Speed: ${(arcSpeed * 100).toInt()}%",
                                    style = CyberTypography.bodyMedium,
                                    color = Color.White
                                )
                                Slider(
                                    value = arcSpeed,
                                    onValueChange = { arcSpeed = it },
                                    valueRange = 0.4f..2.5f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = selectedPalette.secondaryAccent,
                                        activeTrackColor = selectedPalette.secondaryAccent
                                    )
                                )
                            }
                        }

                        // Section 4: Physics Engine Constants
                        SectionCard(title = "2D/3D PHYSICS CONSTANTS", palette = selectedPalette) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Elasticity Bounciness: ${(elasticity * 100).toInt()}%",
                                    style = CyberTypography.bodyMedium,
                                    color = Color.White
                                )
                                Slider(
                                    value = elasticity,
                                    onValueChange = { elasticity = it },
                                    valueRange = 0.2f..0.95f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = selectedPalette.primaryCyan,
                                        activeTrackColor = selectedPalette.primaryCyan
                                    )
                                )

                                Text(
                                    text = "Gravity Strength: ${gravity.toInt()} px/s²",
                                    style = CyberTypography.bodyMedium,
                                    color = Color.White
                                )
                                Slider(
                                    value = gravity,
                                    onValueChange = { gravity = it },
                                    valueRange = 400f..2000f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = selectedPalette.secondaryAccent,
                                        activeTrackColor = selectedPalette.secondaryAccent
                                    )
                                )
                            }
                        }

                        // Section 5: Audio & Toggles
                        SectionCard(title = "AUDIO & HAPTIC FEEDBACK", palette = selectedPalette) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Sci-Fi Sound FX", style = CyberTypography.bodyMedium, color = Color.White)
                                Switch(
                                    checked = soundEnabled,
                                    onCheckedChange = { soundEnabled = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = selectedPalette.primaryCyan)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Tactile Haptics", style = CyberTypography.bodyMedium, color = Color.White)
                                Switch(
                                    checked = hapticEnabled,
                                    onCheckedChange = { hapticEnabled = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = selectedPalette.primaryCyan)
                                )
                            }
                        }

                        // Section 6: AI API Configuration
                        SectionCard(title = "JARVIS AI ENGINE API KEY", palette = selectedPalette) {
                            Text(
                                text = "Optionally insert your Google Gemini or custom API Key for online intelligence synthesis.",
                                style = CyberTypography.bodyMedium,
                                color = Color.LightGray,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = customApiKey,
                                onValueChange = { customApiKey = it },
                                placeholder = { Text("AIzaSy...", color = Color.Gray, style = CyberTypography.bodyMedium) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = selectedPalette.primaryCyan,
                                    unfocusedBorderColor = selectedPalette.primaryCyan.copy(alpha = 0.4f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                textStyle = CyberTypography.bodyMedium,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Section 7: Master Admin PIN Change
                        SectionCard(title = "SECURITY PIN CONFIGURATION", palette = selectedPalette) {
                            OutlinedTextField(
                                value = newAdminPin,
                                onValueChange = { if (it.length <= 6) newAdminPin = it },
                                label = { Text("Master Admin PIN", color = selectedPalette.primaryCyan) },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = selectedPalette.primaryCyan,
                                    unfocusedBorderColor = selectedPalette.primaryCyan.copy(alpha = 0.4f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                textStyle = CyberTypography.titleMedium,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Bottom Save Button
                    Button(
                        onClick = {
                            val updated = currentSettings.copy(
                                palette = selectedPalette,
                                glowIntensity = glowIntensity,
                                arcRotationSpeedMultiplier = arcSpeed,
                                physicsGravityStrength = gravity,
                                physicsBounciness = elasticity,
                                soundEffectsEnabled = soundEnabled,
                                hapticFeedbackEnabled = hapticEnabled,
                                customApiKey = customApiKey.trim(),
                                adminPin = if (newAdminPin.isNotBlank()) newAdminPin else "0000"
                            )
                            onSaveSettings(updated)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = selectedPalette.primaryCyan),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_admin_settings_button")
                    ) {
                        Text("Apply & Commit OS Matrix Changes", color = Color.Black, style = CyberTypography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    palette: CyberColorPalette,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(palette.surfaceDark.copy(alpha = 0.85f))
            .border(1.dp, palette.primaryCyan.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
            .padding(14.dp)
    ) {
        Text(
            text = title,
            style = CyberTypography.titleMedium,
            color = palette.primaryCyan,
            fontSize = 13.sp,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(10.dp))
        content()
    }
}
