package com.example.soleilracingproject.model.analysis

import com.example.soleilracingproject.model.telemetry.GpsGate
import com.example.soleilracingproject.model.telemetry.TelemetryPoint
import com.example.soleilracingproject.util.tools.lapLogic.LapLogic
import kotlin.math.abs

object KnownTracks {

    val LIME_ROCK_PARK = TrackLayout(
        id = "limerock_park",
        trackName = "Lime Rock Park",
        // Sam Posey Main Straight (~-73.3837 W)
        startFinishGate = GpsGate(
            lat1 = 41.9260,
            lon1 = -73.3837,
            lat2 = 41.9300,
            lon2 = -73.3837,
        ),
        sector1Gate = GpsGate(
            lat1 = 41.9260,
            lon1 = -73.3850,
            lat2 = 41.9280,
            lon2 = -73.3850,
        ),
        sector2Gate = GpsGate(
            lat1 = 41.9280,
            lon1 = -73.3810,
            lat2 = 41.9310,
            lon2 = -73.3810,
        ),
    )

    val MAISHIMA_AUTOCROSS = TrackLayout(
        id = "maishima_autocross",
        trackName = "Maishima Autocross",
        startFinishGate = GpsGate(
            lat1 = 34.66833,
            lon1 = 135.39278,
            lat2 = 34.66825,
            lon2 = 135.39283,
        )
    )

    val ALL_PRESETS = listOf(LIME_ROCK_PARK, MAISHIMA_AUTOCROSS)

    fun findMatchingTrack(points: List<TelemetryPoint>): TrackLayout? {
        val validPoints = points.filter { (it.latitude != 0f) || (it.longitude != 0f) }
        if (validPoints.isEmpty()) return null

        return ALL_PRESETS.firstOrNull { track ->
            val sf = track.startFinishGate
            val trackLat = (sf.lat1 + sf.lat2) / 2.0
            val trackLon = (sf.lon1 + sf.lon2) / 2.0

            validPoints.any { pt ->
                val ptLat = pt.latitude.toDouble()
                val ptLon = LapLogic.normalizeLon(pt.longitude.toDouble(), trackLon)

                val dLat = abs(ptLat - trackLat)
                val dLon = abs(ptLon - trackLon)
                (dLat < 0.03) && (dLon < 0.03) // Within ~3km of start/finish line
            }
        }
    }
}
