package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trips")
data class Trip(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val startTime: Long,
    val endTime: Long,
    val durationSeconds: Long,
    val distanceMeters: Double,
    val maxSpeedKmh: Float,
    val avgSpeedKmh: Float,
    val pointsJson: String
) {
    fun getPoints(): List<TrackPoint> {
        return TrackPoint.listFromJsonString(pointsJson)
    }
}
