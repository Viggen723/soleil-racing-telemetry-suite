package com.example.soleilracingproject.model.analysis

import com.example.soleilracingproject.model.telemetry.GpsGate
import kotlinx.serialization.Serializable

@Serializable
data class TrackLayoutDto(
    val id: String,
    val trackName: String,
    val startFinishGate: GpsGateDto
)

@Serializable
data class GpsGateDto(
    val lat1: Double,
    val lon1: Double,
    val lat2: Double,
    val lon2: Double,
    val targetBearingDegrees: Float? = null
) {
    fun toDomain() = GpsGate(
        lat1 = lat1, lon1 = lon1, lat2 = lat2, lon2 = lon2, targetBearingDegrees = targetBearingDegrees
    )
}

fun TrackLayoutDto.toDomain() = TrackLayout(
    id = id,
    trackName = trackName,
    startFinishGate = startFinishGate.toDomain()
)
