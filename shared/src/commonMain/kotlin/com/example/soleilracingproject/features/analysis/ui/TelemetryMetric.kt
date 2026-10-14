package com.example.soleilracingproject.features.analysis.ui

import com.example.soleilracingproject.model.telemetry.TelemetryPoint

enum class TelemetryMetric(val title: String, val extractValue: (TelemetryPoint) -> Float) {
    SPEED("Speed (km/h)", { it.speed }),
    ACCEL_X("X-Accel (G)", { it.accelX }),
    ACCEL_Y("Y-Accel (G)", { it.accelY }),
}