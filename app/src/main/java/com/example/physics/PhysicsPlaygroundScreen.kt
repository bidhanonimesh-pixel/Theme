package com.example.physics

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.core.model.CyberColorPalette
import com.example.core.model.PhysicsMode
import com.example.core.theme.CyberTypography
import com.example.core.theme.sciFiHudBackground
import com.example.launcher.model.AppItem
import kotlin.math.roundToInt

@Composable
fun PhysicsPlaygroundScreen(
    palette: CyberColorPalette,
    glowIntensity: Float,
    physicsEngine: PhysicsEngine,
    physicsBodies: List<PhysicsBody>,
    currentMode: PhysicsMode,
    onModeChange: (PhysicsMode) -> Unit,
    onExplosionClick: () -> Unit,
    onAppClick: (AppItem) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    var frameTicker by remember { mutableStateOf(0L) }
    var elasticity by remember { mutableFloatStateOf(0.75f) }

    // Register Accelerometer for tilt physics
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (accelerometer != null) {
            sensorManager.registerListener(physicsEngine, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        }
        onDispose {
            sensorManager?.unregisterListener(physicsEngine)
        }
    }

    // High performance 60-120fps simulation stepping loop
    LaunchedEffect(Unit) {
        var lastTime = System.nanoTime()
        while (true) {
            withInfiniteAnimationFrameMillis {
                val now = System.nanoTime()
                val dt = ((now - lastTime) / 1_000_000_000f).coerceIn(0.008f, 0.033f)
                lastTime = now
                physicsEngine.stepSimulation(physicsBodies, dt)
                frameTicker = now
            }
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .sciFiHudBackground(palette, glowIntensity)
            .testTag("physics_playground_screen"),
        color = Color.Transparent
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val widthPx = with(density) { maxWidth.toPx() }
            val heightPx = with(density) { maxHeight.toPx() }

            LaunchedEffect(widthPx, heightPx) {
                physicsEngine.updateScreenBounds(widthPx, heightPx)
            }

            // Top HUD Control Panel
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(top = 36.dp, start = 16.dp, end = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "GRAVITY SIMULATOR",
                            style = CyberTypography.titleLarge,
                            color = palette.primaryCyan,
                            fontSize = 17.sp
                        )
                        Text(
                            text = if (currentMode == PhysicsMode.RAIN_GRAVITY) "Mode: 9.8m/s² Kinetic Collision" else "Mode: Anti-Gravity Gyro Drift",
                            style = CyberTypography.labelSmall,
                            color = palette.secondaryAccent,
                            fontSize = 11.sp
                        )
                    }

                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(38.dp)
                            .background(palette.surfaceDark, CircleShape)
                            .border(1.dp, palette.primaryCyan.copy(alpha = 0.6f), CircleShape)
                            .testTag("close_physics_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = palette.primaryCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action Controls: Mode toggle + Blast button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            val nextMode = if (currentMode == PhysicsMode.RAIN_GRAVITY) PhysicsMode.CLOUD_FLOATING else PhysicsMode.RAIN_GRAVITY
                            onModeChange(nextMode)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = palette.surfaceDark.copy(alpha = 0.85f)),
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, palette.primaryCyan, RoundedCornerShape(6.dp))
                    ) {
                        Icon(
                            imageVector = if (currentMode == PhysicsMode.RAIN_GRAVITY) Icons.Default.Grain else Icons.Default.Cloud,
                            contentDescription = null,
                            tint = palette.primaryCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(
                            text = if (currentMode == PhysicsMode.RAIN_GRAVITY) "Rain Gravity" else "Cloud Float",
                            style = CyberTypography.labelSmall,
                            color = palette.primaryCyan
                        )
                    }

                    Button(
                        onClick = onExplosionClick,
                        colors = ButtonDefaults.buttonColors(containerColor = palette.surfaceDark.copy(alpha = 0.85f)),
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, palette.secondaryAccent, RoundedCornerShape(6.dp))
                            .testTag("burst_explosion_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Whatshot,
                            contentDescription = null,
                            tint = palette.secondaryAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(
                            text = "Supernova Blast",
                            style = CyberTypography.labelSmall,
                            color = palette.secondaryAccent
                        )
                    }
                }
            }

            // Render all dynamic physics bodies / app icon nodes
            // Reading frameTicker forces recomposition cleanly every simulation frame
            val currentFrame = frameTicker

            physicsBodies.forEach { body ->
                var dragStartOffset by remember { mutableStateOf(Offset.Zero) }
                var lastDragTime by remember { mutableStateOf(0L) }
                var lastDragPos by remember { mutableStateOf(Offset.Zero) }

                val posX = with(density) { (body.x - body.radius).toDp() }
                val posY = with(density) { (body.y - body.radius).toDp() }
                val sizeDp = with(density) { (body.radius * 2f).toDp() }

                Box(
                    modifier = Modifier
                        .offset { IntOffset(with(density) { posX.roundToPx() }, with(density) { posY.roundToPx() }) }
                        .size(sizeDp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    palette.surfaceDark,
                                    Color(0xFF03070E)
                                )
                            )
                        )
                        .border(1.5.dp, palette.primaryCyan.copy(alpha = 0.8f * glowIntensity), CircleShape)
                        .rotate(body.rotationAngle)
                        .pointerInput(body.id) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    body.isDragging = true
                                    dragStartOffset = offset
                                    lastDragPos = Offset(body.x, body.y)
                                    lastDragTime = System.currentTimeMillis()
                                    body.vx = 0f
                                    body.vy = 0f
                                },
                                onDragEnd = {
                                    body.isDragging = false
                                    val now = System.currentTimeMillis()
                                    val dtSec = ((now - lastDragTime) / 1000f).coerceAtLeast(0.016f)
                                    // Fling kinetic momentum calculation
                                    body.vx = ((body.x - lastDragPos.x) / dtSec).coerceIn(-2500f, 2500f)
                                    body.vy = ((body.y - lastDragPos.y) / dtSec).coerceIn(-2500f, 2500f)
                                },
                                onDragCancel = {
                                    body.isDragging = false
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    lastDragPos = Offset(body.x, body.y)
                                    lastDragTime = System.currentTimeMillis()
                                    body.x += dragAmount.x
                                    body.y += dragAmount.y
                                }
                            )
                        }
                        .clickable { onAppClick(body.appItem) }
                        .testTag("physics_icon_${body.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    if (body.appItem.icon != null) {
                        val bmp = remember(body.id) { body.appItem.icon.toBitmap(80, 80) }
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = body.appItem.appName,
                            modifier = Modifier
                                .size(sizeDp * 0.72f)
                                .clip(CircleShape)
                        )
                    } else {
                        Text(
                            text = body.appItem.appName.take(2).uppercase(),
                            style = CyberTypography.labelSmall,
                            color = palette.primaryCyan,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}
