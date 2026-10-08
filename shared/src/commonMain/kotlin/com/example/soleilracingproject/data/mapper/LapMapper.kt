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
        lapTime = this.lapTimeMs.milliseconds,
        sector1Time = this.sector1TimeMs?.milliseconds,
        sector2Time = this.sector2TimeMs?.milliseconds,
        sector3Time = this.sector3TimeMs?.milliseconds,
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
        lapTimeMs = lapTime.inWholeMilliseconds,
        sector1TimeMs = sector1Time?.inWholeMilliseconds,
        sector2TimeMs = sector2Time?.inWholeMilliseconds,
        sector3TimeMs = sector3Time?.inWholeMilliseconds,
        topSpeedKmh = topSpeedKmh,
        isDnf = isDnf
    )
}