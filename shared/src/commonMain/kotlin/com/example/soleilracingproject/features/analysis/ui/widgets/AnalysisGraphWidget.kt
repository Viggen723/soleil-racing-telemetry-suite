package com.example.soleilracingproject.features.analysis.ui.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.soleilracingproject.features.analysis.ui.TelemetryMetric
import com.example.soleilracingproject.features.analysis.ui.widgets.graph.GraphCanvasArea
import com.example.soleilracingproject.features.analysis.ui.widgets.graph.GraphHeaderRow
import com.example.soleilracingproject.features.analysis.ui.widgets.graph.GraphTooltipBadge
import com.example.soleilracingproject.features.analysis.ui.widgets.graph.GraphXAxisLabels
import com.example.soleilracingproject.features.analysis.ui.widgets.graph.GraphYAxisLabels
import com.example.soleilracingproject.model.analysis.Lap

@Composable
fun AnalysisGraphWidget(
    lap: Lap?,
    metric: TelemetryMetric,
    selectedIndex: Int?,
    onPointSelected: (Int?) -> Unit,
    modifier: Modifier = Modifier,
    comparisonLap: Lap? = null,
) {
    val telemetry = lap?.telemetryPoints ?: emptyList()
    val compTelemetry = comparisonLap?.telemetryPoints ?: emptyList()
    val labelColor = MaterialTheme.colorScheme.outline
    val labelStyle = MaterialTheme.typography.labelSmall

    if (telemetry.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("No Telemetry Data Available", style = labelStyle, color = labelColor)
        }
        return
    }

    val primaryMax = telemetry.maxOfOrNull { metric.extractValue(it) } ?: 100f
    val primaryMin = telemetry.minOfOrNull { metric.extractValue(it) } ?: 0f
    val compMax = compTelemetry.maxOfOrNull { metric.extractValue(it) } ?: primaryMax
    val compMin = compTelemetry.minOfOrNull { metric.extractValue(it) } ?: primaryMin

    val maxValue = maxOf(primaryMax, compMax)
    val minValue = minOf(primaryMin, compMin)
    val valueRange = (maxValue - minValue).coerceAtLeast(0.1f)

    val startPointTime = telemetry.firstOrNull()?.time?.toLong() ?: 0L
    val endPointTime = telemetry.lastOrNull()?.time?.toLong() ?: 0L
    val totalDurationMs = (endPointTime - startPointTime).coerceAtLeast(0L)

    val yAxisWidth = 48.dp
    val topPaddingDp = 12.dp
    val bottomPaddingDp = 12.dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(12.dp),
            )
            .padding(12.dp),
    ) {
        GraphHeaderRow(
            metric = metric,
            selectedPoint = selectedIndex?.let { telemetry.getOrNull(it) },
            comparisonPoint = selectedIndex?.let { idx ->
                if (compTelemetry.isNotEmpty()) {
                    val compIdx = (idx.toFloat() / (telemetry.size - 1).coerceAtLeast(1) * (compTelemetry.size - 1)).toInt().coerceIn(0, compTelemetry.size - 1)
                    compTelemetry.getOrNull(compIdx)
                } else null
            },
            startPointTime = startPointTime,
            maxValue = maxValue,
            minValue = minValue,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            GraphYAxisLabels(
                metric = metric,
                maxValue = maxValue,
                minValue = minValue,
                width = yAxisWidth,
                topPadding = topPaddingDp,
                bottomPadding = bottomPaddingDp,
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                GraphCanvasArea(
                    telemetry = telemetry,
                    compTelemetry = compTelemetry,
                    metric = metric,
                    minValue = minValue,
                    valueRange = valueRange,
                    selectedIndex = selectedIndex,
                    onPointSelected = onPointSelected,
                    topPaddingDp = topPaddingDp,
                    bottomPaddingDp = bottomPaddingDp,
                )

                GraphTooltipBadge(
                    telemetry = telemetry,
                    compTelemetry = compTelemetry,
                    metric = metric,
                    selectedIndex = selectedIndex,
                    startPointTime = startPointTime,
                    topPaddingDp = topPaddingDp,
                )
            }
        }

        GraphXAxisLabels(
            totalDurationMs = totalDurationMs,
            yAxisWidth = yAxisWidth,
        )
    }
}