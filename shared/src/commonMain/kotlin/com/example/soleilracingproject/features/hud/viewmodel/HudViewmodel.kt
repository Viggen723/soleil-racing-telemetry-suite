package com.example.soleilracingproject.features.hud.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.soleilracingproject.data.local.dao.WidgetLayoutDao
import com.example.soleilracingproject.data.local.entity.WidgetLayoutEntity
import com.example.soleilracingproject.features.hud.data.HudRepository
import com.example.soleilracingproject.features.hud.data.RecordingSessionOwner
import com.example.soleilracingproject.features.hud.ui.widget.*
import com.example.soleilracingproject.features.hud.ui.widget.widgetTypes.DeltaMode
import com.example.soleilracingproject.features.settings.data.SettingsRepository
import com.example.soleilracingproject.model.settings.AppSettings
import com.example.soleilracingproject.model.telemetry.TelemetryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// How many recent readings we remember for the graphs (200 readings = about 20 seconds)
private const val HISTORY_SIZE = 200
// If no packet arrives for this long, we say the logger is disconnected
private const val LOGGER_TIMEOUT_MS = 2000L

class HudViewModel(
    private val hudRepository: HudRepository,
    settingsRepository: SettingsRepository,
    private val recordingOwner: RecordingSessionOwner,
    private val widgetLayoutDao: WidgetLayoutDao? = null,
) : ViewModel() {
    val settings = settingsRepository.settings.stateIn(
        viewModelScope, SharingStarted.Eagerly, AppSettings()
    )

    private val _telemetryState = MutableStateFlow<TelemetryPoint?>(null)
    val telemetryState: StateFlow<TelemetryPoint?> = _telemetryState.asStateFlow()

    private val _isEditMode = MutableStateFlow(false)
    val isEditMode: StateFlow<Boolean> = _isEditMode.asStateFlow()

    val isRecording = recordingOwner.isRecording
    val recordingState = recordingOwner.recordingState
    val lapsState = recordingOwner.lapsState
    val recordingSeconds = recordingOwner.recordingSeconds
    val lapDeltas = recordingOwner.lapDeltas

    private val _recentPoints = MutableStateFlow<List<TelemetryPoint>>(emptyList())
    val recentPoints: StateFlow<List<TelemetryPoint>> = _recentPoints.asStateFlow()
    private val _deltaMode = MutableStateFlow(DeltaMode.BEST_LAP)
    val deltaMode: StateFlow<DeltaMode> = _deltaMode.asStateFlow()
    private val _widgetLayout = MutableStateFlow(
        listOf(
            WidgetConfig(id = "speed_default", content = WidgetContent.SPEEDOMETER, title = "Speedometer"),
            WidgetConfig(id = "g_force_default", content = WidgetContent.G_FORCE_CIRCLE, title = "G-Force"),
            WidgetConfig(id = "recording_button_default", content = WidgetContent.RECORDING_BUTTON, title = "Recording Button")
        )
    )
    // True while telemetry packets keep arriving from the logger
    private val _isLoggerConnected = MutableStateFlow(false)
    val isLoggerConnected: StateFlow<Boolean> = _isLoggerConnected.asStateFlow()
    private var lastPacketTimeMs = 0L
    val widgetLayout: StateFlow<List<WidgetConfig>> = _widgetLayout.asStateFlow()

    init {
        loadSavedLayout()
        watchLoggerConnection()
        viewModelScope.launch {
            hudRepository.observeTelemetry().collect { point ->
                processTelemetryPoint(point)
            }
        }
    }

    fun reconnectUdp() = hudRepository.reconnectUdp()

    private fun processTelemetryPoint(point: TelemetryPoint) {
        lastPacketTimeMs = kotlin.time.Clock.System.now().toEpochMilliseconds()
        _isLoggerConnected.value = true
        _telemetryState.value = point
        _recentPoints.value = (_recentPoints.value + point).takeLast(HISTORY_SIZE)
    }

    // Twice a second, check whether packets stopped arriving
    private fun watchLoggerConnection() {
        viewModelScope.launch {
            while (true) {
                val now = kotlin.time.Clock.System.now().toEpochMilliseconds()
                if (now - lastPacketTimeMs > LOGGER_TIMEOUT_MS) {
                    _isLoggerConnected.value = false
                }
                delay(500)
            }
        }
    }

    fun startRecording() = recordingOwner.startRecording(isLoggerConnected.value)

    fun stopRecording() {
        recordingOwner.activeRecordingId.value?.let(recordingOwner::stopRecording)
    }

    fun nextDeltaMode() {
        _deltaMode.value = DeltaMode.entries[(_deltaMode.value.ordinal + 1) % DeltaMode.entries.size]
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
}
