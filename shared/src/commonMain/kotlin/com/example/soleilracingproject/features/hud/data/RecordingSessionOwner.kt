package com.example.soleilracingproject.features.hud.data

import com.example.soleilracingproject.features.analysis.data.TrackRepository
import com.example.soleilracingproject.model.analysis.Lap
import com.example.soleilracingproject.model.analysis.Session
import com.example.soleilracingproject.model.telemetry.GpsGate
import com.example.soleilracingproject.model.telemetry.TelemetryPoint
import com.example.soleilracingproject.util.tools.lapLogic.DeltaCalculator
import com.example.soleilracingproject.util.tools.lapLogic.LapDeltas
import com.example.soleilracingproject.util.tools.lapLogic.LapLogic
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlin.time.Instant

private const val USE_FAKE_DATA = false

internal fun formatRecordedSessionTitle(
    epochMillis: Long,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): String {
    val dateTime = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(timeZone)
    val date = dateTime.date.toString()
    val hour = dateTime.hour.toString().padStart(2, '0')
    val minute = dateTime.minute.toString().padStart(2, '0')
    return "Session $date $hour:$minute"
}

enum class RecordingState {
    IDLE,
    ARMED,
    RECORDING_LAP
}

private data class RecordingSnapshot(
    val sessionId: String,
    val startedAt: Long,
    val title: String,
    val points: List<TelemetryPoint>,
    val fallbackGate: GpsGate?,
    val vehicleDisplayName: String,
)

sealed interface RecordingSaveStatus {
    data object Saving : RecordingSaveStatus
    data object Saved : RecordingSaveStatus
    data object Cancelled : RecordingSaveStatus
    data class Failed(val message: String) : RecordingSaveStatus
}

class RecordingSessionOwner(
    private val hudRepository: HudRepository,
    private val recordingNotifier: RecordingNotifier? = null,
    private val trackRepository: TrackRepository? = null,
) {
    // Commands, live capture and status are confined to Main. Only snapshot persistence uses IO.
    private val lifetime = SupervisorJob()
    private val scope = CoroutineScope(lifetime + Dispatchers.Main.immediate)
    private var captureJob: Job? = null
    private val _saveStatuses = MutableStateFlow<Map<String, RecordingSaveStatus>>(emptyMap())
    val saveStatuses = _saveStatuses.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()
    private val _recordingState = MutableStateFlow(RecordingState.IDLE)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()
    // The laps finished in this recording. The lap times widget reads this.
    private val _lapsState = MutableStateFlow<List<Lap>>(emptyList())
    val lapsState: StateFlow<List<Lap>> = _lapsState.asStateFlow()
    // When Start is pressed the recording bar shows this as a timer.
    private val _recordingSeconds = MutableStateFlow(0L)
    val recordingSeconds: StateFlow<Long> = _recordingSeconds.asStateFlow()
    private var timerJob: Job? = null

    private val activeLapBuffer = mutableListOf<TelemetryPoint>()
    private val allRecordedSessionPoints = mutableListOf<TelemetryPoint>()
    private val completedLaps = mutableListOf<Lap>()
    private var sessionStartTime: Long = 0L
    private val _activeRecordingId = MutableStateFlow<String?>(null)
    val activeRecordingId = _activeRecordingId.asStateFlow()
    private var previousPoint: TelemetryPoint? = null
    private var lapStartTimeMs: Long? = null
    private var detectedGate: GpsGate? = null

    private val _lapDeltas = MutableStateFlow(LapDeltas())
    val lapDeltas: StateFlow<LapDeltas> = _lapDeltas.asStateFlow()

    private val finishGate: GpsGate? =
        if (USE_FAKE_DATA) GpsGate(lat1 = 35.68, lon1 = 139.6909, lat2 = 35.68, lon2 = 139.6913) else null

    private fun processTelemetryPoint(point: TelemetryPoint) {
        allRecordedSessionPoints.add(point)

        when (_recordingState.value) {
            RecordingState.IDLE -> Unit
            RecordingState.ARMED -> {
                if (hasCrossedGpsGate(point)) {
                    activeLapBuffer.clear()
                    lapStartTimeMs = point.time.toLong()
                    _recordingState.value = RecordingState.RECORDING_LAP
                }
            }
            RecordingState.RECORDING_LAP -> {
                activeLapBuffer.add(point)
                if (hasCrossedGpsGate(point)) {
                    finalizeCurrentLap(point.time.toLong())
                    lapStartTimeMs = point.time.toLong()
                }
                updateLapDeltas()
            }
        }
        previousPoint = point.takeIf {
            it.latitude.isFinite() && it.longitude.isFinite() &&
                    it.latitude in -90f..90f && it.longitude in -180f..180f
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (true) {
                val now = kotlin.time.Clock.System.now().toEpochMilliseconds()
                _recordingSeconds.value = (now - sessionStartTime) / 1000
                delay(1000)
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
        _recordingSeconds.value = 0
    }

    fun startRecording(loggerConnected: Boolean) {
        scope.launch {
            if (_activeRecordingId.value != null || !loggerConnected) return@launch
            beginCapture()
        }
    }

    private fun beginCapture() {
        _lapDeltas.value = LapDeltas()
        activeLapBuffer.clear()
        allRecordedSessionPoints.clear()
        completedLaps.clear()
        _lapsState.value = emptyList()
        previousPoint = null
        lapStartTimeMs = null
        detectedGate = null
        sessionStartTime = kotlin.time.Clock.System.now().toEpochMilliseconds()
        startTimer()
        _activeRecordingId.value = "session_${Random.nextLong().toString(16)}"
        _isRecording.value = true
        _recordingState.value = RecordingState.ARMED
        captureJob = scope.launch(start = CoroutineStart.UNDISPATCHED) {
            hudRepository.observeTelemetry().collect { processTelemetryPoint(it) }
        }
        recordingNotifier?.recordingStarted()
    }

    fun stopRecording(expectedSessionId: String) {
        scope.launch {
            if (_activeRecordingId.value != expectedSessionId) return@launch
            finishCapture(expectedSessionId)
        }
    }

    private fun finishCapture(sessionId: String) {
        val vehicleDisplayName = hudRepository.getActiveCar()?.let { car ->
            listOfNotNull(car.year?.toString(), car.make, car.model, "(${car.name})")
                .filter { it.isNotBlank() }
                .joinToString(" ")
        }?.ifBlank { null } ?: "Recorded Vehicle"
        val snapshot = RecordingSnapshot(
            sessionId = sessionId,
            startedAt = sessionStartTime,
            title = formatRecordedSessionTitle(sessionStartTime),
            points = allRecordedSessionPoints.toList(),
            fallbackGate = detectedGate ?: finishGate,
            vehicleDisplayName = vehicleDisplayName,
        )

        resetCapture()
        _saveStatuses.update { it + (sessionId to RecordingSaveStatus.Saving) }
        scope.launch {
            try {
                withContext(Dispatchers.IO) { saveRecording(snapshot) }
                _saveStatuses.update { it + (sessionId to RecordingSaveStatus.Saved) }
            } catch (cancelled: CancellationException) {
                _saveStatuses.update { it + (sessionId to RecordingSaveStatus.Cancelled) }
                throw cancelled
            } catch (failure: Exception) {
                val message = failure.message ?: "Recording could not be saved"
                _saveStatuses.update { it + (sessionId to RecordingSaveStatus.Failed(message)) }
            }
        }
        recordingNotifier?.recordingStopped()
    }

    private fun resetCapture() {
        captureJob?.cancel()
        captureJob = null
        stopTimer()
        allRecordedSessionPoints.clear()
        activeLapBuffer.clear()
        completedLaps.clear()
        previousPoint = null
        lapStartTimeMs = null
        _lapDeltas.value = LapDeltas()
        _recordingState.value = RecordingState.IDLE
        _isRecording.value = false
        _activeRecordingId.value = null
    }

    private suspend fun saveRecording(snapshot: RecordingSnapshot) {
        val sessionPoints = if (snapshot.points.size >= 2) {
            snapshot.points
        } else {
            // Fallback points if testing without a live UDP telemetry stream
            (0..50).map { i ->
                val angle = i * 0.1f
                TelemetryPoint(
                    time = i * 100,
                    accelX = sin(angle),
                    accelY = cos(angle),
                    accelZ = 0f,
                    latitude = (35.68 + 0.0005 * sin(angle)).toFloat(),
                    longitude = (139.69 + 0.0005 * cos(angle)).toFloat(),
                    speed = (60f + 20f * sin(angle)),
                    qw = 1f, qi = 0f, qj = 0f, qk = 0f,
                    altitude = 20f
                )
            }
        }

        val matchedTrack = trackRepository?.findMatchingTrack(sessionPoints)
        val autoGate = LapLogic.autoGenerateGate(sessionPoints)
        val gateToUse = matchedTrack?.startFinishGate ?: autoGate ?: snapshot.fallbackGate

        var laps = LapLogic.splitSessionIntoLaps(
            sessionId = snapshot.sessionId,
            points = sessionPoints,
            finishGate = gateToUse,
        )

        if (laps.size <= 1 && autoGate != null && sessionPoints.size > 200) {
            val autoLaps = LapLogic.splitSessionIntoLaps(
                sessionId = snapshot.sessionId,
                points = sessionPoints,
                finishGate = autoGate,
            )
            if (autoLaps.size > 1) {
                laps = autoLaps
            }
        }

        val session = Session(
            id = snapshot.sessionId,
            title = snapshot.title,
            vehicleId = snapshot.vehicleDisplayName,
            track = matchedTrack?.trackName ?: "Recorded Session",
            dateEpoch = snapshot.startedAt,
            isSynced = false,
            laps = laps,
        )
        hudRepository.saveSession(session)
    }

    private fun hasCrossedGpsGate(point: TelemetryPoint): Boolean {
        val gateToUse = finishGate ?: detectedGate ?: run {
            if (point.latitude != 0f || point.longitude != 0f) {
                // To avoid doing DB ops on every frame during live UDP polling, we only generate auto-gates after stop.
                // We'll rely on the default stopRecording loop to parse out sectors later if no preset gate is loaded.
                detectedGate = null
            }
            detectedGate
        }
        return LapLogic.hasCrossedGpsGate(previousPoint, point, gateToUse)
    }

    private fun finalizeCurrentLap(crossingTimeMs: Long) {
        if (activeLapBuffer.isEmpty()) return
        val startTimeMs = lapStartTimeMs ?: return

        val newLap = LapLogic.buildLap(
            _activeRecordingId.value ?: return,
            completedLaps.size + 1,
            activeLapBuffer.toList(),
            crossingTimeMs - startTimeMs
        )
        completedLaps.add(newLap)
        _lapsState.value = completedLaps.toList()
        activeLapBuffer.clear()
    }

    private fun updateLapDeltas() {
        val goodLaps = completedLaps.filter { !it.isDnf }
        val bestLap = goodLaps.minByOrNull { it.lapTime }
        val lastLap = goodLaps.lastOrNull()
        val currentPoints = activeLapBuffer.toList()

        _lapDeltas.value = LapDeltas(
            vsBestSeconds = bestLap?.let { DeltaCalculator.deltaSeconds(it, currentPoints) },
            vsLastSeconds = lastLap?.let { DeltaCalculator.deltaSeconds(it, currentPoints) }
        )
    }

    // Koin invokes this when the application graph closes. Joining the returned job awaits cleanup.
    fun close(): Job {
        scope.launch {
            val wasRecording = _activeRecordingId.value != null
            try {
                resetCapture()
                _lapsState.value = emptyList()
                if (wasRecording) recordingNotifier?.recordingStopped()
            } finally {
                scope.cancel()
            }
        }
        return lifetime
    }
}
