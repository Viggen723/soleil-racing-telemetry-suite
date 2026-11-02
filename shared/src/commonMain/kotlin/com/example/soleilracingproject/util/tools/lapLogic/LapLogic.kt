package com.example.soleilracingproject.util.tools.lapLogic

import com.example.soleilracingproject.model.analysis.Lap
import com.example.soleilracingproject.model.telemetry.GpsGate
import com.example.soleilracingproject.model.telemetry.TelemetryPoint
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.sqrt

object LapLogic {

    private const val EARTH_RADIUS_METERS = 6_371_000.0
    private const val DEG_TO_RAD = PI / 180.0
    private const val RAD_TO_DEG = 180.0 / PI
    private const val METERS_PER_DEG_LAT = EARTH_RADIUS_METERS * DEG_TO_RAD

    /** Normalizes longitude sign if logger omitted minus sign for Western Hemisphere coordinates */
    fun normalizeLon(lon: Double, refLon: Double = 0.0): Double {
        if (refLon < -20.0 && lon > 20.0 && abs(lon + refLon) < 5.0) return -lon
        if (refLon > 20.0 && lon < -20.0 && abs(-lon - refLon) < 5.0) return -lon
        return lon
    }

    /** Smallest difference between two bearings in degrees [0, 180] */
    fun angularDifferenceDegrees(a: Float, b: Float): Float {
        val diff = abs(a - b) % 360f
        return if (diff > 180f) 360f - diff else diff
    }

    /** High-precision distance in meters between two raw GPS points */
    fun haversineDistanceMeters(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val nLon2 = normalizeLon(lon2, lon1)
        val dLat = (lat2 - lat1) * DEG_TO_RAD
        val dLon = (nLon2 - lon1) * DEG_TO_RAD
        val a = sin(dLat * 0.5) * sin(dLat * 0.5) +
                cos(lat1 * DEG_TO_RAD) * cos(lat2 * DEG_TO_RAD) *
                sin(dLon * 0.5) * sin(dLon * 0.5)
        return 2.0 * EARTH_RADIUS_METERS * atan2(sqrt(a), sqrt(1.0 - a))
    }

    /** Fast local equirectangular distance from point to gate segment in meters */
    fun distanceToGateSegmentMeters(
        ptLat: Double, ptLon: Double,
        gate: GpsGate
    ): Double {
        val nPtLon = normalizeLon(ptLon, gate.lon1)
        val nG2Lon = normalizeLon(gate.lon2, gate.lon1)

        val cosLat = cos(ptLat * DEG_TO_RAD)
        val metersPerDegLon = METERS_PER_DEG_LAT * cosLat

        // Convert gate vector to local metric offsets
        val dx = (nG2Lon - gate.lon1) * metersPerDegLon
        val dy = (gate.lat2 - gate.lat1) * METERS_PER_DEG_LAT
        val lenSq = dx * dx + dy * dy

        if (lenSq < 1e-6) {
            return haversineDistanceMeters(ptLat, nPtLon, gate.lat1, gate.lon1)
        }

        // Point vector relative to gate start in local meters
        val px = (nPtLon - gate.lon1) * metersPerDegLon
        val py = (ptLat - gate.lat1) * METERS_PER_DEG_LAT

        // Projection scalar t clamped to segment [0, 1]
        val t = ((px * dx + py * dy) / lenSq).coerceIn(0.0, 1.0)

        // Residual distance vector to projected point
        val rx = px - t * dx
        val ry = py - t * dy
        return sqrt(rx * rx + ry * ry)
    }

    fun calculateBearingDegrees(
        prevLat: Double, prevLon: Double,
        currLat: Double, currLon: Double
    ): Float {
        val nLon2 = normalizeLon(currLon, prevLon)
        val dLon = (nLon2 - prevLon) * DEG_TO_RAD
        val phi1 = prevLat * DEG_TO_RAD
        val phi2 = currLat * DEG_TO_RAD

        val y = sin(dLon) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(dLon)
        val bearing = atan2(y, x) * RAD_TO_DEG
        return (((bearing + 360.0) % 360.0).toFloat())
    }

    fun inferTargetBearingDegrees(
        points: List<TelemetryPoint>,
        gate: GpsGate
    ): Float? {
        if (gate.targetBearingDegrees != null) return gate.targetBearingDegrees
        if (points.size < 2) return null

        val candidateHeadings = ArrayList<Float>()

        for (i in 1 until points.size) {
            val prev = points[i - 1]
            val curr = points[i]

            val dist = distanceToGateSegmentMeters(curr.latitude.toDouble(), curr.longitude.toDouble(), gate)
            if (dist <= 50.0) {
                val motionDx = curr.longitude.toDouble() - prev.longitude.toDouble()
                val motionDy = curr.latitude.toDouble() - prev.latitude.toDouble()
                if (abs(motionDx) > 1e-7 || abs(motionDy) > 1e-7) {
                    candidateHeadings.add(
                        calculateBearingDegrees(
                            prev.latitude.toDouble(), prev.longitude.toDouble(),
                            curr.latitude.toDouble(), curr.longitude.toDouble()
                        )
                    )
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
            angularDifferenceDegrees(heading, targetBinCenter) <= 45f
        }

        if (dominantHeadings.isEmpty()) return null

        var sumSin = 0.0
        var sumCos = 0.0
        for (h in dominantHeadings) {
            val rad = h * DEG_TO_RAD
            sumSin += sin(rad)
            sumCos += cos(rad)
        }

        val meanRad = atan2(sumSin, sumCos)
        return (((meanRad * RAD_TO_DEG) + 360.0) % 360.0).toFloat()
    }

    fun isForwardCrossing(
        prev: TelemetryPoint,
        curr: TelemetryPoint,
        gate: GpsGate,
        maxAngleDiffDegrees: Float = 75f
    ): Boolean {
        val carBearing = calculateBearingDegrees(
            prev.latitude.toDouble(), prev.longitude.toDouble(),
            curr.latitude.toDouble(), curr.longitude.toDouble()
        )

        val target = gate.targetBearingDegrees ?: run {
            val gateLineBearing = calculateBearingDegrees(
                gate.lat1, gate.lon1,
                gate.lat2, normalizeLon(gate.lon2, gate.lon1)
            )
            (gateLineBearing + 90f) % 360f
        }

        return angularDifferenceDegrees(carBearing, target) <= maxAngleDiffDegrees
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

        if (!validGps(ay, ax) || !validGps(by, bx) || !validGps(cy, cx) || !validGps(dy, dx)) {
            return false
        }

        // Project X by local cos(latitude) to keep cross products isotropic
        val cosLat = cos(ay * DEG_TO_RAD)
        val axProj = ax * cosLat
        val bxProj = bx * cosLat
        val cxProj = cx * cosLat
        val dxProj = dx * cosLat

        val abc = orientation(axProj, ay, bxProj, by, cxProj, cy)
        val abd = orientation(axProj, ay, bxProj, by, dxProj, dy)
        val cda = orientation(cxProj, cy, dxProj, dy, axProj, ay)
        val cdb = orientation(cxProj, cy, dxProj, dy, bxProj, by)

        val crossedLine = (abc * abd < 0 && cda * cdb < 0) ||
                (abc == 0 && onSegment(axProj, ay, bxProj, by, cxProj, cy)) ||
                (abd == 0 && onSegment(axProj, ay, bxProj, by, dxProj, dy)) ||
                (cda == 0 && onSegment(cxProj, cy, dxProj, dy, axProj, ay)) ||
                (cdb == 0 && onSegment(cxProj, cy, dxProj, dy, bxProj, by))

        if (!crossedLine) return false

        return !enforceDirection || isForwardCrossing(previousPoint, currentPoint, finishGate)
    }

    /**
     * Calculates exact sub-sample interpolated crossing timestamp (in milliseconds)
     * using 2D parametric vector cross-product scalar projection: t = ((q - p) x s) / (r x s)
     * (The formula I learned)
     */
    fun calculateExactCrossingTimeMs(
        prev: TelemetryPoint,
        curr: TelemetryPoint,
        gate: GpsGate
    ): Double {
        val px = normalizeLon(prev.longitude.toDouble(), gate.lon1)
        val py = prev.latitude.toDouble()
        val qx = gate.lon1
        val qy = gate.lat1

        val rx = normalizeLon(curr.longitude.toDouble(), gate.lon1) - px
        val ry = curr.latitude.toDouble() - py

        val sx = normalizeLon(gate.lon2, gate.lon1) - qx
        val sy = gate.lat2 - qy

        val cosLat = cos(py * DEG_TO_RAD)
        val rxProj = rx * cosLat
        val sxProj = sx * cosLat
        val qpxProj = (qx - px) * cosLat
        val qpy = qy - py

        val rCrossS = rxProj * sy - ry * sxProj
        if (abs(rCrossS) < 1e-12) {
            return curr.time.toDouble()
        }

        val qpCrossS = qpxProj * sy - qpy * sxProj
        val t = (qpCrossS / rCrossS).coerceIn(0.0, 1.0)

        val tPrevMs = prev.time.toDouble()
        val tCurrMs = curr.time.toDouble()
        return tPrevMs + t * (tCurrMs - tPrevMs)
    }

    private fun validGps(latitude: Double, longitude: Double): Boolean =
        latitude.isFinite() && longitude.isFinite() &&
                latitude in -90.0..90.0 && longitude in -180.0..180.0

    private fun orientation(
        ax: Double, ay: Double,
        bx: Double, by: Double,
        cx: Double, cy: Double
    ): Int {
        val cross = (bx - ax) * (cy - ay) - (by - ay) * (cx - ax)
        val epsilon = 1e-12 * (abs((bx - ax) * (cy - ay)) + abs((by - ay) * (cx - ax))) + 1e-18
        return when {
            cross > epsilon -> 1
            cross < -epsilon -> -1
            else -> 0
        }
    }

    private fun onSegment(
        ax: Double, ay: Double,
        bx: Double, by: Double,
        px: Double, py: Double
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
        return Lap(
            id = "${sessionId}_lap_$lapNumber",
            sessionId = sessionId,
            lapNumber = lapNumber,
            lapTime = durationMs,
            topSpeedKmh = points.maxOfOrNull { it.speed } ?: 0f,
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
            .takeIf { it in 0..validPoints.size - 10 } ?: (validPoints.size / 10)

        val refPoint = validPoints[refIndex]
        val nextPoint = validPoints[(refIndex + 5).coerceAtMost(validPoints.size - 1)]

        val refLat = refPoint.latitude.toDouble()
        val refLon = normalizeLon(refPoint.longitude.toDouble(), refLat)
        val nextLat = nextPoint.latitude.toDouble()
        val nextLon = normalizeLon(nextPoint.longitude.toDouble(), nextLat)

        val dLat = nextLat - refLat
        val dLon = (nextLon - refLon) * cos(refLat * DEG_TO_RAD)

        val len = sqrt(dLat * dLat + dLon * dLon)
        if (len < 1e-7) return null

        // ~100m gate width in degrees
        val widthDeg = 0.001
        val perpLat = (-dLon / len) * widthDeg
        val perpLon = (dLat / len) * widthDeg / cos(refLat * DEG_TO_RAD)

        return GpsGate(
            lat1 = refLat - perpLat,
            lon1 = refLon - perpLon,
            lat2 = refLat + perpLat,
            lon2 = refLon + perpLon,
            targetBearingDegrees = calculateBearingDegrees(refLat, refLon, nextLat, nextLon)
        )
    }

    fun splitSessionIntoLaps(
        sessionId: String,
        points: List<TelemetryPoint>,
        finishGate: GpsGate? = null,
        minLapDurationMs: Long = 15_000L,
        enforceDirection: Boolean = true,
        discardOutInLaps: Boolean = true
    ): List<Lap> {
        if (points.size < 2) {
            return listOf(buildLap(sessionId, 1, points, 0L))
        }

        val baseGate = finishGate ?: autoGenerateGate(points) ?: return listOf(
            buildLap(sessionId, 1, points, (points.last().time - points.first().time).toLong())
        )

        val gateToUse = if (baseGate.targetBearingDegrees == null) {
            baseGate.copy(targetBearingDegrees = inferTargetBearingDegrees(points, baseGate))
        } else {
            baseGate
        }

        val laps = ArrayList<Lap>()
        val currentLapPoints = ArrayList<TelemetryPoint>()
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
                    lastCrossingTime = calculateExactCrossingTimeMs(prev, curr, gateToUse).roundToLong()
                    currentLapPoints.clear()
                    currentLapPoints.add(curr)
                }
            } else {
                currentLapPoints.add(curr)
                val timeSinceLastCrossing = curr.time.toLong() - lastCrossingTime

                if (isCrossing && timeSinceLastCrossing >= minLapDurationMs) {
                    val exactCrossingMs = calculateExactCrossingTimeMs(prev, curr, gateToUse)
                    val exactDurationMs = if (lastCrossingTime > 0) {
                        (exactCrossingMs - lastCrossingTime).roundToLong()
                    } else {
                        timeSinceLastCrossing
                    }

                    laps.add(
                        buildLap(
                            sessionId = sessionId,
                            lapNumber = lapNumber++,
                            points = ArrayList(currentLapPoints),
                            durationMs = exactDurationMs
                        )
                    )
                    currentLapPoints.clear()
                    currentLapPoints.add(curr)
                    lastCrossingTime = exactCrossingMs.roundToLong()
                    hasCrossedFirstGate = true
                }
            }
        }

        // Handle trailing points / final lap
        if (currentLapPoints.size >= 2) {
            val lapDuration = (currentLapPoints.last().time - currentLapPoints.first().time).toLong()
            if (laps.isEmpty() || !discardOutInLaps) {
                laps.add(buildLap(sessionId, lapNumber, ArrayList(currentLapPoints), lapDuration))
            }
        }

        return laps
    }
}