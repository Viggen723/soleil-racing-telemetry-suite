package com.example.soleilracingproject.data.local.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "telemetry_points",
    foreignKeys = [
        ForeignKey(
            entity = LapEntity::class,
            parentColumns = ["id"],
            childColumns = ["lapId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("lapId"),
        Index(value = ["lapId", "timeStamp"])
    ]
)

data class TelemetryPointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val lapId: String? = null,
    val timeStamp: Int,
    val accelX: Float,
    val accelY: Float,
    val accelZ: Float,
    val latitude: Float,
    val longitude: Float,
    val speed: Float,
    val qw: Float,
    val qi: Float,
    val qj: Float,
    val qk: Float,
    val altitude: Float
)
