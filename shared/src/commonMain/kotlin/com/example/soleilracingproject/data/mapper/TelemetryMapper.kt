package com.example.soleilracingproject.data.mapper

import com.example.soleilracingproject.data.local.entity.TelemetryPointEntity
import com.example.soleilracingproject.model.telemetry.TelemetryPoint

fun TelemetryPointEntity.toDomain(): TelemetryPoint
{
    return TelemetryPoint(
        time = this.timeStamp,
        accelX = this.accelX,
        accelY = this.accelY,
        accelZ = this.accelZ,
        latitude = this.latitude,
        longitude = this.longitude,
        speed = this.speed,
        qw = this.qw,
        qi = this.qi,
        qj = this.qj,
        qk = this.qk,
        altitude = this.altitude
    )
}

fun TelemetryPoint.toEntity(sessionId: String, lapId: String? = null): TelemetryPointEntity
{
    return TelemetryPointEntity(
        sessionId = sessionId, // Needs to pass this through the function as the normal point does not come with a sessionId String,
        lapId = lapId,
        timeStamp = this.time,
        accelX = this.accelX,
        accelY = this.accelY,
        accelZ = this.accelZ,
        latitude = this.latitude,
        longitude = this.longitude,
        speed = this.speed,
        qw = this.qw,
        qi = this.qi,
        qj = this.qj,
        qk = this.qk,
        altitude = this.altitude
    )
}