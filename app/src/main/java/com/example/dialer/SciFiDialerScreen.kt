package com.example.dialer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
import com.example.core.model.CyberColorPalette
import com.example.core.theme.CyberTypography
import com.example.core.theme.sciFiHudBackground
import com.example.data.QuickContactEntity
import kotlinx.coroutines.delay
import kotlin.math.sin

@Composable
fun SciFiDialerScreen(
    palette: CyberColorPalette,
    glowIntensity: Float,
    dialerInput: String,
    quickContacts: List<QuickContactEntity>,
    onDigitPress: (Char) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onCallInitiate: (String) -> Unit,
    onSendSms: (String) -> Unit,
    onClose: () -> Unit
) {
    var activeInCallNumber by remember { mutableStateOf<String?>(null) }
    var inCallSeconds by remember { mutableIntStateOf(0) }
    var isMuted by remember { mutableStateOf(false) }
    var isSpeakerOn by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "dialerWaves")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )

    // Call timer loop if in simulated in-call HUD
    LaunchedEffect(activeInCallNumber) {
        if (activeInCallNumber != null) {
            inCallSeconds = 0
            while (activeInCallNumber != null) {
                delay(1000)
                inCallSeconds++
            }
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .sciFiHudBackground(palette, glowIntensity)
            .testTag("scifi_dialer_screen"),
        color = Color.Transparent
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 36.dp, bottom = 20.dp, start = 20.dp, end = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "QUANTUM COMMUNICATOR",
                            style = CyberTypography.titleLarge,
                            color = palette.primaryCyan,
                            fontSize = 17.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SimCard,
                                contentDescription = null,
                                tint = palette.secondaryAccent,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "SIM 1: RYZE 5G (ACTIVE) | SIM 2: STANDBY",
                                style = CyberTypography.labelSmall,
                                color = palette.secondaryAccent,
                                fontSize = 10.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(38.dp)
                            .background(palette.surfaceDark, CircleShape)
                            .border(1.dp, palette.primaryCyan.copy(alpha = 0.5f), CircleShape)
                            .testTag("close_dialer_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = palette.primaryCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Display Screen with Typed Number
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(palette.surfaceDark.copy(alpha = 0.75f))
                        .border(1.dp, palette.primaryCyan.copy(alpha = 0.6f * glowIntensity), RoundedCornerShape(10.dp))
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (dialerInput.isEmpty()) "ENTER FREQUENCY / NUMBER" else dialerInput,
                            style = CyberTypography.titleLarge,
                            color = if (dialerInput.isEmpty()) palette.primaryCyan.copy(alpha = 0.35f) else Color.White,
                            fontSize = if (dialerInput.length > 12) 18.sp else 22.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )

                        if (dialerInput.isNotEmpty()) {
                            IconButton(
                                onClick = onBackspace,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Backspace,
                                    contentDescription = "Backspace",
                                    tint = palette.secondaryAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Curved 3D Quick Contact Carousel
                Text(
                    text = "FAVORITE NEURAL CONTACTS",
                    style = CyberTypography.labelSmall,
                    color = palette.primaryCyan.copy(alpha = 0.7f),
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(84.dp)
                ) {
                    items(quickContacts) { contact ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(68.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(palette.surfaceDark.copy(alpha = 0.6f))
                                .border(1.dp, palette.primaryCyan.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .clickable {
                                    activeInCallNumber = "${contact.name} (${contact.phoneNumber})"
                                }
                                .padding(vertical = 6.dp)
                                .testTag("contact_${contact.id}")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(palette.primaryCyan.copy(alpha = 0.2f))
                                    .border(1.dp, palette.primaryCyan, CircleShape)
                            ) {
                                Text(
                                    text = contact.avatarInitials,
                                    style = CyberTypography.labelSmall,
                                    color = palette.primaryCyan,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = contact.name.split(" ").firstOrNull() ?: contact.name,
                                style = CyberTypography.bodyMedium,
                                color = Color.White,
                                fontSize = 10.sp,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Sci-Fi Holographic 3x4 Dialpad
                val dialKeys = listOf(
                    listOf(DialKey('1', ""), DialKey('2', "ABC"), DialKey('3', "DEF")),
                    listOf(DialKey('4', "GHI"), DialKey('5', "JKL"), DialKey('6', "MNO")),
                    listOf(DialKey('7', "PQRS"), DialKey('8', "TUV"), DialKey('9', "WXYZ")),
                    listOf(DialKey('*', ""), DialKey('0', "+"), DialKey('#', ""))
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    dialKeys.forEach { rowKeys ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            rowKeys.forEach { key ->
                                DialPadButton(
                                    key = key,
                                    palette = palette,
                                    glowIntensity = glowIntensity,
                                    onClick = { onDigitPress(key.digit) }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Bottom Call Dispatcher Action Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Quick SMS Button
                    IconButton(
                        onClick = {
                            if (dialerInput.isNotEmpty()) onSendSms(dialerInput)
                        },
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(palette.surfaceDark)
                            .border(1.dp, palette.primaryCyan.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = "Send SMS",
                            tint = palette.primaryCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Main Glowing Neon Call Button
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        Color(0xFF00E676),
                                        Color(0xFF00897B)
                                    )
                                )
                            )
                            .border(2.dp, Color(0xFF69F0AE), CircleShape)
                            .clickable {
                                if (dialerInput.isNotBlank()) {
                                    activeInCallNumber = dialerInput
                                    onCallInitiate(dialerInput)
                                }
                            }
                            .testTag("initiate_call_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Call",
                            tint = Color.Black,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Clear All Digits Button
                    IconButton(
                        onClick = onClear,
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(palette.surfaceDark)
                            .border(1.dp, palette.secondaryAccent.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Text(
                            text = "CLR",
                            style = CyberTypography.labelSmall,
                            color = palette.secondaryAccent,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // In-Call Holographic HUD Overlay Screen
            AnimatedVisibility(
                visible = activeInCallNumber != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xF0020710))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "TRANSMISSION LINK ACTIVE",
                            style = CyberTypography.titleMedium,
                            color = palette.primaryCyan,
                            letterSpacing = 2.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = activeInCallNumber ?: "",
                            style = CyberTypography.titleLarge,
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        val min = inCallSeconds / 60
                        val sec = inCallSeconds % 60
                        Text(
                            text = String.format("%02d:%02d", min, sec),
                            style = CyberTypography.titleMedium,
                            color = palette.secondaryAccent,
                            fontSize = 18.sp
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Audio Waveform Visualizer
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .height(60.dp)
                        ) {
                            val bars = 24
                            val barWidth = size.width / (bars * 1.6f)
                            for (i in 0 until bars) {
                                val h = (sin(wavePhase + i * 0.4f) * 20f + 25f).coerceAtLeast(4f)
                                val x = i * (barWidth * 1.6f)
                                drawLine(
                                    color = palette.primaryCyan,
                                    start = Offset(x, size.height / 2f - h / 2f),
                                    end = Offset(x, size.height / 2f + h / 2f),
                                    strokeWidth = barWidth,
                                    cap = StrokeCap.Round
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(36.dp))

                        // In-Call Controls
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Mute
                            IconButton(
                                onClick = { isMuted = !isMuted },
                                modifier = Modifier
                                    .size(54.dp)
                                    .background(if (isMuted) palette.secondaryAccent else palette.surfaceDark, CircleShape)
                                    .border(1.dp, palette.primaryCyan, CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                    contentDescription = "Mute",
                                    tint = if (isMuted) Color.Black else palette.primaryCyan
                                )
                            }

                            // End Call
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFF1744))
                                    .border(2.dp, Color(0xFFFF8A80), CircleShape)
                                    .clickable { activeInCallNumber = null }
                                    .testTag("end_call_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CallEnd,
                                    contentDescription = "End Call",
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            // Speaker
                            IconButton(
                                onClick = { isSpeakerOn = !isSpeakerOn },
                                modifier = Modifier
                                    .size(54.dp)
                                    .background(if (isSpeakerOn) palette.primaryCyan else palette.surfaceDark, CircleShape)
                                    .border(1.dp, palette.primaryCyan, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = "Speaker",
                                    tint = if (isSpeakerOn) Color.Black else palette.primaryCyan
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

data class DialKey(val digit: Char, val subtext: String)

@Composable
private fun DialPadButton(
    key: DialKey,
    palette: CyberColorPalette,
    glowIntensity: Float,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(62.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    listOf(
                        palette.surfaceDark,
                        Color(0xFF03070E)
                    )
                )
            )
            .border(1.2.dp, palette.primaryCyan.copy(alpha = 0.6f * glowIntensity), CircleShape)
            .clickable { onClick() }
            .testTag("dialpad_key_${key.digit}"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = key.digit.toString(),
                style = CyberTypography.titleLarge,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            if (key.subtext.isNotEmpty()) {
                Text(
                    text = key.subtext,
                    style = CyberTypography.labelSmall,
                    color = palette.primaryCyan.copy(alpha = 0.7f),
                    fontSize = 8.5.sp
                )
            }
        }
    }
}
