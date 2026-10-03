package com.example.gps

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import com.example.data.model.TrackPoint
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class GpsUiState(
    val currentSpeedKmh: Float = 0f,
    val maxSpeedKmh: Float = 0f,
    val avgSpeedKmh: Float = 0f,
    val totalDistanceMeters: Double = 0.0,
    val altitudeMeters: Double = 0.0,
    val accuracyMeters: Float = 0f,
    val latitude: Double = 55.7558, // Default fallback (e.g. Moscow/City center)
    val longitude: Double = 37.6173,
    val bearing: Float = 0f,
    val hasGpsFix: Boolean = false,
    val isRecording: Boolean = false,
    val recordingDurationSec: Long = 0L,
    val recordingDistanceMeters: Double = 0.0,
    val recordingPointsCount: Int = 0,
    val isIdle: Boolean = false,
    val idleSecondsLeft: Int = 30,
    val isSimulationMode: Boolean = false,
    val speedUnit: SpeedUnit = SpeedUnit.KMH
)

enum class SpeedUnit(val label: String, val factor: Float) {
    KMH("км/ч", 1.0f),
    MPH("mph", 0.621371f)
}

class GpsTracker(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.Default)

    private val _uiState = MutableStateFlow(GpsUiState())
    val uiState: StateFlow<GpsUiState> = _uiState.asStateFlow()

    private var fusedClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null
    private var locationManager: LocationManager? = null
    private var legacyListener: LocationListener? = null

    // Real-time stats
    private var maxSpeedKmh: Float = 0f
    private var accumulatedDistanceMeters: Double = 0.0
    private var accumulatedMovingTimeSec: Long = 0L
    private var lastLocation: Location? = null
    private var lastMovementTimestamp: Long = System.currentTimeMillis()

    // Idle detection for auto-resetting average speed
    private val IDLE_THRESHOLD_KMH = 1.8f
    private val IDLE_TIMEOUT_SECONDS = 30
    private var idleTimerJob: Job? = null
    private var idleDurationSec = 0

    // Trip recording
    private val recordedTrackPoints = mutableListOf<TrackPoint>()
    private var recordingStartTime: Long = 0L
    private var recordingTimerJob: Job? = null
    private var recordingMaxSpeed: Float = 0f
    private var recordingTotalDistance: Double = 0.0

    // Simulation
    private var simulationJob: Job? = null

    init {
        try {
            fusedClient = LocationServices.getFusedLocationProviderClient(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

        startIdleMonitor()
    }

    private fun startIdleMonitor() {
        idleTimerJob?.cancel()
        idleTimerJob = scope.launch {
            while (isActive) {
                delay(1000)
                val currentSpeed = _uiState.value.currentSpeedKmh
                if (currentSpeed < IDLE_THRESHOLD_KMH) {
                    idleDurationSec++
                    val left = (IDLE_TIMEOUT_SECONDS - idleDurationSec).coerceAtLeast(0)
                    if (left > 0) {
                        _uiState.value = _uiState.value.copy(
                            isIdle = true,
                            idleSecondsLeft = left
                        )
                    } else {
                        // Reset average speed after idle timeout
                        accumulatedDistanceMeters = 0.0
                        accumulatedMovingTimeSec = 0L
                        _uiState.value = _uiState.value.copy(
                            avgSpeedKmh = 0f,
                            isIdle = true,
                            idleSecondsLeft = 0
                        )
                    }
                } else {
                    idleDurationSec = 0
                    if (_uiState.value.isIdle) {
                        _uiState.value = _uiState.value.copy(isIdle = false, idleSecondsLeft = IDLE_TIMEOUT_SECONDS)
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startGpsTracking() {
        if (_uiState.value.isSimulationMode) return

        try {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
                .setMinUpdateIntervalMillis(500)
                .setMinUpdateDistanceMeters(0.5f)
                .build()

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.lastLocation?.let { handleNewLocation(it) }
                }
            }

            fusedClient?.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to LocationManager
            startLegacyLocationManager()
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLegacyLocationManager() {
        try {
            legacyListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    handleNewLocation(location)
                }
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }
            locationManager?.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000L,
                0.5f,
                legacyListener!!,
                Looper.getMainLooper()
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopGpsTracking() {
        locationCallback?.let { fusedClient?.removeLocationUpdates(it) }
        legacyListener?.let { locationManager?.removeUpdates(it) }
    }

    private fun handleNewLocation(location: Location) {
        val rawSpeedMps = if (location.hasSpeed()) location.speed else {
            // compute from lastLocation if speed not present
            val prev = lastLocation
            if (prev != null) {
                val dt = (location.time - prev.time) / 1000f
                if (dt > 0.5f) location.distanceTo(prev) / dt else 0f
            } else 0f
        }

        var speedKmh = rawSpeedMps * 3.6f
        if (speedKmh < 1.0f) speedKmh = 0f // Filter out GPS jitter

        // Update Max speed
        if (speedKmh > maxSpeedKmh) {
            maxSpeedKmh = speedKmh
        }

        // Distance and Average speed calculation
        val prev = lastLocation
        var stepDistance = 0.0
        if (prev != null && speedKmh > 1.0f) {
            stepDistance = location.distanceTo(prev).toDouble()
            accumulatedDistanceMeters += stepDistance
            accumulatedMovingTimeSec += 1
            idleDurationSec = 0 // reset idle
        }

        val calculatedAvgSpeed = if (accumulatedMovingTimeSec > 2) {
            ((accumulatedDistanceMeters / accumulatedMovingTimeSec) * 3.6f).toFloat()
        } else {
            speedKmh
        }

        lastLocation = location

        // Trip recording
        if (_uiState.value.isRecording) {
            recordingTotalDistance += stepDistance
            if (speedKmh > recordingMaxSpeed) {
                recordingMaxSpeed = speedKmh
            }
            val point = TrackPoint(
                latitude = location.latitude,
                longitude = location.longitude,
                speedKmh = speedKmh,
                altitude = location.altitude,
                timestamp = System.currentTimeMillis(),
                distanceFromStartMeters = recordingTotalDistance.toFloat()
            )
            synchronized(recordedTrackPoints) {
                recordedTrackPoints.add(point)
            }
        }

        _uiState.value = _uiState.value.copy(
            currentSpeedKmh = speedKmh,
            maxSpeedKmh = maxSpeedKmh,
            avgSpeedKmh = calculatedAvgSpeed,
            totalDistanceMeters = accumulatedDistanceMeters,
            altitudeMeters = location.altitude,
            accuracyMeters = if (location.hasAccuracy()) location.accuracy else 5f,
            latitude = location.latitude,
            longitude = location.longitude,
            bearing = if (location.hasBearing()) location.bearing else _uiState.value.bearing,
            hasGpsFix = true,
            recordingDistanceMeters = recordingTotalDistance,
            recordingPointsCount = recordedTrackPoints.size
        )
    }

    fun resetMaxSpeed() {
        maxSpeedKmh = _uiState.value.currentSpeedKmh
        _uiState.value = _uiState.value.copy(maxSpeedKmh = maxSpeedKmh)
    }

    fun resetAvgSpeed() {
        accumulatedDistanceMeters = 0.0
        accumulatedMovingTimeSec = 0L
        idleDurationSec = 0
        _uiState.value = _uiState.value.copy(avgSpeedKmh = 0f, isIdle = false)
    }

    fun toggleSpeedUnit() {
        val nextUnit = if (_uiState.value.speedUnit == SpeedUnit.KMH) SpeedUnit.MPH else SpeedUnit.KMH
        _uiState.value = _uiState.value.copy(speedUnit = nextUnit)
    }

    fun startTripRecording() {
        synchronized(recordedTrackPoints) {
            recordedTrackPoints.clear()
        }
        recordingStartTime = System.currentTimeMillis()
        recordingMaxSpeed = _uiState.value.currentSpeedKmh
        recordingTotalDistance = 0.0

        // Add current location if available
        if (_uiState.value.hasGpsFix) {
            recordedTrackPoints.add(
                TrackPoint(
                    latitude = _uiState.value.latitude,
                    longitude = _uiState.value.longitude,
                    speedKmh = _uiState.value.currentSpeedKmh,
                    altitude = _uiState.value.altitudeMeters,
                    timestamp = recordingStartTime,
                    distanceFromStartMeters = 0f
                )
            )
        }

        _uiState.value = _uiState.value.copy(
            isRecording = true,
            recordingDurationSec = 0L,
            recordingDistanceMeters = 0.0,
            recordingPointsCount = recordedTrackPoints.size
        )

        recordingTimerJob?.cancel()
        recordingTimerJob = scope.launch {
            while (isActive) {
                delay(1000)
                val durationSec = (System.currentTimeMillis() - recordingStartTime) / 1000L
                _uiState.value = _uiState.value.copy(recordingDurationSec = durationSec)
            }
        }
    }

    fun stopTripRecording(): RecordedTripResult {
        recordingTimerJob?.cancel()
        recordingTimerJob = null

        val pointsCopy: List<TrackPoint>
        synchronized(recordedTrackPoints) {
            pointsCopy = ArrayList(recordedTrackPoints)
        }

        val durationSec = (System.currentTimeMillis() - recordingStartTime) / 1000L
        val distance = recordingTotalDistance
        val maxSpd = recordingMaxSpeed
        val avgSpd = if (durationSec > 0) ((distance / durationSec) * 3.6f).toFloat() else 0f

        _uiState.value = _uiState.value.copy(
            isRecording = false,
            recordingDurationSec = 0L,
            recordingDistanceMeters = 0.0,
            recordingPointsCount = 0
        )

        return RecordedTripResult(
            points = pointsCopy,
            startTime = recordingStartTime,
            endTime = System.currentTimeMillis(),
            durationSeconds = durationSec,
            distanceMeters = distance,
            maxSpeedKmh = maxSpd,
            avgSpeedKmh = avgSpd
        )
    }

    fun getCurrentlyRecordedPoints(): List<TrackPoint> {
        synchronized(recordedTrackPoints) {
            return ArrayList(recordedTrackPoints)
        }
    }

    // Toggle Simulation Mode for testing or emulator
    fun toggleSimulationMode() {
        val newSim = !_uiState.value.isSimulationMode
        if (newSim) {
            stopGpsTracking()
            startSimulation()
        } else {
            stopSimulation()
            startGpsTracking()
        }
    }

    private fun startSimulation() {
        simulationJob?.cancel()
        _uiState.value = _uiState.value.copy(
            isSimulationMode = true,
            hasGpsFix = true
        )

        simulationJob = scope.launch {
            // City center coordinates (e.g. Red Square / Garden Ring loop in Moscow)
            var lat = 55.751244
            var lon = 37.618423
            var simSpeed = 0f
            var targetSpeed = 48f
            var heading = 45f
            var timeStep = 0

            while (isActive) {
                delay(800)
                timeStep++

                // Realistic speed variations: cruise, accelerate, brake at lights
                when {
                    timeStep % 35 in 28..34 -> {
                        // Decelerate and stop at red light
                        targetSpeed = 0f
                    }
                    timeStep % 35 in 0..7 -> {
                        // Accelerate
                        targetSpeed = 65f
                    }
                    timeStep % 35 in 8..18 -> {
                        // Highway / avenue speed
                        targetSpeed = 82f
                    }
                    else -> {
                        targetSpeed = 42f
                    }
                }

                // Smooth acceleration / deceleration
                simSpeed += (targetSpeed - simSpeed) * 0.18f
                if (simSpeed < 0.5f && targetSpeed == 0f) simSpeed = 0f

                // Advance coordinate in direction of heading
                if (simSpeed > 0.5f) {
                    val speedMps = simSpeed / 3.6f
                    val distanceMoved = speedMps * 0.8f // for 0.8s
                    val dLat = (distanceMoved * cos(Math.toRadians(heading.toDouble()))) / 111111.0
                    val dLon = (distanceMoved * sin(Math.toRadians(heading.toDouble()))) / (111111.0 * cos(Math.toRadians(lat)))
                    lat += dLat
                    lon += dLon

                    // Gentle road curving
                    heading = (heading + (sin(timeStep * 0.15) * 4.0).toFloat()) % 360f
                }

                val simulatedLocation = Location("Simulation").apply {
                    latitude = lat
                    longitude = lon
                    speed = simSpeed / 3.6f
                    altitude = 156.0 + sin(timeStep * 0.1) * 12.0
                    bearing = heading
                    accuracy = 3.5f
                    time = System.currentTimeMillis()
                }

                handleNewLocation(simulatedLocation)
            }
        }
    }

    private fun stopSimulation() {
        simulationJob?.cancel()
        simulationJob = null
        _uiState.value = _uiState.value.copy(isSimulationMode = false)
    }

    fun onDestroy() {
        stopGpsTracking()
        stopSimulation()
        idleTimerJob?.cancel()
        recordingTimerJob?.cancel()
    }
}

data class RecordedTripResult(
    val points: List<TrackPoint>,
    val startTime: Long,
    val endTime: Long,
    val durationSeconds: Long,
    val distanceMeters: Double,
    val maxSpeedKmh: Float,
    val avgSpeedKmh: Float
)
