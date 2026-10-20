package com.example.soleilracingproject.features.analysis.ui.widgets.graph

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.soleilracingproject.features.analysis.ui.TelemetryMetric
import com.example.soleilracingproject.model.telemetry.TelemetryPoint

@Composable
fun GraphTooltipBadge(
    telemetry: List<TelemetryPoint>,
    compTelemetry: List<TelemetryPoint>,
    metric: TelemetryMetric,
    selectedIndex: Int?,
    startPointTime: Long,
    topPaddingDp: Dp,
    modifier: Modifier = Modifier,
) {
    val labelStyle = MaterialTheme.typography.labelSmall
    val primaryColor = Color(0xFFD71344) // Soleil Red
    val compColor = Color(0xFF00E5FF) // Cyan

    selectedIndex?.let { index ->
        val selectedPt = telemetry.getOrNull(index)
        if (selectedPt != null) {
            val fraction = index.toFloat() / (telemetry.size - 1).coerceAtLeast(1)
            val valStr = metric.formatValue(metric.extractValue(selectedPt))
            val ptTimeMs = (selectedPt.time.toLong() - startPointTime).coerceAtLeast(0L)
            val timeStr = formatGraphTime(ptTimeMs)

            val compPt = if (compTelemetry.isNotEmpty()) {
                val compIdx = (fraction * (compTelemetry.size - 1)).toInt().coerceIn(0, compTelemetry.size - 1)
                compTelemetry.getOrNull(compIdx)
            } else null

            val horizontalBias = (fraction * 2f - 1f).coerceIn(-0.85f, 0.85f)

            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(top = topPaddingDp),
            ) {
                Surface(
                    modifier = Modifier
                        .align(BiasAlignment(horizontalBias = horizontalBias, verticalBias = -1f))
                        .padding(horizontal = 4.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.6f)),
                    shadowElevation = 4.dp,
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = valStr,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = primaryColor,
                            )
                            Text(
                                text = timeStr,
                                style = labelStyle,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (compPt != null) {
                            val compValStr = metric.formatValue(metric.extractValue(compPt))
                            val diff = metric.extractValue(selectedPt) - metric.extractValue(compPt)
                            val diffStr = if (diff >= 0) "+${metric.formatValue(diff)}" else metric.formatValue(diff)

                            Text(
                                text = "$compValStr ($diffStr)",
                                style = labelStyle.copy(fontWeight = FontWeight.Bold),
                                color = compColor,
                            )
                        }
                    }
                }
            }
        }
    }
}