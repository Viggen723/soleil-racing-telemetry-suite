package com.example.soleilracingproject.model.analysis

import com.example.soleilracingproject.model.telemetry.GpsGate
import com.example.soleilracingproject.model.telemetry.TelemetryPoint
import kotlin.math.abs

object KnownTracks {

    val LIME_ROCK_PARK = TrackLayout(
        id = "limerock_park",
        trackName = "Lime Rock Park",
        startFinishGate = GpsGate(
            lat1 = 41.9288,
            lon1 = -73.3842,
            lat2 = 41.9288,
            lon2 = -73.3832,
        ),
        sector1Gate = GpsGate(
            lat1 = 41.9269,
            lon1 = -73.3852,
            lat2 = 41.9267,
            lon2 = -73.3848,
        ),
        sector2Gate = GpsGate(
            lat1 = 41.9296,
            lon1 = -73.3812,
            lat2 = 41.9294,
            lon2 = -73.3808,
        ),
    )

    val ALL_PRESETS = listOf(LIME_ROCK_PARK)

    fun findMatchingTrack(points: List<TelemetryPoint>): TrackLayout? {
        val validPoints = points.filter { (it.latitude != 0f) && (it.longitude != 0f) }
        if (validPoints.isEmpty()) return null

        return ALL_PRESETS.firstOrNull { track ->
            val sf = track.startFinishGate
            val trackLat = (sf.lat1 + sf.lat2) / 2.0
            val trackLon = (sf.lon1 + sf.lon2) / 2.0

            validPoints.any { pt ->
                val dLat = abs(pt.latitude.toDouble() - trackLat)
                val dLon = abs(abs(pt.longitude.toDouble()) - abs(trackLon))
                (dLat < 0.02) && (dLon < 0.02) // Within 2km of start/finish line
            }
        }
    }
}
