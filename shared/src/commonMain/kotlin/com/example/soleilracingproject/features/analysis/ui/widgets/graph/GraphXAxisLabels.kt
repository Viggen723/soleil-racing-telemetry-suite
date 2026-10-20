package com.example.soleilracingproject.features.analysis.ui.widgets.graph

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun GraphXAxisLabels(
    totalDurationMs: Long,
    yAxisWidth: Dp,
    modifier: Modifier = Modifier,
) {
    val labelColor = MaterialTheme.colorScheme.outline
    val labelStyle = MaterialTheme.typography.labelSmall

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
    ) {
        Spacer(modifier = Modifier.width(yAxisWidth))
        Box(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = "0.0s",
                style = labelStyle,
                color = labelColor,
                modifier = Modifier.align(Alignment.CenterStart),
            )
            Text(
                text = formatGraphTime(totalDurationMs / 2),
                style = labelStyle,
                color = labelColor,
                modifier = Modifier.align(Alignment.Center),
            )
            Text(
                text = formatGraphTime(totalDurationMs),
                style = labelStyle,
                color = labelColor,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }
    }
}