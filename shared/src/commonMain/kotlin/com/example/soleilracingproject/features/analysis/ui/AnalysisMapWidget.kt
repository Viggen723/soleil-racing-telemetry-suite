package com.example.soleilracingproject.features.analysis.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PaintingStyle.Companion.Stroke
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.soleilracingproject.model.analysis.Lap
import com.example.soleilracingproject.model.telemetry.LatLng
import com.example.soleilracingproject.model.telemetry.TelemetryPoint
import com.example.soleilracingproject.util.tools.ConversionTools.normalizeCoordinates
import kotlin.collections.emptyList

@Composable
fun AnalysisMapWidget(modifier: Modifier, lap: Lap)
{
    val lapPoints: List<TelemetryPoint?> = lap.telemetryPoints

    val coords: List<LatLng?> =
        lapPoints.mapNotNull { point ->  point?.let { LatLng(it.latitude, it.longitude) } }

    Canvas(modifier = Modifier.fillMaxSize())
    {
        // If the points contain less than 2 points, return the Canvas empty
        if (coords.size < 2) return@Canvas

        // Check Conversion Tools to see the algorithm to change this points to normalized, readable ones
        val graphPoints = normalizeCoordinates(
            coords = coords,
            canvasWidth = size.width,
            canvasHeight = size.height
        )

        // Path is more efficient to draw then line
        val trackPath = Path().apply {
            moveTo(graphPoints.first().x, graphPoints.first().y)
            for (i in 1 until graphPoints.size)
            {
                lineTo(graphPoints[i].x, graphPoints[i].y)
            }
        }

        drawPath(
            path = trackPath,
            color = Color.Red, // After change to Soleil Red
            style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}