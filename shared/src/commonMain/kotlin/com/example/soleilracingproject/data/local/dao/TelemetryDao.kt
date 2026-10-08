package com.example.soleilracingproject.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.example.soleilracingproject.data.local.entity.TelemetryPointEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TelemetryDao {

    // Insert a single point (Probably will not be used too often for now
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTelemetryPoint(telemetryPoint: TelemetryPointEntity)

    // Insert a buffer of points (Will be gathering while the stream is steady from receiver)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTelemetryBatch(telemetryPoints: List<TelemetryPointEntity>)

    // Get points that are emitted using flow for analysis screen like the GPS lines
    @Query("SELECT * FROM telemetry_points WHERE lapId = :lapId ORDER BY timeStamp ASC")
    fun getTelemetryForLap(lapId: String): Flow<List<TelemetryPointEntity>>

    // Get all the points at once for a lap. Could be used for any algorithms
    // that operate on the map before displaying
    @Query("SELECT * FROM telemetry_points WHERE lapId = :lapId ORDER BY timeStamp ASC")
    suspend fun getAllTelemetryForLap(lapId: String): List<TelemetryPointEntity>

    // All the points that are in a session emitted by FlowList
    @Query("SELECT * FROM telemetry_points WHERE sessionId = :sessionId ORDER BY timeStamp ASC")
    fun getTelemetryForSession(sessionId: String): Flow<List<TelemetryPointEntity>>

    @Query("DELETE FROM telemetry_points WHERE lapId = :lapId")
    suspend fun deleteTelemetryForLap(lapId: String)

    // I think that is all we need for delete for now; just deleting points from lap
}