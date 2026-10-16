package com.example.soleilracingproject.features.analysis.ui.widgets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.soleilracingproject.features.analysis.ui.TelemetryMetric
import com.example.soleilracingproject.model.analysis.Lap
import com.example.soleilracingproject.model.telemetry.TelemetryPoint
import kotlin.math.roundToInt

@Composable
fun AnalysisGraphWidget(
    lap: Lap?,
    metric: TelemetryMetric,
    selectedIndex: Int?,
    onPointSelected: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val telemetry = lap?.telemetryPoints ?: emptyList()
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

    val maxValue = telemetry.maxOfOrNull { metric.extractValue(it) } ?: 100f
    val minValue = telemetry.minOfOrNull { metric.extractValue(it) } ?: 0f
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

@Composable
private fun GraphHeaderRow(
    metric: TelemetryMetric,
    selectedPoint: TelemetryPoint?,
    startPointTime: Long,
    maxValue: Float,
    minValue: Float,
) {
    val labelStyle = MaterialTheme.typography.labelSmall

    Row(
        modifier = Modifier
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
            Text(
                text = "$valStr @ ${formatGraphTime(ptTimeMs)}",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.Cyan,
            )
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

@Composable
private fun GraphYAxisLabels(
    metric: TelemetryMetric,
    maxValue: Float,
    minValue: Float,
    width: Dp,
    topPadding: Dp,
    bottomPadding: Dp,
) {
    val labelColor = MaterialTheme.colorScheme.outline
    val labelStyle = MaterialTheme.typography.labelSmall

    Box(
        modifier = Modifier
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

@Composable
private fun GraphCanvasArea(
    telemetry: List<TelemetryPoint>,
    metric: TelemetryMetric,
    minValue: Float,
    valueRange: Float,
    selectedIndex: Int?,
    onPointSelected: (Int?) -> Unit,
    topPaddingDp: Dp,
    bottomPaddingDp: Dp,
) {
    val labelColor = MaterialTheme.colorScheme.outline

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(telemetry.size) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val xStep = size.width / (telemetry.size - 1).coerceAtLeast(1)
                        val index = (offset.x / xStep).toInt().coerceIn(0, telemetry.size - 1)
                        onPointSelected(index)
                    },
                    onDragEnd = { onPointSelected(null) },
                    onDragCancel = { onPointSelected(null) },
                ) { change, _ ->
                    val xStep = size.width / (telemetry.size - 1).coerceAtLeast(1)
                    val index = (change.position.x / xStep).toInt().coerceIn(0, telemetry.size - 1)
                    onPointSelected(index)
                }
            }
            .pointerInput(telemetry.size) {
                detectTapGestures { offset ->
                    val xStep = size.width / (telemetry.size - 1).coerceAtLeast(1)
                    val index = (offset.x / xStep).toInt().coerceIn(0, telemetry.size - 1)
                    onPointSelected(index)
                }
            },
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val topPx = topPaddingDp.toPx()
        val bottomPx = bottomPaddingDp.toPx()
        val usableHeight = (canvasHeight - topPx - bottomPx).coerceAtLeast(1f)

        val xStep = canvasWidth / (telemetry.size - 1).coerceAtLeast(1)

        val pixelPoints = telemetry.mapIndexed { index, point ->
            val x = index * xStep
            val value = metric.extractValue(point)
            val percentage = (value - minValue) / valueRange
            val y = canvasHeight - bottomPx - (percentage * usableHeight)
            Offset(x, y)
        }

        val gridYs = listOf(topPx, topPx + usableHeight / 2f, canvasHeight - bottomPx)
        for (y in gridYs) {
            drawLine(
                color = labelColor.copy(alpha = 0.15f),
                start = Offset(0f, y),
                end = Offset(canvasWidth, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f),
            )
        }

        val gridXs = listOf(0f, canvasWidth / 2f, canvasWidth)
        for (x in gridXs) {
            drawLine(
                color = labelColor.copy(alpha = 0.15f),
                start = Offset(x, topPx),
                end = Offset(x, canvasHeight - bottomPx),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f),
            )
        }

        if (pixelPoints.size >= 2) {
            val fillPath = Path().apply {
                moveTo(pixelPoints.first().x, canvasHeight - bottomPx)
                lineTo(pixelPoints.first().x, pixelPoints.first().y)
                for (i in 1 until pixelPoints.size) {
                    lineTo(pixelPoints[i].x, pixelPoints[i].y)
                }
                lineTo(pixelPoints.last().x, canvasHeight - bottomPx)
                close()
            }

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Cyan.copy(alpha = 0.25f), Color.Transparent),
                    startY = topPx,
                    endY = canvasHeight - bottomPx,
                ),
            )

            val graphPath = Path().apply {
                moveTo(pixelPoints.first().x, pixelPoints.first().y)
                for (i in 1 until pixelPoints.size) {
                    lineTo(pixelPoints[i].x, pixelPoints[i].y)
                }
            }

            drawPath(
                path = graphPath,
                color = Color.Cyan,
                style = Stroke(
                    width = 2.5.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )
        }

        selectedIndex?.let { index ->
            if (index in pixelPoints.indices) {
                val point = pixelPoints[index]

                drawLine(
                    color = Color.White.copy(alpha = 0.7f),
                    start = Offset(point.x, topPx),
                    end = Offset(point.x, canvasHeight - bottomPx),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f),
                )

                drawCircle(color = Color.Cyan.copy(alpha = 0.35f), radius = 8.dp.toPx(), center = point)
                drawCircle(color = Color.White, radius = 5.dp.toPx(), center = point)
                drawCircle(color = Color.Cyan, radius = 3.dp.toPx(), center = point)
            }
        }
    }
}

@Composable
private fun GraphTooltipBadge(
    telemetry: List<TelemetryPoint>,
    metric: TelemetryMetric,
    selectedIndex: Int?,
    startPointTime: Long,
    topPaddingDp: Dp,
) {
    val labelStyle = MaterialTheme.typography.labelSmall

    selectedIndex?.let { index ->
        val selectedPt = telemetry.getOrNull(index)
        if (selectedPt != null) {
            val fraction = index.toFloat() / (telemetry.size - 1).coerceAtLeast(1)
            val valStr = metric.formatValue(metric.extractValue(selectedPt))
            val ptTimeMs = (selectedPt.time.toLong() - startPointTime).coerceAtLeast(0L)
            val timeStr = formatGraphTime(ptTimeMs)

            val horizontalBias = (fraction * 2f - 1f).coerceIn(-0.85f, 0.85f)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = topPaddingDp),
            ) {
                Surface(
                    modifier = Modifier
                        .align(BiasAlignment(horizontalBias = horizontalBias, verticalBias = -1f))
                        .padding(horizontal = 4.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, Color.Cyan.copy(alpha = 0.6f)),
                    shadowElevation = 4.dp,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = valStr,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.Cyan,
                        )
                        Text(
                            text = timeStr,
                            style = labelStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GraphXAxisLabels(
    totalDurationMs: Long,
    yAxisWidth: Dp,
) {
    val labelColor = MaterialTheme.colorScheme.outline
    val labelStyle = MaterialTheme.typography.labelSmall

    Row(
        modifier = Modifier
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

private fun formatGraphTime(millis: Long): String {
    if (millis <= 0) return "0.0s"
    val totalSeconds = millis / 1000f
    return if (totalSeconds < 60f) {
        val wholeSec = totalSeconds.toInt()
        val tenths = ((totalSeconds - wholeSec) * 10).roundToInt() % 10
        "$wholeSec.${tenths}s"
    } else {
        val minutes = (totalSeconds / 60).toInt()
        val remainingSec = totalSeconds % 60
        val secInt = remainingSec.toInt()
        val tenths = ((remainingSec - secInt) * 10).roundToInt() % 10
        val secStr = if (secInt < 10) "0$secInt" else secInt.toString()
        "$minutes:$secStr.${tenths}s"
    }
}
