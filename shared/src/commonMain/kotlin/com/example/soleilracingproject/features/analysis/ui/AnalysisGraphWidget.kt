package com.example.soleilracingproject.features.analysis.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.soleilracingproject.model.analysis.Lap

@Composable
fun AnalysisGraphWidget(
    lap: Lap?,
    metric: TelemetryMetric,
    selectedIndex: Int?,
    onPointSelected: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    val telemetry = lap?.telemetryPoints?.filterNotNull() ?: emptyList()
    if (telemetry.isEmpty()) return

    // Dynamic bounds based on the chosen metric
    val maxValue = telemetry.maxOfOrNull { metric.extractValue(it) } ?: 100f
    val minValue = telemetry.minOfOrNull { metric.extractValue(it) } ?: 0f
    val valueRange = (maxValue - minValue).coerceAtLeast(1f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(Color(0xFF121212))
            .pointerInput(telemetry.size) {
                // Handle dragging across the graph
                detectDragGestures(
                    onDragStart = { offset ->
                        val xStep = size.width / (telemetry.size - 1).coerceAtLeast(1)
                        val index = (offset.x / xStep).toInt().coerceIn(0, telemetry.size - 1)
                        onPointSelected(index)
                    },
                    onDragEnd = { onPointSelected(null) }, // Clear selection when finger lifts
                    onDragCancel = { onPointSelected(null) }
                ) { change, _ ->
                    val xStep = size.width / (telemetry.size - 1).coerceAtLeast(1)
                    val index = (change.position.x / xStep).toInt().coerceIn(0, telemetry.size - 1)
                    onPointSelected(index)
                }
            }
            .pointerInput(telemetry.size) {
                // Handle simple taps
                detectTapGestures { offset ->
                    val xStep = size.width / (telemetry.size - 1).coerceAtLeast(1)
                    val index = (offset.x / xStep).toInt().coerceIn(0, telemetry.size - 1)
                    onPointSelected(index)
                }
            }
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val xStep = canvasWidth / (telemetry.size - 1).coerceAtLeast(1)

        val pixelPoints = telemetry.mapIndexed { index, point ->
            val x = index * xStep
            val value = metric.extractValue(point)
            val percentage = (value - minValue) / valueRange
            val y = canvasHeight - (percentage * canvasHeight)
            Offset(x, y)
        }

        // Draw the main graph line
        val graphPath = Path().apply {
            moveTo(pixelPoints.first().x, pixelPoints.first().y)
            for (i in 1 until pixelPoints.size) {
                lineTo(pixelPoints[i].x, pixelPoints[i].y)
            }
        }

        drawPath(
            path = graphPath,
            color = Color.Cyan,
            style = Stroke(width = 4f)
        )

        // Draw the interactive scrubber line and dot if a point is selected
        selectedIndex?.let { index ->
            if (index in pixelPoints.indices) {
                val point = pixelPoints[index]

                // Vertical line
                drawLine(
                    color = Color.White.copy(alpha = 0.5f),
                    start = Offset(point.x, 0f),
                    end = Offset(point.x, canvasHeight),
                    strokeWidth = 2f
                )

                // Highlight dot
                drawCircle(color = Color.White, radius = 12f, center = point)
                drawCircle(color = Color.Cyan, radius = 8f, center = point)
            }
        }
    }
}