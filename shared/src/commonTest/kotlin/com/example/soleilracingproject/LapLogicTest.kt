package com.example.soleilracingproject

import com.example.soleilracingproject.model.telemetry.GpsGate
import com.example.soleilracingproject.model.telemetry.TelemetryPoint
import com.example.soleilracingproject.util.tools.lapLogic.LapLogic
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LapLogicTest {
    private val gate = GpsGate(-0.001, 0.0, 0.001, 0.0)

    private fun point(latitude: Float, longitude: Float, timeMs: Int = 0) = TelemetryPoint(
        time = timeMs, accelX = 0f, accelY = 0f, accelZ = 0f,
        latitude = latitude, longitude = longitude, speed = 100f,
        qw = 1f, qi = 0f, qj = 0f, qk = 0f, altitude = 0f
    )

    @Test fun crossesGateInEitherDirection() {
        val before = point(0f, -0.001f)
        val after = point(0f, 0.001f)
        assertTrue(LapLogic.hasCrossedGpsGate(before, after, gate, enforceDirection = false))
        assertTrue(LapLogic.hasCrossedGpsGate(after, before, gate, enforceDirection = false))
    }

    @Test fun staysOnOneSide() {
        assertFalse(LapLogic.hasCrossedGpsGate(point(0f, -0.002f), point(0f, -0.001f), gate))
    }

    @Test fun missingOrInvalidGps() {
        val valid = point(0f, 0.001f)
        assertFalse(LapLogic.hasCrossedGpsGate(null, valid, gate))
        assertFalse(LapLogic.hasCrossedGpsGate(point(Float.NaN, 0f), valid, gate))
        assertFalse(LapLogic.hasCrossedGpsGate(valid, point(0f, Float.POSITIVE_INFINITY), gate))
        assertFalse(LapLogic.hasCrossedGpsGate(valid, point(91f, 0f), gate))
        assertFalse(LapLogic.hasCrossedGpsGate(valid, valid, null))
    }

    @Test fun limeRockMultiLapSessionSplitting() {
        val limeRockGate = GpsGate(
            lat1 = 41.9260,
            lon1 = -73.3837,
            lat2 = 41.9300,
            lon2 = -73.3837
        )

        val pts = mutableListOf<TelemetryPoint>()
        var t = 0
        for (lap in 0..2) {
            pts.add(point(41.9288f, -73.3850f, t))
            t += 30000
            pts.add(point(41.9288f, -73.3820f, t))
            t += 30000
        }

        val laps = LapLogic.splitSessionIntoLaps("session_1", pts, limeRockGate)
        assertTrue(laps.size >= 2, "Session should split into multiple laps, got ${laps.size}")
    }
}
