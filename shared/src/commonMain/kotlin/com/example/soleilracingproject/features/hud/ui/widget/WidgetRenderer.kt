package com.example.soleilracingproject.features.hud.ui.widget

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.soleilracingproject.features.hud.ui.widget.widgetTypes.AltitudeWidget
import com.example.soleilracingproject.features.hud.ui.widget.widgetTypes.SpeedometerWidget
import com.example.soleilracingproject.features.hud.ui.widget.widgetTypes.GraphWidget
import com.example.soleilracingproject.features.hud.ui.widget.widgetTypes.StartButtonWidget
import com.example.soleilracingproject.model.telemetry.TelemetryPoint
import com.example.soleilracingproject.util.tools.ConversionTools.formatOneDecimal
import com.example.soleilracingproject.features.hud.ui.widget.widgetTypes.GForceCircleWidget
import com.example.soleilracingproject.features.hud.ui.widget.widgetTypes.DeltaWidget
import com.example.soleilracingproject.util.tools.lapLogic.LapDeltas
import com.example.soleilracingproject.features.hud.ui.widget.widgetTypes.DeltaMode
import com.example.soleilracingproject.features.hud.ui.widget.widgetTypes.LapTimesWidget
import com.example.soleilracingproject.model.analysis.Lap

@Composable
private fun LoggerDisconnected(title: String) {
    Box(modifier = Modifier.fillMaxSize()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp)
        )
        Text(
            text = "LOGGER DISCONNECTED",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}
@Composable
fun WidgetRenderer(
    content: WidgetContent,
    telemetry: TelemetryPoint?,
    isRecording: Boolean,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    lapDeltas: LapDeltas = LapDeltas(),
    recentPoints: List<TelemetryPoint> = emptyList(),
    laps: List<Lap> = emptyList(),
    deltaMode: DeltaMode = DeltaMode.BEST_LAP,
    onNextDeltaMode: () -> Unit = {},
    modifier: Modifier = Modifier,
    speedometerMax: Float = 180f,
) {
    when (content) {
        WidgetContent.SPEEDOMETER -> SpeedometerWidget(telemetry = telemetry, maxSpeed = speedometerMax)
        WidgetContent.ALTITUDE -> AltitudeWidget(telemetry = telemetry)
        WidgetContent.LAP_DELTA -> DeltaWidget(lapDeltas, isRecording, deltaMode, onNextDeltaMode)
        WidgetContent.LAP_TIMES -> LapTimesWidget(laps, isRecording)
        WidgetContent.SPEED_GRAPH -> {
            if (telemetry != null) {
                GraphWidget(telemetry, title = "SPEED GRAPH", unit = "mph", value = telemetry.speed, history = recentPoints.map { it.speed }, colorByChange = true)
            } else {
                LoggerDisconnected("SPEED GRAPH")
            }
        }
        WidgetContent.G_FORCE_CIRCLE -> {
            if (telemetry != null) {
                GForceCircleWidget(telemetry, recentPoints)
            } else {
                LoggerDisconnected("G-FORCE")
            }
        }
        WidgetContent.RECORDING_BUTTON -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                StartButtonWidget(
                    isRecording = isRecording,
                    onToggleRecording = { if (isRecording) onStopRecording() else onStartRecording() },
                    modifier = Modifier.fillMaxWidth().height(64.dp)
                )
            }
        }
    }
}
