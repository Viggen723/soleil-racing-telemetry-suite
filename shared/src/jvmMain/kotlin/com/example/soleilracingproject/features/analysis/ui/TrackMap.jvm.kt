package com.example.soleilracingproject.features.analysis.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.soleilracingproject.model.telemetry.TelemetryPoint

actual val isMapAvailable: Boolean = false

@Composable
actual fun TrackMap(
    lapPoints: List<TelemetryPoint>,
    comparisonPoints: List<TelemetryPoint>,
    selectedIndex: Int?,
    onPointSelected: (Int?) -> Unit,
    recenterCount: Int,
    modifier: Modifier
) {
    // No map on this platform yet. The widget draws its own track instead.
}