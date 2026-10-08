package com.example.soleilracingproject.data.local.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val vehicleId: String,
    val track: String,
    val dateEpoch: Long,
    val rawTelemetryPath: String? = null,
    val remoteStorageUrl: String? = null, // For the Firebase-stored location
    val isSynced: Boolean = false,
)
