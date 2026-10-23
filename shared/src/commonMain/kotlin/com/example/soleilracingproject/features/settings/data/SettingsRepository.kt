package com.example.soleilracingproject.features.settings.data

import com.example.soleilracingproject.data.local.dao.SettingsDao
import com.example.soleilracingproject.data.mapper.toDomain
import com.example.soleilracingproject.data.mapper.toEntity
import com.example.soleilracingproject.model.settings.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SettingsRepository(private val dao: SettingsDao) {
    val settings: Flow<AppSettings> = dao.observeSettings().map { it?.toDomain() ?: AppSettings() }
    private val writeMutex = Mutex()

    // Read the latest row so separate edits don't overwrite each other.
    private suspend fun update(change: (AppSettings) -> AppSettings) = writeMutex.withLock {
        val current = dao.getSettings()?.toDomain() ?: AppSettings()
        dao.saveSettings(change(current).toEntity())
    }

    suspend fun updateDisplayName(value: String) = update { it.copy(displayName = value.trim()) }
    suspend fun updateSpeedometerMax(value: Float) {
        require(AppSettings.isValidSpeedometerMax(value)) { "Enter a maximum from 10 to 500 mph." }
        update { it.copy(speedometerMax = value) }
    }
    suspend fun updateFocusModeEnabled(value: Boolean) = update { it.copy(focusModeEnabled = value) }
}
