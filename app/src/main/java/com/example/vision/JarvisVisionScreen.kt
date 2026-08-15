package com.example.vision

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.core.model.CyberColorPalette
import com.example.core.theme.CyberTypography

@Composable
fun JarvisVisionScreen(
    palette: CyberColorPalette,
    glowIntensity: Float,
    onAnalyzeTarget: (String) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var scanStatusText by remember { mutableStateOf("TARGETING MATRIX STANDBY") }
    var cameraInitError by remember { mutableStateOf<String?>(null) }
    var isSimulatedMode by remember { mutableStateOf(false) }
    var activeCameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    // Always cleanly unbind all camera use cases on disposal
    DisposableEffect(lifecycleOwner) {
        onDispose {
            try {
                activeCameraProvider?.unbindAll()
            } catch (_: Throwable) {
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            scanStatusText = "CAMERA PERMISSION DENIED: Simulation Mode Active"
            isSimulatedMode = true
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "visionRadar")
    val scanLineY by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanLineY"
    )

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("jarvis_vision_screen"),
        color = Color.Black
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 1. CameraX Preview View (using COMPATIBLE TextureView mode to prevent BufferQueue abandoned errors)
            if (hasCameraPermission && !isSimulatedMode && cameraInitError == null) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx).apply {
                            // COMPATIBLE mode uses TextureView instead of SurfaceView to prevent BufferQueue abandoned crashes
                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                        }
                        try {
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            cameraProviderFuture.addListener({
                                try {
                                    val cameraProvider = cameraProviderFuture.get()
                                    activeCameraProvider = cameraProvider
                                    val preview = Preview.Builder().build().also {
                                        it.setSurfaceProvider(previewView.surfaceProvider)
                                    }
                                    val selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
                                    if (cameraProvider.hasCamera(selector)) {
                                        cameraProvider.unbindAll()
                                        cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview)
                                    } else {
                                        cameraProvider.unbindAll()
                                        cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview)
                                    }
                                } catch (e: Throwable) {
                                    cameraInitError = e.localizedMessage ?: "Camera hardware unavailable"
                                    isSimulatedMode = true
                                }
                            }, ContextCompat.getMainExecutor(ctx))
                        } catch (e: Throwable) {
                            cameraInitError = e.localizedMessage ?: "ProcessCameraProvider error"
                            isSimulatedMode = true
                        }
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Tactical Grid & Permission Fallback Canvas
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF03070E)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .padding(24.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(palette.surfaceDark.copy(alpha = 0.9f))
                            .border(1.5.dp, palette.primaryCyan.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                            .padding(24.dp)
                    ) {
                        Icon(
                            imageVector = if (!hasCameraPermission) Icons.Default.Security else Icons.Default.Radar,
                            contentDescription = "Sensor Status",
                            tint = palette.primaryCyan,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (!hasCameraPermission) "OPTICAL SENSORS OFFLINE" else "TACTICAL SCANNER SIMULATION",
                            style = CyberTypography.titleMedium,
                            color = palette.primaryCyan
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (!hasCameraPermission)
                                "Camera hardware permission required for optical reconnaissance feed."
                            else
                                "Hardware feed offline or virtualized. Operating in synthetic telemetry simulation mode.",
                            style = CyberTypography.labelSmall,
                            color = Color.LightGray,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        if (!hasCameraPermission) {
                            Button(
                                onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                colors = ButtonDefaults.buttonColors(containerColor = palette.primaryCyan),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("grant_camera_permission_button")
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.Black)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Grant Camera Access", color = Color.Black, style = CyberTypography.labelSmall)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { isSimulatedMode = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Use Tactical Simulation Mode", color = Color.White, style = CyberTypography.labelSmall)
                            }
                        } else if (cameraInitError != null) {
                            Button(
                                onClick = {
                                    cameraInitError = null
                                    isSimulatedMode = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = palette.primaryCyan),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Proceed in Simulation Mode", color = Color.Black, style = CyberTypography.labelSmall)
                            }
                        }
                    }
                }
            }

            // 2. Futuristic Sci-Fi Targeting HUD Overlay Canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerOffset = center
                val scanY = size.height * scanLineY

                // Horizontal Laser Scan Line
                drawLine(
                    color = palette.primaryCyan.copy(alpha = 0.75f * glowIntensity),
                    start = Offset(40f, scanY),
                    end = Offset(size.width - 40f, scanY),
                    strokeWidth = 2.5f
                )

                // Central Targeting Reticle Box
                val boxWidth = size.width * 0.65f
                val boxHeight = size.height * 0.38f
                val left = (size.width - boxWidth) / 2f
                val top = (size.height - boxHeight) / 2f

                // Corner brackets
                val bracketLen = 30f
                val strokeW = 3.5f

                // Top-Left
                drawLine(palette.primaryCyan, Offset(left, top), Offset(left + bracketLen, top), strokeW)
                drawLine(palette.primaryCyan, Offset(left, top), Offset(left, top + bracketLen), strokeW)

                // Top-Right
                drawLine(palette.primaryCyan, Offset(left + boxWidth, top), Offset(left + boxWidth - bracketLen, top), strokeW)
                drawLine(palette.primaryCyan, Offset(left + boxWidth, top), Offset(left + boxWidth, top + bracketLen), strokeW)

                // Bottom-Left
                drawLine(palette.primaryCyan, Offset(left, top + boxHeight), Offset(left + bracketLen, top + boxHeight), strokeW)
                drawLine(palette.primaryCyan, Offset(left, top + boxHeight), Offset(left, top + boxHeight - bracketLen), strokeW)

                // Bottom-Right
                drawLine(palette.primaryCyan, Offset(left + boxWidth, top + boxHeight), Offset(left + boxWidth - bracketLen, top + boxHeight), strokeW)
                drawLine(palette.primaryCyan, Offset(left + boxWidth, top + boxHeight), Offset(left + boxWidth, top + bracketLen), strokeW)

                // Rotating circular reticle in center
                drawCircle(
                    color = palette.secondaryAccent.copy(alpha = 0.5f * glowIntensity),
                    radius = 45.dp.toPx(),
                    center = centerOffset,
                    style = Stroke(
                        width = 1.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 10f), 0f)
                    )
                )

                // Reticle crosshairs
                drawLine(
                    color = palette.secondaryAccent,
                    start = Offset(centerOffset.x - 18.dp.toPx(), centerOffset.y),
                    end = Offset(centerOffset.x + 18.dp.toPx(), centerOffset.y),
                    strokeWidth = 2f
                )
                drawLine(
                    color = palette.secondaryAccent,
                    start = Offset(centerOffset.x, centerOffset.y - 18.dp.toPx()),
                    end = Offset(centerOffset.x, centerOffset.y + 18.dp.toPx()),
                    strokeWidth = 2f
                )
            }

            // 3. Top HUD Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(top = 40.dp, start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "TACTICAL OPTICAL RECON",
                        style = CyberTypography.titleMedium,
                        color = palette.primaryCyan
                    )
                    Text(
                        text = if (isSimulatedMode) "SIMULATION MODE ACTIVE" else "JARVIS Vision Matrix v4.2",
                        style = CyberTypography.labelSmall,
                        color = palette.secondaryAccent
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (hasCameraPermission && !isSimulatedMode) {
                        IconButton(
                            onClick = {
                                lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                                    CameraSelector.LENS_FACING_FRONT
                                } else {
                                    CameraSelector.LENS_FACING_BACK
                                }
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                .border(1.dp, palette.primaryCyan, CircleShape)
                                .testTag("flip_camera_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FlipCameraAndroid,
                                contentDescription = "Switch Camera",
                                tint = palette.primaryCyan
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            try {
                                activeCameraProvider?.unbindAll()
                            } catch (_: Throwable) {
                            }
                            onClose()
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            .border(1.dp, palette.primaryCyan, CircleShape)
                            .testTag("close_vision_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = palette.primaryCyan
                        )
                    }
                }
            }

            // 4. Bottom Diagnostic & Capture Trigger
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 30.dp, start = 20.dp, end = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.85f))
                        .border(1.dp, palette.primaryCyan.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = scanStatusText,
                        style = CyberTypography.bodyMedium,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        scanStatusText = "NEURAL OPTIC SCAN: Target locked. Analyzing geometric signatures..."
                        val sampleResults = listOf(
                            "Biological Signature: Homo Sapiens detected. Pulse rate: Nominal.",
                            "Object Recognition: Electronic computing hardware verified.",
                            "Spatial Telemetry: Distance 0.8 meters. Lighting conditions: Optimal.",
                            "Security Analysis: No tactical threats observed in sector.",
                            "Environment Matrix: Atmospheric pressure stable. Temperature nominal."
                        )
                        val chosen = sampleResults.random()
                        scanStatusText = chosen
                        onAnalyzeTarget(chosen)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.primaryCyan),
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(46.dp)
                        .testTag("scan_target_button")
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Analyze Field Target", color = Color.Black, style = CyberTypography.labelSmall)
                }
            }
        }
    }
}
