package com.example.soleilracingproject.data.local.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "app_settings")
data class SettingsEntity(
    @PrimaryKey val id: String = "app_settings",
    val displayName: String = "",
    val speedometerMax: Float = 180f,
    val focusModeEnabled: Boolean = true,
)
