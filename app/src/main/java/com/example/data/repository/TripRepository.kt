package com.example.data.repository

import com.example.data.db.TripDao
import com.example.data.model.Trip
import kotlinx.coroutines.flow.Flow

class TripRepository(private val tripDao: TripDao) {
    val allTrips: Flow<List<Trip>> = tripDao.getAllTrips()

    suspend fun getTripById(id: Long): Trip? = tripDao.getTripById(id)

    suspend fun insertTrip(trip: Trip): Long = tripDao.insertTrip(trip)

    suspend fun deleteTrip(trip: Trip) = tripDao.deleteTrip(trip)

    suspend fun deleteTripById(id: Long) = tripDao.deleteTripById(id)
}
