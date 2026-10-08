package com.example.soleilracingproject.data.local.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import kotlin.time.Duration

@Entity(
    tableName = "laps",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("sessionId"),
        Index(value = ["sessionId", "lapNumber"], unique = true) // Make sure the lap numbers are unique
    ]
)
data class LapEntity(
    @PrimaryKey val id: String,
    val sessionId: String,               // Foreign Key pointing to SessionEntity.id
    val lapNumber: Int,
    val lapTimeMs: Long,                 // Stored as Long milliseconds for SQLite compatibility
    val sector1TimeMs: Long? = null,
    val sector2TimeMs: Long? = null,
    val sector3TimeMs: Long? = null,
    val topSpeedKmh: Float,
    val isDnf: Boolean = false
)