package com.example.soleilracingproject.features.analysis.ui.widgets.graph

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.soleilracingproject.features.analysis.ui.TelemetryMetric

@Composable
fun GraphYAxisLabels(
    metric: TelemetryMetric,
    maxValue: Float,
    minValue: Float,
    width: Dp,
    topPadding: Dp,
    bottomPadding: Dp,
    modifier: Modifier = Modifier,
) {
    val labelColor = MaterialTheme.colorScheme.outline
    val labelStyle = MaterialTheme.typography.labelSmall

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(width)
            .padding(end = 8.dp, top = topPadding, bottom = bottomPadding),
    ) {
        Text(
            text = metric.formatAxisLabel(maxValue),
            style = labelStyle,
            color = labelColor,
            modifier = Modifier.align(Alignment.TopEnd),
        )
        Text(
            text = metric.formatAxisLabel((maxValue + minValue) / 2f),
            style = labelStyle,
            color = labelColor,
            modifier = Modifier.align(Alignment.CenterEnd),
        )
        Text(
            text = metric.formatAxisLabel(minValue),
            style = labelStyle,
            color = labelColor,
            modifier = Modifier.align(Alignment.BottomEnd),
        )
    }
}