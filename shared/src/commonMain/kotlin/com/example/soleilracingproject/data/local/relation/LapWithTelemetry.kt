package com.example.soleilracingproject.data.local.relation

import androidx.room3.Embedded
import androidx.room3.Relation
import com.example.soleilracingproject.data.local.entity.LapEntity
import com.example.soleilracingproject.data.local.entity.TelemetryPointEntity

data class LapWithTelemetry(
    @Embedded val lap: LapEntity,

    @Relation(
        parentColumns = ["id"],
        entityColumns = ["lapId"]
    )
    val telemetryPoints: List<TelemetryPointEntity>
)
