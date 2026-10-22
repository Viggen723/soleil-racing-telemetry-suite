package com.example.soleilracingproject.features.hud.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.soleilracingproject.data.local.dao.LapDao
import com.example.soleilracingproject.features.hud.data.HudRepository
import com.example.soleilracingproject.features.hud.ui.widget.*
import com.example.soleilracingproject.features.hud.ui.widget.widgetTypes.DeltaMode
import com.example.soleilracingproject.model.analysis.KnownTracks
import com.example.soleilracingproject.model.analysis.Lap
import com.example.soleilracingproject.model.analysis.Session
import com.example.soleilracingproject.model.telemetry.GpsGate
import com.example.soleilracingproject.model.telemetry.TelemetryPoint
import com.example.soleilracingproject.util.tools.lapLogic.DeltaCalculator
import com.example.soleilracingproject.util.tools.lapLogic.LapDeltas
import com.example.soleilracingproject.util.tools.lapLogic.LapLogic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import com.example.soleilracingproject.data.local.dao.WidgetLayoutDao
import com.example.soleilracingproject.data.local.entity.WidgetLayoutEntity
import kotlinx.coroutines.Job

private const val USE_FAKE_DATA = true
// How many recent readings we remember for the graphs (200 readings = about 20 seconds)
private const val HISTORY_SIZE = 200

enum class RecordingState {
    IDLE,
    ARMED,
    RECORDING_LAP
}

class HudViewModel(
    private val hudRepository: HudRepository,
    private val lapDao: LapDao? = null,
    private val widgetLayoutDao: WidgetLayoutDao? = null
) : ViewModel() {

    private val _telemetryState = MutableStateFlow<TelemetryPoint?>(null)
    val telemetryState: StateFlow<TelemetryPoint?> = _telemetryState.asStateFlow()

    private val _isEditMode = MutableStateFlow(false)
    val isEditMode: StateFlow<Boolean> = _isEditMode.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()
    private val _recordingState = MutableStateFlow(RecordingState.IDLE)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()
    private val _recentPoints = MutableStateFlow<List<TelemetryPoint>>(emptyList())
    val recentPoints: StateFlow<List<TelemetryPoint>> = _recentPoints.asStateFlow()
    private val _deltaMode = MutableStateFlow(DeltaMode.BEST_LAP)
    val deltaMode: StateFlow<DeltaMode> = _deltaMode.asStateFlow()
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
    private var currentSessionId: String = ""
    private var previousPoint: TelemetryPoint? = null
    private var lapStartTimeMs: Long? = null
    private var detectedGate: GpsGate? = null

    private val _lapDeltas = MutableStateFlow(LapDeltas())
    val lapDeltas: StateFlow<LapDeltas> = _lapDeltas.asStateFlow()

    private val finishGate: GpsGate? =
        if (USE_FAKE_DATA) GpsGate(lat1 = 35.68, lon1 = 139.6909, lat2 = 35.68, lon2 = 139.6913) else null

    private val _widgetLayout = MutableStateFlow(
        listOf(
            WidgetConfig(id = "speed_default", content = WidgetContent.SPEEDOMETER, title = "Speedometer"),
            WidgetConfig(id = "g_force_default", content = WidgetContent.G_FORCE_CIRCLE, title = "G-Force"),
            WidgetConfig(id = "recording_button_default", content = WidgetContent.RECORDING_BUTTON, title = "Recording Button")
        )
    )
    val widgetLayout: StateFlow<List<WidgetConfig>> = _widgetLayout.asStateFlow()

    init {
        loadSavedLayout()
        if (USE_FAKE_DATA) startFakeData() else startUdpListening()
    }

    private fun startUdpListening() {
        viewModelScope.launch(Dispatchers.IO) {
            hudRepository.beginListening(4120)
        }

        viewModelScope.launch {
            hudRepository.observeTelemetry().collect { point ->
                processTelemetryPoint(point)
            }
        }
    }

    private fun processTelemetryPoint(point: TelemetryPoint) {
        _telemetryState.value = point
        _recentPoints.value = (_recentPoints.value + point).takeLast(HISTORY_SIZE)

        if (_isRecording.value) {
            allRecordedSessionPoints.add(point)
        }

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

    private fun startFakeData() {
        viewModelScope.launch {
            var t = 0
            while (true) {
                val angle = t * 0.03f + 0.3f * sin(t / 100f)
                val fakePoint = TelemetryPoint(
                    time = t * 100,
                    accelX = cos(t / 15f),
                    accelY = 0.8f * sin(t / 25f),
                    accelZ = 0f,
                    latitude = 35.68f + 0.0009f * sin(angle),
                    longitude = 139.69f + 0.0011f * cos(angle),
                    speed = 80f + 50f * sin(t / 15f),
                    qw = 1f, qi = 0f, qj = 0f, qk = 0f,
                    altitude = 20f + 5f * sin(t / 40f)
                )
                processTelemetryPoint(fakePoint)
                t++
                delay(100)
            }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                val now = kotlin.time.Clock.System.now().toEpochMilliseconds()
                _recordingSeconds.value = (now - sessionStartTime) / 1000
                delay(1000)
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        _recordingSeconds.value = 0
    }

    fun startRecording() {
        if (_recordingState.value != RecordingState.IDLE) return
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
        currentSessionId = "session_${Random.nextLong().toString(16)}"
        _isRecording.value = true
        _recordingState.value = RecordingState.ARMED
    }

    fun stopRecording() {
        if (!_isRecording.value) return

        val sessionPoints = if (allRecordedSessionPoints.size >= 2) {
            allRecordedSessionPoints.toList()
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

        allRecordedSessionPoints.clear()
        activeLapBuffer.clear()
        completedLaps.clear()
        previousPoint = null
        lapStartTimeMs = null
        _lapDeltas.value = LapDeltas()
        _recordingState.value = RecordingState.IDLE
        _isRecording.value = false
        stopTimer()

        viewModelScope.launch(Dispatchers.IO) {
            val matchedTrack = KnownTracks.findMatchingTrack(sessionPoints)
            val presetGate = matchedTrack?.startFinishGate
            val autoGate = LapLogic.autoGenerateGate(sessionPoints)
            val gateToUse = presetGate ?: autoGate ?: detectedGate ?: finishGate

            var laps = LapLogic.splitSessionIntoLaps(
                sessionId = currentSessionId,
                points = sessionPoints,
                finishGate = gateToUse,
            )

            if (laps.size <= 1 && autoGate != null && sessionPoints.size > 200) {
                val autoLaps = LapLogic.splitSessionIntoLaps(
                    sessionId = currentSessionId,
                    points = sessionPoints,
                    finishGate = autoGate,
                )
                if (autoLaps.size > 1) {
                    laps = autoLaps
                }
            }

            val activeCar = hudRepository.getActiveCar()

            val vehicleDisplayName = activeCar?.let { car ->
                listOfNotNull(car.year?.toString(), car.make, car.model, "(${car.name})")
                    .filter { it.isNotBlank() }
                    .joinToString(" ")
            }?.ifBlank { null } ?: "Recorded Vehicle"

            val session = Session(
                id = currentSessionId,
                title = "HUD Session (${laps.size} laps)",
                vehicleId = vehicleDisplayName,
                track = matchedTrack?.trackName ?: "Recorded Session",
                dateEpoch = sessionStartTime,
                isSynced = false,
                laps = laps,
            )

            hudRepository.saveSession(session)
        }
    }

    fun nextDeltaMode() {
        _deltaMode.value = DeltaMode.entries[(_deltaMode.value.ordinal + 1) % DeltaMode.entries.size]
    }

    private fun hasCrossedGpsGate(point: TelemetryPoint): Boolean {
        val gateToUse = finishGate ?: detectedGate ?: run {
            if (point.latitude != 0f || point.longitude != 0f) {
                val matched = KnownTracks.findMatchingTrack(listOf(point))
                detectedGate = matched?.startFinishGate
            }
            detectedGate
        }
        return LapLogic.hasCrossedGpsGate(previousPoint, point, gateToUse)
    }

    private fun finalizeCurrentLap(crossingTimeMs: Long) {
        if (activeLapBuffer.isEmpty()) return
        val startTimeMs = lapStartTimeMs ?: return

        val newLap = LapLogic.buildLap(
            currentSessionId,
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

    fun toggleEditMode() {
        _isEditMode.value = !_isEditMode.value
    }

    fun makeMain(index: Int) {
        val list = _widgetLayout.value.toMutableList()
        if (index in 1 until list.size) {
            val temp = list[0]
            list[0] = list[index]
            list[index] = temp
            _widgetLayout.value = list
            saveLayout()
        }
    }

    // Writes the current order of the widgets to the database as one line of text
    private fun saveLayout() {
        val text = _widgetLayout.value.joinToString(",") { it.content.name }
        viewModelScope.launch(Dispatchers.IO) {
            widgetLayoutDao?.saveLayout(WidgetLayoutEntity(id = "hud", widgets = text))
        }
    }

    // Reads the saved line when the app starts and rebuilds the three widgets from it
    private fun loadSavedLayout() {
        viewModelScope.launch(Dispatchers.IO) {
            val saved = widgetLayoutDao?.getLayout() ?: return@launch
            // turn each saved name back into a widget type, skipping names that no longer exist
            val contents = saved.widgets.split(",").mapNotNull { name ->
                WidgetContent.entries.find { it.name == name }
            }
            // a good layout has exactly 3 different widgets, otherwise keep the default
            if (contents.size == 3 && contents.toSet().size == 3) {
                _widgetLayout.value = contents.map { content ->
                    WidgetConfig(id = "widget_${content.name}", content = content, title = content.name)
                }
            }
        }
    }

    fun replaceWidget(id: String, newContent: WidgetContent) {
        if (_widgetLayout.value.any { it.content == newContent }) return

        _widgetLayout.update { list ->
            list.map { widget ->
                if (widget.id == id) widget.copy(content = newContent, title = newContent.name) else widget
            }
        }
        saveLayout()
    }

    override fun onCleared() {
        super.onCleared()
        hudRepository.stopListening()
    }
}
