package com.example.soleilracingproject.data.local

import android.content.Context
import androidx.room3.Room.databaseBuilder
import androidx.room3.RoomDatabase

fun getDatabaseBuilder(context: Context): RoomDatabase.Builder<AppDatabase> {
    val appContext = context.applicationContext
    val dbFile = appContext.getDatabasePath("soleil_racing_project.db")
    return databaseBuilder<AppDatabase>(
        context = appContext,
        name = dbFile.absolutePath
    )
}
