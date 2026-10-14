package com.example.soleilracingproject

import com.example.soleilracingproject.model.telemetry.GpsGate
import com.example.soleilracingproject.model.telemetry.TelemetryPoint
import com.example.soleilracingproject.util.tools.lapLogic.LapLogic
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LapLogicTest {
    private val gate = GpsGate(-0.001, 0.0, 0.001, 0.0)

    private fun point(latitude: Float, longitude: Float) = TelemetryPoint(
        time = 0, accelX = 0f, accelY = 0f, accelZ = 0f,
        latitude = latitude, longitude = longitude, speed = 0f,
        qw = 1f, qi = 0f, qj = 0f, qk = 0f, altitude = 0f
    )

    @Test fun crossesGateInEitherDirection() {
        val before = point(0f, -0.001f)
        val after = point(0f, 0.001f)
        assertTrue(LapLogic.hasCrossedGpsGate(before, after, gate))
        assertTrue(LapLogic.hasCrossedGpsGate(after, before, gate))
    }

    @Test fun staysOnOneSide() {
        assertFalse(LapLogic.hasCrossedGpsGate(point(0f, -0.002f), point(0f, -0.001f), gate))
    }

    @Test fun crossesOnlyTheGateExtension() {
        assertFalse(LapLogic.hasCrossedGpsGate(point(0.00101f, -0.001f), point(0.00101f, 0.001f), gate))
    }

    @Test fun missingOrInvalidGps() {
        val valid = point(0f, 0.001f)
        assertFalse(LapLogic.hasCrossedGpsGate(null, valid, gate))
        assertFalse(LapLogic.hasCrossedGpsGate(point(Float.NaN, 0f), valid, gate))
        assertFalse(LapLogic.hasCrossedGpsGate(valid, point(0f, Float.POSITIVE_INFINITY), gate))
        assertFalse(LapLogic.hasCrossedGpsGate(valid, point(91f, 0f), gate))
        assertFalse(LapLogic.hasCrossedGpsGate(valid, valid, GpsGate(0.0, Double.NaN, 1.0, 0.0)))
        assertFalse(LapLogic.hasCrossedGpsGate(valid, valid, null))
    }

    @Test fun endpointTouchAndCollinearOverlap() {
        val exactGate = GpsGate(-0.001, 0.0, 0.0, 0.0)
        assertTrue(LapLogic.hasCrossedGpsGate(point(0f, -0.001f), point(0f, 0f), exactGate))
        assertTrue(LapLogic.hasCrossedGpsGate(point(-0.0005f, 0f), point(0.0005f, 0f), gate))
        assertFalse(LapLogic.hasCrossedGpsGate(point(0.002f, 0f), point(0.003f, 0f), gate))
    }
}
