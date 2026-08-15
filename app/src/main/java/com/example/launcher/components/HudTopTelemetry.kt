package com.example.launcher.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NetworkCell
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.CyberColorPalette
import com.example.core.telemetry.SystemTelemetryState
import com.example.core.theme.CyberTypography
import com.example.core.theme.HudTopLeftPanelShape
import com.example.core.theme.HudTopRightPanelShape

@Composable
fun HudTopTelemetry(
    telemetry: SystemTelemetryState,
    palette: CyberColorPalette,
    glowIntensity: Float,
    onWifiClick: () -> Unit,
    onAudioClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "hudPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringRotation"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        // Status Bar network speed & status banner
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${telemetry.timeFormatted} | ${telemetry.networkSpeedFormatted}",
                style = CyberTypography.labelSmall,
                color = palette.primaryCyan.copy(alpha = 0.85f),
                fontSize = 11.sp
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "5G+",
                    style = CyberTypography.labelSmall,
                    color = palette.secondaryAccent,
                    fontSize = 10.sp
                )
                Icon(
                    imageVector = Icons.Default.NetworkCell,
                    contentDescription = "Signal",
                    tint = palette.primaryCyan,
                    modifier = Modifier.size(13.dp)
                )
                Box(
                    modifier = Modifier
                        .border(1.dp, palette.primaryCyan.copy(alpha = 0.7f), RoundedCornerShape(3.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "${telemetry.batteryPercent}",
                        style = CyberTypography.labelSmall,
                        color = palette.primaryCyan,
                        fontSize = 10.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Left & Right HUD Telemetry Wings + Center Time Widget
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // LEFT WING: Network Info
            Box(
                modifier = Modifier
                    .width(118.dp)
                    .clip(HudTopLeftPanelShape)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                palette.surfaceDark.copy(alpha = 0.88f),
                                Color(0xFF020710).copy(alpha = 0.95f)
                            )
                        )
                    )
                    .border(1.dp, palette.primaryCyan.copy(alpha = 0.4f * glowIntensity), HudTopLeftPanelShape)
                    .padding(8.dp)
                    .clickable { onWifiClick() }
            ) {
                Column {
                    // Header
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(palette.primaryCyan.copy(alpha = 0.12f), RoundedCornerShape(2.dp))
                            .padding(vertical = 2.dp)
                    ) {
                        Text(
                            text = "Network Info",
                            style = CyberTypography.labelSmall,
                            color = palette.primaryCyan,
                            fontSize = 10.sp,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Wi-Fi
                    TelemetryRow(
                        icon = Icons.Default.Wifi,
                        label = if (telemetry.isWifiEnabled) "Online" else "Disabled",
                        tint = if (telemetry.isWifiEnabled) palette.primaryCyan else Color.Gray
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Data
                    TelemetryRow(
                        icon = Icons.Default.NetworkCell,
                        label = if (telemetry.isDataEnabled) "Enabled" else "Disabled",
                        tint = if (telemetry.isDataEnabled) palette.primaryCyan else Color.Gray
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Carrier
                    TelemetryRow(
                        icon = Icons.Default.NetworkCell,
                        label = telemetry.carrierName,
                        tint = palette.secondaryAccent
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Bluetooth
                    TelemetryRow(
                        icon = Icons.Default.Bluetooth,
                        label = if (telemetry.isBluetoothEnabled) "Active" else "Disabled",
                        tint = if (telemetry.isBluetoothEnabled) palette.primaryCyan else Color.Gray
                    )
                }
            }

            // CENTER: Futuristic Hologram Clock & Orbit Ring
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .weight(1f)
            ) {
                Text(
                    text = telemetry.timeFormatted,
                    style = CyberTypography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    letterSpacing = 1.sp
                )
                Text(
                    text = telemetry.dateFormatted,
                    style = CyberTypography.labelSmall,
                    color = palette.primaryCyan.copy(alpha = 0.85f),
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Small center rotating arc ring
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(26.dp)
                ) {
                    Canvas(modifier = Modifier.size(24.dp)) {
                        drawCircle(
                            color = palette.secondaryAccent.copy(alpha = 0.3f),
                            radius = size.minDimension / 2f,
                            style = Stroke(width = 1.5f)
                        )
                        drawArc(
                            color = palette.secondaryAccent,
                            startAngle = ringRotation,
                            sweepAngle = 120f,
                            useCenter = false,
                            style = Stroke(width = 2.5f, cap = StrokeCap.Round)
                        )
                        drawCircle(
                            color = palette.primaryCyan.copy(alpha = pulseAlpha),
                            radius = 3.dp.toPx()
                        )
                    }
                }
            }

            // RIGHT WING: System Info
            Box(
                modifier = Modifier
                    .width(118.dp)
                    .clip(HudTopRightPanelShape)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                palette.surfaceDark.copy(alpha = 0.88f),
                                Color(0xFF020710).copy(alpha = 0.95f)
                            )
                        )
                    )
                    .border(1.dp, palette.primaryCyan.copy(alpha = 0.4f * glowIntensity), HudTopRightPanelShape)
                    .padding(8.dp)
                    .clickable { onAudioClick() }
            ) {
                Column {
                    // Header
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(palette.primaryCyan.copy(alpha = 0.12f), RoundedCornerShape(2.dp))
                            .padding(vertical = 2.dp)
                    ) {
                        Text(
                            text = "System Info",
                            style = CyberTypography.labelSmall,
                            color = palette.primaryCyan,
                            fontSize = 10.sp,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // RAM %
                    TelemetryRow(
                        icon = Icons.Default.Memory,
                        label = "${telemetry.ramUsagePercent}% RAM",
                        tint = if (telemetry.ramUsagePercent > 80) palette.secondaryAccent else palette.primaryCyan
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Storage %
                    TelemetryRow(
                        icon = Icons.Default.Storage,
                        label = "${telemetry.storageUsagePercent}% Disk",
                        tint = palette.primaryCyan
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Battery %
                    TelemetryRow(
                        icon = if (telemetry.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull,
                        label = "${telemetry.batteryPercent}% Bat",
                        tint = if (telemetry.batteryPercent < 20) Color(0xFFFF1744) else palette.primaryCyan
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Audio profile
                    TelemetryRow(
                        icon = Icons.Default.VolumeUp,
                        label = telemetry.audioProfile,
                        tint = palette.secondaryAccent
                    )
                }
            }
        }
    }
}

@Composable
private fun TelemetryRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(11.dp)
        )
        Text(
            text = label,
            style = CyberTypography.labelSmall,
            color = tint.copy(alpha = 0.9f),
            fontSize = 9.5.sp,
            maxLines = 1
        )
    }
}
