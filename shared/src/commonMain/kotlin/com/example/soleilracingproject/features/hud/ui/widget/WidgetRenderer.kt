package com.example.soleilracingproject.features.hud.ui.widget

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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

// Is accelX in m/s² or g ?? (QN for Adam)
private const val ACCEL_TO_G = 1f
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
    modifier: Modifier = Modifier
) {
    when (content) {
        WidgetContent.SPEEDOMETER -> SpeedometerWidget(telemetry = telemetry)
        WidgetContent.ALTITUDE -> AltitudeWidget(telemetry = telemetry)
        WidgetContent.SPEED_GRAPH -> {
            if (telemetry != null) {
                 GraphWidget(telemetry, title = "SPEED GRAPH", unit = "km/h", value = telemetry.speed, colorByChange = true)
                     } else {
                         LoggerDisconnected("SPEED GRAPH")
                     }
                 }
        WidgetContent.G_FORCE_GRAPH -> {
            if (telemetry != null) {
                GraphWidget(telemetry, title = "G-FORCE GRAPH", unit = "g",value = telemetry.accelX * ACCEL_TO_G, colorBySign = true, centerOnZero = true, roundTo = 0.5f, formatLabel = { it.formatOneDecimal() })
                     } else {
                         LoggerDisconnected("G-FORCE GRAPH")
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
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}