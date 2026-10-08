package com.example.soleilracingproject.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Update
import com.example.soleilracingproject.data.local.entity.SessionEntity
import com.example.soleilracingproject.data.local.relation.SessionWithLaps
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity)

    @Update
    suspend fun updateSession(session: SessionEntity)

    // Gets a single session where it is loaded on the the screen with the laps that were saved with it
    @Transaction
    @Query("SELECT * FROM sessions WHERE id = :sessionId")
    fun getSessionWithLaps(sessionId: String): Flow<SessionWithLaps?>

    // To get all the session that come as a list decreasing by order that they were recorded
    @Transaction
    @Query("SELECT * FROM sessions ORDER BY dateEpoch DESC")
    fun getAllSessionsWithLaps(): Flow<List<SessionWithLaps>>

    @Query("SELECT * FROM sessions WHERE id = :sessionId")
    fun getSession(sessionId: String): Flow<SessionEntity>

    @Query("DELETE FROM sessions WHERE id = :sessionId")
    suspend fun deleteSessionById(sessionId: String)

}