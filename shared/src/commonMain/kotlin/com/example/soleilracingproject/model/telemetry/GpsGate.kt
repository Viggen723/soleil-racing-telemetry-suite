package com.example.soleilracingproject.model.telemetry

// GPS gate for making the sectors and finish lines
data class GpsGate(
    val lat1: Double,
    val lon1: Double,
    val lat2: Double,
    val lon2: Double,
    val targetBearingDegrees: Float? = null
)
