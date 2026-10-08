package com.example.soleilracingproject.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Update
import com.example.soleilracingproject.data.local.entity.LapEntity
import com.example.soleilracingproject.data.local.relation.LapWithTelemetry
import kotlinx.coroutines.flow.Flow

@Dao
interface LapDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLap(lap: LapEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLaps(laps: List<LapEntity>)

    @Update
    suspend fun update(lap: LapEntity)

    @Transaction
    @Query("SELECT * FROM laps WHERE id = :lapId")
    fun getLapWithTelemetry(lapId: String): Flow<LapWithTelemetry?> // For getting single lap telemetry for the analysis page

    @Query("SELECT * FROM laps where sessionId = :sessionId ORDER BY lapNumber ASC")
    fun getLapsForSession(sessionId: String): Flow<List<LapEntity>>

    @Query("DELETE FROM laps WHERE id = :lapId")
    suspend fun deleteLapById(lapId: String)
}