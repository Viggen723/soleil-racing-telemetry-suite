package com.example.soleilracingproject.features.analysis.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.soleilracingproject.model.telemetry.TelemetryPoint

// only on Android for now
expect val isMapAvailable: Boolean

// A real map with the lap drawn on it.
// recenterCount goes up by one each time the user presses the recenter button.
@Composable
expect fun TrackMap(
    lapPoints: List<TelemetryPoint>,
    comparisonPoints: List<TelemetryPoint>,
    selectedIndex: Int?,
    onPointSelected: (Int?) -> Unit,
    recenterCount: Int,
    modifier: Modifier = Modifier
)