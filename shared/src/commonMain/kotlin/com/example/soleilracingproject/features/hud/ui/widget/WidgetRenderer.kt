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
import com.example.soleilracingproject.features.hud.ui.widget.widgetTypes.SpeedGraphWidget
import com.example.soleilracingproject.model.telemetry.TelemetryPoint

@Composable
fun WidgetRenderer(
    content: WidgetContent,
    telemetry: TelemetryPoint?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize().padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        if (telemetry == null) {
            Text(
                text = "LOGGER DISCONNECTED",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline
            )
            return@Box
        }

        when (content) {
            WidgetContent.SPEEDOMETER -> SpeedometerWidget(telemetry)
            WidgetContent.ALTITUDE -> AltitudeWidget(telemetry)
            WidgetContent.SPEEDGRAPH -> SpeedGraphWidget(telemetry)
        }
    }
}