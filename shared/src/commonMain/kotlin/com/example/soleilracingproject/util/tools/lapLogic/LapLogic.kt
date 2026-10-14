package com.example.soleilracingproject.util.tools.lapLogic

import com.example.soleilracingproject.model.analysis.Lap
import com.example.soleilracingproject.model.telemetry.GpsGate
import com.example.soleilracingproject.model.telemetry.TelemetryPoint
import kotlin.random.Random
import kotlin.math.abs

object LapLogic {

    fun hasCrossedGpsGate(
        previousPoint: TelemetryPoint?,
        currentPoint: TelemetryPoint,
        finishGate: GpsGate?
    ): Boolean {
        if (previousPoint == null || finishGate == null) return false
        val ax = previousPoint.longitude.toDouble()
        val ay = previousPoint.latitude.toDouble()
        val bx = currentPoint.longitude.toDouble()
        val by = currentPoint.latitude.toDouble()
        val cx = finishGate.lon1
        val cy = finishGate.lat1
        val dx = finishGate.lon2
        val dy = finishGate.lat2
        if (!validGps(ay, ax) || !validGps(by, bx) ||
            !validGps(cy, cx) || !validGps(dy, dx)
        ) return false

        // Longitude is X and latitude is Y over our small track area.
        // Direction filtering can come later when track configuration is connected.
        val abc = orientation(ax, ay, bx, by, cx, cy)
        val abd = orientation(ax, ay, bx, by, dx, dy)
        val cda = orientation(cx, cy, dx, dy, ax, ay)
        val cdb = orientation(cx, cy, dx, dy, bx, by)
        if (abc * abd < 0 && cda * cdb < 0) return true

        // Endpoint touches and collinear overlap count, but extensions beyond the gate don't.
        return (abc == 0 && onSegment(ax, ay, bx, by, cx, cy)) ||
            (abd == 0 && onSegment(ax, ay, bx, by, dx, dy)) ||
            (cda == 0 && onSegment(cx, cy, dx, dy, ax, ay)) ||
            (cdb == 0 && onSegment(cx, cy, dx, dy, bx, by))
    }

    private fun validGps(latitude: Double, longitude: Double): Boolean =
        latitude.isFinite() && longitude.isFinite() &&
            latitude in -90.0..90.0 && longitude in -180.0..180.0

    private fun orientation(
        ax: Double, ay: Double, bx: Double, by: Double, cx: Double, cy: Double
    ): Int {
        val left = (bx - ax) * (cy - ay)
        val right = (by - ay) * (cx - ax)
        val cross = left - right
        // Tiny numerical tolerance in squared degrees, not a proximity radius.
        val epsilon = 1e-18 + 1e-12 * (abs(left) + abs(right))
        return when {
            cross > epsilon -> 1
            cross < -epsilon -> -1
            else -> 0
        }
    }

    private fun onSegment(
        ax: Double, ay: Double, bx: Double, by: Double, px: Double, py: Double
    ): Boolean {
        val epsilon = 1e-10
        return px >= minOf(ax, bx) - epsilon && px <= maxOf(ax, bx) + epsilon &&
            py >= minOf(ay, by) - epsilon && py <= maxOf(ay, by) + epsilon
    }

    fun buildLap(
        sessionId: String,
        lapNumber: Int,
        points: List<TelemetryPoint>,
        durationMs: Long
    ): Lap {
        // The ViewModel times crossings explicitly; buffered samples may start a little later.
        val maxSpeed = points.maxOfOrNull { it.speed } ?: 0f

        return Lap(
            id = "lap_${Random.nextLong().toString(16)}",
            sessionId = sessionId,
            lapNumber = lapNumber,
            lapTime = durationMs,
            topSpeedKmh = maxSpeed,
            telemetryPoints = points
        )
    }
}
