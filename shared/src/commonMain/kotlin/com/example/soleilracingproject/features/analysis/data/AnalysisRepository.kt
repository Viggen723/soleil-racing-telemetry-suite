package com.example.soleilracingproject.features.analysis.data

import com.example.soleilracingproject.data.local.dao.LapDao
import com.example.soleilracingproject.data.local.dao.SessionDao
import com.example.soleilracingproject.data.mapper.toDomain
import com.example.soleilracingproject.model.analysis.Lap
import com.example.soleilracingproject.model.analysis.Session
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AnalysisRepository(
    private val sessionDao: SessionDao,
    private val lapDao: LapDao
) {
    fun getAllSessions(): Flow<List<Session>> {
        return sessionDao.getAllSessionsWithLaps().map { sessionsWithLaps ->
            sessionsWithLaps.map { sessionWithLaps ->
                val laps = sessionWithLaps.laps.map { it.toDomain() }
                sessionWithLaps.session.toDomain(laps)
            }
        }
    }

    fun getLapWithTelemetry(lapId: String): Flow<Lap?> {
        return lapDao.getLapWithTelemetry(lapId).map { lapWithTelemetry ->
            lapWithTelemetry?.toDomain()
        }
    }
}
