package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.SpeedometerViewModel
import com.example.ui.screens.SpeedometerScreen
import com.example.ui.screens.TripDetailScreen
import com.example.ui.screens.TripsHistoryScreen
import com.example.ui.theme.MonoBlack
import com.example.ui.theme.MonoGray
import com.example.ui.theme.MonoSurface
import com.example.ui.theme.MonoSurfaceBorder
import com.example.ui.theme.MonoSurfaceElevated
import com.example.ui.theme.MonoWhite
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainApp()
            }
        }
    }
}

@Composable
fun MainApp(
    viewModel: SpeedometerViewModel = viewModel()
) {
    val context = LocalContext.current
    val gpsState by viewModel.gpsState.collectAsStateWithLifecycle()
    val allTrips by viewModel.allTrips.collectAsStateWithLifecycle()
    val selectedTrip by viewModel.selectedTrip.collectAsStateWithLifecycle()
    val pendingSaveTrip by viewModel.pendingSaveTrip.collectAsStateWithLifecycle()

    var currentTab by remember { mutableIntStateOf(0) } // 0: Speedometer, 1: Trips

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasLocationPermission = fineGranted || coarseGranted
        if (hasLocationPermission) {
            viewModel.startGps()
        }
    }

    LaunchedEffect(Unit) {
        if (hasLocationPermission) {
            viewModel.startGps()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopGps()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MonoBlack,
        bottomBar = {
            if (selectedTrip == null) {
                NavigationBar(
                    containerColor = MonoSurface,
                    tonalElevation = 0.dp,
                    modifier = Modifier.border(
                        1.dp,
                        MonoSurfaceBorder,
                        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                    )
                ) {
                    NavigationBarItem(
                        selected = currentTab == 0,
                        onClick = { currentTab = 0 },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = "Спидометр"
                            )
                        },
                        label = {
                            Text(
                                "Спидометр",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MonoWhite,
                            selectedTextColor = MonoWhite,
                            unselectedIconColor = MonoGray,
                            unselectedTextColor = MonoGray,
                            indicatorColor = MonoSurfaceElevated
                        ),
                        modifier = Modifier.testTag("nav_tab_speedometer")
                    )

                    NavigationBarItem(
                        selected = currentTab == 1,
                        onClick = { currentTab = 1 },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Route,
                                contentDescription = "Поездки"
                            )
                        },
                        label = {
                            Text(
                                if (allTrips.isNotEmpty()) "Поездки (${allTrips.size})" else "Поездки",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MonoWhite,
                            selectedTextColor = MonoWhite,
                            unselectedIconColor = MonoGray,
                            unselectedTextColor = MonoGray,
                            indicatorColor = MonoSurfaceElevated
                        ),
                        modifier = Modifier.testTag("nav_tab_trips")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (selectedTrip != null) {
                TripDetailScreen(
                    trip = selectedTrip!!,
                    onBack = { viewModel.selectTrip(null) }
                )
            } else {
                when (currentTab) {
                    0 -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            if (!hasLocationPermission && !gpsState.isSimulationMode) {
                                MinimalPermissionBanner(
                                    onRequestPermission = {
                                        permissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION
                                            )
                                        )
                                    },
                                    onEnableSimulation = {
                                        viewModel.toggleSimulation()
                                    }
                                )
                            }

                            SpeedometerScreen(
                                modifier = Modifier.weight(1f),
                                gpsState = gpsState,
                                recordedPoints = viewModel.gpsTracker.getCurrentlyRecordedPoints(),
                                pendingSaveTrip = pendingSaveTrip,
                                onResetMaxSpeed = { viewModel.resetMaxSpeed() },
                                onResetAvgSpeed = { viewModel.resetAvgSpeed() },
                                onToggleUnit = { viewModel.toggleSpeedUnit() },
                                onToggleSimulation = { viewModel.toggleSimulation() },
                                onStartRecording = { viewModel.startRecording() },
                                onStopRecording = { viewModel.stopRecording() },
                                onSaveTrip = { title -> viewModel.saveTripWithTitle(title) },
                                onDismissSaveDialog = { viewModel.dismissSaveDialog() }
                            )
                        }
                    }
                    1 -> {
                        TripsHistoryScreen(
                            trips = allTrips,
                            onSelectTrip = { trip -> viewModel.selectTrip(trip) },
                            onDeleteTrip = { tripId -> viewModel.deleteTrip(tripId) },
                            onNavigateToSpeedometer = { currentTab = 0 }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MinimalPermissionBanner(
    onRequestPermission: () -> Unit,
    onEnableSimulation: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        color = MonoSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MonoSurfaceBorder)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MonoWhite,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "ТРЕБУЕТСЯ GPS",
                    color = MonoWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Text(
                text = "Для оффлайн записи скорости и трека требуется доступ к координатам устройства.",
                color = MonoGray,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onRequestPermission,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MonoWhite,
                        contentColor = MonoBlack
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Разрешить GPS", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }

                Button(
                    onClick = onEnableSimulation,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MonoSurfaceElevated,
                        contentColor = MonoWhite
                    ),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MonoSurfaceBorder),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Тест-драйв", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}
