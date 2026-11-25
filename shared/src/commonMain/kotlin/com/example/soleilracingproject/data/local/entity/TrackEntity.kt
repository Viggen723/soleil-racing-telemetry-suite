package com.example.soleilracingproject.data.local.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey val id: String,
    val name: String,
    val sfLat1: Double,
    val sfLon1: Double,
    val sfLat2: Double,
    val sfLon2: Double,
    val targetBearingDegrees: Float? = null,
    val isCustom: Boolean = true // To differentiate user-added vs seeded tracks
)
