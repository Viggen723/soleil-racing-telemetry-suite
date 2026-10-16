package com.example.soleilracingproject.util.tools.lapLogic

import com.example.soleilracingproject.model.analysis.Lap
import com.example.soleilracingproject.model.telemetry.TelemetryPoint
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DeltaCalculatorTest {

    // A car driving north from the equator. 0.0001 degrees of latitude is about 11 meters.
    private fun point(timeMs: Int, latitude: Float) = TelemetryPoint(
        time = timeMs, accelX = 0f, accelY = 0f, accelZ = 0f,
        latitude = latitude, longitude = 0f, speed = 0f,
        qw = 1f, qi = 0f, qj = 0f, qk = 0f, altitude = 0f
    )

    // 11 readings, one every 100 ms, each one "step" degrees further north
    private fun drive(step: Float) = (0..10).map { point(it * 100, it * step) }

    private val referenceLap = Lap(
        id = "ref", sessionId = "s", lapNumber = 1, lapTime = 1000L,
        topSpeedKmh = 0f, telemetryPoints = drive(step = 0.0001f)
    )

    @Test
    fun slowerThanReferenceGivesPositiveDelta() {
        val slowCar = drive(step = 0.00005f)      // half as fast: after 1 s it is where the reference was at 0.5 s
        val delta = DeltaCalculator.deltaSeconds(referenceLap, slowCar)
        assertTrue(delta != null && abs(delta - 0.5f) < 0.05f, "delta was $delta")
    }

    @Test
    fun sameSpeedGivesZeroDelta() {
        val delta = DeltaCalculator.deltaSeconds(referenceLap, drive(step = 0.0001f))
        assertTrue(delta != null && abs(delta) < 0.05f, "delta was $delta")
    }

    @Test
    fun noDeltaWithoutEnoughPoints() {
        assertNull(DeltaCalculator.deltaSeconds(referenceLap, listOf(point(0, 0f))))
    }
}