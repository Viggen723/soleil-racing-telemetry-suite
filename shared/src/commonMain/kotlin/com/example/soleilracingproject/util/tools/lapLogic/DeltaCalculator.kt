package com.example.soleilracingproject.util.tools.lapLogic

import com.example.soleilracingproject.model.analysis.Lap
import com.example.soleilracingproject.model.telemetry.TelemetryPoint

// The deltas the HUD can show, in seconds.
// Negative = ahead of the reference lap (faster), positive = behind. null = nothing to compare yet.
data class LapDeltas(
    val vsBestSeconds: Float? = null,
    val vsLastSeconds: Float? = null
)

object DeltaCalculator {

    // How far (in meters) the car had driven at each point of a lap
    private fun distancesAlong(points: List<TelemetryPoint>): List<Double> {
        val distances = mutableListOf(0.0)
        for (i in 1 until points.size) {

            val step = LapLogic.haversineDistanceMeters(
                points[i - 1].latitude.toDouble(), points[i - 1].longitude.toDouble(),
                points[i].latitude.toDouble(), points[i].longitude.toDouble()
            )
            // a jump of 100 m between two readings can't be real (bad GPS fix), so we skip it
            val safeStep = if (step < 100.0) step else 0.0
            distances.add(distances.last() + safeStep)
        }
        return distances
    }

    // How many seconds ahead (-) or behind (+) the current lap is, compared to referenceLap.
    // currentPoints = the points of the lap we are driving right now.
    fun deltaSeconds(referenceLap: Lap, currentPoints: List<TelemetryPoint>): Float? {
        val refPoints = referenceLap.telemetryPoints
        if (refPoints.size < 2 || currentPoints.size < 2) return null

        val refDistances = distancesAlong(refPoints)
        val distanceNow = distancesAlong(currentPoints).last()    // how far we have driven

        // Default: if we are past the end of the reference lap, use the reference lap's full time
        var refElapsedMs = (refPoints.last().time - refPoints.first().time).toDouble()

        // Find the first reference point that is at least as far as we are now
        for (i in 1 until refPoints.size) {
            if (refDistances[i] >= distanceNow) {
                val before = refDistances[i - 1]
                val after = refDistances[i]
                // how far between the two points are we? 0 = at the first, 1 = at the second
                val share = if (after > before) (distanceNow - before) / (after - before) else 0.0
                val timeBefore = refPoints[i - 1].time - refPoints.first().time
                val timeAfter = refPoints[i].time - refPoints.first().time
                refElapsedMs = timeBefore + share * (timeAfter - timeBefore)
                break
            }
        }

        val currentElapsedMs = (currentPoints.last().time - currentPoints.first().time).toDouble()
        return ((currentElapsedMs - refElapsedMs) / 1000.0).toFloat()
    }
}