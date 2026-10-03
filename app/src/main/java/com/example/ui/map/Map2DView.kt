package com.example.ui.map

import android.graphics.Bitmap
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TrackPoint
import com.example.ui.theme.MonoBlack
import com.example.ui.theme.MonoGray
import com.example.ui.theme.MonoGrayDark
import com.example.ui.theme.MonoSurface
import com.example.ui.theme.MonoSurfaceBorder
import com.example.ui.theme.MonoWhite
import com.example.ui.theme.MonoWhite20
import com.example.ui.theme.MonoWhite50
import com.example.ui.theme.MonoWhite80
import com.example.ui.theme.SpeedShadeBrisk
import com.example.ui.theme.SpeedShadeFast
import com.example.ui.theme.SpeedShadeMedium
import com.example.ui.theme.SpeedShadeSlow
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

fun getSpeedColor(speedKmh: Float): Color {
    return when {
        speedKmh < 30f -> SpeedShadeSlow
        speedKmh < 60f -> SpeedShadeMedium
        speedKmh < 90f -> SpeedShadeBrisk
        else -> SpeedShadeFast
    }
}

@Composable
fun Map2DView(
    modifier: Modifier = Modifier,
    userLat: Double,
    userLon: Double,
    userBearing: Float = 0f,
    userSpeedKmh: Float = 0f,
    recordedPoints: List<TrackPoint> = emptyList(),
    selectedPoint: TrackPoint? = null,
    onPointSelected: ((TrackPoint?) -> Unit)? = null,
    isFollowUser: Boolean = true,
    showControls: Boolean = true,
    initialZoom: Int = 16
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tileLoader = remember { TileLoader(context) }

    var zoom by remember { mutableIntStateOf(initialZoom.coerceIn(12, 18)) }
    var centerLat by remember { mutableStateOf(userLat) }
    var centerLon by remember { mutableStateOf(userLon) }
    var following by remember { mutableStateOf(isFollowUser) }

    LaunchedEffect(userLat, userLon, following) {
        if (following) {
            centerLat = userLat
            centerLon = userLon
        }
    }

    val loadedTiles = remember { mutableStateMapOf<String, Bitmap>() }

    // Pure Monochrome contrast filter for tiles
    val monochromeMapPaint = remember {
        android.graphics.Paint().apply {
            isFilterBitmap = true
            isAntiAlias = true
            // Convert tiles to high-contrast monochrome (dark ground, light roads)
            val matrix = ColorMatrix(
                floatArrayOf(
                    -0.85f, -0.85f, -0.85f, 0f, 255f,
                    -0.85f, -0.85f, -0.85f, 0f, 255f,
                    -0.85f, -0.85f, -0.85f, 0f, 255f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            colorFilter = ColorMatrixColorFilter(matrix)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(MonoBlack)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("map_2d_canvas")
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, gestureZoom, _ ->
                        following = false
                        if (gestureZoom != 1f) {
                            if (gestureZoom > 1.08f && zoom < 18) {
                                zoom++
                            } else if (gestureZoom < 0.92f && zoom > 12) {
                                zoom--
                            }
                        }
                        val centerPxX = MapProjection.lonToPixelX(centerLon, zoom) - pan.x
                        val centerPxY = MapProjection.latToPixelY(centerLat, zoom) - pan.y
                        centerLon = MapProjection.pixelXToLon(centerPxX, zoom)
                        centerLat = MapProjection.pixelYToLat(centerPxY, zoom)
                    }
                }
        ) {
            val width = size.width
            val height = size.height

            val centerPxX = MapProjection.lonToPixelX(centerLon, zoom)
            val centerPxY = MapProjection.latToPixelY(centerLat, zoom)

            val minPxX = centerPxX - width / 2f
            val maxPxX = centerPxX + width / 2f
            val minPxY = centerPxY - height / 2f
            val maxPxY = centerPxY + height / 2f

            val minTileX = floor(minPxX / MapProjection.TILE_SIZE).toInt()
            val maxTileX = ceil(maxPxX / MapProjection.TILE_SIZE).toInt()
            val minTileY = floor(minPxY / MapProjection.TILE_SIZE).toInt()
            val maxTileY = ceil(maxPxY / MapProjection.TILE_SIZE).toInt()

            val maxCoord = 1 shl zoom

            // 1. Draw 2D Background / Offline Tactical City Grid
            drawRect(color = MonoBlack)

            // Draw offline coordinate grid lines (64px spacing)
            val gridStep = 64f
            val startGridX = ((-minPxX % gridStep + gridStep) % gridStep).toFloat()
            val startGridY = ((-minPxY % gridStep + gridStep) % gridStep).toFloat()

            var gx = startGridX
            while (gx < width) {
                drawLine(
                    color = MonoSurfaceBorder.copy(alpha = 0.5f),
                    start = Offset(gx, 0f),
                    end = Offset(gx, height),
                    strokeWidth = 1f
                )
                gx += gridStep
            }

            var gy = startGridY
            while (gy < height) {
                drawLine(
                    color = MonoSurfaceBorder.copy(alpha = 0.5f),
                    start = Offset(0f, gy),
                    end = Offset(width, gy),
                    strokeWidth = 1f
                )
                gy += gridStep
            }

            // 2. Draw Map Tiles if available (online or cached on disk)
            for (tx in minTileX..maxTileX) {
                for (ty in minTileY..maxTileY) {
                    if (ty in 0 until maxCoord) {
                        val normTx = ((tx % maxCoord) + maxCoord) % maxCoord
                        val key = "$zoom/$normTx/$ty"
                        val bitmap = loadedTiles[key] ?: tileLoader.getCachedTile(zoom, normTx, ty)

                        val tileScreenX = (tx * MapProjection.TILE_SIZE - minPxX).toFloat()
                        val tileScreenY = (ty * MapProjection.TILE_SIZE - minPxY).toFloat()

                        if (bitmap != null) {
                            loadedTiles[key] = bitmap
                            drawIntoCanvas { canvas ->
                                canvas.nativeCanvas.drawBitmap(bitmap, tileScreenX, tileScreenY, monochromeMapPaint)
                            }
                        } else {
                            scope.launch {
                                val fetched = tileLoader.fetchTile(zoom, normTx, ty)
                                if (fetched != null) {
                                    loadedTiles[key] = fetched
                                }
                            }
                        }
                    }
                }
            }

            fun toScreenOffset(lat: Double, lon: Double): Offset {
                val px = MapProjection.lonToPixelX(lon, zoom)
                val py = MapProjection.latToPixelY(lat, zoom)
                return Offset((px - minPxX).toFloat(), (py - minPxY).toFloat())
            }

            // 3. Draw Recorded Route with speed-shaded road segments
            if (recordedPoints.size >= 2) {
                for (i in 0 until recordedPoints.size - 1) {
                    val p1 = recordedPoints[i]
                    val p2 = recordedPoints[i + 1]
                    val pt1 = toScreenOffset(p1.latitude, p1.longitude)
                    val pt2 = toScreenOffset(p2.latitude, p2.longitude)

                    val segmentSpeed = max(p1.speedKmh, p2.speedKmh)
                    val segColor = getSpeedColor(segmentSpeed)

                    // Glow aura
                    drawLine(
                        color = segColor.copy(alpha = 0.25f),
                        start = pt1,
                        end = pt2,
                        strokeWidth = 12f,
                        cap = StrokeCap.Round
                    )
                    // Sharp solid track
                    drawLine(
                        color = segColor,
                        start = pt1,
                        end = pt2,
                        strokeWidth = 5f,
                        cap = StrokeCap.Round
                    )
                }

                // Origin dot
                val startPt = toScreenOffset(recordedPoints.first().latitude, recordedPoints.first().longitude)
                drawCircle(color = MonoWhite, radius = 6f, center = startPt)
                drawCircle(color = MonoBlack, radius = 3f, center = startPt)

                // Current end dot
                val endPt = toScreenOffset(recordedPoints.last().latitude, recordedPoints.last().longitude)
                drawCircle(color = MonoWhite, radius = 8f, center = endPt)
            }

            // 4. Highlight Selected Segment Point
            selectedPoint?.let { sp ->
                val selPt = toScreenOffset(sp.latitude, sp.longitude)
                drawCircle(
                    color = MonoWhite20,
                    radius = 22f,
                    center = selPt
                )
                drawCircle(
                    color = MonoWhite,
                    radius = 7f,
                    center = selPt
                )
                drawCircle(
                    color = MonoBlack,
                    radius = 3f,
                    center = selPt
                )
            }

            // 5. Draw User Location & Direction Cursor
            val userScreenPos = toScreenOffset(userLat, userLon)

            // Radar pulse ring
            drawCircle(
                color = MonoWhite20,
                radius = 26f,
                center = userScreenPos
            )
            drawCircle(
                color = MonoWhite,
                radius = 8f,
                center = userScreenPos
            )
            drawCircle(
                color = MonoBlack,
                radius = 4f,
                center = userScreenPos
            )

            // Minimalist Direction Cone / Arrow
            val rad = Math.toRadians(userBearing.toDouble() - 90)
            val dirLength = 22f
            val tipX = userScreenPos.x + (cos(rad) * dirLength).toFloat()
            val tipY = userScreenPos.y + (sin(rad) * dirLength).toFloat()

            val leftWingRad = Math.toRadians(userBearing.toDouble() + 145)
            val rightWingRad = Math.toRadians(userBearing.toDouble() - 145)
            val wingDist = 11f
            val lx = userScreenPos.x + (sin(leftWingRad) * wingDist).toFloat()
            val ly = userScreenPos.y - (cos(leftWingRad) * wingDist).toFloat()
            val rx = userScreenPos.x + (sin(rightWingRad) * wingDist).toFloat()
            val ry = userScreenPos.y - (cos(rightWingRad) * wingDist).toFloat()

            val path = Path().apply {
                moveTo(tipX, tipY)
                lineTo(lx, ly)
                lineTo(userScreenPos.x, userScreenPos.y)
                lineTo(rx, ry)
                close()
            }
            drawPath(path = path, color = MonoWhite)
        }

        // Selected Point Speed Callout Banner
        AnimatedVisibility(
            visible = selectedPoint != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp)
        ) {
            selectedPoint?.let { pt ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MonoSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MonoSurfaceBorder),
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MonoWhite)
                        )
                        Text(
                            text = "${pt.speedKmh.roundToInt()} км/ч",
                            color = MonoWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "• ${(pt.distanceFromStartMeters / 1000f).let { String.format("%.2f км", it) }}",
                            color = MonoGray,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Map Minimalist Controls
        if (showControls) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = {
                        following = true
                        centerLat = userLat
                        centerLon = userLon
                    },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (following) MonoWhite else MonoSurface,
                        contentColor = if (following) MonoBlack else MonoWhite
                    ),
                    modifier = Modifier
                        .size(40.dp)
                        .border(1.dp, MonoSurfaceBorder, CircleShape)
                        .testTag("map_follow_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "Следовать за мной",
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = { if (zoom < 18) zoom++ },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MonoSurface,
                        contentColor = MonoWhite
                    ),
                    modifier = Modifier
                        .size(40.dp)
                        .border(1.dp, MonoSurfaceBorder, CircleShape)
                        .testTag("map_zoom_in")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Приблизить", modifier = Modifier.size(18.dp))
                }

                IconButton(
                    onClick = { if (zoom > 12) zoom-- },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MonoSurface,
                        contentColor = MonoWhite
                    ),
                    modifier = Modifier
                        .size(40.dp)
                        .border(1.dp, MonoSurfaceBorder, CircleShape)
                        .testTag("map_zoom_out")
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Отдалить", modifier = Modifier.size(18.dp))
                }
            }

            // Speed Range Indicator at bottom left
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp),
                shape = RoundedCornerShape(8.dp),
                color = MonoSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MonoSurfaceBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MonoLegendChip(color = SpeedShadeSlow, label = "<30")
                    MonoLegendChip(color = SpeedShadeMedium, label = "60")
                    MonoLegendChip(color = SpeedShadeBrisk, label = "90")
                    MonoLegendChip(color = SpeedShadeFast, label = "120+")
                }
            }
        }
    }
}

@Composable
private fun MonoLegendChip(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(text = label, color = MonoGray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
    }
}
