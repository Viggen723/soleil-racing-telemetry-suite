package com.example.soleilracingproject.features.analysis.ui.widgets

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.soleilracingproject.model.analysis.Lap
import com.example.soleilracingproject.model.telemetry.LatLng
import com.example.soleilracingproject.model.telemetry.TelemetryPoint
import com.example.soleilracingproject.util.tools.ConversionTools.formatOneDecimal
import com.example.soleilracingproject.util.tools.ConversionTools.normalizeCoordinates
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

@Composable
fun AnalysisMapWidget(
    lap: Lap?,
    selectedIndex: Int?,
    onPointSelected: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lapPoints: List<TelemetryPoint> = remember(lap) {
        lap?.telemetryPoints ?: emptyList()
    }

    val coords: List<LatLng> = remember(lapPoints) {
        lapPoints.map { LatLng(it.latitude, it.longitude) }
    }

    // Transform State for the moving map and zooming
    var zoomLevel by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        if (coords.size < 2) {
            Text(
                text = "No GPS Telemetry",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center),
            )
            return@BoxWithConstraints
        }

        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()

        val graphPoints: List<Offset> = remember(coords, widthPx, heightPx) {
            if ((widthPx > 0f) && (heightPx > 0f)) {
                normalizeCoordinates(coords, widthPx, heightPx)
            } else {
                emptyList()
            }
        }

        val trackPath: Path? = remember(graphPoints) {
            if (graphPoints.size < 2) {
                null
            } else {
                Path().apply {
                    moveTo(graphPoints.first().x, graphPoints.first().y)
                    for (i in 1 until graphPoints.size) {
                        lineTo(graphPoints[i].x, graphPoints[i].y)
                    }
                    close()
                }
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        zoomLevel = (zoomLevel * zoom).coerceIn(0.5f, 5f)
                        offset += pan
                    }
                }
                .pointerInput(graphPoints) {
                    detectTapGestures { tapOffset ->
                        if (graphPoints.isEmpty()) return@detectTapGestures

                        val canvasCenter = Offset(size.width / 2f, size.height / 2f)
                        val transformedTap = ((tapOffset - offset - canvasCenter) / zoomLevel) + canvasCenter

                        var closestIndex = -1
                        var minDistance = Float.MAX_VALUE

                        val step = (graphPoints.size / 1000).coerceAtLeast(1)
                        var i = 0
                        while (i < graphPoints.size) {
                            val pt = graphPoints[i]
                            val dist = hypot(pt.x - transformedTap.x, pt.y - transformedTap.y)
                            if (dist < minDistance) {
                                minDistance = dist
                                closestIndex = i
                            }
                            i += step
                        }

                        if (step > 1 && closestIndex != -1) {
                            val searchStart = (closestIndex - step).coerceAtLeast(0)
                            val searchEnd = (closestIndex + step).coerceAtMost(graphPoints.size - 1)
                            for (idx in searchStart..searchEnd) {
                                val pt = graphPoints[idx]
                                val dist = hypot(pt.x - transformedTap.x, pt.y - transformedTap.y)
                                if (dist < minDistance) {
                                    minDistance = dist
                                    closestIndex = idx
                                }
                            }
                        }

                        if ((minDistance < (40f / zoomLevel)) && (closestIndex != -1)) {
                            onPointSelected(closestIndex)
                        } else {
                            onPointSelected(null)
                        }
                    }
                },
        ) {
            if ((trackPath == null) || (graphPoints.isEmpty())) return@Canvas

            withTransform(
                transformBlock = {
                    translate(left = offset.x, top = offset.y)
                    scale(scale = zoomLevel, pivot = center)
                },
            ) {
                drawGridBackground(
                    gridSizePx = 60f,
                    gridColor = Color.White.copy(alpha = 0.12f),
                    strokeWidth = 1f / zoomLevel,
                )

                drawPath(
                    path = trackPath,
                    color = Color.Red,
                    style = Stroke(
                        width = 6f / zoomLevel,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                )

                if (graphPoints.size >= 2) {
                    val startPoint = graphPoints[0]
                    val nextPoint = graphPoints[1]

                    val angle = atan2(nextPoint.y - startPoint.y, nextPoint.x - startPoint.x)
                    val perpAngle = angle + (PI / 2)

                    val lineLength = 24f / zoomLevel
                    val dx = (cos(perpAngle) * lineLength).toFloat()
                    val dy = (sin(perpAngle) * lineLength).toFloat()

                    drawLine(
                        color = Color.White,
                        start = Offset(startPoint.x - dx, startPoint.y - dy),
                        end = Offset(startPoint.x + dx, startPoint.y + dy),
                        strokeWidth = 8f / zoomLevel,
                        cap = StrokeCap.Square
                    )

                    drawLine(
                        color = Color.Black,
                        start = Offset(startPoint.x - dx, startPoint.y - dy),
                        end = Offset(startPoint.x + dx, startPoint.y + dy),
                        strokeWidth = 3f / zoomLevel,
                        cap = StrokeCap.Square
                    )
                }

                // Use the passed-in selectedIndex
                selectedIndex?.let { index ->
                    if (index in graphPoints.indices) {
                        val point = graphPoints[index]
                        drawCircle(color = Color.White, radius = 10f / zoomLevel, center = point)
                        drawCircle(color = Color.Red, radius = 6f / zoomLevel, center = point)
                    }
                }
            }
        }

        selectedIndex?.let { index ->
            val point = lapPoints.getOrNull(index)
            if (point != null) {
                Card(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "Point #${index + 1}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Speed: ${point.speed.formatOneDecimal()} km/h",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                        )
                        Text(
                            text = "X-Accel: ${point.accelX.formatOneDecimal()} G",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                        )
                        Text(
                            text = "Y-Accel: ${point.accelY.formatOneDecimal()} G",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                        )
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawGridBackground(
    gridSizePx: Float = 60f,
    gridColor: Color = Color.LightGray,
    strokeWidth: Float = 1f,
) {
    var x = 0f
    while (x <= size.width) {
        drawLine(color = gridColor, start = Offset(x, 0f), end = Offset(x, size.height), strokeWidth = strokeWidth)
        x += gridSizePx
    }
    var y = 0f
    while (y <= size.height) {
        drawLine(color = gridColor, start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = strokeWidth)
        y += gridSizePx
    }
}