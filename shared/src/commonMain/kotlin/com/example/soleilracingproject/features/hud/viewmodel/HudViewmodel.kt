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
import com.example.soleilracingproject.util.tools.lapLogic.LapLogic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.random.Random


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

    private val activeLapBuffer = mutableListOf<TelemetryPoint>()
    private val completedLaps = mutableListOf<Lap>()
    private var sessionStartTime: Long = 0L
    private var currentSessionId: String = ""
    
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
                content = WidgetContent.SPEEDGRAPH,
                title = "Speed Graph",
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
        startUdpListening()
    }

    private fun startUdpListening() {
        viewModelScope.launch(Dispatchers.IO) {
            udpListener.beginListening(4120)
        }

        viewModelScope.launch(Dispatchers.Default) {
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

        if (_isRecording.value) {
            activeLapBuffer.add(point)

            if (hasCrossedGpsGate(point)) {
                finalizeCurrentLap()
            }
        }
    }

    fun startRecording() {
        if (_isRecording.value) return

        activeLapBuffer.clear()
        completedLaps.clear()
        sessionStartTime = kotlin.time.Clock.System.now().toEpochMilliseconds()
        currentSessionId = "session_${Random.nextLong().toString(16)}"
        _isRecording.value = true
    }

    fun stopRecording() {
        if (!_isRecording.value) return
        _isRecording.value = false

        // Flush any remaining active buffer points as the final lap
        if (activeLapBuffer.isNotEmpty()) {
            completedLaps.add(
                LapLogic.buildLap(currentSessionId, completedLaps.size + 1, activeLapBuffer.toList())
            )
            activeLapBuffer.clear()
        }

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
            lapDao?.insertLaps(completedLaps.map { it.toEntity() })
        }
    }

    private fun hasCrossedGpsGate(point: TelemetryPoint): Boolean {
        return LapLogic.hasCrossedGpsGate(point)
    }

    private fun finalizeCurrentLap() {
        if (activeLapBuffer.isEmpty()) return

        val newLap = LapLogic.buildLap(currentSessionId, completedLaps.size + 1, activeLapBuffer.toList())
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