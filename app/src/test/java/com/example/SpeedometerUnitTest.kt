package com.example

import com.example.data.model.TrackPoint
import com.example.gps.SpeedUnit
import com.example.ui.map.MapProjection
import com.example.ui.map.getSpeedColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SpeedometerUnitTest {

    @Test
    fun testTrackPointJsonSerialization() {
        val original = listOf(
            TrackPoint(
                latitude = 55.7558,
                longitude = 37.6173,
                speedKmh = 64.5f,
                altitude = 150.0,
                timestamp = 1000L,
                distanceFromStartMeters = 250f
            ),
            TrackPoint(
                latitude = 55.7560,
                longitude = 37.6180,
                speedKmh = 82.0f,
                altitude = 152.0,
                timestamp = 2000L,
                distanceFromStartMeters = 380f
            )
        )

        val jsonStr = TrackPoint.listToJsonString(original)
        val deserialized = TrackPoint.listFromJsonString(jsonStr)

        assertEquals(2, deserialized.size)
        assertEquals(55.7558, deserialized[0].latitude, 0.0001)
        assertEquals(64.5f, deserialized[0].speedKmh, 0.1f)
        assertEquals(82.0f, deserialized[1].speedKmh, 0.1f)
    }

    @Test
    fun testMapProjectionCalculations() {
        val zoom = 15
        val lon = 37.6173
        val lat = 55.7558

        val px = MapProjection.lonToPixelX(lon, zoom)
        val py = MapProjection.latToPixelY(lat, zoom)

        val restoredLon = MapProjection.pixelXToLon(px, zoom)
        val restoredLat = MapProjection.pixelYToLat(py, zoom)

        assertEquals(lon, restoredLon, 0.0001)
        assertEquals(lat, restoredLat, 0.0001)
    }

    @Test
    fun testSpeedUnitConversion() {
        val speedKmh = 100f
        val speedMph = speedKmh * SpeedUnit.MPH.factor
        assertEquals(62.1371f, speedMph, 0.01f)
    }

    @Test
    fun testSpeedColorThresholds() {
        assertNotNull(getSpeedColor(15f))
        assertNotNull(getSpeedColor(35f))
        assertNotNull(getSpeedColor(60f))
        assertNotNull(getSpeedColor(85f))
        assertNotNull(getSpeedColor(120f))
    }
}
