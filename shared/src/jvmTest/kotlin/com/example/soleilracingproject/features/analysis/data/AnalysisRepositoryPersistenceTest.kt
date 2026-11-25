package com.example.soleilracingproject.features.analysis.data

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import androidx.room3.Room
import androidx.room3.useWriterConnection
import androidx.sqlite.SQLiteException
import com.example.soleilracingproject.data.local.AppDatabase
import com.example.soleilracingproject.data.local.getRoomDatabase
import com.example.soleilracingproject.data.remote.udp.UdpTelemetryFixtures
import com.example.soleilracingproject.data.remote.udp.UdpTelemetryParser
import com.example.soleilracingproject.features.analysis.viewmodel.AnalysisViewModel
import com.example.soleilracingproject.model.analysis.Session
import com.example.soleilracingproject.util.tools.lapLogic.LapLogic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AnalysisRepositoryPersistenceTest {
    @Test
    fun lapInsertFailureRollsBackSessionAndEarlierLapWithTelemetry() = withDatabase { database, repository ->
        database.useWriterConnection { connection ->
            connection.usePrepared(
                "CREATE TRIGGER fail_lap BEFORE INSERT ON laps WHEN NEW.lapNumber = 2 " +
                    "BEGIN SELECT RAISE(ABORT, 'test lap failure'); END"
            ) { it.step() }
        }
        val session = sessionWithLaps(firstLapSampleCount = 2)

        val failure = assertFailsWith<SQLiteException> { repository.addSession(session) }
        assertTrue(failure.message.orEmpty().contains("test lap failure"))
        assertNoPartialSession(database, session.id)
    }

    @Test
    fun telemetryInsertFailureRollsBackPreviouslyInsertedBatch() = withDatabase { database, repository ->
        database.useWriterConnection { connection ->
            connection.usePrepared(
                "CREATE TRIGGER fail_telemetry BEFORE INSERT ON telemetry_points WHEN NEW.timeStamp = 1001 " +
                    "BEGIN SELECT RAISE(ABORT, 'test telemetry failure'); END"
            ) { it.step() }
        }
        val session = sessionWithLaps(firstLapSampleCount = 1001)

        val failure = assertFailsWith<SQLiteException> { repository.addSession(session) }
        assertTrue(failure.message.orEmpty().contains("test telemetry failure"))
        assertNoPartialSession(database, session.id)
    }

    @Test
    fun csvImportReopensWithItsLapsAndTelemetry() = withDatabase { _, repository ->
        val store = ViewModelStore()
        val viewModel = withContext(Dispatchers.Main) {
            AnalysisViewModel(repository).also { store.put("analysis", it) }
        }
        try {
            withContext(Dispatchers.Main) {
                viewModel.loadCsvSession("practice.csv", "time,speedmph\n10000,60\n11000,70")
            }
            val session = withTimeout(10_000) {
                viewModel.sessionsState.first { it.isNotEmpty() }.single()
            }
            val lap = session.laps.single()
            assertEquals("practice", session.title)
            assertEquals(session.id, lap.sessionId)
            assertEquals(listOf(60f, 70f), lap.telemetryPoints.sortedBy { it.time }.map { it.speed })
            assertEquals(lap.telemetryPoints.size, repository.getLapWithTelemetry(lap.id).first()?.telemetryPoints?.size)
        } finally {
            withContext(Dispatchers.Main) { store.clear() }
            viewModel.viewModelScope.coroutineContext.job.join()
        }
    }

    private fun withDatabase(test: suspend (AppDatabase, AnalysisRepository) -> Unit) = runBlocking {
        val database = getRoomDatabase(Room.inMemoryDatabaseBuilder<AppDatabase>())
        try {
            test(database, AnalysisRepository(database))
        } finally {
            database.close()
        }
    }

    private fun sessionWithLaps(firstLapSampleCount: Int): Session {
        val sample = requireNotNull(UdpTelemetryParser.parseUdpData(UdpTelemetryFixtures.validPacket()))
        val sessionId = "transaction-test"
        val firstLapPoints = (1..firstLapSampleCount).map { sample.copy(time = it) }
        return Session(
            id = sessionId,
            title = "Test",
            vehicleId = "Vehicle",
            track = "Track",
            dateEpoch = 100,
            laps = listOf(
                LapLogic.buildLap(sessionId, 1, firstLapPoints, 1_000),
                LapLogic.buildLap(sessionId, 2, listOf(sample), 1_000),
            ),
        )
    }

    private suspend fun assertNoPartialSession(database: AppDatabase, sessionId: String) {
        assertNull(database.sessionDao().getSessionWithLaps(sessionId).first())
        assertTrue(database.lapDao().getLapsForSession(sessionId).first().isEmpty())
        assertTrue(database.telemetryDao().getTelemetryForSession(sessionId).first().isEmpty())
    }
}
