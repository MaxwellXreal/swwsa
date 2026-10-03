package com.example.data.model

import org.json.JSONArray
import org.json.JSONObject

data class TrackPoint(
    val latitude: Double,
    val longitude: Double,
    val speedKmh: Float,
    val altitude: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis(),
    val distanceFromStartMeters: Float = 0f
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("lat", latitude)
            put("lon", longitude)
            put("spd", speedKmh.toDouble())
            put("alt", altitude)
            put("time", timestamp)
            put("dist", distanceFromStartMeters.toDouble())
        }
    }

    companion object {
        fun fromJson(obj: JSONObject): TrackPoint {
            return TrackPoint(
                latitude = obj.optDouble("lat", 0.0),
                longitude = obj.optDouble("lon", 0.0),
                speedKmh = obj.optDouble("spd", 0.0).toFloat(),
                altitude = obj.optDouble("alt", 0.0),
                timestamp = obj.optLong("time", 0L),
                distanceFromStartMeters = obj.optDouble("dist", 0.0).toFloat()
            )
        }

        fun listToJsonString(points: List<TrackPoint>): String {
            val array = JSONArray()
            for (p in points) {
                array.put(p.toJson())
            }
            return array.toString()
        }

        fun listFromJsonString(jsonStr: String?): List<TrackPoint> {
            if (jsonStr.isNullOrEmpty()) return emptyList()
            val list = mutableListOf<TrackPoint>()
            try {
                val array = JSONArray(jsonStr)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(fromJson(obj))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return list
        }
    }
}
