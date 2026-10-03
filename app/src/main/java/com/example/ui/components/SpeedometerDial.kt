package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gps.SpeedUnit
import com.example.ui.theme.MonoBlack
import com.example.ui.theme.MonoGray
import com.example.ui.theme.MonoGrayDark
import com.example.ui.theme.MonoSurfaceBorder
import com.example.ui.theme.MonoSurfaceElevated
import com.example.ui.theme.MonoWhite
import com.example.ui.theme.MonoWhite10
import com.example.ui.theme.MonoWhite20
import com.example.ui.theme.MonoWhite80
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private const val START_ANGLE = 140f
private const val SWEEP_ANGLE = 260f
private const val MAX_SPEED_DISPLAY = 240f

@Composable
fun SpeedometerDial(
    modifier: Modifier = Modifier,
    currentSpeedKmh: Float,
    speedUnit: SpeedUnit,
    onUnitClick: () -> Unit
) {
    val displaySpeed = currentSpeedKmh * speedUnit.factor
    val animatedSpeed = remember { Animatable(0f) }

    LaunchedEffect(displaySpeed) {
        animatedSpeed.animateTo(
            targetValue = displaySpeed.coerceIn(0f, MAX_SPEED_DISPLAY),
            animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
        )
    }

    val speedFraction = (animatedSpeed.value / MAX_SPEED_DISPLAY).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .aspectRatio(1.05f)
            .testTag("speedometer_dial"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(20.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = (size.width / 2f) - 18f

            // 1. Subtle Outer Track Ring
            drawArc(
                color = MonoSurfaceBorder,
                startAngle = START_ANGLE,
                sweepAngle = SWEEP_ANGLE,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = 2.5f, cap = StrokeCap.Round)
            )

            // 2. Active Speed Crisp White Arc
            if (speedFraction > 0.002f) {
                // Precision thin white arc
                drawArc(
                    color = MonoWhite,
                    startAngle = START_ANGLE,
                    sweepAngle = SWEEP_ANGLE * speedFraction,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = 5f, cap = StrokeCap.Round)
                )
            }

            // 3. Precision Minimalist Ticks & Numbers
            val totalTicks = 24 // Every 10 units
            val textPaint = android.graphics.Paint().apply {
                isAntiAlias = true
                textSize = 26f
                color = android.graphics.Color.argb(160, 200, 200, 200)
                textAlign = android.graphics.Paint.Align.CENTER
                typeface = android.graphics.Typeface.MONOSPACE
            }

            for (i in 0..totalTicks) {
                val tickSpeed = i * 10
                val angleDeg = START_ANGLE + (i.toFloat() / totalTicks) * SWEEP_ANGLE
                val angleRad = Math.toRadians(angleDeg.toDouble())

                val isMajor = i % 2 == 0 // Every 20 units
                val isNumbered = i % 4 == 0 // 0, 40, 80, 120, 160, 200, 240
                val tickLength = if (isMajor) 16f else 8f
                val tickWidth = if (isMajor) 2f else 1.2f
                val tickColor = if (tickSpeed <= animatedSpeed.value) MonoWhite else MonoGrayDark

                val startRadius = radius - 8f
                val endRadius = startRadius - tickLength

                val startPt = Offset(
                    center.x + (cos(angleRad) * startRadius).toFloat(),
                    center.y + (sin(angleRad) * startRadius).toFloat()
                )
                val endPt = Offset(
                    center.x + (cos(angleRad) * endRadius).toFloat(),
                    center.y + (sin(angleRad) * endRadius).toFloat()
                )

                drawLine(
                    color = tickColor,
                    start = startPt,
                    end = endPt,
                    strokeWidth = tickWidth,
                    cap = StrokeCap.Square
                )

                if (isNumbered) {
                    val numRadius = endRadius - 22f
                    val numX = center.x + (cos(angleRad) * numRadius).toFloat()
                    val numY = center.y + (sin(angleRad) * numRadius).toFloat() + 9f

                    drawIntoCanvas { canvas ->
                        canvas.nativeCanvas.drawText(tickSpeed.toString(), numX, numY, textPaint)
                    }
                }
            }

            // 4. Razor Minimalist Needle
            val needleAngleDeg = START_ANGLE + speedFraction * SWEEP_ANGLE
            val needleRad = Math.toRadians(needleAngleDeg.toDouble())
            val needleLength = radius - 30f

            val needleTip = Offset(
                center.x + (cos(needleRad) * needleLength).toFloat(),
                center.y + (sin(needleRad) * needleLength).toFloat()
            )

            // Needle Line
            drawLine(
                color = MonoWhite,
                start = center,
                end = needleTip,
                strokeWidth = 2.5f,
                cap = StrokeCap.Round
            )

            // Center Precision Hub
            drawCircle(color = MonoBlack, radius = 24f, center = center)
            drawCircle(color = MonoSurfaceBorder, radius = 24f, center = center, style = Stroke(width = 1.5f))
            drawCircle(color = MonoWhite, radius = 6f, center = center)
        }

        // 5. Central Digital Readout
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(top = 90.dp)
        ) {
            Text(
                text = "${animatedSpeed.value.roundToInt()}",
                color = MonoWhite,
                fontSize = 68.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = (-2).sp
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Clean Monochrome Unit Toggle
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MonoSurfaceElevated,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, MonoSurfaceBorder, RoundedCornerShape(20.dp))
                    .clickable { onUnitClick() }
                    .testTag("speed_unit_toggle")
            ) {
                Text(
                    text = speedUnit.label.uppercase(),
                    color = MonoWhite80,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                )
            }
        }
    }
}
