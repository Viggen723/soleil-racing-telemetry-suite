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
        WidgetContent.GRAPH -> GraphWidget(telemetry = telemetry)
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