package com.example.soleilracingproject.features.analysis.ui.widgets.graph

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.soleilracingproject.features.analysis.ui.TelemetryMetric
import com.example.soleilracingproject.model.telemetry.TelemetryPoint

@Composable
fun GraphHeaderRow(
    metric: TelemetryMetric,
    selectedPoint: TelemetryPoint?,
    comparisonPoint: TelemetryPoint?,
    startPointTime: Long,
    maxValue: Float,
    minValue: Float,
    modifier: Modifier = Modifier,
) {
    val labelStyle = MaterialTheme.typography.labelSmall
    val primaryColor = Color(0xFFD71344)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = metric.title.uppercase(),
            style = labelStyle.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
        )

        if (selectedPoint != null) {
            val valStr = metric.formatValue(metric.extractValue(selectedPoint))
            val ptTimeMs = (selectedPoint.time.toLong() - startPointTime).coerceAtLeast(0L)

            if (comparisonPoint != null) {
                val diff = metric.extractValue(selectedPoint) - metric.extractValue(comparisonPoint)
                val diffStr = if (diff >= 0) "+${metric.formatValue(diff)}" else metric.formatValue(diff)

                Text(
                    text = "($diffStr)",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = primaryColor,
                )
            } else {
                Text(
                    text = "$valStr @ ${formatGraphTime(ptTimeMs)}",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = primaryColor,
                )
            }
        } else {
            val maxStr = metric.formatValue(maxValue)
            val minStr = metric.formatValue(minValue)
            Text(
                text = "MAX: $maxStr  |  MIN: $minStr",
                style = labelStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}