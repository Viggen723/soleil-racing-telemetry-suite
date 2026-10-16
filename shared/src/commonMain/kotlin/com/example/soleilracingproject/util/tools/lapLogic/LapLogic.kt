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

    fun normalizeLon(lon: Double, lat: Double): Double {
        return if (lat in 10.0..75.0 && lon > 0.0) -lon else lon
    }

    fun haversineDistanceMeters(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val nLon1 = normalizeLon(lon1, lat1)
        val nLon2 = normalizeLon(lon2, lat2)
        val earthRadiusMeters = 6371000.0
        val dLat = (lat2 - lat1) * (PI / 180.0)
        val dLon = (nLon2 - nLon1) * (PI / 180.0)
        val a = sin(dLat / 2.0).pow(2) +
            cos(lat1 * (PI / 180.0)) * cos(lat2 * (PI / 180.0)) *
            sin(dLon / 2.0).pow(2)
        val c = 2.0 * atan2(sqrt(a), sqrt(1.0 - a))
        return earthRadiusMeters * c
    }

    fun distanceToGateSegmentMeters(
        ptLat: Double, ptLon: Double,
        gate: GpsGate
    ): Double {
        val nPtLon = normalizeLon(ptLon, ptLat)
        val nG1Lon = normalizeLon(gate.lon1, gate.lat1)
        val nG2Lon = normalizeLon(gate.lon2, gate.lat2)

        val radLat = ptLat * (PI / 180.0)
        val cosLat = cos(radLat)

        val dx = (nG2Lon - nG1Lon) * cosLat
        val dy = (gate.lat2 - gate.lat1)
        val lenSq = dx * dx + dy * dy

        if (lenSq < 1e-18) {
            return haversineDistanceMeters(ptLat, nPtLon, gate.lat1, nG1Lon)
        }

        val px = (nPtLon - nG1Lon) * cosLat
        val py = (ptLat - gate.lat1)
        val t = ((px * dx + py * dy) / lenSq).coerceIn(0.0, 1.0)

        val projLon = nG1Lon + t * (nG2Lon - nG1Lon)
        val projLat = gate.lat1 + t * (gate.lat2 - gate.lat1)

        return haversineDistanceMeters(ptLat, nPtLon, projLat, projLon)
    }

    fun hasCrossedGpsGate(
        previousPoint: TelemetryPoint?,
        currentPoint: TelemetryPoint,
        finishGate: GpsGate?
    ): Boolean {
        if (previousPoint == null || finishGate == null) return false
        val ax = normalizeLon(previousPoint.longitude.toDouble(), previousPoint.latitude.toDouble())
        val ay = previousPoint.latitude.toDouble()
        val bx = normalizeLon(currentPoint.longitude.toDouble(), currentPoint.latitude.toDouble())
        val by = currentPoint.latitude.toDouble()
        val cx = normalizeLon(finishGate.lon1, finishGate.lat1)
        val cy = finishGate.lat1
        val dx = normalizeLon(finishGate.lon2, finishGate.lat2)
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

    fun autoGenerateGate(points: List<TelemetryPoint>): GpsGate? {
        val validPoints = points.filter {
            validGps(it.latitude.toDouble(), it.longitude.toDouble()) &&
                (it.latitude != 0f || it.longitude != 0f)
        }
        if (validPoints.size < 10) return null

        val refIndex = validPoints.indexOfFirst { it.speed > 10f }
            .let { if (it in 0..validPoints.size - 10) it else (validPoints.size / 10).coerceIn(0, validPoints.size - 1) }

        val refPoint = validPoints[refIndex]
        val targetIdx = (refIndex + 5).coerceAtMost(validPoints.size - 1)
        val nextPoint = validPoints[targetIdx]

        val refLat = refPoint.latitude.toDouble()
        val refLon = normalizeLon(refPoint.longitude.toDouble(), refLat)
        val nextLat = nextPoint.latitude.toDouble()
        val nextLon = normalizeLon(nextPoint.longitude.toDouble(), nextLat)

        val dLat = nextLat - refLat
        val dLon = nextLon - refLon

        if (abs(dLat) < 1e-7 && abs(dLon) < 1e-7) return null

        val len = sqrt(dLat * dLat + dLon * dLon)
        val widthDeg = 0.0005
        val perpLat = (-dLon / len) * widthDeg
        val perpLon = (dLat / len) * widthDeg

        return GpsGate(
            lat1 = refLat - perpLat,
            lon1 = refLon - perpLon,
            lat2 = refLat + perpLat,
            lon2 = refLon + perpLon,
        )
    }

    fun splitSessionIntoLaps(
        sessionId: String,
        points: List<TelemetryPoint>,
        finishGate: GpsGate? = null,
        minLapDurationMs: Long = 15000L,
        maxProximityMeters: Double = 35.0
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

        val gateToUse: GpsGate = finishGate ?: autoGenerateGate(points) ?: return listOf(
            buildLap(
                sessionId = sessionId,
                lapNumber = 1,
                points = points,
                durationMs = (points.last().time - points.first().time).toLong()
            )
        )

        val laps = mutableListOf<Lap>()
        val currentLapPoints = mutableListOf<TelemetryPoint>()
        var lapNumber = 1
        var lastCrossingTime = points.first().time

        currentLapPoints.add(points.first())

        for (i in 1 until points.size) {
            val prev = points[i - 1]
            val curr = points[i]
            currentLapPoints.add(curr)

            val timeSinceLastCrossing = (curr.time - lastCrossingTime).toLong()

            val isSegmentCrossing = hasCrossedGpsGate(prev, curr, gateToUse)
            val distanceToGateSegment = distanceToGateSegmentMeters(
                curr.latitude.toDouble(), curr.longitude.toDouble(),
                gateToUse
            )
            val isProximityCrossing = distanceToGateSegment <= maxProximityMeters

            val isCrossing = isSegmentCrossing || isProximityCrossing

            if (isCrossing) {
                if (timeSinceLastCrossing >= minLapDurationMs) {
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
                } else if (laps.isEmpty() && currentLapPoints.size > 1 && timeSinceLastCrossing < minLapDurationMs) {
                    currentLapPoints.clear()
                    currentLapPoints.add(curr)
                    lastCrossingTime = curr.time
                }
            }
        }

        if (currentLapPoints.size >= 2) {
            val lapDuration = (currentLapPoints.last().time - currentLapPoints.first().time).toLong()
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
