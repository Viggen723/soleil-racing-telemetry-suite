package com.example.soleilracingproject.util.tools

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
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

    // Converts acceleration to Gs
    fun normalizeAccel(mPerSecSq: Float): Float {
        return mPerSecSq / 9.80665f
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

        val avgLat = (minLat + maxLat) / 2f
        val avgLatRad = (avgLat * (PI / 180.0)).toFloat()
        val lngCorrection = cos(avgLatRad)

        val rangeLat = (maxLat - minLat).coerceAtLeast(1e-6f)
        val rangeLng = ((maxLng - minLng) * lngCorrection).coerceAtLeast(1e-6f)

        val availableWidth = (canvasWidth - (2 * padding)).coerceAtLeast(1f)
        val availableHeight = (canvasHeight - (2 * padding)).coerceAtLeast(1f)

        val scaleX = availableWidth / rangeLng
        val scaleY = availableHeight / rangeLat
        val uniformScale = minOf(scaleX, scaleY)

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

    /**
     * Ramer-Douglas-Peucker (RDP) polyline simplification algorithm.
     * Reduces 2D offsets while preserving sharp turns, apexes, and peaks.
     * @param points List of 2D screen/canvas Offsets
     * @param epsilon Distance threshold in pixels (e.g. 1.0f to 2.5f)
     */
    fun ramerDouglasPeucker(points: List<Offset>, epsilon: Float): List<Offset> {
        if (points.size <= 2) return points

        var maxDistance = 0f
        var index = 0

        val start = points.first()
        val end = points.last()

        val dx = end.x - start.x
        val dy = end.y - start.y
        val lineLen = kotlin.math.hypot(dx, dy)

        for (i in 1 until points.size - 1) {
            val pt = points[i]
            val distance = if (lineLen == 0f) {
                kotlin.math.hypot(pt.x - start.x, pt.y - start.y)
            } else {
                kotlin.math.abs(dy * pt.x - dx * pt.y + end.x * start.y - end.y * start.x) / lineLen
            }

            if (distance > maxDistance) {
                maxDistance = distance
                index = i
            }
        }

        return if (maxDistance > epsilon) {
            val left = ramerDouglasPeucker(points.subList(0, index + 1), epsilon)
            val right = ramerDouglasPeucker(points.subList(index, points.size), epsilon)
            left.dropLast(1) + right
        } else {
            listOf(start, end)
        }
    }

    /**
     * Applies a 5-Point Gaussian Low-Pass Filter ([1, 4, 6, 4, 1] / 16) to 2D Offsets
     * to eliminate lateral GPS/sensor wiggles before curve fitting.
     */
    fun smoothPointsMovingAverage(points: List<Offset>, passes: Int = 2): List<Offset> {
        if (points.size <= 3) return points

        var current = points
        repeat(passes) {
            val smoothed = ArrayList<Offset>(current.size)
            smoothed.add(current[0])
            smoothed.add(current[1])

            for (i in 2 until current.size - 2) {
                val p0 = current[i - 2]
                val p1 = current[i - 1]
                val p2 = current[i]
                val p3 = current[i + 1]
                val p4 = current[i + 2]

                // 5-point Gaussian binomial kernel [1, 4, 6, 4, 1] / 16
                val avgX = (p0.x + 4f * p1.x + 6f * p2.x + 4f * p3.x + p4.x) / 16f
                val avgY = (p0.y + 4f * p1.y + 6f * p2.y + 4f * p3.y + p4.y) / 16f

                smoothed.add(Offset(avgX, avgY))
            }

            smoothed.add(current[current.size - 2])
            smoothed.add(current.last())
            current = smoothed
        }

        return current
    }

    /**
     * Converts a list of 2D points into a silky smooth Cubic Bezier / Catmull-Rom Path.
     * Prevents the "square/polygonal" look when points are simplified.
     */
    fun createSmoothPath(points: List<Offset>, isClosed: Boolean = false): Path {
        val path = Path()
        if (points.isEmpty()) return path
        if (points.size < 3) {
            path.moveTo(points.first().x, points.first().y)
            for (i in 1 until points.size) {
                path.lineTo(points[i].x, points[i].y)
            }
            if (isClosed) path.close()
            return path
        }

        path.moveTo(points.first().x, points.first().y)

        for (i in 0 until points.size - 1) {
            val p0 = if (i > 0) points[i - 1] else points[i]
            val p1 = points[i]
            val p2 = points[i + 1]
            val p3 = if (i + 2 < points.size) points[i + 2] else p2

            // Calculate direction vectors
            val dx1 = p2.x - p1.x
            val dy1 = p2.y - p1.y
            val len1 = kotlin.math.hypot(dx1, dy1)

            val dx0 = p1.x - p0.x
            val dy0 = p1.y - p0.y
            val len0 = kotlin.math.hypot(dx0, dy0)

            // Normalized dot product (cos theta)
            val isNearlyStraight = if (len1 > 0f && len0 > 0f) {
                val dot = (dx1 * dx0 + dy1 * dy0) / (len1 * len0)
                dot > 0.90f // Turn angle < ~26 degrees
            } else false

            if (isNearlyStraight) {
                // On straight sections, draw straight line to eliminate cubic S-curve overshoot / side waving
                path.lineTo(p2.x, p2.y)
            } else {
                // On corners and curves, use cubic Bezier splines
                val control1X = p1.x + (p2.x - p0.x) / 6f
                val control1Y = p1.y + (p2.y - p0.y) / 6f

                val control2X = p2.x - (p3.x - p1.x) / 6f
                val control2Y = p2.y - (p3.y - p1.y) / 6f

                path.cubicTo(
                    control1X, control1Y,
                    control2X, control2Y,
                    p2.x, p2.y
                )
            }
        }

        if (isClosed) {
            path.close()
        }

        return path
    }
}