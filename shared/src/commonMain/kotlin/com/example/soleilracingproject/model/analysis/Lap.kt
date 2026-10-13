package com.example.soleilracingproject.model.analysis

import com.example.soleilracingproject.model.telemetry.TelemetryPoint

data class Lap(
    val id: String,
    val sessionId: String,
    val lapNumber: Int,
    val lapTime: Long,
    val sector1Time: Long? = null,
    val sector2Time: Long? = null,
    val sector3Time: Long? = null,
    val topSpeedKmh: Float,
    val isDnf: Boolean = false, // A bad lap
    val telemetryPoints: List<TelemetryPoint> = emptyList()
    )
