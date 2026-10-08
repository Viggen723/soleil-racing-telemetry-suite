package com.example.soleilracingproject.data.local

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import com.example.soleilracingproject.data.local.dao.LapDao
import com.example.soleilracingproject.data.local.dao.SessionDao
import com.example.soleilracingproject.data.local.dao.TelemetryDao
import com.example.soleilracingproject.data.local.entity.LapEntity
import com.example.soleilracingproject.data.local.entity.SessionEntity
import com.example.soleilracingproject.data.local.entity.TelemetryPointEntity

// https://developer.android.com/kotlin/multiplatform/room?hl=ja
// https://medium.com/@hidayatasep43/implementing-room-database-in-kotlin-multiplatform-a-step-by-step-guide-2bc3e1b3aa16
@Database(
    version = 1,
    entities = [
        TelemetryPointEntity::class,
        LapEntity::class,
        SessionEntity::class],
    exportSchema = true
)
@ConstructedBy(AppDatabaseConstructor:: class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun telemetryDao(): TelemetryDao
    abstract fun lapDao(): LapDao
    abstract fun sessionDao(): SessionDao
}

@Suppress("KotlinNoActualForExpect") // When compiles, does not through "no actual implementation" error
expect object AppDatabaseConstructor: RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}