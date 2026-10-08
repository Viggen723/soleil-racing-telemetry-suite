package com.example.soleilracingproject.data.local

import androidx.room3.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

// This is the common one that has to be implemented in both of the Android and iOS packages
fun getRoomDatabase(
    builder: RoomDatabase.Builder<AppDatabase>
): AppDatabase {
    return builder
        .setDriver(BundledSQLiteDriver()) // Uses  this for across the platforms
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}