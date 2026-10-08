package com.example.soleilracingproject.model.analysis

import com.example.soleilracingproject.model.telemetry.GpsGate

data class TrackLayout(
    val id: String,
    val trackName: String,
    val startFinishGate: GpsGate,
    val sector1Gate: GpsGate,
    val sector2Gate: GpsGate // Sector 3 is between gate 2 and finish line

    // In the future, let's make a list so we can have user-defined sector amount (Not just 3)
)
