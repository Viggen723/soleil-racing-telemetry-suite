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

    @Query("UPDATE sessions SET title = :title, vehicleId = :vehicleId WHERE id = :id")
    suspend fun updateSessionDetails(id: String, title: String, vehicleId: String)

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun getSessionEntity(id: String): SessionEntity?

    @Query("""
        SELECT id FROM sessions WHERE isSynced = 0
        AND (remoteStorageUrl IS NULL OR substr(remoteStorageUrl, 1, length(:prefix)) = :prefix)
        ORDER BY dateEpoch ASC, id ASC
    """)
    fun observePendingBackupIds(prefix: String): Flow<List<String>>

    @Query("UPDATE sessions SET remoteStorageUrl = :path WHERE id = :id AND remoteStorageUrl IS NULL AND isSynced = 0")
    suspend fun claimBackup(id: String, path: String)

    @Query("UPDATE sessions SET isSynced = 1 WHERE id = :id AND remoteStorageUrl = :path")
    suspend fun confirmBackup(id: String, path: String)

    // Gets a single session where it is loaded on the screen with the laps that were saved with it
    @Transaction
    @Query("SELECT * FROM sessions WHERE id = :sessionId")
    fun getSessionWithLaps(sessionId: String): Flow<SessionWithLaps?>

    @Transaction
    @Query("SELECT * FROM sessions WHERE id = :sessionId")
    suspend fun readSessionWithLaps(sessionId: String): SessionWithLaps?

    // To get all the session that come as a list decreasing by order that they were recorded
    @Transaction
    @Query("SELECT * FROM sessions ORDER BY dateEpoch DESC")
    fun getAllSessionsWithLaps(): Flow<List<SessionWithLaps>>

    @Query("SELECT * FROM sessions WHERE id = :sessionId")
    fun getSession(sessionId: String): Flow<SessionEntity>

    @Query("DELETE FROM sessions WHERE id = :sessionId")
    suspend fun deleteSessionById(sessionId: String)

}
