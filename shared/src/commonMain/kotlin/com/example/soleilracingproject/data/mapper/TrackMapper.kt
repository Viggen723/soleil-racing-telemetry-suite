package com.example.soleilracingproject.data.mapper

import com.example.soleilracingproject.data.local.entity.TrackEntity
import com.example.soleilracingproject.model.analysis.TrackLayout
import com.example.soleilracingproject.model.telemetry.GpsGate

fun TrackEntity.toDomain(): TrackLayout {
    return TrackLayout(
        id = id,
        trackName = name,
        startFinishGate = GpsGate(
            lat1 = sfLat1,
            lon1 = sfLon1,
            lat2 = sfLat2,
            lon2 = sfLon2,
            targetBearingDegrees = targetBearingDegrees
        )
    )
}

fun TrackLayout.toEntity(isCustom: Boolean): TrackEntity {
    return TrackEntity(
        id = id,
        name = trackName,
        sfLat1 = startFinishGate.lat1,
        sfLon1 = startFinishGate.lon1,
        sfLat2 = startFinishGate.lat2,
        sfLon2 = startFinishGate.lon2,
        targetBearingDegrees = startFinishGate.targetBearingDegrees,
        isCustom = isCustom
    )
}
