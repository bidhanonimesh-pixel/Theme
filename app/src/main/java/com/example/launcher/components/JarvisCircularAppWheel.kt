package com.example.launcher.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.core.model.CyberColorPalette
import com.example.core.theme.CyberTypography
import com.example.launcher.model.AppItem
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun JarvisCircularAppWheel(
    palette: CyberColorPalette,
    glowIntensity: Float,
    rotationSpeedMultiplier: Float,
    favoriteAppPackages: List<String>,
    allApps: List<AppItem>,
    onAppClick: (AppItem) -> Unit,
    onSlotLongClick: (slotIndex: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var currentAngleDeg by remember { mutableFloatStateOf(0f) }

    // Smooth ambient auto-rotation around arc reactor
    LaunchedEffect(rotationSpeedMultiplier) {
        val stepTime = 20L
        val degPerSec = 12f * rotationSpeedMultiplier
        val degPerStep = degPerSec * (stepTime / 1000f)
        while (true) {
            kotlinx.coroutines.delay(stepTime)
            currentAngleDeg = (currentAngleDeg + degPerStep) % 360f
        }
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val containerSize = minOf(maxWidth, maxHeight)
        val orbitRadiusDp = containerSize * 0.38f

        // 5 Equatorial Orbital Slots at 72 degree increments (360 / 5)
        for (index in 0 until 5) {
            val baseAngleDeg = index * 72f
            val totalAngleDeg = (baseAngleDeg + currentAngleDeg) % 360f
            val angleRad = Math.toRadians(totalAngleDeg.toDouble())

            val offsetX = (orbitRadiusDp.value * cos(angleRad)).dp
            val offsetY = (orbitRadiusDp.value * sin(angleRad)).dp

            val assignedPkg = favoriteAppPackages.getOrNull(index)
            val assignedApp = if (assignedPkg != null) {
                allApps.firstOrNull { it.packageName == assignedPkg }
            } else {
                when (index) {
                    0 -> allApps.firstOrNull { it.packageName.contains("camera") || it.appName.lowercase().contains("camera") }
                    1 -> allApps.firstOrNull { it.packageName.contains("dialer") || it.packageName.contains("phone") }
                    2 -> allApps.firstOrNull { it.packageName.contains("chrome") || it.appName.lowercase().contains("chrome") || it.packageName.contains("browser") }
                    3 -> allApps.firstOrNull { it.packageName.contains("whatsapp") || it.packageName.contains("message") || it.packageName.contains("chat") }
                    else -> allApps.firstOrNull { it.packageName.contains("vending") || it.packageName.contains("play") || it.packageName.contains("store") }
                }
            }

            val scaleAnim = remember { Animatable(1f) }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .offset(x = offsetX, y = offsetY)
                    .width(68.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .scale(scaleAnim.value)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    palette.surfaceDark,
                                    Color(0xFF03070E)
                                )
                            )
                        )
                        .border(
                            width = 1.8.dp,
                            color = palette.primaryCyan.copy(alpha = 0.85f * glowIntensity),
                            shape = CircleShape
                        )
                        .combinedClickable(
                            onClick = {
                                scope.launch {
                                    scaleAnim.animateTo(1.2f, tween(90))
                                    scaleAnim.animateTo(1.0f, tween(110))
                                }
                                if (assignedApp != null) {
                                    onAppClick(assignedApp)
                                } else {
                                    onSlotLongClick(index)
                                }
                            },
                            onLongClick = {
                                onSlotLongClick(index)
                            }
                        )
                        .testTag("circular_wheel_slot_$index"),
                    contentAlignment = Alignment.Center
                ) {
                    if (assignedApp?.icon != null) {
                        val bmp = remember(assignedApp.packageName) { assignedApp.icon.toBitmap(96, 96) }
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = assignedApp.appName,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        val defaultIcon = when (index) {
                            0 -> Icons.Default.CameraAlt
                            1 -> Icons.Default.Phone
                            2 -> Icons.Default.Language
                            3 -> Icons.Default.Chat
                            else -> Icons.Default.ShoppingCart
                        }
                        Icon(
                            imageVector = defaultIcon,
                            contentDescription = "App Slot $index",
                            tint = palette.primaryCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = assignedApp?.appName ?: "Slot ${index + 1}",
                    style = CyberTypography.labelSmall,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
