package com.example.soleilracingproject.util.tools.lapLogic

import com.example.soleilracingproject.model.analysis.Lap
import com.example.soleilracingproject.model.telemetry.GpsGate
import com.example.soleilracingproject.model.telemetry.TelemetryPoint
import kotlin.random.Random

object LapLogic {

    fun hasCrossedGpsGate(point: TelemetryPoint, gate: GpsGate? = null): Boolean {
        if (gate == null) return false
        val centerLat = (gate.lat1 + gate.lat2) / 2.0
        val centerLon = (gate.lon1 + gate.lon2) / 2.0
        val deltaLat = point.latitude.toDouble() - centerLat
        val deltaLon = point.longitude.toDouble() - centerLon
        val distDegrees = kotlin.math.sqrt((deltaLat * deltaLat) + (deltaLon * deltaLon))
        val distMeters = distDegrees * 111000.0
        return distMeters <= 15.0
    }

    fun buildLap(
        sessionId: String,
        lapNumber: Int,
        points: List<TelemetryPoint>
    ): Lap {
        val lapDurationMillis: Long = if (points.size > 1) {
            (points.last().time - points.first().time).toLong()
        } else {
            0L
        }
        val maxSpeed = points.maxOfOrNull { it.speed } ?: 0f

        return Lap(
            id = "lap_${Random.nextLong().toString(16)}",
            sessionId = sessionId,
            lapNumber = lapNumber,
            lapTime = lapDurationMillis,
            topSpeedKmh = maxSpeed,
            telemetryPoints = points
        )
    }
}
