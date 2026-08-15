package com.example.ai

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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.core.model.CyberColorPalette
import com.example.core.theme.CyberTypography
import com.example.core.theme.sciFiHudBackground
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JarvisOverlayDialog(
    palette: CyberColorPalette,
    glowIntensity: Float,
    voiceEngine: JarvisVoiceEngine,
    onDismiss: () -> Unit
) {
    val voiceState by voiceEngine.voiceState.collectAsState()
    val rmsDb by voiceEngine.liveRmsDb.collectAsState()
    val lastQuery by voiceEngine.lastQuery.collectAsState()
    val lastResponse by voiceEngine.lastResponse.collectAsState()

    var textInput by remember { mutableStateOf("") }

    val infiniteTransition = rememberInfiniteTransition(label = "jarvisCoreSpin")
    val coreRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "coreRotation"
    )
    val pulseWave by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseWave"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .sciFiHudBackground(palette, glowIntensity)
                .testTag("jarvis_voice_overlay"),
            color = Color.Transparent
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 36.dp, bottom = 20.dp, start = 20.dp, end = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "J.A.R.V.I.S. NEURAL CORE",
                            style = CyberTypography.titleLarge,
                            color = palette.primaryCyan,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Vocal Command & Device Synthesis Matrix",
                            style = CyberTypography.labelSmall,
                            color = palette.secondaryAccent,
                            fontSize = 11.sp
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(38.dp)
                            .background(palette.surfaceDark, CircleShape)
                            .border(1.dp, palette.primaryCyan.copy(alpha = 0.5f), CircleShape)
                            .testTag("close_jarvis_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = palette.primaryCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Central Holographic Orb & Audio-Reactive Spectrum
                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .clickable {
                            if (voiceState is JarvisVoiceState.Speaking) {
                                voiceEngine.stopSpeaking()
                            } else {
                                voiceEngine.startListening()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val centerOffset = center
                        val baseRadius = size.minDimension * 0.35f
                        val audioBoost = (rmsDb * 3f).coerceIn(0f, 40f)

                        // Outer waveform ring
                        val outerRadius = baseRadius * pulseWave + audioBoost
                        drawCircle(
                            color = palette.primaryCyan.copy(alpha = 0.25f),
                            radius = outerRadius,
                            style = Stroke(width = 2f)
                        )

                        // Rotating Arc segments
                        drawArc(
                            color = palette.secondaryAccent,
                            startAngle = coreRotation,
                            sweepAngle = 100f,
                            useCenter = false,
                            style = Stroke(width = 3.5f, cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = palette.primaryCyan,
                            startAngle = coreRotation + 180f,
                            sweepAngle = 90f,
                            useCenter = false,
                            style = Stroke(width = 3.5f, cap = StrokeCap.Round)
                        )

                        // Central Orb Glow
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    palette.primaryCyan.copy(alpha = 0.8f),
                                    palette.secondaryAccent.copy(alpha = 0.3f),
                                    Color.Transparent
                                ),
                                center = centerOffset,
                                radius = baseRadius
                            ),
                            radius = baseRadius
                        )

                        // Inner circular grid particles
                        for (i in 0 until 12) {
                            val a = Math.toRadians((i * 30.0 + coreRotation))
                            val r = baseRadius * 0.7f
                            val px = centerOffset.x + (r * cos(a)).toFloat()
                            val py = centerOffset.y + (r * sin(a)).toFloat()
                            drawCircle(
                                color = Color.White,
                                radius = 2.5f,
                                center = Offset(px, py)
                            )
                        }
                    }

                    // Mic Icon at Center
                    Icon(
                        imageVector = if (voiceState is JarvisVoiceState.Speaking) Icons.Default.GraphicEq else Icons.Default.Mic,
                        contentDescription = "Microphone",
                        tint = Color.White,
                        modifier = Modifier.size(42.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Status Badge
                val statusText = when (voiceState) {
                    is JarvisVoiceState.Listening -> "LISTENING TO DIRECTIVE..."
                    is JarvisVoiceState.Processing -> "PROCESSING NEURAL INPUT..."
                    is JarvisVoiceState.Speaking -> "JARVIS VOCAL RESPONSE"
                    is JarvisVoiceState.Error -> "SYSTEM FAULT"
                    else -> "TAP CORE TO SPEAK COMMAND"
                }

                Text(
                    text = statusText,
                    style = CyberTypography.titleMedium,
                    color = when (voiceState) {
                        is JarvisVoiceState.Listening -> Color(0xFF00E676)
                        is JarvisVoiceState.Processing -> palette.secondaryAccent
                        is JarvisVoiceState.Speaking -> palette.primaryCyan
                        else -> palette.primaryCyan.copy(alpha = 0.8f)
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Query & Response Readout Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(palette.surfaceDark.copy(alpha = 0.8f))
                        .border(1.dp, palette.primaryCyan.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .padding(14.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (lastQuery.isNotEmpty()) {
                            Text(
                                text = "USER DIRECTIVE: \"$lastQuery\"",
                                style = CyberTypography.labelSmall,
                                color = palette.secondaryAccent,
                                fontSize = 11.sp
                            )
                        }

                        Text(
                            text = lastResponse,
                            style = CyberTypography.bodyLarge,
                            color = Color.White,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Suggestion Command Chips
                Text(
                    text = "QUICK TACTICAL DIRECTIVES",
                    style = CyberTypography.labelSmall,
                    color = palette.primaryCyan.copy(alpha = 0.6f),
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(4.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(
                        "Turn on flashlight",
                        "System status",
                        "Battery report",
                        "Open Camera",
                        "Set silent mode"
                    ).forEach { suggestion ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(palette.surfaceDark)
                                .border(1.dp, palette.primaryCyan.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                                .clickable { voiceEngine.submitTextCommand(suggestion) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = suggestion,
                                style = CyberTypography.labelSmall,
                                color = palette.primaryCyan,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Manual Text Command Input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = {
                            Text(
                                text = "Type voice directive...",
                                style = CyberTypography.bodyMedium,
                                color = Color.Gray
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = palette.primaryCyan,
                            unfocusedBorderColor = palette.primaryCyan.copy(alpha = 0.4f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        textStyle = CyberTypography.bodyMedium,
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(palette.surfaceDark)
                            .testTag("jarvis_text_input")
                    )

                    IconButton(
                        onClick = {
                            if (textInput.isNotBlank()) {
                                voiceEngine.submitTextCommand(textInput)
                                textInput = ""
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(palette.primaryCyan)
                            .testTag("submit_jarvis_command")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send",
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
