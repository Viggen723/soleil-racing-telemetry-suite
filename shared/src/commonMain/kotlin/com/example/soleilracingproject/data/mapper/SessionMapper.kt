package com.example.soleilracingproject.data.mapper

import com.example.soleilracingproject.data.local.entity.SessionEntity
import com.example.soleilracingproject.data.local.relation.SessionWithLaps
import com.example.soleilracingproject.model.analysis.Lap
import com.example.soleilracingproject.model.analysis.Session

fun SessionEntity.toDomain(laps: List<Lap> = emptyList()): Session
{
    return Session(
        id = this.id,
        title = this.title,
        vehicleId = this.vehicleId,
        track = this.track,
        dateEpoch = this.dateEpoch,
        rawTelemetryPath = this.rawTelemetryPath,
        remoteStorageUrl = this.remoteStorageUrl,
        isSynced = this.isSynced,
        laps = laps
    )
}

fun Session.toEntity(): SessionEntity
{
    return SessionEntity(
        id = this.id,
        title = this.title,
        vehicleId = this.vehicleId,
        track = this.track,
        dateEpoch = this.dateEpoch,
        rawTelemetryPath = this.rawTelemetryPath,
        remoteStorageUrl = this.remoteStorageUrl,
        isSynced = this.isSynced,
    )
}

fun SessionWithLaps.toDomain() : List<Lap>
{
    return laps.map {it.toDomain()}
}

