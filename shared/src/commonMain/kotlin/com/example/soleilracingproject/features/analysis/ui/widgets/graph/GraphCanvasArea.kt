package com.example.soleilracingproject.features.analysis.ui.widgets.graph

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.soleilracingproject.features.analysis.ui.TelemetryMetric
import com.example.soleilracingproject.model.telemetry.TelemetryPoint

@Composable
fun GraphCanvasArea(
    telemetry: List<TelemetryPoint>,
    compTelemetry: List<TelemetryPoint>,
    metric: TelemetryMetric,
    minValue: Float,
    valueRange: Float,
    selectedIndex: Int?,
    onPointSelected: (Int?) -> Unit,
    topPaddingDp: Dp,
    bottomPaddingDp: Dp,
    modifier: Modifier = Modifier,
) {
    val labelColor = MaterialTheme.colorScheme.outline
    val primaryColor = Color(0xFFD71344) // Soleil Red
    val compColor = Color(0xFF00E5FF) // Cyan

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(telemetry.size) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val totalSize = telemetry.size
                    if (totalSize > 0 && size.width > 0) {
                        val xStep = size.width.toFloat() / (totalSize - 1).coerceAtLeast(1)
                        val index = (down.position.x / xStep).toInt().coerceIn(0, totalSize - 1)
                        onPointSelected(index)
                    }

                    val pointer = down.id
                    while (true) {
                        val event = awaitPointerEvent()
                        val dragChange = event.changes.firstOrNull { it.id == pointer } ?: break
                        if (!dragChange.pressed) break

                        if (dragChange.positionChange() != Offset.Zero) {
                            dragChange.consume()
                        }

                        val totalSize = telemetry.size
                        if (totalSize > 0 && size.width > 0) {
                            val xStep = size.width.toFloat() / (totalSize - 1).coerceAtLeast(1)
                            val index = (dragChange.position.x / xStep).toInt().coerceIn(0, totalSize - 1)
                            onPointSelected(index)
                        }
                    }
                }
            },
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val topPx = topPaddingDp.toPx()
        val bottomPx = bottomPaddingDp.toPx()
        val usableHeight = (canvasHeight - topPx - bottomPx).coerceAtLeast(1f)

        val totalPoints = telemetry.size
        val maxRenderPoints = 1200
        val step = (totalPoints / maxRenderPoints).coerceAtLeast(1)

        val renderIndices = if (step > 1) (0 until totalPoints step step) else 0 until totalPoints
        val renderXStep = canvasWidth / (totalPoints - 1).coerceAtLeast(1)

        val primaryPixelPoints = renderIndices.map { index ->
            val point = telemetry[index]
            val x = index * renderXStep
            val value = metric.extractValue(point)
            val percentage = (value - minValue) / valueRange
            val y = canvasHeight - bottomPx - (percentage * usableHeight)
            Offset(x, y)
        }

        // Downsample comparison telemetry points to match canvas width
        val compPixelPoints = if (compTelemetry.isNotEmpty()) {
            val compTotal = compTelemetry.size
            val compStep = (compTotal / maxRenderPoints).coerceAtLeast(1)
            val compIndices = if (compStep > 1) (0 until compTotal step compStep) else 0 until compTotal
            val compXStep = canvasWidth / (compTotal - 1).coerceAtLeast(1)

            compIndices.map { index ->
                val point = compTelemetry[index]
                val x = index * compXStep
                val value = metric.extractValue(point)
                val percentage = (value - minValue) / valueRange
                val y = canvasHeight - bottomPx - (percentage * usableHeight)
                Offset(x, y)
            }
        } else emptyList()

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

        // Draw Comparison Lap Curve first (Cyan)
        if (compPixelPoints.size >= 2) {
            val compPath = Path().apply {
                moveTo(compPixelPoints.first().x, compPixelPoints.first().y)
                for (i in 1 until compPixelPoints.size) {
                    lineTo(compPixelPoints[i].x, compPixelPoints[i].y)
                }
            }

            drawPath(
                path = compPath,
                color = compColor,
                style = Stroke(
                    width = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )
        }

        // Draw Primary Lap Curve on top (Soleil Red + Fill)
        if (primaryPixelPoints.size >= 2) {
            val fillPath = Path().apply {
                moveTo(primaryPixelPoints.first().x, canvasHeight - bottomPx)
                lineTo(primaryPixelPoints.first().x, primaryPixelPoints.first().y)
                for (i in 1 until primaryPixelPoints.size) {
                    lineTo(primaryPixelPoints[i].x, primaryPixelPoints[i].y)
                }
                lineTo(primaryPixelPoints.last().x, canvasHeight - bottomPx)
                close()
            }

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(primaryColor.copy(alpha = 0.25f), Color.Transparent),
                    startY = topPx,
                    endY = canvasHeight - bottomPx,
                ),
            )

            val graphPath = Path().apply {
                moveTo(primaryPixelPoints.first().x, primaryPixelPoints.first().y)
                for (i in 1 until primaryPixelPoints.size) {
                    lineTo(primaryPixelPoints[i].x, primaryPixelPoints[i].y)
                }
            }

            drawPath(
                path = graphPath,
                color = primaryColor,
                style = Stroke(
                    width = 2.5.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )
        }

        // Scrubbing indicator circles
        selectedIndex?.let { index ->
            if (index in 0 until totalPoints) {
                val point = telemetry[index]
                val x = index * renderXStep
                val value = metric.extractValue(point)
                val percentage = (value - minValue) / valueRange
                val y = canvasHeight - bottomPx - (percentage * usableHeight)
                val selPoint = Offset(x, y)

                drawLine(
                    color = Color.White.copy(alpha = 0.7f),
                    start = Offset(selPoint.x, topPx),
                    end = Offset(selPoint.x, canvasHeight - bottomPx),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f),
                )

                drawCircle(color = primaryColor.copy(alpha = 0.35f), radius = 8.dp.toPx(), center = selPoint)
                drawCircle(color = Color.White, radius = 5.dp.toPx(), center = selPoint)
                drawCircle(color = primaryColor, radius = 3.dp.toPx(), center = selPoint)

                // Comparison point marker
                if (compTelemetry.isNotEmpty()) {
                    val compIdx = (index.toFloat() / (totalPoints - 1).coerceAtLeast(1) * (compTelemetry.size - 1)).toInt().coerceIn(0, compTelemetry.size - 1)
                    val compPt = compTelemetry[compIdx]
                    val compXStep = canvasWidth / (compTelemetry.size - 1).coerceAtLeast(1)
                    val compX = compIdx * compXStep
                    val compVal = metric.extractValue(compPt)
                    val compPerc = (compVal - minValue) / valueRange
                    val compY = canvasHeight - bottomPx - (compPerc * usableHeight)
                    val compSelPt = Offset(compX, compY)

                    drawCircle(color = compColor.copy(alpha = 0.35f), radius = 8.dp.toPx(), center = compSelPt)
                    drawCircle(color = Color.White, radius = 5.dp.toPx(), center = compSelPt)
                    drawCircle(color = compColor, radius = 3.dp.toPx(), center = compSelPt)
                }
            }
        }
    }
}