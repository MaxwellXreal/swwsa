package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsNotFixed
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TrackPoint
import com.example.gps.GpsUiState
import com.example.gps.RecordedTripResult
import com.example.ui.components.SpeedStatsGrid
import com.example.ui.components.SpeedometerDial
import com.example.ui.components.TripRecordBar
import com.example.ui.map.Map2DView
import com.example.ui.theme.MonoBlack
import com.example.ui.theme.MonoGray
import com.example.ui.theme.MonoSurface
import com.example.ui.theme.MonoSurfaceBorder
import com.example.ui.theme.MonoSurfaceElevated
import com.example.ui.theme.MonoWhite
import com.example.ui.theme.MonoWhite80
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun SpeedometerScreen(
    modifier: Modifier = Modifier,
    gpsState: GpsUiState,
    recordedPoints: List<TrackPoint>,
    pendingSaveTrip: RecordedTripResult?,
    onResetMaxSpeed: () -> Unit,
    onResetAvgSpeed: () -> Unit,
    onToggleUnit: () -> Unit,
    onToggleSimulation: () -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onSaveTrip: (String) -> Unit,
    onDismissSaveDialog: () -> Unit
) {
    var viewMode by remember { mutableIntStateOf(0) } // 0: Dashboard, 1: Full 2D Map

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MonoBlack)
    ) {
        if (viewMode == 0) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Top Bar Status
                TopBarStatus(
                    gpsState = gpsState,
                    onToggleViewMode = { viewMode = 1 }
                )

                // Large Minimalist Speedometer Dial
                SpeedometerDial(
                    modifier = Modifier.fillMaxWidth(),
                    currentSpeedKmh = gpsState.currentSpeedKmh,
                    speedUnit = gpsState.speedUnit,
                    onUnitClick = onToggleUnit
                )

                // Stats Grid
                SpeedStatsGrid(
                    maxSpeedKmh = gpsState.maxSpeedKmh,
                    avgSpeedKmh = gpsState.avgSpeedKmh,
                    distanceMeters = gpsState.totalDistanceMeters,
                    altitudeMeters = gpsState.altitudeMeters,
                    accuracyMeters = gpsState.accuracyMeters,
                    speedUnit = gpsState.speedUnit,
                    isIdle = gpsState.isIdle,
                    idleSecondsLeft = gpsState.idleSecondsLeft,
                    onResetMaxSpeed = onResetMaxSpeed,
                    onResetAvgSpeed = onResetAvgSpeed
                )

                // 2D Offline Map Card
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                imageVector = Icons.Default.Map,
                                contentDescription = null,
                                tint = MonoWhite80,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "2D КАРТА (ОФФЛАЙН / СЕТЬ)",
                                color = MonoGray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { viewMode = 1 }
                                .padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fullscreen,
                                contentDescription = "На весь экран",
                                tint = MonoWhite,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "На весь экран",
                                color = MonoWhite,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, MonoSurfaceBorder, RoundedCornerShape(14.dp))
                    ) {
                        Map2DView(
                            userLat = gpsState.latitude,
                            userLon = gpsState.longitude,
                            userBearing = gpsState.bearing,
                            userSpeedKmh = gpsState.currentSpeedKmh,
                            recordedPoints = recordedPoints,
                            isFollowUser = true,
                            showControls = true,
                            initialZoom = 16
                        )
                    }
                }

                // Trip Recording Action Bar
                TripRecordBar(
                    isRecording = gpsState.isRecording,
                    recordingDurationSec = gpsState.recordingDurationSec,
                    recordingDistanceMeters = gpsState.recordingDistanceMeters,
                    isSimulationMode = gpsState.isSimulationMode,
                    onStartRecording = onStartRecording,
                    onStopRecording = onStopRecording,
                    onToggleSimulation = onToggleSimulation
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        } else {
            // Full-screen 2D Map View
            Box(modifier = Modifier.fillMaxSize()) {
                Map2DView(
                    userLat = gpsState.latitude,
                    userLon = gpsState.longitude,
                    userBearing = gpsState.bearing,
                    userSpeedKmh = gpsState.currentSpeedKmh,
                    recordedPoints = recordedPoints,
                    isFollowUser = true,
                    showControls = true,
                    initialZoom = 16
                )

                // Top Minimal HUD
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, MonoSurfaceBorder, RoundedCornerShape(14.dp)),
                    color = MonoSurface.copy(alpha = 0.95f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "${(gpsState.currentSpeedKmh * gpsState.speedUnit.factor).roundToInt()}",
                                color = MonoWhite,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = gpsState.speedUnit.label,
                                color = MonoGray,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(bottom = 3.dp)
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "МАКС: ${(gpsState.maxSpeedKmh * gpsState.speedUnit.factor).roundToInt()} ${gpsState.speedUnit.label}",
                                color = MonoWhite,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "СРЕД: ${(gpsState.avgSpeedKmh * gpsState.speedUnit.factor).roundToInt()} ${gpsState.speedUnit.label}",
                                color = MonoGray,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        IconButton(
                            onClick = { viewMode = 0 },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(MonoSurfaceElevated)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FullscreenExit,
                                contentDescription = "Назад",
                                tint = MonoWhite
                            )
                        }
                    }
                }

                // Bottom Record Bar in Map View
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(14.dp)
                ) {
                    TripRecordBar(
                        isRecording = gpsState.isRecording,
                        recordingDurationSec = gpsState.recordingDurationSec,
                        recordingDistanceMeters = gpsState.recordingDistanceMeters,
                        isSimulationMode = gpsState.isSimulationMode,
                        onStartRecording = onStartRecording,
                        onStopRecording = onStopRecording,
                        onToggleSimulation = onToggleSimulation
                    )
                }
            }
        }

        // Save Trip Dialog
        if (pendingSaveTrip != null) {
            SaveTripDialog(
                trip = pendingSaveTrip,
                onSave = onSaveTrip,
                onDismiss = onDismissSaveDialog
            )
        }
    }
}

@Composable
private fun TopBarStatus(
    gpsState: GpsUiState,
    onToggleViewMode: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "SPEEDOMETER",
                color = MonoWhite,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
            Text(
                text = "GPS 2D ТЕЛЕМЕТРИЯ",
                color = MonoGray,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(MonoSurfaceElevated)
                .border(1.dp, MonoSurfaceBorder, RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = if (gpsState.hasGpsFix) Icons.Default.GpsFixed else Icons.Default.GpsNotFixed,
                contentDescription = null,
                tint = if (gpsState.hasGpsFix) MonoWhite else MonoGray,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = if (gpsState.isSimulationMode) "ТЕСТ-ДРАЙВ"
                else if (gpsState.hasGpsFix) "GPS АКТИВЕН"
                else "ПОИСК GPS",
                color = if (gpsState.hasGpsFix) MonoWhite else MonoGray,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun SaveTripDialog(
    trip: RecordedTripResult,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember {
        val dateStr = SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()).format(Date(trip.startTime))
        mutableStateOf("Поездка $dateStr")
    }

    val distanceKm = trip.distanceMeters / 1000.0
    val durationMin = trip.durationSeconds / 60
    val durationSec = trip.durationSeconds % 60

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MonoSurface,
        title = {
            Text(
                text = "Сохранить поездку?",
                color = MonoWhite,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MonoBlack,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MonoSurfaceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Дистанция:", color = MonoGray, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            Text(
                                text = String.format("%.2f км", distanceKm),
                                color = MonoWhite,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Время:", color = MonoGray, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            Text(
                                text = "${durationMin}м ${durationSec}с",
                                color = MonoWhite,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Макс. скорость:", color = MonoGray, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            Text(
                                text = "${trip.maxSpeedKmh.roundToInt()} км/ч",
                                color = MonoWhite,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название маршрута") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MonoWhite,
                        unfocusedTextColor = MonoWhite,
                        focusedBorderColor = MonoWhite,
                        unfocusedBorderColor = MonoSurfaceBorder,
                        focusedLabelColor = MonoWhite,
                        unfocusedLabelColor = MonoGray
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(title) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MonoWhite,
                    contentColor = MonoBlack
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Сохранить", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = MonoGray, fontFamily = FontFamily.Monospace)
            }
        }
    )
}
