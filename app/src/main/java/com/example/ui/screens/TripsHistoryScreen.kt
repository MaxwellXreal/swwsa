package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Trip
import com.example.ui.theme.MonoBlack
import com.example.ui.theme.MonoGray
import com.example.ui.theme.MonoSurface
import com.example.ui.theme.MonoSurfaceBorder
import com.example.ui.theme.MonoSurfaceElevated
import com.example.ui.theme.MonoWhite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun TripsHistoryScreen(
    modifier: Modifier = Modifier,
    trips: List<Trip>,
    onSelectTrip: (Trip) -> Unit,
    onDeleteTrip: (Long) -> Unit,
    onNavigateToSpeedometer: () -> Unit
) {
    var tripToDelete by remember { mutableStateOf<Trip?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MonoBlack)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "МОИ ПОЕЗДКИ",
                        color = MonoWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Оффлайн база записанных маршрутов",
                        color = MonoGray,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MonoSurfaceElevated,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MonoSurfaceBorder)
                ) {
                    Text(
                        text = "${trips.size} ЗАП.",
                        color = MonoWhite,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            if (trips.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(64.dp),
                            shape = CircleShape,
                            color = MonoSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MonoSurfaceBorder)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Route,
                                    contentDescription = null,
                                    tint = MonoWhite,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }

                        Text(
                            text = "Нет сохраненных поездок",
                            color = MonoWhite,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )

                        Text(
                            text = "Нажмите «Начать запись» на главном экране перед поездкой, и маршрут с участками скоростей сохранится здесь оффлайн.",
                            color = MonoGray,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Button(
                            onClick = onNavigateToSpeedometer,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MonoWhite,
                                contentColor = MonoBlack
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Перейти к спидометру", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    items(trips, key = { it.id }) { trip ->
                        TripItemCard(
                            trip = trip,
                            onClick = { onSelectTrip(trip) },
                            onDelete = { tripToDelete = trip }
                        )
                    }
                }
            }
        }

        // Delete Dialog
        tripToDelete?.let { trip ->
            AlertDialog(
                onDismissRequest = { tripToDelete = null },
                containerColor = MonoSurface,
                title = { Text("Удалить поездку?", color = MonoWhite, fontFamily = FontFamily.Monospace) },
                text = {
                    Text(
                        "Удалить маршрут «${trip.title}»?",
                        color = MonoGray,
                        fontFamily = FontFamily.Monospace
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onDeleteTrip(trip.id)
                            tripToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MonoWhite,
                            contentColor = MonoBlack
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Удалить", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { tripToDelete = null }) {
                        Text("Отмена", color = MonoGray, fontFamily = FontFamily.Monospace)
                    }
                }
            )
        }
    }
}

@Composable
fun TripItemCard(
    trip: Trip,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormatted = SimpleDateFormat("dd.MM.yyyy • HH:mm", Locale.getDefault()).format(Date(trip.startTime))
    val distanceKm = trip.distanceMeters / 1000.0
    val durationMin = trip.durationSeconds / 60
    val durationSec = trip.durationSeconds % 60
    val durationFormatted = if (durationMin > 0) "${durationMin}м ${durationSec}с" else "${durationSec}с"

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, MonoSurfaceBorder, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .testTag("trip_item_${trip.id}"),
        color = MonoSurface
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = trip.title,
                        color = MonoWhite,
                        fontSize = 14.sp,
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

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Удалить",
                        tint = MonoGray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MonoSurfaceElevated)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "ДИСТ.", color = MonoGray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    Text(
                        text = String.format("%.2f км", distanceKm),
                        color = MonoWhite,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Column {
                    Text(text = "ВРЕМЯ", color = MonoGray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    Text(
                        text = durationFormatted,
                        color = MonoWhite,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Column {
                    Text(text = "МАКС.", color = MonoGray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    Text(
                        text = "${trip.maxSpeedKmh.roundToInt()} км/ч",
                        color = MonoWhite,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Column {
                    Text(text = "СРЕДН.", color = MonoGray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    Text(
                        text = "${trip.avgSpeedKmh.roundToInt()} км/ч",
                        color = MonoWhite,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint = MonoGray,
                    modifier = Modifier.size(11.dp)
                )
            }
        }
    }
}
