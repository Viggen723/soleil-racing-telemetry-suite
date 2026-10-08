package com.example.soleilracingproject.model.analysis

import com.example.soleilracingproject.model.telemetry.TelemetryPoint
import kotlin.time.Duration

data class Lap(
    val id: String,
    val sessionId: String,
    val lapNumber: Int,
    val lapTime: Duration,
    val sector1Time: Duration? = null,
    val sector2Time: Duration? = null,
    val sector3Time: Duration? = null,
    val topSpeedKmh: Float,
    val isDnf: Boolean = false, // A bad lap
    val telemetryPoints: List<TelemetryPoint> = emptyList()
    )
