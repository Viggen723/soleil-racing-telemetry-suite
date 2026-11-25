package com.example.soleilracingproject.features.customtrack.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.soleilracingproject.features.analysis.data.TrackRepository
import com.example.soleilracingproject.features.hud.data.HudRepository
import com.example.soleilracingproject.model.analysis.TrackLayout
import com.example.soleilracingproject.model.telemetry.GpsGate
import com.example.soleilracingproject.model.telemetry.TelemetryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class CustomTrackStep {
    WAITING_P1,
    WAITING_P2,
    READY_TO_SAVE
}

class CustomTrackViewModel(
    private val trackRepository: TrackRepository,
    private val hudRepository: HudRepository
) : ViewModel() {

    private val _currentTelemetry = MutableStateFlow<TelemetryPoint?>(null)
    val currentTelemetry = _currentTelemetry.asStateFlow()

    private val _step = MutableStateFlow(CustomTrackStep.WAITING_P1)
    val step = _step.asStateFlow()

    private val _point1 = MutableStateFlow<TelemetryPoint?>(null)
    val point1 = _point1.asStateFlow()

    private val _point2 = MutableStateFlow<TelemetryPoint?>(null)
    val point2 = _point2.asStateFlow()

    val customTracks = trackRepository.getAllTracks().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    init {
        viewModelScope.launch {
            hudRepository.observeTelemetry().collect { pt ->
                if (pt.latitude != 0f || pt.longitude != 0f) {
                    _currentTelemetry.value = pt
                }
            }
        }
    }

    fun setPoint1() {
        _currentTelemetry.value?.let { pt ->
            _point1.value = pt
            _step.value = CustomTrackStep.WAITING_P2
        }
    }

    fun setPoint2() {
        _currentTelemetry.value?.let { pt ->
            _point2.value = pt
            _step.value = CustomTrackStep.READY_TO_SAVE
        }
    }

    fun reset() {
        _point1.value = null
        _point2.value = null
        _step.value = CustomTrackStep.WAITING_P1
    }

    fun saveTrack(name: String, onComplete: () -> Unit) {
        val p1 = _point1.value ?: return
        val p2 = _point2.value ?: return

        val track = TrackLayout(
            id = "custom_${Random.nextLong().toString(16)}",
            trackName = name.ifBlank { "Custom Track" },
            startFinishGate = GpsGate(
                lat1 = p1.latitude.toDouble(),
                lon1 = p1.longitude.toDouble(),
                lat2 = p2.latitude.toDouble(),
                lon2 = p2.longitude.toDouble(),
                targetBearingDegrees = null
            )
        )

        viewModelScope.launch {
            trackRepository.saveTrack(track, isCustom = true)
            reset()
            onComplete()
        }
    }

    fun deleteTrack(id: String) {
        viewModelScope.launch {
            trackRepository.deleteTrack(id)
        }
    }
}
