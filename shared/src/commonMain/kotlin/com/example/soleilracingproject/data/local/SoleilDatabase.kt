package com.example.soleilracingproject.data.local

import androidx.room3.ConstructedBy
import com.example.soleilracingproject.data.local.dao.SettingsDao
import com.example.soleilracingproject.data.local.entity.SettingsEntity
import androidx.room3.AutoMigration
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import com.example.soleilracingproject.data.local.dao.LapDao
import com.example.soleilracingproject.data.local.dao.CarDao
import com.example.soleilracingproject.data.local.dao.SessionDao
import com.example.soleilracingproject.data.local.dao.TelemetryDao
import com.example.soleilracingproject.data.local.entity.LapEntity
import com.example.soleilracingproject.data.local.entity.CarEntity
import com.example.soleilracingproject.data.local.entity.SessionEntity
import com.example.soleilracingproject.data.local.entity.TelemetryPointEntity
import com.example.soleilracingproject.data.local.dao.WidgetLayoutDao
import com.example.soleilracingproject.data.local.entity.WidgetLayoutEntity

// https://developer.android.com/kotlin/multiplatform/room?hl=ja
// https://medium.com/@hidayatasep43/implementing-room-database-in-kotlin-multiplatform-a-step-by-step-guide-2bc3e1b3aa16
@Database(
    version = 4,
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3), AutoMigration(from = 3, to = 4)],
    entities = [
        TelemetryPointEntity::class,
        LapEntity::class,
        SessionEntity::class,
        CarEntity::class,
        WidgetLayoutEntity::class,
        SettingsEntity::class],
exportSchema = true
)
@ConstructedBy(AppDatabaseConstructor:: class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun settingsDao(): SettingsDao

    abstract fun telemetryDao(): TelemetryDao
    abstract fun lapDao(): LapDao
    abstract fun sessionDao(): SessionDao
    abstract fun carDao(): CarDao
    abstract fun widgetLayoutDao(): WidgetLayoutDao
}

@Suppress("KotlinNoActualForExpect") // When compiles, does not through "no actual implementation" error
expect object AppDatabaseConstructor: RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}
