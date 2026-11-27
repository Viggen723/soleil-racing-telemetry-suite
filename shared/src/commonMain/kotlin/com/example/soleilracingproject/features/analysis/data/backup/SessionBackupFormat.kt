package com.example.soleilracingproject.features.analysis.data.backup

import com.example.soleilracingproject.model.analysis.Lap
import com.example.soleilracingproject.model.analysis.Session
import com.example.soleilracingproject.model.telemetry.TelemetryPoint
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

internal object SessionBackupFormat {
    const val VERSION = 1
    const val CHUNK_SIZE = 128
    const val MAX_NODE_BYTES = 131_072

    fun prefix(uid: String) = "users/$uid/sessions/"

    fun path(uid: String, id: String): String {
        val key = id.encodeToByteArray().toHexString()
        require(key.isNotEmpty() && key.length <= 768) { "Session ID cannot be used as a backup key" }
        return prefix(uid) + key
    }

    fun encodeChunk(points: List<TelemetryPoint>): String = Json.encodeToString(points.map { point ->
        // Float bit patterns preserve signed zero, infinities and NaN without JSON number coercion.
        listOf(
            point.time, point.accelX.toRawBits(), point.accelY.toRawBits(), point.accelZ.toRawBits(),
            point.latitude.toRawBits(), point.longitude.toRawBits(), point.speed.toRawBits(),
            point.qw.toRawBits(), point.qi.toRawBits(), point.qj.toRawBits(), point.qk.toRawBits(),
            point.altitude.toRawBits(),
        )
    })

    fun decodeChunk(data: String): List<TelemetryPoint> = Json.decodeFromString<List<List<Int>>>(data).map { row ->
        check(row.size == 12) { "Invalid telemetry row" }
        TelemetryPoint(
            row[0], Float.fromBits(row[1]), Float.fromBits(row[2]), Float.fromBits(row[3]),
            Float.fromBits(row[4]), Float.fromBits(row[5]), Float.fromBits(row[6]),
            Float.fromBits(row[7]), Float.fromBits(row[8]), Float.fromBits(row[9]),
            Float.fromBits(row[10]), Float.fromBits(row[11]),
        )
    }
}

@Serializable
internal data class BackupManifest(
    val version: Int,
    val sessionId: String,
    val title: String,
    val vehicle: String,
    val track: String,
    val startedAt: Long,
    val completedAt: Long,
    val laps: List<BackupLap>,
) {
    fun toSession(restoredLaps: List<Lap>) = Session(
        id = sessionId, title = title, vehicleId = vehicle, track = track,
        dateEpoch = startedAt, laps = restoredLaps,
    )
}

@Serializable
internal data class BackupLap(
    val id: String,
    val number: Int,
    val lapTime: Long,
    val sector1: Long?,
    val sector2: Long?,
    val sector3: Long?,
    val topSpeedMphBits: Int,
    val isDnf: Boolean,
    val sampleCount: Int,
) {
    constructor(lap: Lap) : this(
        lap.id, lap.lapNumber, lap.lapTime, lap.sector1Time, lap.sector2Time, lap.sector3Time,
        lap.topSpeedKmh.toRawBits(), lap.isDnf, lap.telemetryPoints.size,
    )

    fun toLap(sessionId: String, points: List<TelemetryPoint>) = Lap(
        id, sessionId, number, lapTime, sector1, sector2, sector3,
        Float.fromBits(topSpeedMphBits), isDnf, points,
    )
}
