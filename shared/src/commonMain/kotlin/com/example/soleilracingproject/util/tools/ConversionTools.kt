package com.example.soleilracingproject.util.tools

import androidx.compose.ui.geometry.Offset
import com.example.soleilracingproject.model.telemetry.EulerAngles
import com.example.soleilracingproject.model.telemetry.LatLng
import kotlin.math.roundToInt
import kotlin.math.atan2
import kotlin.math.asin
import kotlin.math.PI
import kotlin.math.cos

object ConversionTools {

    // Formatters designed to avoid java.lang.String.format in KMP commonMain
    fun Float.formatOneDecimal(): String {
        val rounded = (this * 10).roundToInt() / 10.0f
        return rounded.toString()
    }

    fun Long.formatLapTime(): String {
        if (this <= 0) return "--:--.---"
        val minutes = this / 60000
        val seconds = (this % 60000) / 1000
        val millis = this % 1000
        val secStr = if (seconds < 10) "0$seconds" else seconds.toString()
        val msStr = when {
            millis < 10 -> "00$millis"
            millis < 100 -> "0$millis"
            else -> "$millis"
        }
        return "$minutes:$secStr.$msStr"
    }

    // Assumes a unit quaternion; returns roll, pitch, and yaw in degrees.
    fun quaternionToEuler(
        x: Double,
        y: Double,
        z: Double,
        w: Double
    ): EulerAngles {
        val roll = atan2(2.0 * (w * x + y * z), 1.0 - 2.0 * (x * x + y * y))
        val pitch = asin((2.0 * (w * y - z * x)).coerceIn(-1.0, 1.0))
        val yaw = atan2(2.0 * (w * z + x * y), 1.0 - 2.0 * (y * y + z * z))
        val radiansToDegrees = 180.0 / PI
        return EulerAngles(
            roll = roll * radiansToDegrees,
            pitch = pitch * radiansToDegrees,
            yaw = yaw * radiansToDegrees
        )
    }

// Normalize coordinates for drawing on to the Canvas Composable relative to size
    fun normalizeCoordinates(
        coords: List<LatLng?>,
        canvasWidth: Float,
        canvasHeight: Float,
        padding: Float = 40f
    ): List<Offset> {
        if (canvasWidth <= 0f || canvasHeight <= 0f) return emptyList()

        val validCoords = coords.filterNotNull()
        if (validCoords.size < 2) return emptyList()

        val minLat = validCoords.minOf { it.latitude }
        val maxLat = validCoords.maxOf { it.latitude }
        val minLng = validCoords.minOf { it.longitude }
        val maxLng = validCoords.maxOf { it.longitude }

        // Calculate real-world aspect ratio correction (KMP Safe)
        // Convert average latitude to radians: degrees * (PI / 180)
        val avgLat = (minLat + maxLat) / 2f
        val avgLatRad = (avgLat * (PI / 180.0)).toFloat()
        val lngCorrection = cos(avgLatRad)

        val rangeLat = (maxLat - minLat).coerceAtLeast(1e-6f)
        val rangeLng = ((maxLng - minLng) * lngCorrection).coerceAtLeast(1e-6f)

        val availableWidth = (canvasWidth - (2 * padding)).coerceAtLeast(1f)
        val availableHeight = (canvasHeight - (2 * padding)).coerceAtLeast(1f)

        // Uniform scaling: find which dimension is the bottleneck
        val scaleX = availableWidth / rangeLng
        val scaleY = availableHeight / rangeLat
        val uniformScale = minOf(scaleX, scaleY)

        // Calculate offsets to center the track in the canvas
        val trackPixelWidth = rangeLng * uniformScale
        val trackPixelHeight = rangeLat * uniformScale

        val offsetX = padding + (availableWidth - trackPixelWidth) / 2f
        val offsetY = padding + (availableHeight - trackPixelHeight) / 2f

        return validCoords.map { coord ->
            val correctedLng = (coord.longitude - minLng) * lngCorrection

            Offset(
                x = offsetX + (correctedLng * uniformScale),
                y = canvasHeight - offsetY - ((coord.latitude - minLat) * uniformScale)
            )
        }
    }
}

