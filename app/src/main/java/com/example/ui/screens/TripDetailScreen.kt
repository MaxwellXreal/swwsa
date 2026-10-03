package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Trip
import com.example.ui.map.Map2DView
import com.example.ui.map.getSpeedColor
import com.example.ui.theme.MonoBlack
import com.example.ui.theme.MonoGray
import com.example.ui.theme.MonoSurface
import com.example.ui.theme.MonoSurfaceBorder
import com.example.ui.theme.MonoSurfaceElevated
import com.example.ui.theme.MonoWhite
import com.example.ui.theme.MonoWhite20
import com.example.ui.theme.MonoWhite50
import com.example.ui.theme.MonoWhite80
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun TripDetailScreen(
    modifier: Modifier = Modifier,
    trip: Trip,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val points = remember(trip) { trip.getPoints() }
    val initialCenter = remember(points) {
        if (points.isNotEmpty()) points[points.size / 2] else null
    }

    var selectedIndex by remember { mutableIntStateOf(if (points.isNotEmpty()) points.size / 2 else -1) }
    val selectedPoint = if (selectedIndex in points.indices) points[selectedIndex] else null

    val dateFormatted = SimpleDateFormat("dd.MM.yyyy • HH:mm", Locale.getDefault()).format(Date(trip.startTime))
    val distanceKm = trip.distanceMeters / 1000.0
    val durationMin = trip.durationSeconds / 60
    val durationSec = trip.durationSeconds % 60

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MonoBlack)
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top App Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MonoSurface)
                    .border(1.dp, MonoSurfaceBorder, CircleShape)
                    .testTag("trip_detail_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Назад к поездкам",
                    tint = MonoWhite,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = trip.title,
                    color = MonoWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = dateFormatted,
                    color = MonoGray,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // 2D Offline Map View
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, MonoSurfaceBorder, RoundedCornerShape(14.dp))
        ) {
            Map2DView(
                userLat = initialCenter?.latitude ?: 55.7558,
                userLon = initialCenter?.longitude ?: 37.6173,
                recordedPoints = points,
                selectedPoint = selectedPoint,
                isFollowUser = false,
                showControls = true,
                initialZoom = 15
            )
        }

        // Speed on Selected Road Segment Indicator
        selectedPoint?.let { sp ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, MonoSurfaceBorder, RoundedCornerShape(12.dp)),
                color = MonoSurface
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(MonoWhite)
                        )
                        Column {
                            Text(
                                text = "СКОРОСТЬ НА ВЫБРАННОМ УЧАСТКЕ",
                                color = MonoGray,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "${sp.speedKmh.roundToInt()} км/ч",
                                color = MonoWhite,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Отметка",
                            color = MonoGray,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = String.format("%.2f км", sp.distanceFromStartMeters / 1000.0),
                            color = MonoWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Speed Profile Chart
        if (points.size >= 2) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, MonoSurfaceBorder, RoundedCornerShape(12.dp)),
                color = MonoSurface
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = MonoWhite,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "ПРОФИЛЬ СКОРОСТИ",
                                color = MonoGray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.TouchApp,
                                contentDescription = null,
                                tint = MonoWhite80,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "Коснитесь графика",
                                color = MonoWhite80,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val maxSpeed = max(trip.maxSpeedKmh, 30f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MonoSurfaceElevated)
                    ) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 6.dp, vertical = 10.dp)
                                .pointerInput(points) {
                                    detectTapGestures { offset ->
                                        val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                                        selectedIndex = (fraction * (points.size - 1)).roundToInt()
                                    }
                                }
                                .pointerInput(points) {
                                    detectDragGestures { change, _ ->
                                        change.consume()
                                        val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                                        selectedIndex = (fraction * (points.size - 1)).roundToInt()
                                    }
                                }
                        ) {
                            val w = size.width
                            val h = size.height

                            // Draw horizontal reference lines
                            drawLine(
                                color = MonoSurfaceBorder,
                                start = Offset(0f, h),
                                end = Offset(w, h),
                                strokeWidth = 1f
                            )
                            drawLine(
                                color = MonoSurfaceBorder,
                                start = Offset(0f, h / 2f),
                                end = Offset(w, h / 2f),
                                strokeWidth = 1f
                            )

                            val path = Path()
                            for (i in points.indices) {
                                val pt = points[i]
                                val x = (i.toFloat() / (points.size - 1)) * w
                                val y = h - (pt.speedKmh / maxSpeed).coerceIn(0f, 1f) * h

                                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            }

                            drawPath(
                                path = path,
                                color = MonoWhite,
                                style = Stroke(width = 2.5f, cap = StrokeCap.Round)
                            )

                            // Scrubber indicator
                            if (selectedIndex in points.indices) {
                                val curX = (selectedIndex.toFloat() / (points.size - 1)) * w
                                val curY = h - (points[selectedIndex].speedKmh / maxSpeed).coerceIn(0f, 1f) * h

                                drawLine(
                                    color = MonoWhite50,
                                    start = Offset(curX, 0f),
                                    end = Offset(curX, h),
                                    strokeWidth = 1.5f
                                )
                                drawCircle(
                                    color = MonoWhite,
                                    radius = 5f,
                                    center = Offset(curX, curY)
                                )
                                drawCircle(
                                    color = MonoBlack,
                                    radius = 2f,
                                    center = Offset(curX, curY)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Summary Stats Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MonoDetailStatBox(
                modifier = Modifier.weight(1f),
                title = "МАКС. СКОРОСТЬ",
                value = "${trip.maxSpeedKmh.roundToInt()} км/ч"
            )
            MonoDetailStatBox(
                modifier = Modifier.weight(1f),
                title = "СРЕДН. СКОРОСТЬ",
                value = "${trip.avgSpeedKmh.roundToInt()} км/ч"
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MonoDetailStatBox(
                modifier = Modifier.weight(1f),
                title = "ДИСТАНЦИЯ",
                value = String.format("%.2f км", distanceKm)
            )
            MonoDetailStatBox(
                modifier = Modifier.weight(1f),
                title = "ВРЕМЯ В ПУТИ",
                value = if (durationMin > 0) "${durationMin}м ${durationSec}с" else "${durationSec}с"
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun MonoDetailStatBox(
    modifier: Modifier = Modifier,
    title: String,
    value: String
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, MonoSurfaceBorder, RoundedCornerShape(12.dp)),
        color = MonoSurface
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                color = MonoGray,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = MonoWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
