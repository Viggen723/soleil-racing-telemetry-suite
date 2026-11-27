package com.example.soleilracingproject.features.analysis.data

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import androidx.room3.Room
import com.example.soleilracingproject.data.local.AppDatabase
import com.example.soleilracingproject.data.local.getRoomDatabase
import com.example.soleilracingproject.data.remote.udp.UdpTelemetryFixtures
import com.example.soleilracingproject.data.remote.udp.UdpTelemetryParser
import com.example.soleilracingproject.features.analysis.data.backup.BackupAccount
import com.example.soleilracingproject.features.analysis.data.backup.BackupTransport
import com.example.soleilracingproject.features.analysis.data.backup.SessionBackupFormat
import com.example.soleilracingproject.features.analysis.data.backup.SessionBackupRepository
import com.example.soleilracingproject.features.analysis.data.backup.SessionBackupStatus
import com.example.soleilracingproject.features.analysis.viewmodel.AnalysisViewModel
import com.example.soleilracingproject.model.analysis.Session
import com.example.soleilracingproject.util.tools.lapLogic.LapLogic
import java.nio.file.Files
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SessionBackupTest {
    @Test
    fun savesMultipleLapsAndLargeTelemetryWithStableOrderAndNoDuplicateUpload() = runBlocking {
        val fixture = Fixture()
        val cloud = fixture.start()
        try {
            val session = sampleSession()
            fixture.local.addSession(session)
            fixture.waitForConfirmation(session.id)
            val saved = assertNotNull(cloud.readBackedUpSession(session.id))
            assertEquals(session, saved)
            val path = SessionBackupFormat.path("alice", session.id)
            assertEquals(4, fixture.remote.nodes.keys.count { it.startsWith("$path/chunks/") })
            val completed = fixture.remote.nodes.toMap()
            val writes = fixture.remote.writes.get()

            fixture.local.updateSession(session.copy(title = "Edited locally"))
            fixture.local.addSession(session)
            cloud.close().join()
            fixture.start()
            assertTrue(fixture.local.getAllSessions().first().single().isSynced)
            assertEquals(completed, fixture.remote.nodes)
            assertEquals(writes, fixture.remote.writes.get())
        } finally {
            fixture.close()
        }
    }

    @Test
    fun failureLeavesLocalSessionAndRestartsIncompleteUpload() = runBlocking {
        val directory = Files.createTempDirectory("soleil-backup")
        val file = directory.resolve("backup.db").toString()
        val account = FakeAccount()
        val remote = FakeTransport(account).apply { failPathSuffix = "/chunks/0/1" }
        val session = sampleSession()
        try {
            val first = Fixture(account, remote, file)
            val cloud = first.start()
            try {
                first.local.addSession(session)
                withTimeout(10_000) { cloud.statuses.first { it[session.id] == SessionBackupStatus.FAILED } }
                val committed = first.local.getAllSessions().first().single()
                assertEquals(session.laps.sumOf { it.telemetryPoints.size }, committed.laps.sumOf { it.telemetryPoints.size })
                assertFalse(committed.isSynced)
                assertNull(cloud.readBackedUpSession(session.id))
            } finally {
                first.close()
            }
            remote.failPathSuffix = null
            val reopened = Fixture(account, remote, file)
            try {
                val backup = reopened.start()
                reopened.waitForConfirmation(session.id)
                assertEquals(session, backup.readBackedUpSession(session.id))
                assertEquals(5, remote.nodes.size)
            } finally {
                reopened.close()
            }
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun queuedOrUnacknowledgedMarkerDoesNotConfirmUntilServerAcceptsRetry() = runBlocking {
        val fixture = Fixture()
        val cloud = fixture.start()
        fixture.remote.loseMarkerAck = true
        try {
            val session = sampleSession()
            fixture.local.addSession(session)
            withTimeout(10_000) { cloud.statuses.first { it[session.id] == SessionBackupStatus.FAILED } }
            assertFalse(fixture.local.getAllSessions().first().single().isSynced)
            val savedNodes = fixture.remote.nodes.toMap()
            assertTrue(savedNodes.keys.any { it.endsWith("/complete") })
            val writes = fixture.remote.writes.get()
            fixture.remote.loseMarkerAck = false
            fixture.waitForConfirmation(session.id)
            assertEquals(savedNodes, fixture.remote.nodes)
            assertEquals(writes + 1, fixture.remote.writes.get())
        } finally {
            fixture.close()
        }
    }

    @Test
    fun accountBindingsSurviveOfflineAndAnotherUserCannotReceiveThePendingSession() = runBlocking {
        val account = FakeAccount(null)
        val fixture = Fixture(account)
        val cloud = fixture.start()
        fixture.remote.online = false
        try {
            val orphan = sampleSession("offline")
            fixture.local.addSession(orphan)
            assertNull(cloud.readBackedUpSession(orphan.id))
            assertTrue(fixture.remote.nodes.isEmpty())
            account.uid.value = "alice"
            withTimeout(10_000) { cloud.statuses.first { it[orphan.id] == SessionBackupStatus.PENDING } }
            assertEquals(SessionBackupFormat.path("alice", orphan.id), fixture.local.getAllSessions().first().single().remoteStorageUrl)

            val owned = sampleSession("owned-by-alice")
            fixture.local.addSession(owned)
            account.uid.value = "bob"
            fixture.remote.online = true
            fixture.local.addSession(sampleSession("owned-by-bob"))
            fixture.waitForConfirmation("owned-by-bob")
            assertNull(cloud.readBackedUpSession(orphan.id))
            assertTrue(fixture.remote.nodes.keys.all { it.startsWith("users/bob/") })
            assertFailsWith<IllegalStateException> {
                fixture.remote.read(SessionBackupFormat.path("alice", orphan.id) + "/complete")
            }

            account.uid.value = "alice"
            fixture.waitForConfirmation(orphan.id)
            fixture.waitForConfirmation(owned.id)
            assertEquals(orphan, cloud.readBackedUpSession(orphan.id))
        } finally {
            fixture.close()
        }
    }

    @Test
    fun csvImportsAreBackedUpThroughTheSharedCommitPath() = runBlocking {
        val fixture = Fixture()
        val backup = fixture.start()
        val store = ViewModelStore()
        val viewModel = withContext(Dispatchers.Main) {
            AnalysisViewModel(fixture.local).also { store.put("analysis", it) }
        }
        try {
            withContext(Dispatchers.Main) {
                viewModel.loadCsvSession("practice.csv", "time,speedmph\n10000,60\n11000,70")
            }
            val saved = withTimeout(10_000) { fixture.local.getAllSessions().first { it.singleOrNull()?.isSynced == true }.single() }
            val restored = assertNotNull(backup.readBackedUpSession(saved.id))
            assertEquals("practice", restored.title)
            assertEquals(listOf(60f, 70f), restored.laps.single().telemetryPoints.map { it.speed })
        } finally {
            withContext(Dispatchers.Main) { store.clear() }
            viewModel.viewModelScope.coroutineContext.job.join()
            fixture.close()
        }
    }

    @Test
    fun chunkCodecPreservesIeee754Values() {
        val sample = sampleSession().laps.first().telemetryPoints.first().copy(
            latitude = -0f, longitude = Float.POSITIVE_INFINITY, accelX = Float.fromBits(0x7fc01234),
        )
        val decoded = SessionBackupFormat.decodeChunk(SessionBackupFormat.encodeChunk(listOf(sample))).single()
        assertEquals(sample.latitude.toRawBits(), decoded.latitude.toRawBits())
        assertEquals(sample.longitude.toRawBits(), decoded.longitude.toRawBits())
        assertEquals(sample.accelX.toRawBits(), decoded.accelX.toRawBits())
        assertEquals(sample, decoded)
    }

    private class Fixture(
        val account: FakeAccount = FakeAccount(),
        val remote: FakeTransport = FakeTransport(account),
        databasePath: String? = null,
    ) {
        private val database = getRoomDatabase(
            if (databasePath == null) Room.inMemoryDatabaseBuilder<AppDatabase>()
            else Room.databaseBuilder<AppDatabase>(databasePath)
        )
        val local = AnalysisRepository(database) { id -> account.currentUid()?.let { SessionBackupFormat.path(it, id) } }
        private var worker: SessionBackupRepository? = null

        fun start() = SessionBackupRepository(local, account, remote, retryIntervalMs = 100).also { worker = it }

        suspend fun waitForConfirmation(id: String) = withTimeout(10_000) {
            local.getAllSessions().first { list -> list.any { it.id == id && it.isSynced } }
        }

        suspend fun close() {
            worker?.close()?.join()
            database.close()
        }
    }

    private class FakeAccount(initial: String? = "alice") : BackupAccount {
        val uid = MutableStateFlow(initial)
        override val uidChanges = uid
        override fun currentUid() = uid.value
    }

    private class FakeTransport(private val account: FakeAccount) : BackupTransport {
        val nodes = ConcurrentHashMap<String, String>()
        val writes = AtomicInteger()
        @Volatile var online = true
        @Volatile var failPathSuffix: String? = null
        @Volatile var loseMarkerAck = false

        override suspend fun awaitConnection() = online

        override suspend fun read(path: String): String? {
            authorize(path)
            return nodes[path]
        }

        override suspend fun write(path: String, value: String) {
            authorize(path)
            check(online && failPathSuffix?.let(path::endsWith) != true) { "Upload interrupted" }
            val previous = nodes.putIfAbsent(path, value)
            check(previous == null || previous == value) { "Attempt to replace an existing node" }
            writes.incrementAndGet()
            check(!loseMarkerAck || !path.endsWith("/complete")) { "No server acknowledgement received" }
        }

        private fun authorize(path: String) {
            val uid = account.currentUid()
            check(uid != null && path.startsWith(SessionBackupFormat.prefix(uid))) { "Permission denied" }
        }
    }

    private fun sampleSession(id: String = "session.test/1"): Session {
        val sample = requireNotNull(UdpTelemetryParser.parseUdpData(UdpTelemetryFixtures.validPacket()))
        val firstLap = (0..257).map { sample.copy(time = 300 - it) }
        val secondLap = listOf(sample.copy(time = 5), sample.copy(time = 5, speed = 50f))
        return Session(
            id, "Practice", "Vehicle", "Track", 1_234L,
            laps = listOf(
                LapLogic.buildLap(id, 1, firstLap, 1_000).copy(sector1Time = 250),
                LapLogic.buildLap(id, 2, secondLap, 2_000).copy(isDnf = true),
            ),
        )
    }
}
