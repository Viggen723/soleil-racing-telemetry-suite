package com.example.soleilracingproject.features.hud.ui.widget

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import kotlin.math.roundToInt

@Composable
fun LineChart(
    values: List<Float>,
    lineColor: Color,
    modifier: Modifier = Modifier,
    segmentColors: List<Color> = emptyList(),
    maxPoints: Int = 200,
    xLabels: List<String> = emptyList(),
    yAxisTitle: String = "",
    xAxisTitle: String = ""
) {
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.outline
    val labelStyle = MaterialTheme.typography.labelSmall


    val maxValue = if (values.isEmpty()) 0f else values.max()
    val stepSize = (ceil(maxValue / 4f / 10f) * 10f).coerceAtLeast(10f)
    val topValue = stepSize * 4f

    Column(modifier = modifier) {

        if (yAxisTitle.isNotEmpty()) {
            Text(
                text = yAxisTitle,
                style = labelStyle,
                color = labelColor,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }

        Row(modifier = Modifier.fillMaxWidth().weight(1f)) {

            Column(
                modifier = Modifier.fillMaxHeight().width(32.dp).padding(end = 4.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                for (k in 4 downTo 0) {
                    Text(
                        text = (stepSize * k).roundToInt().toString(),
                        style = labelStyle,
                        color = labelColor
                    )
                }
            }

            Canvas(
                modifier = Modifier.weight(1f).fillMaxHeight().padding(end = 6.dp)
            ) {
                // The grid
                for (k in 0..4) {
                    val y = size.height * k / 4f
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                val tallLines = if (xLabels.size >= 2) xLabels.size else 2
                for (k in 0 until tallLines) {
                    val x = size.width * k / (tallLines - 1)
                    drawLine(
                        color = gridColor,
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // The line
                if (values.size < 2) return@Canvas

                val stepX = size.width / (maxPoints - 1)


                val points = values.indices.map { i ->

                    val x = size.width - (values.size - 1 - i) * stepX
                    val fraction = (values[i] / topValue).coerceIn(0f, 1f)
                    val y = size.height - fraction * size.height
                    Offset(x, y)
                }


                val useSegmentColors = segmentColors.size == values.size


                for (i in 1 until points.size) {
                    val pieceColor = if (useSegmentColors) segmentColors[i] else lineColor
                    drawLine(
                        color = pieceColor,
                        start = points[i - 1],
                        end = points[i],
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }


                val dotColor = if (useSegmentColors) segmentColors.last() else lineColor
                drawCircle(color = dotColor, radius = 4.dp.toPx(), center = points.last())
            }
        }

        if (xLabels.isNotEmpty()) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.width(32.dp))
                Row(
                    modifier = Modifier.weight(1f).padding(end = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    xLabels.forEach { label ->
                        Text(label, style = labelStyle, color = labelColor)
                    }
                }
            }
        }

        if (xAxisTitle.isNotEmpty()) {
            Text(
                text = xAxisTitle,
                style = labelStyle,
                color = labelColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}