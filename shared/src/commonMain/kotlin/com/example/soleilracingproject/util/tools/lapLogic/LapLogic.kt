package com.example.soleilracingproject.util.tools.lapLogic

import com.example.soleilracingproject.model.analysis.Lap
import com.example.soleilracingproject.model.telemetry.GpsGate
import com.example.soleilracingproject.model.telemetry.TelemetryPoint
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object LapLogic {

    fun haversineDistanceMeters(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val earthRadiusMeters = 6371000.0
        val dLat = (lat2 - lat1) * (PI / 180.0)
        val dLon = (lon2 - lon1) * (PI / 180.0)
        val a = sin(dLat / 2.0).pow(2) +
            cos(lat1 * (PI / 180.0)) * cos(lat2 * (PI / 180.0)) *
            sin(dLon / 2.0).pow(2)
        val c = 2.0 * atan2(sqrt(a), sqrt(1.0 - a))
        return earthRadiusMeters * c
    }

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

        val abc = orientation(ax, ay, bx, by, cx, cy)
        val abd = orientation(ax, ay, bx, by, dx, dy)
        val cda = orientation(cx, cy, dx, dy, ax, ay)
        val cdb = orientation(cx, cy, dx, dy, bx, by)

        if (abc * abd < 0 && cda * cdb < 0) return true

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
        val maxSpeed = points.maxOfOrNull { it.speed } ?: 0f

        return Lap(
            id = "${sessionId}_lap_$lapNumber",
            sessionId = sessionId,
            lapNumber = lapNumber,
            lapTime = durationMs,
            topSpeedKmh = maxSpeed,
            telemetryPoints = points
        )
    }

    fun splitSessionIntoLaps(
        sessionId: String,
        points: List<TelemetryPoint>,
        finishGate: GpsGate? = null,
        minLapDurationMs: Long = 15000L,
        maxProximityMeters: Double = 20.0
    ): List<Lap> {
        if (points.size < 2) {
            return listOf(
                buildLap(
                    sessionId = sessionId,
                    lapNumber = 1,
                    points = points,
                    durationMs = 0L
                )
            )
        }

        val gateToUse: GpsGate? = finishGate

        if (gateToUse == null) {
            val duration = (points.last().time - points.first().time).toLong()
            return listOf(
                buildLap(
                    sessionId = sessionId,
                    lapNumber = 1,
                    points = points,
                    durationMs = duration
                )
            )
        }

        val laps = mutableListOf<Lap>()
        val currentLapPoints = mutableListOf<TelemetryPoint>()
        var lapNumber = 1
        var lastCrossingTime = points.first().time

        currentLapPoints.add(points.first())

        val gateCenterLat = (gateToUse.lat1 + gateToUse.lat2) / 2.0
        val gateCenterLon = (gateToUse.lon1 + gateToUse.lon2) / 2.0

        for (i in 1 until points.size) {
            val prev = points[i - 1]
            val curr = points[i]
            currentLapPoints.add(curr)

            val timeSinceLastCrossing = (curr.time - lastCrossingTime).toLong()

            val isSegmentCrossing = hasCrossedGpsGate(prev, curr, gateToUse)
            val distanceToGateMeters = haversineDistanceMeters(
                curr.latitude.toDouble(), curr.longitude.toDouble(),
                gateCenterLat, gateCenterLon
            )
            val isProximityCrossing = distanceToGateMeters <= maxProximityMeters

            if ((isSegmentCrossing || isProximityCrossing) && (timeSinceLastCrossing >= minLapDurationMs)) {
                laps.add(
                    buildLap(
                        sessionId = sessionId,
                        lapNumber = lapNumber++,
                        points = currentLapPoints.toList(),
                        durationMs = timeSinceLastCrossing
                    )
                )

                currentLapPoints.clear()
                currentLapPoints.add(curr)
                lastCrossingTime = curr.time
            }
        }

        if (currentLapPoints.size >= 2) {
            val lapDuration =
                (currentLapPoints.last().time - currentLapPoints.first().time).toLong()
            laps.add(
                buildLap(
                    sessionId = sessionId,
                    lapNumber = lapNumber,
                    points = currentLapPoints.toList(),
                    durationMs = lapDuration
                )
            )
        } else if (laps.isEmpty()) {
            val duration = (points.last().time - points.first().time).toLong()
            laps.add(
                buildLap(
                    sessionId = sessionId,
                    lapNumber = 1,
                    points = points,
                    durationMs = duration
                )
            )
        }

        return laps
    }
}
