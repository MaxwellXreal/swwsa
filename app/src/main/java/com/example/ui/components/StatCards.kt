package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gps.SpeedUnit
import com.example.ui.theme.MonoBlack
import com.example.ui.theme.MonoGray
import com.example.ui.theme.MonoGrayDark
import com.example.ui.theme.MonoSurface
import com.example.ui.theme.MonoSurfaceBorder
import com.example.ui.theme.MonoWhite
import com.example.ui.theme.MonoWhite20
import com.example.ui.theme.MonoWhite80
import kotlin.math.roundToInt

@Composable
fun SpeedStatsGrid(
    modifier: Modifier = Modifier,
    maxSpeedKmh: Float,
    avgSpeedKmh: Float,
    distanceMeters: Double,
    altitudeMeters: Double,
    accuracyMeters: Float,
    speedUnit: SpeedUnit,
    isIdle: Boolean,
    idleSecondsLeft: Int,
    onResetMaxSpeed: () -> Unit,
    onResetAvgSpeed: () -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Row 1: Max Speed and Average Speed
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MonoStatCardWithAction(
                modifier = Modifier.weight(1f),
                label = "МАКС. СКОРОСТЬ",
                value = (maxSpeedKmh * speedUnit.factor).roundToInt().toString(),
                unit = speedUnit.label,
                onReset = onResetMaxSpeed,
                resetDescription = "Сбросить максимум",
                resetTestTag = "reset_max_speed_button"
            )

            MonoStatCardWithAction(
                modifier = Modifier.weight(1f),
                label = "СРЕДН. СКОРОСТЬ",
                value = (avgSpeedKmh * speedUnit.factor).roundToInt().toString(),
                unit = speedUnit.label,
                onReset = onResetAvgSpeed,
                resetDescription = "Сбросить среднюю",
                resetTestTag = "reset_avg_speed_button",
                statusBadge = when {
                    isIdle && idleSecondsLeft > 0 -> "Сброс через ${idleSecondsLeft}с"
                    isIdle && idleSecondsLeft == 0 -> "Сброшено (стоянка)"
                    else -> null
                }
            )
        }

        // Row 2: Distance and Altitude
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val distKm = distanceMeters / 1000.0
            val distText = if (distKm < 1.0) "${distanceMeters.roundToInt()} м" else String.format("%.2f км", distKm)

            MonoStatCardSimple(
                modifier = Modifier.weight(1f),
                label = "ДИСТАНЦИЯ",
                value = distText
            )

            MonoStatCardSimple(
                modifier = Modifier.weight(1f),
                label = "ВЫСОТА / GPS",
                value = "${altitudeMeters.roundToInt()} м",
                subtext = "±${accuracyMeters.roundToInt()}м"
            )
        }
    }
}

@Composable
fun MonoStatCardWithAction(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    unit: String,
    onReset: () -> Unit,
    resetDescription: String,
    resetTestTag: String,
    statusBadge: String? = null
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, MonoSurfaceBorder, RoundedCornerShape(12.dp)),
        color = MonoSurface
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    color = MonoGray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )

                // Neat minimal reset button in the corner
                IconButton(
                    onClick = onReset,
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(MonoSurfaceBorder.copy(alpha = 0.5f))
                        .testTag(resetTestTag)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = resetDescription,
                        tint = MonoWhite80,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = value,
                    color = MonoWhite,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = unit,
                    color = MonoGray,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            if (statusBadge != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(MonoWhite)
                    )
                    Text(
                        text = statusBadge,
                        color = MonoGray,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
fun MonoStatCardSimple(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    subtext: String? = null
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, MonoSurfaceBorder, RoundedCornerShape(12.dp)),
        color = MonoSurface
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = label,
                color = MonoGray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = value,
                    color = MonoWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                if (subtext != null) {
                    Text(
                        text = subtext,
                        color = MonoGray,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }
        }
    }
}
