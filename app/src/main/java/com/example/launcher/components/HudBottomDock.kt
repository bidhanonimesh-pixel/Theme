package com.example.launcher.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Grid3x3
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.example.core.model.CyberColorPalette
import com.example.launcher.model.AppItem

@Composable
fun HudBottomDock(
    palette: CyberColorPalette,
    glowIntensity: Float,
    allApps: List<AppItem>,
    onAppClick: (AppItem) -> Unit,
    onDialerClick: () -> Unit,
    onDrawerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "dockChevron")
    val chevronOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "chevronOffset"
    )

    // Locate common apps for dock
    val cameraApp = allApps.firstOrNull {
        it.packageName.contains("camera") || it.appName.lowercase().contains("camera")
    }
    val browserApp = allApps.firstOrNull {
        it.packageName.contains("chrome") || it.packageName.contains("browser") || it.appName.lowercase().contains("chrome")
    }
    val phoneApp = allApps.firstOrNull {
        it.packageName.contains("dialer") || it.packageName.contains("phone")
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp)
    ) {
        // Upward chevron indicator
        Box(
            modifier = Modifier
                .clickable { onDrawerClick() }
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = "Open App Drawer",
                tint = palette.primaryCyan.copy(alpha = 0.8f),
                modifier = Modifier
                    .size(24.dp)
                    .padding(bottom = (chevronOffset * -1).dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 4 Bottom Cyber Dock Pods
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Camera Dock
            DockItem(
                palette = palette,
                glowIntensity = glowIntensity,
                app = cameraApp,
                defaultIcon = Icons.Default.CameraAlt,
                tag = "dock_camera",
                onClick = {
                    if (cameraApp != null) onAppClick(cameraApp)
                    else onDrawerClick()
                }
            )

            // 2. Browser Dock
            DockItem(
                palette = palette,
                glowIntensity = glowIntensity,
                app = browserApp,
                defaultIcon = Icons.Default.Language,
                tag = "dock_browser",
                onClick = {
                    if (browserApp != null) onAppClick(browserApp)
                    else onDrawerClick()
                }
            )

            // 3. Dialer / Phone Dock
            DockItem(
                palette = palette,
                glowIntensity = glowIntensity,
                app = phoneApp,
                defaultIcon = Icons.Default.Phone,
                tag = "dock_phone",
                onClick = { onDialerClick() }
            )

            // 4. JARVIS OS / App Drawer Hub Dock
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                palette.primaryCyan.copy(alpha = 0.28f * glowIntensity),
                                palette.surfaceDark
                            )
                        )
                    )
                    .border(2.dp, palette.primaryCyan.copy(alpha = 0.9f * glowIntensity), CircleShape)
                    .clickable { onDrawerClick() }
                    .testTag("dock_jarvis_hub"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "JARVIS OS Hub",
                    tint = palette.primaryCyan,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
    }
}

@Composable
private fun DockItem(
    palette: CyberColorPalette,
    glowIntensity: Float,
    app: AppItem?,
    defaultIcon: androidx.compose.ui.graphics.vector.ImageVector,
    tag: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    listOf(
                        palette.surfaceDark,
                        Color(0xFF040A14)
                    )
                )
            )
            .border(1.5.dp, palette.primaryCyan.copy(alpha = 0.75f * glowIntensity), CircleShape)
            .clickable { onClick() }
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        if (app?.icon != null) {
            val bitmap = app.icon.toBitmap(84, 84)
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = app.appName,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
            )
        } else {
            Icon(
                imageVector = defaultIcon,
                contentDescription = null,
                tint = palette.primaryCyan,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}
