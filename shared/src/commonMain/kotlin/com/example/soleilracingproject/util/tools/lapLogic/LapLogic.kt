package com.example.soleilracingproject.util.tools.lapLogic

import com.example.soleilracingproject.model.analysis.Lap
import com.example.soleilracingproject.model.telemetry.GpsGate
import com.example.soleilracingproject.model.telemetry.TelemetryPoint
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

object LapLogic {

    /** Normalizes longitude sign if logger omitted minus sign for Western Hemisphere coordinates */
    fun normalizeLon(lon: Double, refLon: Double = 0.0): Double {
        if (refLon < -20.0 && lon > 20.0 && abs(lon + refLon) < 5.0) return -lon
        if (refLon > 20.0 && lon < -20.0 && abs(-lon - refLon) < 5.0) return -lon
        return lon
    }

    /** Distance in meters between two GPS coordinates */
    fun haversineDistanceMeters(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val nLon1 = lon1
        val nLon2 = normalizeLon(lon2, lon1)
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
        val nPtLon = normalizeLon(ptLon, gate.lon1)
        val nG1Lon = gate.lon1
        val nG2Lon = normalizeLon(gate.lon2, gate.lon1)

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

    fun calculateBearingDegrees(
        prevLat: Double, prevLon: Double,
        currLat: Double, currLon: Double
    ): Float {
        val nLon1 = prevLon
        val nLon2 = normalizeLon(currLon, prevLon)
        val dLon = (nLon2 - nLon1) * (PI / 180.0)
        val y = sin(dLon) * cos(currLat * PI / 180.0)
        val x = cos(prevLat * PI / 180.0) * sin(currLat * PI / 180.0) -
                sin(prevLat * PI / 180.0) * cos(currLat * PI / 180.0) * cos(dLon)
        val bearing = atan2(y, x) * (180.0 / PI)
        return ((bearing + 360.0) % 360.0).toFloat()
    }

    fun inferTargetBearingDegrees(
        points: List<TelemetryPoint>,
        gate: GpsGate
    ): Float? {
        if (gate.targetBearingDegrees != null) {
            return gate.targetBearingDegrees
        }
        if (points.size < 2) return null

        val candidateHeadings = mutableListOf<Float>()

        for (i in 1 until points.size) {
            val prev = points[i - 1]
            val curr = points[i]

            val dist = distanceToGateSegmentMeters(curr.latitude.toDouble(), curr.longitude.toDouble(), gate)
            if (dist <= 50.0) {
                val motionDx = curr.longitude.toDouble() - prev.longitude.toDouble()
                val motionDy = curr.latitude.toDouble() - prev.latitude.toDouble()
                if (abs(motionDx) > 1e-7 || abs(motionDy) > 1e-7) {
                    val bearing = calculateBearingDegrees(
                        prev.latitude.toDouble(), prev.longitude.toDouble(),
                        curr.latitude.toDouble(), curr.longitude.toDouble()
                    )
                    candidateHeadings.add(bearing)
                }
            }
        }

        if (candidateHeadings.isEmpty()) return null

        val bins = IntArray(36)
        for (heading in candidateHeadings) {
            val binIndex = ((heading.roundToInt() % 360 + 360) % 360) / 10
            bins[binIndex]++
        }

        val maxBin = bins.indices.maxByOrNull { bins[it] } ?: 0
        if (bins[maxBin] == 0) return null

        val targetBinCenter = maxBin * 10f + 5f
        val dominantHeadings = candidateHeadings.filter { heading ->
            val diff = abs(heading - targetBinCenter)
            val angleDiff = if (diff > 180f) 360f - diff else diff
            angleDiff <= 45f
        }

        if (dominantHeadings.isEmpty()) return null

        var sumSin = 0.0
        var sumCos = 0.0
        for (h in dominantHeadings) {
            val rad = h * (PI / 180.0)
            sumSin += sin(rad)
            sumCos += cos(rad)
        }

        val meanRad = atan2(sumSin, sumCos)
        val meanDeg = (meanRad * (180.0 / PI) + 360.0) % 360.0
        return meanDeg.toFloat()
    }

    fun isForwardCrossing(
        prev: TelemetryPoint,
        curr: TelemetryPoint,
        gate: GpsGate,
        maxAngleDiffDegrees: Float = 75f
    ): Boolean {
        val ax = normalizeLon(prev.longitude.toDouble(), gate.lon1)
        val ay = prev.latitude.toDouble()
        val bx = normalizeLon(curr.longitude.toDouble(), gate.lon1)
        val by = curr.latitude.toDouble()

        val motionDx = bx - ax
        val motionDy = by - ay

        if (abs(motionDx) < 1e-9 && abs(motionDy) < 1e-9) return true

        val carBearing = calculateBearingDegrees(ay, ax, by, bx)

        val target = if (gate.targetBearingDegrees != null) {
            (gate.targetBearingDegrees + 360f) % 360f
        } else {
            val cx = gate.lon1
            val cy = gate.lat1
            val dx = normalizeLon(gate.lon2, gate.lon1)
            val dy = gate.lat2
            val gateLineBearing = calculateBearingDegrees(cy, cx, dy, dx)
            (gateLineBearing + 90f) % 360f
        }

        val diff = abs(carBearing - target)
        val angleDiff = if (diff > 180f) 360f - diff else diff

        return angleDiff <= maxAngleDiffDegrees
    }

    fun hasCrossedGpsGate(
        previousPoint: TelemetryPoint?,
        currentPoint: TelemetryPoint,
        finishGate: GpsGate?,
        enforceDirection: Boolean = true
    ): Boolean {
        if (previousPoint == null || finishGate == null) return false
        val ax = normalizeLon(previousPoint.longitude.toDouble(), finishGate.lon1)
        val ay = previousPoint.latitude.toDouble()
        val bx = normalizeLon(currentPoint.longitude.toDouble(), finishGate.lon1)
        val by = currentPoint.latitude.toDouble()
        val cx = finishGate.lon1
        val cy = finishGate.lat1
        val dx = normalizeLon(finishGate.lon2, finishGate.lon1)
        val dy = finishGate.lat2

        if (!validGps(ay, ax) || !validGps(by, bx) ||
            !validGps(cy, cx) || !validGps(dy, dx)
        ) return false

        val abc = orientation(ax, ay, bx, by, cx, cy)
        val abd = orientation(ax, ay, bx, by, dx, dy)
        val cda = orientation(cx, cy, dx, dy, ax, ay)
        val cdb = orientation(cx, cy, dx, dy, bx, by)

        val crossedLine = (abc * abd < 0 && cda * cdb < 0) ||
            (abc == 0 && onSegment(ax, ay, bx, by, cx, cy)) ||
            (abd == 0 && onSegment(ax, ay, bx, by, dx, dy)) ||
            (cda == 0 && onSegment(cx, cy, dx, dy, ax, ay)) ||
            (cdb == 0 && onSegment(cx, cy, dx, dy, bx, by))

        if (!crossedLine) return false

        if (enforceDirection && !isForwardCrossing(previousPoint, currentPoint, finishGate)) {
            return false
        }

        return true
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
        val widthDeg = 0.001
        val perpLat = (-dLon / len) * widthDeg
        val perpLon = (dLat / len) * widthDeg

        val targetHeading = calculateBearingDegrees(refLat, refLon, nextLat, nextLon)

        return GpsGate(
            lat1 = refLat - perpLat,
            lon1 = refLon - perpLon,
            lat2 = refLat + perpLat,
            lon2 = refLon + perpLon,
            targetBearingDegrees = targetHeading
        )
    }

    fun splitSessionIntoLaps(
        sessionId: String,
        points: List<TelemetryPoint>,
        finishGate: GpsGate? = null,
        minLapDurationMs: Long = 15000L,
        enforceDirection: Boolean = true,
        discardOutInLaps: Boolean = true
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

        val baseGate: GpsGate = finishGate ?: autoGenerateGate(points) ?: return listOf(
            buildLap(
                sessionId = sessionId,
                lapNumber = 1,
                points = points,
                durationMs = (points.last().time - points.first().time).toLong()
            )
        )

        val inferredBearing = inferTargetBearingDegrees(points, baseGate)
        val gateToUse = if (baseGate.targetBearingDegrees == null && inferredBearing != null) {
            baseGate.copy(targetBearingDegrees = inferredBearing)
        } else {
            baseGate
        }

        val laps = mutableListOf<Lap>()
        val currentLapPoints = mutableListOf<TelemetryPoint>()
        var lapNumber = 1
        var lastCrossingTime = 0L
        var hasCrossedFirstGate = false

        for (i in 1 until points.size) {
            val prev = points[i - 1]
            val curr = points[i]

            val isCrossing = hasCrossedGpsGate(prev, curr, gateToUse, enforceDirection)

            if (discardOutInLaps && !hasCrossedFirstGate) {
                if (isCrossing) {
                    hasCrossedFirstGate = true
                    lastCrossingTime = curr.time.toLong()
                    currentLapPoints.clear()
                    currentLapPoints.add(curr)
                }
            } else {
                currentLapPoints.add(curr)
                val timeSinceLastCrossing = curr.time.toLong() - lastCrossingTime

                if (isCrossing && timeSinceLastCrossing >= minLapDurationMs) {
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
                    lastCrossingTime = curr.time.toLong()
                    hasCrossedFirstGate = true
                }
            }
        }

        if (laps.isEmpty() && currentLapPoints.size >= 2) {
            val duration = (currentLapPoints.last().time - currentLapPoints.first().time).toLong()
            laps.add(
                buildLap(
                    sessionId = sessionId,
                    lapNumber = 1,
                    points = currentLapPoints.toList(),
                    durationMs = duration
                )
            )
        } else if (!discardOutInLaps && currentLapPoints.size >= 2) {
            val lapDuration = (currentLapPoints.last().time - currentLapPoints.first().time).toLong()
            laps.add(
                buildLap(
                    sessionId = sessionId,
                    lapNumber = lapNumber,
                    points = currentLapPoints.toList(),
                    durationMs = lapDuration
                )
            )
        }

        return laps
    }
}
