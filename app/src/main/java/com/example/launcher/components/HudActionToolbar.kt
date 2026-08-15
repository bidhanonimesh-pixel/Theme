package com.example.launcher.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.core.model.CyberColorPalette

@Composable
fun HudActionToolbar(
    palette: CyberColorPalette,
    glowIntensity: Float,
    isTorchOn: Boolean,
    onSettingsClick: () -> Unit,
    onFlashlightClick: () -> Unit,
    onPhysicsClick: () -> Unit,
    onAiMicClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "toolPulse")
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Settings Gear
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(palette.surfaceDark.copy(alpha = 0.6f))
                .border(1.dp, palette.primaryCyan.copy(alpha = 0.5f), CircleShape)
                .clickable { onSettingsClick() }
                .testTag("action_settings")
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = palette.primaryCyan,
                modifier = Modifier.size(24.dp)
            )
        }

        // Center Flashlight Lightning Bolt (Primary Glowing Action)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(
                    if (isTorchOn) Brush.radialGradient(
                        listOf(
                            palette.secondaryAccent.copy(alpha = 0.8f * glowPulse),
                            palette.surfaceDark
                        )
                    ) else Brush.radialGradient(
                        listOf(
                            palette.primaryCyan.copy(alpha = 0.25f),
                            palette.surfaceDark
                        )
                    )
                )
                .border(
                    width = if (isTorchOn) 2.dp else 1.2.dp,
                    color = if (isTorchOn) palette.secondaryAccent else palette.primaryCyan.copy(alpha = 0.7f),
                    shape = CircleShape
                )
                .clickable { onFlashlightClick() }
                .testTag("action_flashlight")
        ) {
            Icon(
                imageVector = Icons.Default.FlashOn,
                contentDescription = "Flashlight",
                tint = if (isTorchOn) palette.secondaryAccent else palette.primaryCyan,
                modifier = Modifier.size(30.dp)
            )
        }

        // Physics Playground / Sensor Radar
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(palette.surfaceDark.copy(alpha = 0.6f))
                .border(1.dp, palette.primaryCyan.copy(alpha = 0.5f), CircleShape)
                .clickable { onPhysicsClick() }
                .testTag("action_physics")
        ) {
            Icon(
                imageVector = Icons.Default.Radar,
                contentDescription = "Physics Gravity Engine",
                tint = palette.primaryCyan,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
