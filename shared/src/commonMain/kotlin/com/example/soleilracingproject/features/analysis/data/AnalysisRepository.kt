package com.example.soleilracingproject.features.analysis.data

import com.example.soleilracingproject.data.local.dao.LapDao
import com.example.soleilracingproject.data.local.dao.SessionDao
import com.example.soleilracingproject.data.local.dao.TelemetryDao
import com.example.soleilracingproject.data.mapper.toDomain
import com.example.soleilracingproject.data.mapper.toEntity
import com.example.soleilracingproject.model.analysis.Lap
import com.example.soleilracingproject.model.analysis.Session
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AnalysisRepository(
    private val sessionDao: SessionDao,
    private val lapDao: LapDao,
    private val telemetryDao: TelemetryDao,
) {
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

    suspend fun addSession(session: Session) {
        sessionDao.insertSession(session.toEntity())

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
