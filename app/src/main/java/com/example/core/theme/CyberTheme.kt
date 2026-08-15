package com.example.core.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.CyberColorPalette

val CyberTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        letterSpacing = 2.sp
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        letterSpacing = 1.5.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        letterSpacing = 1.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        letterSpacing = 0.8.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        letterSpacing = 0.4.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        letterSpacing = 1.2.sp
    )
)

val SciFiAngledCornerShape = GenericShape { size, _ ->
    val cut = 16f
    moveTo(cut, 0f)
    lineTo(size.width - cut, 0f)
    lineTo(size.width, cut)
    lineTo(size.width, size.height - cut)
    lineTo(size.width - cut, size.height)
    lineTo(cut, size.height)
    lineTo(0f, size.height - cut)
    lineTo(0f, cut)
    close()
}

val HudTopLeftPanelShape = GenericShape { size, _ ->
    val corner = 24f
    moveTo(0f, 0f)
    lineTo(size.width, 0f)
    lineTo(size.width, size.height - 40f)
    lineTo(size.width - 24f, size.height)
    lineTo(0f, size.height)
    close()
}

val HudTopRightPanelShape = GenericShape { size, _ ->
    moveTo(0f, 0f)
    lineTo(size.width, 0f)
    lineTo(size.width, size.height)
    lineTo(24f, size.height)
    lineTo(0f, size.height - 40f)
    close()
}

fun Modifier.sciFiHudBackground(
    palette: CyberColorPalette,
    glowIntensity: Float = 0.8f
): Modifier = this
    .background(
        Brush.verticalGradient(
            colors = listOf(
                palette.backgroundDark,
                Color(0xFF03070E),
                palette.backgroundDark
            )
        )
    )
    .drawBehind {
        val gridStep = 40.dp.toPx()
        val gridColor = palette.primaryCyan.copy(alpha = 0.04f * glowIntensity)

        // Draw vertical grid lines
        var x = 0f
        while (x <= size.width) {
            drawLine(
                color = gridColor,
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 1f
            )
            x += gridStep
        }

        // Draw horizontal grid lines
        var y = 0f
        while (y <= size.height) {
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f
            )
            y += gridStep
        }
    }

fun Modifier.cyberBorder(
    color: Color,
    borderWidth: Dp = 1.5.dp,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(8.dp)
): Modifier = this
    .border(borderWidth, color.copy(alpha = 0.7f), shape)
    .border(0.5.dp, color.copy(alpha = 0.3f), shape)

fun Modifier.neonGlow(
    color: Color,
    radius: Dp = 12.dp,
    alpha: Float = 0.5f
): Modifier = this.drawBehind {
    val paint = Paint().apply {
        asFrameworkPaint().apply {
            isAntiAlias = true
            this.color = color.copy(alpha = alpha).toArgb()
            setShadowLayer(
                radius.toPx(),
                0f,
                0f,
                color.copy(alpha = alpha).toArgb()
            )
        }
    }
    drawIntoCanvas { canvas ->
        canvas.drawCircle(
            center = center,
            radius = size.minDimension / 2f,
            paint = paint
        )
    }
}
