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

private const val USE_FAKE_DATA = true


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

    private val activeLapBuffer = mutableListOf<TelemetryPoint>()
    private val completedLaps = mutableListOf<Lap>()
    private var sessionStartTime: Long = 0L
    private var currentSessionId: String = ""
    private var previousPoint: TelemetryPoint? = null
    private var lapStartTimeMs: Long? = null
    // TODO: Supply the fixed TrackLayout finish gate and implement segment-crossing detection.
    private val finishGate: GpsGate? = null
    
    private val _widgetLayout = MutableStateFlow(
        listOf(
            WidgetConfig(
                id = "speed_default",
                content = WidgetContent.SPEEDOMETER,
                title = "Speedometer",
                span = WidgetSpan.HALF,
                heightDp = 160
            ),
            WidgetConfig(
                id = "alt_default",
                content = WidgetContent.ALTITUDE,
                title = "Altitude",
                span = WidgetSpan.HALF,
                heightDp = 160
            ),
            WidgetConfig(
                id = "speed_graph_default",
                content = WidgetContent.SPEED_GRAPH,
                title = "Speed Graph",
                span = WidgetSpan.FULL,
                heightDp = 240
            ),
            WidgetConfig(
                id = "g_force_graph_default",
                content = WidgetContent.G_FORCE_GRAPH,
                title = "G-Force Graph",
                span = WidgetSpan.FULL,
                heightDp = 240
            ),
            WidgetConfig(
                id = "recording_button_default",
                content = WidgetContent.RECORDING_BUTTON,
                title = "Recording Button",
                span = WidgetSpan.HALF,
                heightDp = 120
            )
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
                val fakePoint = TelemetryPoint(
                    time = t * 100,                    // milliseconds
                    accelX = cos(t / 15f),             // swings between -1 and 1
                    accelY = 0f,
                    accelZ = 0f,
                    latitude = 35.68f + 0.0009f * sin(t * 0.0105f),    // a circle about 100 m wide,
                    longitude = 139.69f + 0.0011f * cos(t * 0.0105f),  // one lap every 60 seconds
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

        activeLapBuffer.clear()
        completedLaps.clear()
        previousPoint = null
        lapStartTimeMs = null
        sessionStartTime = kotlin.time.Clock.System.now().toEpochMilliseconds()
        currentSessionId = "session_${Random.nextLong().toString(16)}"
        _isRecording.value = true
        _recordingState.value = RecordingState.ARMED
    }

    fun stopRecording() {
        if (_recordingState.value == RecordingState.IDLE) return
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
        activeLapBuffer.clear()
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

    fun removeWidget(id: String) {
        _widgetLayout.update { list -> list.filterNot { it.id == id } }
    }

    override fun onCleared() {
        super.onCleared()
        udpListener.stopListening()
    }
}
