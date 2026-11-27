package com.example.soleilracingproject.features.analysis.data

import androidx.room3.withWriteTransaction
import com.example.soleilracingproject.data.local.AppDatabase
import com.example.soleilracingproject.data.mapper.toDomain
import com.example.soleilracingproject.data.mapper.toEntity
import com.example.soleilracingproject.model.analysis.Lap
import com.example.soleilracingproject.model.analysis.Session
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AnalysisRepository(
    private val database: AppDatabase,
    private val backupPath: (String) -> String? = { null },
) {
    private val sessionDao = database.sessionDao()
    private val lapDao = database.lapDao()
    private val telemetryDao = database.telemetryDao()

    fun getAllSessions(): Flow<List<Session>> {
        return sessionDao.getAllSessionsWithLaps().map { sessionsWithLaps ->
            sessionsWithLaps.map { (sessionEntity, lapWithTelemetryList) ->
                val laps = lapWithTelemetryList.map { it.toDomain() }
                sessionEntity.toDomain(laps)
            }
        }
    }

    fun getLapWithTelemetry(lapId: String): Flow<Lap?> {
        return lapDao.getLapWithTelemetry(lapId).map { lapWithTelemetry ->
            lapWithTelemetry?.toDomain()
        }
    }

    suspend fun deleteSession(session: Session) {
        sessionDao.deleteSessionById(session.id)
    }

    suspend fun updateSession(session: Session) {
        sessionDao.updateSessionDetails(session.id, session.title, session.vehicleId)
    }

    internal fun observePendingBackupIds(prefix: String) = sessionDao.observePendingBackupIds(prefix)

    internal suspend fun claimBackup(id: String, path: String) = sessionDao.claimBackup(id, path)

    internal suspend fun readPendingBackup(id: String, path: String): Session? {
        val stored = sessionDao.readSessionWithLaps(id)
        return stored?.takeIf { !it.session.isSynced && it.session.remoteStorageUrl == path }?.let { (session, laps) ->
            session.toDomain(laps.sortedBy { it.lap.lapNumber }.map { lap ->
                lap.lap.toDomain(lap.telemetryPoints.sortedBy { it.id }.map { it.toDomain() })
            })
        }
    }

    internal suspend fun confirmBackup(id: String, path: String) = sessionDao.confirmBackup(id, path)

    suspend fun addSession(session: Session) = database.withWriteTransaction {
        val previous = sessionDao.getSessionEntity(session.id)
        sessionDao.insertSession(session.toEntity().copy(
            remoteStorageUrl = previous?.remoteStorageUrl ?: backupPath(session.id),
            isSynced = previous?.isSynced ?: false,
        ))

        session.laps.forEach { lap ->
            lapDao.insertLap(lap.toEntity())

            val telemetryEntities = lap.telemetryPoints.map { point ->
                point.toEntity(sessionId = session.id, lapId = lap.id)
            }
            telemetryEntities.chunked(1000).forEach { batch ->
                telemetryDao.insertTelemetryBatch(batch)
            }
        }
    }
}
