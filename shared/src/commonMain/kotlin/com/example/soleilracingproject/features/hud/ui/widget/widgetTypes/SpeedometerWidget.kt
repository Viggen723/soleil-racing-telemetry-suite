package com.example.soleilracingproject.features.hud.ui.widget.widgetTypes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.soleilracingproject.model.telemetry.TelemetryPoint
import com.example.soleilracingproject.util.tools.ConversionTools.formatOneDecimal

@Composable
fun SpeedometerWidget(telemetry: TelemetryPoint) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "SPEED",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary
        )
        Text(
            text = telemetry.speed.formatOneDecimal(),
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "KM/H",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.outline
        )
    }
}