package com.example.soleilracingproject

import com.example.soleilracingproject.data.local.dao.SettingsDao
import com.example.soleilracingproject.data.local.entity.SettingsEntity
import com.example.soleilracingproject.features.settings.data.SettingsRepository
import com.example.soleilracingproject.model.settings.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.*

class SettingsRepositoryTest {
    private class MemorySettingsDao : SettingsDao {
        val row = MutableStateFlow<SettingsEntity?>(null)
        override fun observeSettings() = row
        override suspend fun getSettings() = row.value
        override suspend fun saveSettings(settings: SettingsEntity) { row.value = settings }
    }

    @Test fun defaultsAndIndependentUpdatesSurviveRepositoryRecreation() = runBlocking {
        val dao = MemorySettingsDao()
        val repository = SettingsRepository(dao)
        assertEquals(AppSettings(), repository.settings.first())
        repository.updateSpeedometerMax(300f)
        repository.updateFocusModeEnabled(false)
        repository.updateDisplayName("  Ayra  ")
        assertEquals(AppSettings("Ayra", 300f, false), SettingsRepository(dao).settings.first())
        assertEquals("app_settings", dao.row.value?.id)
        repository.updateDisplayName("")
        assertEquals(AppSettings("", 300f, false), repository.settings.first())
    }

    @Test fun invalidMaximumNeverReplacesSavedValue() = runBlocking {
        val dao = MemorySettingsDao()
        val repository = SettingsRepository(dao)
        repository.updateSpeedometerMax(220f)
        for (value in listOf(0f, -1f, 9f, 501f, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)) {
            assertFailsWith<IllegalArgumentException> { repository.updateSpeedometerMax(value) }
            assertEquals(220f, repository.settings.first().speedometerMax)
        }
        for (value in listOf(10f, 500f)) {
            repository.updateSpeedometerMax(value)
            assertEquals(value, repository.settings.first().speedometerMax)
        }
    }
}
