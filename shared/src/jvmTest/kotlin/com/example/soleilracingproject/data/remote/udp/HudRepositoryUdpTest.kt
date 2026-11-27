package com.example.soleilracingproject.data.remote.udp

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import androidx.room3.Room
import com.example.soleilracingproject.data.local.AppDatabase
import com.example.soleilracingproject.data.local.dao.TrackDao
import com.example.soleilracingproject.data.local.entity.TrackEntity
import com.example.soleilracingproject.data.local.getRoomDatabase
import com.example.soleilracingproject.features.analysis.data.AnalysisRepository
import com.example.soleilracingproject.features.analysis.data.TrackRepository
import com.example.soleilracingproject.features.customtrack.viewmodel.CustomTrackViewModel
import com.example.soleilracingproject.features.garage.data.GarageRepository
import com.example.soleilracingproject.features.hud.data.HudRepository
import com.example.soleilracingproject.features.hud.data.RecordingNotifier
import com.example.soleilracingproject.features.hud.data.RecordingSaveStatus
import com.example.soleilracingproject.features.hud.data.RecordingSessionOwner
import com.example.soleilracingproject.features.hud.viewmodel.HudViewModel
import com.example.soleilracingproject.features.settings.data.SettingsRepository
import com.example.soleilracingproject.model.garage.Car
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock

class HudRepositoryUdpTest {
    @Test
    fun reconnectKeepsOneHudCollectorAndRecordsEachPacketOnce() = withReceiver {
        val hud = hud()
        listener.starts.first { it == 1 }
        listener.packets.emit(UdpTelemetryFixtures.validPacket().copyOf(47))
        listener.send(100)
        hud.recentPoints.first { it.isNotEmpty() }
        assertEquals(listOf(100), hud.recentPoints.value.map { it.time })
        withContext(Dispatchers.Main) { hud.startRecording() }
        assertTrue(hud.isRecording.value)

        repeat(3) { index ->
            hud.reconnectUdp()
            listener.starts.first { it == index + 2 }
        }
        listener.send(200)
        listener.send(300)
        hud.recentPoints.first { points -> points.any { it.time == 300 } }
        withContext(Dispatchers.Main) { hud.stopRecording() }

        val session = analysis.getAllSessions().first { sessions ->
            sessions.singleOrNull()?.laps?.singleOrNull()?.telemetryPoints?.isNotEmpty() == true
        }.single()
        assertEquals(listOf(200, 300), session.laps.single().telemetryPoints.map { it.time }.sorted())
        assertEquals(listOf(100, 200, 300), hud.recentPoints.value.map { it.time })
        assertEquals(1, listener.packets.subscriptionCount.value)
        assertEquals(listOf(4120, 4120, 4120, 4120), listener.ports)
    }

    @Test
    fun rapidRestartKeepsEachRecordingsMetadataAndSamples() = withReceiver {
        backupUid = "alice"
        val firstSaveEntered = CompletableDeferred<Unit>()
        val releaseFirstSave = CompletableDeferred<Unit>()
        val queries = AtomicInteger()
        val delayedTrackDao = object : TrackDao by trackDao {
            override fun getAllTracks() = flow<List<TrackEntity>> {
                if (queries.incrementAndGet() == 1) {
                    firstSaveEntered.complete(Unit)
                    releaseFirstSave.await()
                }
                emit(emptyList())
            }
        }
        garage.saveCar(Car("a", "First", "Make", "A", 2001))
        garage.saveCar(Car("b", "Second", "Make", "B", 2002))
        garage.cars.first { it.size == 2 }
        val hud = hud(TrackRepository(delayedTrackDao))
        listener.starts.first { it == 1 }
        listener.send(100)
        hud.isLoggerConnected.first { it }

        val firstStart = withContext(Dispatchers.Main) {
            garage.selectCar("a")
            val before = Clock.System.now().toEpochMilliseconds()
            hud.startRecording()
            before..Clock.System.now().toEpochMilliseconds()
        }
        listener.send(200)
        listener.send(300)
        hud.telemetryState.first { it?.time == 300 }
        withContext(Dispatchers.Main) { hud.stopRecording() }
        firstSaveEntered.await()
        val firstId = owner.saveStatuses.value.keys.single()
        assertEquals(RecordingSaveStatus.Saving, owner.saveStatuses.value[firstId])
        backupUid = "bob"
        val replacementHud = hud()
        assertFalse(hud.viewModelScope.coroutineContext.job.isActive)
        listener.send(350)
        replacementHud.isLoggerConnected.first { it }

        while (Clock.System.now().toEpochMilliseconds() <= firstStart.last) delay(1)
        val secondStart = withContext(Dispatchers.Main) {
            garage.selectCar("b")
            val before = Clock.System.now().toEpochMilliseconds()
            replacementHud.startRecording()
            before..Clock.System.now().toEpochMilliseconds()
        }
        listener.send(400)
        listener.send(500)
        replacementHud.telemetryState.first { it?.time == 500 }
        withContext(Dispatchers.Main) { replacementHud.stopRecording() }
        val second = analysis.getAllSessions().first { it.isNotEmpty() }.single()
        assertEquals("2002 Make B (Second)", second.vehicleId)
        assertTrue(second.dateEpoch in secondStart)
        releaseFirstSave.complete(Unit)

        val sessions = analysis.getAllSessions().first { it.size == 2 }
        val first = sessions.last()
        assertEquals(firstId, first.id)
        assertTrue(first.remoteStorageUrl?.startsWith("users/alice/sessions/") == true)
        assertTrue(second.remoteStorageUrl?.startsWith("users/bob/sessions/") == true)
        owner.saveStatuses.first { it[firstId] == RecordingSaveStatus.Saved && it[second.id] == RecordingSaveStatus.Saved }
        assertEquals(second.id, sessions.first().id)
        assertEquals("2001 Make A (First)", first.vehicleId)
        assertTrue(first.dateEpoch in firstStart)
        for ((session, timestamps) in listOf(first to listOf(200, 300), second to listOf(400, 500))) {
            assertTrue(session.id.startsWith("session_"))
            val lap = session.laps.single()
            assertEquals(session.id, lap.sessionId)
            assertEquals("${session.id}_lap_1", lap.id)
            assertEquals(timestamps, lap.telemetryPoints.map { it.time }.sorted())
        }
    }

    @Test
    fun recordingSurvivesHudRecreationAndCustomTrackExit() = withReceiver {
        val hud = hud()
        listener.starts.first { it == 1 }
        listener.send(100)
        hud.isLoggerConnected.first { it }
        withContext(Dispatchers.Main) { hud.startRecording() }
        val recordingId = assertNotNull(owner.activeRecordingId.value)
        owner.recordingSeconds.first { it > 0 }
        clear(hudStore)
        assertEquals(listOf("started"), notifier.events)

        val track = customTrack()
        listener.send(200)
        track.currentTelemetry.first { it?.time == 200 }
        withContext(Dispatchers.Main) { track.setPoint1() }
        listener.send(300, UdpTelemetryFixtures.southernEasternPacket())
        track.currentTelemetry.first { it?.time == 300 }
        withContext(Dispatchers.Main) { track.setPoint2() }
        assertEquals(35.6875f, track.point1.value?.latitude)
        assertEquals(-35.6875f, track.point2.value?.latitude)
        clear(trackStore)
        assertEquals(0, listener.stops.value)
        assertEquals(1, listener.starts.value)
        listener.send(400)

        val replacement = hud()
        assertTrue(replacement.isRecording.value)
        assertTrue(replacement.recordingSeconds.value > 0)
        assertEquals(recordingId, owner.activeRecordingId.value)
        listener.send(500)
        replacement.telemetryState.first { it?.time == 500 }
        clear(hudStore)

        owner.stopRecording(recordingId)
        owner.stopRecording(recordingId)
        owner.saveStatuses.first { it[recordingId] == RecordingSaveStatus.Saved }
        listener.stops.first { it == 1 }
        assertEquals(listOf("started", "stopped"), notifier.events)
        val session = analysis.getAllSessions().first().single()
        assertEquals(recordingId, session.id)
        assertEquals(listOf(200, 300, 400, 500), session.laps.single().telemetryPoints.map { it.time }.sorted())
    }

    @Test
    fun staleStopDoesNotEndNewRecordingAndStopKeepsHudReceiving() = withReceiver {
        val hud = hud()
        listener.starts.first { it == 1 }
        listener.send(100)
        hud.isLoggerConnected.first { it }
        withContext(Dispatchers.Main) { hud.startRecording() }
        val firstId = assertNotNull(owner.activeRecordingId.value)
        listener.send(200)
        listener.send(300)
        hud.telemetryState.first { it?.time == 300 }
        withContext(Dispatchers.Main) {
            owner.stopRecording(firstId)
            hud.startRecording()
            owner.stopRecording(firstId)
        }
        val secondId = assertNotNull(owner.activeRecordingId.value)
        assertTrue(secondId != firstId)
        assertTrue(hud.isRecording.value)
        listener.send(400)
        listener.send(500)
        hud.telemetryState.first { it?.time == 500 }
        withContext(Dispatchers.Main) { hud.stopRecording() }
        owner.saveStatuses.first { it[firstId] == RecordingSaveStatus.Saved && it[secondId] == RecordingSaveStatus.Saved }
        listener.send(600)
        hud.telemetryState.first { it?.time == 600 }
        assertEquals(0, listener.stops.value)
        assertEquals(listOf("started", "stopped", "started", "stopped"), notifier.events)
        clear(hudStore)
        listener.stops.first { it == 1 }
    }

    @Test
    fun failedSaveIsReportedForTheStoppedRecording() = withReceiver {
        val failingTrackDao = object : TrackDao by trackDao {
            override fun getAllTracks() = flow<List<TrackEntity>> { error("Track lookup failed") }
        }
        val hud = hud(TrackRepository(failingTrackDao))
        listener.starts.first { it == 1 }
        listener.send(100)
        hud.isLoggerConnected.first { it }
        withContext(Dispatchers.Main) { hud.startRecording() }
        val recordingId = assertNotNull(owner.activeRecordingId.value)
        listener.send(200)
        listener.send(300)
        hud.telemetryState.first { it?.time == 300 }
        withContext(Dispatchers.Main) { hud.stopRecording() }
        val status = owner.saveStatuses.first { it[recordingId] is RecordingSaveStatus.Failed }
        assertEquals(RecordingSaveStatus.Failed("Track lookup failed"), status[recordingId])
        assertTrue(analysis.getAllSessions().first().isEmpty())
        assertFalse(hud.isRecording.value)
    }

    @Test
    fun hudAndCustomTrackShareReceptionUntilTheLastViewModelClears() = withReceiver {
        val hud = hud()
        listener.starts.first { it == 1 }
        val track = customTrack()
        listener.send(100)
        track.currentTelemetry.first { it?.time == 100 }
        hud.telemetryState.first { it?.time == 100 }
        withContext(Dispatchers.Main) { track.setPoint1() }
        listener.send(200, UdpTelemetryFixtures.southernEasternPacket())
        track.currentTelemetry.first { it?.time == 200 }
        hud.telemetryState.first { it?.time == 200 }
        withContext(Dispatchers.Main) { track.setPoint2() }
        assertEquals(35.6875f, track.point1.value?.latitude)
        assertEquals(-35.6875f, track.point2.value?.latitude)
        assertEquals(1, listener.starts.value)

        clear(trackStore)
        listener.send(300)
        hud.telemetryState.first { it?.time == 300 }
        assertEquals(0, listener.stops.value)

        val replacementTrack = customTrack()
        listener.send(400)
        replacementTrack.currentTelemetry.first { it?.time == 400 }
        clear(hudStore)
        listener.send(500)
        replacementTrack.currentTelemetry.first { it?.time == 500 }
        assertEquals(0, listener.stops.value)

        clear(trackStore)
        listener.stops.first { it == 1 }
        assertFalse(listener.active)

        val standaloneTrack = customTrack()
        listener.starts.first { it == 2 }
        assertNull(standaloneTrack.currentTelemetry.value)
        listener.send(600)
        standaloneTrack.currentTelemetry.first { it?.time == 600 }
        clear(trackStore)
        listener.stops.first { it == 2 }
        assertFalse(listener.active)
    }

    @Test
    fun finalCancellationWaitsForDelayedStartAndDiscardsQueuedReconnects() = withReceiver {
        val releaseStart = CompletableDeferred<Unit>()
        listener.startGate = releaseStart
        val subscriber = scope.launch { repository.observeTelemetry().collect {} }
        listener.startAttempts.first { it == 1 }
        repeat(10) { repository.reconnectUdp() }
        subscriber.cancelAndJoin()
        // Release the simulated native bind only after the final subscription is gone.
        while (listener.startJob?.isActive == true) yield()
        releaseStart.complete(Unit)
        listener.stops.first { it == 1 }
        assertFalse(listener.active)
        assertEquals(1, listener.startAttempts.value)

        val replacement = scope.launch { repository.observeTelemetry().collect {} }
        listener.starts.first { it == 2 }
        assertEquals(2, listener.startAttempts.value)
        replacement.cancelAndJoin()
        listener.stops.first { it == 2 }
        assertFalse(listener.active)
    }

    private fun withReceiver(test: suspend Fixture.() -> Unit) = runBlocking {
        val fixture = Fixture(CoroutineScope(coroutineContext + Job()))
        try {
            withTimeout(10_000) { fixture.test() }
        } finally {
            fixture.close()
        }
    }

    private class Fixture(val scope: CoroutineScope) {
        val listener = FakeUdpListener()
        val hudStore = ViewModelStore()
        val trackStore = ViewModelStore()
        private val database = getRoomDatabase(Room.inMemoryDatabaseBuilder<AppDatabase>())
        @Volatile var backupUid: String? = null
        val analysis = AnalysisRepository(database) { backupUid }
        val garage = GarageRepository(database.carDao())
        val trackDao = database.trackDao()
        val notifier = FakeRecordingNotifier()
        lateinit var owner: RecordingSessionOwner
            private set
        val repository = HudRepository(
            listener,
            UdpTelemetryParser::parseUdpData,
            analysis,
            garage,
            scope,
        )

        suspend fun hud(trackRepository: TrackRepository? = null) = withContext(Dispatchers.Main) {
            if (!::owner.isInitialized) {
                owner = RecordingSessionOwner(repository, notifier, trackRepository)
            }
            HudViewModel(repository, SettingsRepository(database.settingsDao()), owner).also {
                hudStore.put("hud", it)
            }
        }

        suspend fun customTrack() = withContext(Dispatchers.Main) {
            CustomTrackViewModel(TrackRepository(database.trackDao()), repository).also {
                trackStore.put("track", it)
            }
        }

        suspend fun clear(store: ViewModelStore) {
            val jobs = withContext(Dispatchers.Main) {
                store.keys().mapNotNull { store[it]?.viewModelScope?.coroutineContext?.job }.also {
                    store.clear()
                }
            }
            jobs.forEach { it.join() }
        }

        suspend fun close() {
            listener.startGate?.complete(Unit)
            clear(hudStore)
            clear(trackStore)
            if (::owner.isInitialized) owner.close().join()
            scope.coroutineContext.job.cancelAndJoin()
            database.close()
        }
    }

    private class FakeRecordingNotifier : RecordingNotifier {
        val events = mutableListOf<String>()

        override fun recordingStarted() {
            events.add("started")
        }

        override fun recordingStopped() {
            events.add("stopped")
        }
    }

    private class FakeUdpListener : UdpSocketListener {
        val packets = MutableSharedFlow<ByteArray>()
        val startAttempts = MutableStateFlow(0)
        val starts = MutableStateFlow(0)
        val stops = MutableStateFlow(0)
        val ports = mutableListOf<Int>()
        var startGate: CompletableDeferred<Unit>? = null
        var startJob: Job? = null
            private set
        var active = false
            private set

        override suspend fun beginListening(port: Int) {
            startJob = currentCoroutineContext().job
            startAttempts.value++
            withContext(NonCancellable) {
                startGate?.await()
                check(!active) { "Receiver started twice" }
                active = true
                ports.add(port)
                starts.value++
            }
        }

        override fun stopListening() {
            active = false
            stops.value++
        }

        override fun observePackets() = packets

        suspend fun send(timestamp: Int, packet: ByteArray = UdpTelemetryFixtures.validPacket()) {
            repeat(4) { index -> packet[index] = (timestamp ushr (8 * index)).toByte() }
            packets.emit(packet)
        }
    }
}
