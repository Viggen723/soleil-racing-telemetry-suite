package com.example.soleilracingproject

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import com.example.soleilracingproject.data.local.AppDatabase_AutoMigration_4_5_Impl
import com.example.soleilracingproject.data.local.dao.SettingsDao
import com.example.soleilracingproject.data.local.entity.SettingsEntity
import com.example.soleilracingproject.features.settings.data.SettingsRepository
import com.example.soleilracingproject.model.settings.SpeedUnit
import com.example.soleilracingproject.data.mapper.toDomain
import com.example.soleilracingproject.data.mapper.toEntity
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

    @Test fun speedUnitPersistsWithoutChangingCanonicalMaximum() = runBlocking {
        val dao = MemorySettingsDao()
        val repository = SettingsRepository(dao)
        assertEquals(SpeedUnit.MPH, repository.settings.first().speedUnit)
        assertEquals(SpeedUnit.MPH, SettingsEntity().toDomain().speedUnit)
        repository.updateSpeedUnit(SpeedUnit.KPH)
        assertEquals("KPH", dao.row.value?.speedUnit)
        assertEquals(SpeedUnit.KPH, SettingsRepository(dao).settings.first().speedUnit)
        assertEquals(180f, repository.settings.first().speedometerMax)
        val settings = AppSettings(speedUnit = SpeedUnit.KPH)
        assertEquals(settings, settings.toEntity().toDomain())
        repository.updateDisplayName("Driver")
        assertEquals(SpeedUnit.KPH, repository.settings.first().speedUnit)
        repository.updateSpeedUnit(SpeedUnit.MPH)
        assertEquals(180f, repository.settings.first().speedometerMax)
    }

    @Test fun migrationDefaultsExistingRowToMph() = runBlocking {
        BundledSQLiteDriver().open(":memory:").use { connection ->
            connection.execSQL("CREATE TABLE app_settings (id TEXT NOT NULL PRIMARY KEY, displayName TEXT NOT NULL, speedometerMax REAL NOT NULL, focusModeEnabled INTEGER NOT NULL)")
            connection.execSQL("INSERT INTO app_settings VALUES ('app_settings', 'Driver', 180, 1)")
            AppDatabase_AutoMigration_4_5_Impl().migrate(connection)
            connection.prepare("SELECT displayName, speedometerMax, focusModeEnabled, speedUnit FROM app_settings").use { row ->
                assertTrue(row.step())
                assertEquals("Driver", row.getText(0))
                assertEquals(180.0, row.getDouble(1))
                assertEquals(1L, row.getLong(2))
                assertEquals("MPH", row.getText(3))
            }
        }
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
