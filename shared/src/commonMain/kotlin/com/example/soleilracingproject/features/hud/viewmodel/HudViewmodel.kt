// commonMain/kotlin/com/example/soleilracingproject/features/hud/ui/HudViewModel.kt
package com.example.soleilracingproject.features.hud.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.soleilracingproject.features.hud.ui.widget.*
import com.example.soleilracingproject.model.telemetry.TelemetryPoint
import com.example.soleilracingproject.remote.udp.UdpSocketListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.random.Random

class HudViewModel(
    private val udpListener: UdpSocketListener,
    private val parseUdpData: (ByteArray) -> TelemetryPoint?
) : ViewModel() {

    private val _telemetryState = MutableStateFlow<TelemetryPoint?>(null)
    val telemetryState: StateFlow<TelemetryPoint?> = _telemetryState.asStateFlow()

    private val _isEditMode = MutableStateFlow(false)
    val isEditMode: StateFlow<Boolean> = _isEditMode.asStateFlow()

    private val _widgetLayout = MutableStateFlow(
        listOf(
            WidgetConfig(
                id = "speed_default",
                content = WidgetContent.SPEEDOMETER,
                title = "Speedometer",
                span = WidgetSpan.FULL,
                heightDp = 160
            ),
            WidgetConfig(
                id = "alt_default",
                content = WidgetContent.ALTITUDE,
                title = "Altitude",
                span = WidgetSpan.HALF,
                heightDp = 160
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
                    _telemetryState.value = parsedPoint
                }
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