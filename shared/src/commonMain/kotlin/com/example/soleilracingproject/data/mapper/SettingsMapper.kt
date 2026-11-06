package com.example.soleilracingproject.data.mapper

import com.example.soleilracingproject.model.settings.SpeedUnit
import com.example.soleilracingproject.data.local.entity.SettingsEntity
import com.example.soleilracingproject.model.settings.AppSettings

fun SettingsEntity.toDomain() = AppSettings(displayName, speedometerMax, focusModeEnabled, SpeedUnit.valueOf(speedUnit))
fun AppSettings.toEntity() = SettingsEntity(
    displayName = displayName,
    speedometerMax = speedometerMax,
    focusModeEnabled = focusModeEnabled,
    speedUnit = speedUnit.name,
)
