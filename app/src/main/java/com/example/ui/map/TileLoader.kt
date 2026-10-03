package com.example.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.collection.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import kotlin.math.PI
import kotlin.math.asinh
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.tan

object MapProjection {
    const val TILE_SIZE = 256

    fun lonToTileX(lon: Double, zoom: Int): Int {
        val n = 1 shl zoom
        return floor((lon + 180.0) / 360.0 * n).toInt().coerceIn(0, n - 1)
    }

    fun latToTileY(lat: Double, zoom: Int): Int {
        val n = 1 shl zoom
        val latRad = Math.toRadians(lat.coerceIn(-85.05112878, 85.05112878))
        return floor((1.0 - asinh(tan(latRad)) / PI) / 2.0 * n).toInt().coerceIn(0, n - 1)
    }

    fun lonToPixelX(lon: Double, zoom: Int): Double {
        val mapSize = (1 shl zoom) * TILE_SIZE.toDouble()
        return (lon + 180.0) / 360.0 * mapSize
    }

    fun latToPixelY(lat: Double, zoom: Int): Double {
        val mapSize = (1 shl zoom) * TILE_SIZE.toDouble()
        val latRad = Math.toRadians(lat.coerceIn(-85.05112878, 85.05112878))
        return (1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / PI) / 2.0 * mapSize
    }

    fun pixelXToLon(px: Double, zoom: Int): Double {
        val mapSize = (1 shl zoom) * TILE_SIZE.toDouble()
        return (px / mapSize) * 360.0 - 180.0
    }

    fun pixelYToLat(py: Double, zoom: Int): Double {
        val mapSize = (1 shl zoom) * TILE_SIZE.toDouble()
        val y = 0.5 - (py / mapSize)
        return 90.0 - 360.0 * Math.atan(Math.exp(-y * 2.0 * PI)) / PI
    }
}

class TileLoader(private val context: Context) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .build()

    private val cacheDir = File(context.cacheDir, "osm_tiles").apply { mkdirs() }
    private val memoryCache = object : LruCache<String, Bitmap>(80) {}

    fun getCachedTile(zoom: Int, x: Int, y: Int): Bitmap? {
        val key = "$zoom/$x/$y"
        memoryCache.get(key)?.let { return it }

        val diskFile = File(cacheDir, "${zoom}_${x}_${y}.png")
        if (diskFile.exists()) {
            try {
                val bitmap = BitmapFactory.decodeFile(diskFile.absolutePath)
                if (bitmap != null) {
                    memoryCache.put(key, bitmap)
                    return bitmap
                }
            } catch (e: Exception) {
                diskFile.delete()
            }
        }
        return null
    }

    suspend fun fetchTile(zoom: Int, x: Int, y: Int): Bitmap? = withContext(Dispatchers.IO) {
        val key = "$zoom/$x/$y"
        getCachedTile(zoom, x, y)?.let { return@withContext it }

        val diskFile = File(cacheDir, "${zoom}_${x}_${y}.png")
        val url = "https://tile.openstreetmap.org/$zoom/$x/$y.png"

        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "SpeedometerMinimal/2.0 (OfflineReady; Android)")
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                response.body?.bytes()?.let { bytes ->
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (bitmap != null) {
                        try {
                            FileOutputStream(diskFile).use { it.write(bytes) }
                        } catch (e: Exception) {
                            // ignore write error
                        }
                        memoryCache.put(key, bitmap)
                        return@withContext bitmap
                    }
                }
            }
        } catch (e: Exception) {
            // Offline gracefully - no crash, no hang
        }
        null
    }
}
