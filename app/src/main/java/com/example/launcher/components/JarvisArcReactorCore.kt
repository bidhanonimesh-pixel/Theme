package com.example.launcher.components

import android.graphics.drawable.Drawable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.core.model.CyberColorPalette
import com.example.core.theme.CyberTypography
import com.example.launcher.model.AppItem
import com.example.launcher.model.OrbitAppSlot
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun JarvisArcReactorCore(
    palette: CyberColorPalette,
    glowIntensity: Float,
    rotationSpeedMultiplier: Float,
    orbitSlots: List<OrbitAppSlot>,
    allApps: List<AppItem>,
    onSlotClick: (OrbitAppSlot) -> Unit,
    onCenterCoreClick: () -> Unit,
    onDialerShortcutClick: () -> Unit,
    onTorchShortcutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "reactorSpin")
    val outerAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween((18000 / rotationSpeedMultiplier).toInt().coerceAtLeast(4000), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "outerAngle"
    )
    val innerAngle by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween((12000 / rotationSpeedMultiplier).toInt().coerceAtLeast(3000), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "innerAngle"
    )
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseGlow"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        val containerSize = minOf(maxWidth, maxHeight)
        val coreRadiusDp = containerSize * 0.44f
        val orbitRadiusDp = containerSize * 0.36f

        // 1. Futuristic Canvas Background Geometry & Rotating Cybernetic Rings
        Canvas(modifier = Modifier.size(containerSize)) {
            val centerOffset = center
            val orbitRadius = orbitRadiusDp.toPx()
            val innerCoreRadius = orbitRadius * 0.62f

            // Faint dotted orbit circle
            drawCircle(
                color = palette.primaryCyan.copy(alpha = 0.15f * glowIntensity),
                radius = orbitRadius,
                style = Stroke(
                    width = 1.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 8f), 0f)
                )
            )

            // Outer cyan rotating segmented arcs
            drawArc(
                color = palette.primaryCyan.copy(alpha = 0.8f * glowIntensity),
                startAngle = outerAngle,
                sweepAngle = 70f,
                useCenter = false,
                style = Stroke(width = 3.5f, cap = StrokeCap.Round)
            )
            drawArc(
                color = palette.primaryCyan.copy(alpha = 0.8f * glowIntensity),
                startAngle = outerAngle + 120f,
                sweepAngle = 60f,
                useCenter = false,
                style = Stroke(width = 3.5f, cap = StrokeCap.Round)
            )
            drawArc(
                color = palette.primaryCyan.copy(alpha = 0.8f * glowIntensity),
                startAngle = outerAngle + 240f,
                sweepAngle = 50f,
                useCenter = false,
                style = Stroke(width = 3.5f, cap = StrokeCap.Round)
            )

            // Segmented tick marks along orbit
            for (i in 0 until 36) {
                val angleRad = Math.toRadians((i * 10.0 + innerAngle))
                val r1 = innerCoreRadius + 14f
                val r2 = innerCoreRadius + (if (i % 3 == 0) 24f else 18f)
                val p1 = Offset(
                    centerOffset.x + (r1 * cos(angleRad)).toFloat(),
                    centerOffset.y + (r1 * sin(angleRad)).toFloat()
                )
                val p2 = Offset(
                    centerOffset.x + (r2 * cos(angleRad)).toFloat(),
                    centerOffset.y + (r2 * sin(angleRad)).toFloat()
                )
                drawLine(
                    color = palette.primaryCyan.copy(alpha = if (i % 3 == 0) 0.5f else 0.2f),
                    start = p1,
                    end = p2,
                    strokeWidth = if (i % 3 == 0) 2f else 1f
                )
            }

            // Inner Amber/Orange Arc Reactor Ring
            drawCircle(
                color = palette.secondaryAccent.copy(alpha = 0.6f * pulseGlow),
                radius = innerCoreRadius,
                style = Stroke(width = 3f)
            )

            // Inner Core Backdrop Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        palette.secondaryAccent.copy(alpha = 0.22f * pulseGlow),
                        palette.primaryCyan.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = centerOffset,
                    radius = innerCoreRadius
                ),
                radius = innerCoreRadius
            )

            // Futuristic Arrow/Reactor Chevron inside Center
            val chevHeight = innerCoreRadius * 0.85f
            val chevWidth = innerCoreRadius * 0.7f
            val path = Path().apply {
                moveTo(centerOffset.x, centerOffset.y - chevHeight * 0.55f)
                lineTo(centerOffset.x + chevWidth * 0.5f, centerOffset.y + chevHeight * 0.35f)
                lineTo(centerOffset.x, centerOffset.y + chevHeight * 0.05f)
                lineTo(centerOffset.x - chevWidth * 0.5f, centerOffset.y + chevHeight * 0.35f)
                close()
            }
            drawPath(
                path = path,
                brush = Brush.verticalGradient(
                    listOf(
                        palette.primaryCyan.copy(alpha = 0.4f * pulseGlow),
                        palette.surfaceDark.copy(alpha = 0.9f)
                    )
                )
            )
            drawPath(
                path = path,
                color = palette.primaryCyan.copy(alpha = 0.85f),
                style = Stroke(width = 2.5f, cap = StrokeCap.Round)
            )
        }

        // 2. Center Clickable Hit Target (JARVIS AI Activator)
        Box(
            modifier = Modifier
                .size(containerSize * 0.32f)
                .clip(CircleShape)
                .clickable { onCenterCoreClick() }
                .testTag("center_arc_reactor_core"),
            contentAlignment = Alignment.Center
        ) {
            // Invisible touch area triggering JARVIS
        }

        // 3. Orbiting App Nodes
        orbitSlots.forEach { slot ->
            val angleRad = Math.toRadians(slot.angleDegrees.toDouble())
            val offsetX = (orbitRadiusDp.value * cos(angleRad)).dp
            val offsetY = (orbitRadiusDp.value * sin(angleRad)).dp

            val assignedApp = slot.assignedPackage?.let { pkg ->
                allApps.firstOrNull { it.packageName == pkg }
            }

            Box(
                modifier = Modifier
                    .offset(x = offsetX, y = offsetY)
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                palette.surfaceDark,
                                Color(0xFF040A14)
                            )
                        )
                    )
                    .border(1.5.dp, palette.primaryCyan.copy(alpha = 0.8f * glowIntensity), CircleShape)
                    .clickable { onSlotClick(slot) }
                    .testTag("orbit_slot_${slot.slotIndex}"),
                contentAlignment = Alignment.Center
            ) {
                if (assignedApp?.icon != null) {
                    val bitmap = assignedApp.icon.toBitmap(80, 80)
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = assignedApp.appName,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                    )
                } else {
                    // Futuristic fallback icon by slot type
                    val iconVector = when (slot.slotIndex) {
                        0 -> Icons.Default.ShoppingBag
                        1 -> Icons.Default.Email
                        2 -> Icons.Default.Search
                        3 -> Icons.Default.Public
                        4 -> Icons.Default.Chat
                        else -> Icons.Default.Camera
                    }
                    Icon(
                        imageVector = iconVector,
                        contentDescription = slot.label,
                        tint = palette.primaryCyan,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // 4. Sub-Shortcut Nodes matching bottom left/right in screenshot
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 4.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(palette.surfaceDark.copy(alpha = 0.8f))
                .border(1.dp, palette.primaryCyan.copy(alpha = 0.5f), CircleShape)
                .clickable { onDialerShortcutClick() }
                .testTag("shortcut_dialer_left"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Dialpad,
                contentDescription = "Dialer",
                tint = palette.primaryCyan,
                modifier = Modifier.size(20.dp)
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 4.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(palette.surfaceDark.copy(alpha = 0.8f))
                .border(1.dp, palette.primaryCyan.copy(alpha = 0.5f), CircleShape)
                .clickable { onTorchShortcutClick() }
                .testTag("shortcut_torch_right"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.FlashOn,
                contentDescription = "Torch",
                tint = palette.primaryCyan,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
