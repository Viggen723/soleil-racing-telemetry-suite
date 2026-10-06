package com.example.soleilracingproject.features.hud.ui.widget.widgetTypes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import com.example.soleilracingproject.model.telemetry.TelemetryPoint
import kotlin.math.roundToInt

@Composable
fun AltitudeWidget(telemetry: TelemetryPoint) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "ALTITUDE",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary
        )
        Text(
            text = "${telemetry.altitude.roundToInt()}",
            style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "METERS",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.outline
        )
    }
}