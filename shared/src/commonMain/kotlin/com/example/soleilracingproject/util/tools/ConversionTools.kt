package com.example.soleilracingproject.util.tools

import androidx.compose.ui.geometry.Offset
import com.example.soleilracingproject.model.telemetry.EulerAngles
import com.example.soleilracingproject.model.telemetry.LatLng
import kotlin.math.roundToInt
import kotlin.math.atan2
import kotlin.math.asin
import kotlin.math.PI

object ConversionTools {

    // Formatters designed to avoid java.lang.String.format in KMP commonMain
    fun Float.formatOneDecimal(): String {
        val rounded = (this * 10).roundToInt() / 10.0f
        return rounded.toString()
    }

    fun Float.formatTwoDecimals(): String {
        val rounded = (this * 100).roundToInt() / 100.0f
        return rounded.toString()
    }

    fun Long.formatLapTime(): String {
        if (this <= 0) return "--:--.---"
        val minutes = this / 60000
        val seconds = (this % 60000) / 1000
        val millis = this % 1000
        val secStr = if (seconds < 10) "0$seconds" else "$seconds"
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
    ): List<Offset>
    {
        if (coords.size < 2 ) return emptyList()

        // Create find the bounds of the graph
        val minLat = coords.minOf { it!!.latitude }
        val maxLat = coords.maxOf { it!!.latitude }
        val minLng = coords.minOf { it!!.longitude }
        val maxLng = coords.maxOf { it!!.longitude }

        //Creating the range and ensuring there is no division by 0 with coerceAtLeast
        val rangeLat = (maxLat - minLat).coerceAtLeast(1e-6.toFloat())
        val rangeLng = (maxLng - minLng).coerceAtLeast(1e-6.toFloat())

        val graphWidth = canvasWidth - (2 * padding)
        val graphHeight = canvasHeight - (2 * padding)

        val result: List<Offset> = coords.map { coord ->
            Offset(
                x = padding + ((coord!!.longitude - minLng) / rangeLng) * graphWidth,
                y = canvasHeight - padding - ((coord!!.latitude - minLat) / rangeLat) * graphHeight
            )}

        return result

    }
}
