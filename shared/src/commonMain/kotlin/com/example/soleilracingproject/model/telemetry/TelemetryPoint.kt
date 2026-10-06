package com.example.soleilracingproject.model.telemetry

data class TelemetryPoint(
    val time: Int,
    val accelX: Float,
    val accelY: Float,
    val accelZ: Float,
    val latitude: Float,
    val longitude: Float,
    val speed: Float,
    val qw: Float,
    val qi: Float,
    val qj: Float,
    val qk: Float,
    val altitude: Float
)
