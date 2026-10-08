// commonMain/kotlin/com/example/soleilracingproject/features/hud/ui/HudViewModel.kt
package com.example.soleilracingproject.features.hud.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.soleilracingproject.features.hud.ui.widget.*
import com.example.soleilracingproject.model.telemetry.TelemetryPoint
import com.example.soleilracingproject.data.remote.udp.UdpSocketListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.math.sin
import kotlinx.coroutines.delay
private const val USE_FAKE_DATA = false

class HudViewModel(
    private val udpListener: UdpSocketListener,
    private val parseUdpData: (ByteArray) -> TelemetryPoint?
) : ViewModel() {

    private val _telemetryState = MutableStateFlow<TelemetryPoint?>(null)
    val telemetryState: StateFlow<TelemetryPoint?> = _telemetryState.asStateFlow()

    private val _isEditMode = MutableStateFlow(false)
    val isEditMode: StateFlow<Boolean> = _isEditMode.asStateFlow()

    // Sets the intial widget layout
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

        viewModelScope.launch(Dispatchers.Default) {
            udpListener.observePackets().collect { rawBytes ->
                val parsedPoint = parseUdpData(rawBytes)
                if (parsedPoint != null) {
                    _telemetryState.value = parsedPoint
                }
            }
        }
    }

    private fun startFakeData() {
        viewModelScope.launch(Dispatchers.Default) {
            var t = 0
            while (true) {
                _telemetryState.value = TelemetryPoint(
                    time = t,
                    accelX = 0f, accelY = 0f, accelZ = 0f,
                    latitude = 0f, longitude = 0f,
                    speed = 80f + 50f * sin(t / 15f),   // this wave will be between 30 and 130 on the y axis
                    qw = 1f, qi = 0f, qj = 0f, qk = 0f,
                    altitude = 20f + 5f * sin(t / 40f)
                )
                t++
                delay(100)   // wait 0.1 second, then send the next reading
            }
        }
    }

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
        // RandomID generated with Kotlin's random
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