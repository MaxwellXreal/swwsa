package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.Trip
import com.example.data.repository.TripRepository
import com.example.gps.GpsTracker
import com.example.gps.GpsUiState
import com.example.gps.RecordedTripResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SpeedometerViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = TripRepository(database.tripDao())
    val gpsTracker = GpsTracker(application)

    val gpsState: StateFlow<GpsUiState> = gpsTracker.uiState

    val allTrips: StateFlow<List<Trip>> = repository.allTrips
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _selectedTrip = MutableStateFlow<Trip?>(null)
    val selectedTrip: StateFlow<Trip?> = _selectedTrip.asStateFlow()

    private val _pendingSaveTrip = MutableStateFlow<RecordedTripResult?>(null)
    val pendingSaveTrip: StateFlow<RecordedTripResult?> = _pendingSaveTrip.asStateFlow()

    fun startGps() {
        gpsTracker.startGpsTracking()
    }

    fun stopGps() {
        gpsTracker.stopGpsTracking()
    }

    fun resetMaxSpeed() {
        gpsTracker.resetMaxSpeed()
    }

    fun resetAvgSpeed() {
        gpsTracker.resetAvgSpeed()
    }

    fun toggleSpeedUnit() {
        gpsTracker.toggleSpeedUnit()
    }

    fun toggleSimulation() {
        gpsTracker.toggleSimulationMode()
    }

    fun startRecording() {
        gpsTracker.startTripRecording()
    }

    fun stopRecording() {
        val result = gpsTracker.stopTripRecording()
        if (result.points.isNotEmpty() || result.distanceMeters > 5.0) {
            _pendingSaveTrip.value = result
        }
    }

    fun saveTripWithTitle(customTitle: String) {
        val result = _pendingSaveTrip.value ?: return
        viewModelScope.launch {
            val dateStr = SimpleDateFormat("dd MMM, HH:mm", Locale("ru")).format(Date(result.startTime))
            val finalTitle = if (customTitle.isNotBlank()) customTitle else "Поездка $dateStr"
            val trip = Trip(
                title = finalTitle,
                startTime = result.startTime,
                endTime = result.endTime,
                durationSeconds = result.durationSeconds,
                distanceMeters = result.distanceMeters,
                maxSpeedKmh = result.maxSpeedKmh,
                avgSpeedKmh = result.avgSpeedKmh,
                pointsJson = com.example.data.model.TrackPoint.listToJsonString(result.points)
            )
            val newId = repository.insertTrip(trip)
            _pendingSaveTrip.value = null
            _selectedTrip.value = trip.copy(id = newId)
        }
    }

    fun dismissSaveDialog() {
        _pendingSaveTrip.value = null
    }

    fun selectTrip(trip: Trip?) {
        _selectedTrip.value = trip
    }

    fun deleteTrip(tripId: Long) {
        viewModelScope.launch {
            repository.deleteTripById(tripId)
            if (_selectedTrip.value?.id == tripId) {
                _selectedTrip.value = null
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        gpsTracker.onDestroy()
    }
}
