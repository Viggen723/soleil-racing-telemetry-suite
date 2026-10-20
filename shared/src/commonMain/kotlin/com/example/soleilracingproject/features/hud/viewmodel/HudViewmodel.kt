import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.soleilracingproject.data.local.dao.LapDao
import com.example.soleilracingproject.data.local.dao.SessionDao
import com.example.soleilracingproject.data.mapper.toEntity
import com.example.soleilracingproject.features.hud.ui.widget.*
import com.example.soleilracingproject.model.telemetry.TelemetryPoint
import com.example.soleilracingproject.data.remote.udp.UdpSocketListener
import com.example.soleilracingproject.model.analysis.Lap
import com.example.soleilracingproject.model.analysis.Session
import com.example.soleilracingproject.model.telemetry.GpsGate
import com.example.soleilracingproject.util.tools.lapLogic.LapLogic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.math.sin
import kotlin.math.cos
import kotlinx.coroutines.delay
import com.example.soleilracingproject.util.tools.lapLogic.DeltaCalculator
import com.example.soleilracingproject.util.tools.lapLogic.LapDeltas
import com.example.soleilracingproject.features.hud.ui.widget.widgetTypes.DeltaMode

private const val USE_FAKE_DATA = true
// How many recent readings we remember for the graphs (200 readings = about 20 seconds)
private const val HISTORY_SIZE = 200

enum class RecordingState {
    IDLE,
    ARMED,
    RECORDING_LAP
}

class HudViewModel(
    private val udpListener: UdpSocketListener,
    private val parseUdpData: (ByteArray) -> TelemetryPoint?,
    private val sessionDao: SessionDao? = null,
    private val lapDao: LapDao? = null

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
    // Which delta the lap delta widget shows (best lap or last lap)
    private val _deltaMode = MutableStateFlow(DeltaMode.BEST_LAP)
    val deltaMode: StateFlow<DeltaMode> = _deltaMode.asStateFlow()
    // The laps finished in this recording. The lap times widget reads this.
    private val _lapsState = MutableStateFlow<List<Lap>>(emptyList())
    val lapsState: StateFlow<List<Lap>> = _lapsState.asStateFlow()

    private val activeLapBuffer = mutableListOf<TelemetryPoint>()
    private val completedLaps = mutableListOf<Lap>()
    private var sessionStartTime: Long = 0L
    private var currentSessionId: String = ""
    private var previousPoint: TelemetryPoint? = null
    private var lapStartTimeMs: Long? = null
    private val _lapDeltas = MutableStateFlow(LapDeltas())
    val lapDeltas: StateFlow<LapDeltas> = _lapDeltas.asStateFlow()
    // TODO: Supply the fixed TrackLayout finish gate and implement segment-crossing detection.

    // While testing with fake data, use a gate across the fake circle. Real data still gets null.
    private val finishGate: GpsGate? =
        if (USE_FAKE_DATA) GpsGate(lat1 = 35.68, lon1 = 139.6909, lat2 = 35.68, lon2 = 139.6913) else null

    private val _widgetLayout = MutableStateFlow(
        listOf(
            // The first one is the big main widget in the middle.
            // The next two are the smaller side widgets.
            WidgetConfig(id = "speed_default", content = WidgetContent.SPEEDOMETER, title = "Speedometer"),
            WidgetConfig(id = "g_force_default", content = WidgetContent.G_FORCE_CIRCLE, title = "G-Force"),
            WidgetConfig(id = "recording_button_default", content = WidgetContent.RECORDING_BUTTON, title = "Recording Button")
        )
    )
    val widgetLayout: StateFlow<List<WidgetConfig>> = _widgetLayout.asStateFlow()

    init {
        if (USE_FAKE_DATA) startFakeData() else startUdpListening()
    }

    private fun startUdpListening() {
        viewModelScope.launch(Dispatchers.IO) {
            udpListener.beginListening(4120)
        }

        // Keep buffer updates on the same dispatcher as the Start/Stop button calls.
        viewModelScope.launch {
            udpListener.observePackets().collect { rawBytes ->
                val parsedPoint = parseUdpData(rawBytes)
                if (parsedPoint != null) {
                    processTelemetryPoint(parsedPoint)
                }
            }
        }
    }

    private fun processTelemetryPoint(point: TelemetryPoint) {
        _telemetryState.value = point
        _recentPoints.value = (_recentPoints.value + point).takeLast(HISTORY_SIZE)

        when (_recordingState.value) {
            RecordingState.IDLE -> Unit
            RecordingState.ARMED -> {
                // Start only arms us; the first crossing starts the clock, not a completed lap.
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
                    // This crossing is also the next lap's starting timestamp.
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

    // Pretend logger: sends a fake reading 10 times a second
    private fun startFakeData() {
        viewModelScope.launch {
            var t = 0
            while (true) {
                val angle = t * 0.03f + 0.3f * sin(t / 100f)   // where the car is on the circle (a lap takes about 21 s)
                val fakePoint = TelemetryPoint(
                    time = t * 100,                    // milliseconds
                    accelX = cos(t / 15f),             // swings between -1 and 1
                    accelY = 0.8f * sin(t / 25f),      // wanders left and right
                    accelZ = 0f,
                    latitude = 35.68f + 0.0009f * sin(angle),
                    longitude = 139.69f + 0.0011f * cos(angle),
                    speed = 80f + 50f * sin(t / 15f),  // swings between 30 and 130
                    qw = 1f, qi = 0f, qj = 0f, qk = 0f,
                    altitude = 20f + 5f * sin(t / 40f)
                )
                processTelemetryPoint(fakePoint)
                t++
                delay(100)                             // wait 0.1 second
            }
        }
    }

    fun startRecording() {
        if (_recordingState.value != RecordingState.IDLE) return
        _lapDeltas.value = LapDeltas()          // empty = "nothing to compare yet
        activeLapBuffer.clear()
        completedLaps.clear()
        _lapsState.value = emptyList()
        previousPoint = null
        lapStartTimeMs = null
        sessionStartTime = kotlin.time.Clock.System.now().toEpochMilliseconds()
        currentSessionId = "session_${Random.nextLong().toString(16)}"
        _isRecording.value = true
        _recordingState.value = RecordingState.ARMED
    }

    fun stopRecording() {
        if (_recordingState.value == RecordingState.IDLE) return
        _lapDeltas.value = LapDeltas()
        _recordingState.value = RecordingState.IDLE
        _isRecording.value = false
        // Only crossed laps count for now; don't turn a stopped partial lap into a valid one.
        // TODO: Decide whether partial laps should later be stored as isDnf = true.
        activeLapBuffer.clear()
        previousPoint = null
        lapStartTimeMs = null

        val session = Session(
            id = currentSessionId,
            title = "Track Session",
            vehicleId = "2022 Kawasaki ZX-6R",
            track = "Lime Rock Park",
            dateEpoch = sessionStartTime,
            laps = completedLaps.toList()
        )

        // Persist session and laps to Room Database
        viewModelScope.launch(Dispatchers.IO) {
            sessionDao?.insertSession(session.toEntity())
            // Save this session's snapshot even if another Start clears the live lists.
            lapDao?.insertLaps(session.laps.map { it.toEntity() })
        }
    }

    // Called when the delta widget is tapped
    fun nextDeltaMode() {
        _deltaMode.value = DeltaMode.entries[(_deltaMode.value.ordinal + 1) % DeltaMode.entries.size]
    }

    private fun hasCrossedGpsGate(point: TelemetryPoint): Boolean {
        return LapLogic.hasCrossedGpsGate(previousPoint, point, finishGate)
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

    // Compare the lap we are driving now with the best lap and the last lap
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

    // Widget Modification functions

    fun toggleEditMode() {
        _isEditMode.value = !_isEditMode.value
    }

    fun moveWidget(index: Int, direction: Int) {
        val currentList = _widgetLayout.value.toMutableList()
        val targetIndex = index + direction
        if (targetIndex in 0 until currentList.size) {
            val item = currentList.removeAt(index)
            currentList.add(targetIndex, item)
            _widgetLayout.value = currentList
        }
    }

    fun toggleWidgetSpan(id: String) {
        _widgetLayout.update { list ->
            list.map { widget ->
                if (widget.id == id) {
                    val nextSpan = if (widget.span == WidgetSpan.HALF) WidgetSpan.FULL else WidgetSpan.HALF
                    widget.copy(span = nextSpan)
                } else widget
            }
        }
    }

    fun adjustWidgetHeight(id: String, deltaDp: Int) {
        _widgetLayout.update { list ->
            list.map { widget ->
                if (widget.id == id) {
                    val newHeight = (widget.heightDp + deltaDp).coerceIn(100, 400)
                    widget.copy(heightDp = newHeight)
                } else widget
            }
        }
    }

    fun addWidget(content: WidgetContent) {
        // If this widget is already on the screen, stop here
        val alreadyThere = _widgetLayout.value.any { it.content == content }
        if (alreadyThere) return

        val randomId = "widget_${Random.nextLong().toString(16)}"
        val newWidget = WidgetConfig(
            id = randomId,
            content = content,
            title = content.name,
            span = WidgetSpan.HALF,
            heightDp = 160
        )
        _widgetLayout.update { it + newWidget }
    }

    // Swap the widget in this position with the main (first) one
    fun makeMain(index: Int) {
        val list = _widgetLayout.value.toMutableList()
        if (index in 1 until list.size) {
            val temp = list[0]
            list[0] = list[index]
            list[index] = temp
            _widgetLayout.value = list
        }
    }

    // Put a different widget in this widget's spot
    fun replaceWidget(id: String, newContent: WidgetContent) {
        // a widget that is already on screen can't be picked again
        if (_widgetLayout.value.any { it.content == newContent }) return

        _widgetLayout.update { list ->
            list.map { widget ->
                if (widget.id == id) widget.copy(content = newContent, title = newContent.name) else widget
            }
        }
    }

    fun removeWidget(id: String) {
        _widgetLayout.update { list -> list.filterNot { it.id == id } }
    }

    override fun onCleared() {
        super.onCleared()
        udpListener.stopListening()
    }
}