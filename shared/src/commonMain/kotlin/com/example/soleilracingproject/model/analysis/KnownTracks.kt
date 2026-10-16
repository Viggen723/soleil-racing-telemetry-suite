package com.example.soleilracingproject.model.analysis

import com.example.soleilracingproject.model.telemetry.GpsGate
import com.example.soleilracingproject.model.telemetry.TelemetryPoint
import kotlin.math.abs

object KnownTracks {

    val LIME_ROCK_PARK = TrackLayout(
        id = "limerock_park",
        trackName = "Lime Rock Park",
        // Main pit straight runs East-West at ~41.9288 N, ~-73.3837 W.
        // Start/Finish gate must run PERPENDICULAR (North-South across the track).
        startFinishGate = GpsGate(
            lat1 = 41.9282,
            lon1 = -73.3837,
            lat2 = 41.9294,
            lon2 = -73.3837,
        ),
        sector1Gate = GpsGate(
            lat1 = 41.9262,
            lon1 = -73.3850,
            lat2 = 41.9274,
            lon2 = -73.3850,
        ),
        sector2Gate = GpsGate(
            lat1 = 41.9290,
            lon1 = -73.3810,
            lat2 = 41.9302,
            lon2 = -73.3810,
        ),
    )

    val ALL_PRESETS = listOf(LIME_ROCK_PARK)

    fun findMatchingTrack(points: List<TelemetryPoint>): TrackLayout? {
        val validPoints = points.filter { (it.latitude != 0f) || (it.longitude != 0f) }
        if (validPoints.isEmpty()) return null

        return ALL_PRESETS.firstOrNull { track ->
            val sf = track.startFinishGate
            val trackLat = (sf.lat1 + sf.lat2) / 2.0
            val trackLon = (sf.lon1 + sf.lon2) / 2.0

            validPoints.any { pt ->
                val ptLat = pt.latitude.toDouble()
                val rawLon = pt.longitude.toDouble()
                val ptLon = if (ptLat in 10.0..75.0 && rawLon > 0.0) -rawLon else rawLon

                val dLat = abs(ptLat - trackLat)
                val dLon = abs(ptLon - trackLon)
                (dLat < 0.03) && (dLon < 0.03) // Within ~3km of start/finish line
            }
        }
    }
}
