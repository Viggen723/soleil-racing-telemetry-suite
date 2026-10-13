package com.example.soleilracingproject.data.mapper

import com.example.soleilracingproject.data.local.entity.LapEntity
import com.example.soleilracingproject.data.local.relation.LapWithTelemetry
import com.example.soleilracingproject.model.analysis.Lap
import com.example.soleilracingproject.model.telemetry.TelemetryPoint
import kotlin.time.Duration.Companion.milliseconds

fun LapEntity.toDomain(telemetryPoints: List<TelemetryPoint> = emptyList()): Lap
{
    return Lap(
        id = this.id,
        sessionId = this.sessionId,
        lapNumber = this.lapNumber,
        lapTime = this.lapTimeMs,          // Matches the Long expected by Lap
        sector1Time = this.sector1TimeMs,  // Also a Long? (or Long?)
        sector2Time = this.sector2TimeMs,
        sector3Time = this.sector3TimeMs,
        topSpeedKmh = this.topSpeedKmh,
        isDnf = this.isDnf,
        telemetryPoints = telemetryPoints
    )
}

// This mapper allows for the separate data points to be removed from lap for further analysis
fun LapWithTelemetry.toDomain() : Lap {
    return lap.toDomain(
        telemetryPoints = telemetryPoints.map { it.toDomain() } // This line is sick
    )
}

fun Lap.toEntity(): LapEntity {
    return LapEntity(
        id = id,
        sessionId = sessionId,
        lapNumber = lapNumber,
        lapTimeMs = lapTime,
        sector1TimeMs = sector1Time,
        sector2TimeMs = sector2Time,
        sector3TimeMs = sector3Time,
        topSpeedKmh = topSpeedKmh,
        isDnf = isDnf
    )
}