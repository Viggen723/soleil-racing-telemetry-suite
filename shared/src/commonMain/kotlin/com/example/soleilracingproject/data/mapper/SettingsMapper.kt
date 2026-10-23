package com.example.soleilracingproject.data.mapper

import com.example.soleilracingproject.data.local.entity.SettingsEntity
import com.example.soleilracingproject.model.settings.AppSettings

fun SettingsEntity.toDomain() = AppSettings(displayName, speedometerMax, focusModeEnabled)
fun AppSettings.toEntity() = SettingsEntity(
    displayName = displayName,
    speedometerMax = speedometerMax,
    focusModeEnabled = focusModeEnabled,
)
