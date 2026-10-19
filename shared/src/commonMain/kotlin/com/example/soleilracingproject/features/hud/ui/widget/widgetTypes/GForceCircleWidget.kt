package com.example.soleilracingproject.features.hud.ui.widget.widgetTypes

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.soleilracingproject.model.telemetry.TelemetryPoint
import com.example.soleilracingproject.util.tools.ConversionTools.formatOneDecimal
import kotlin.math.sqrt

// If accelX and accelY are in m/s² instead of g, i will change this to 1f / 9.81f (Have to ask adam)
private const val ACCEL_TO_G = 1f
// The outer ring of the circle stands for this many g
private const val MAX_G = 1.5f
// How many past readings the trail remembers (50 readings = about 5 seconds)
private const val TRAIL_LENGTH = 50
// The ONLY place that decides how a reading becomes forward / sideways g
// (NB: FOR US if the dot goes the wrong way when we test the real logger, put a minus in front of p.accelY here)
private fun forwardG(p: TelemetryPoint) = (p.accelX * ACCEL_TO_G).coerceIn(-MAX_G, MAX_G)
private fun sidewaysG(p: TelemetryPoint) = (p.accelY * ACCEL_TO_G).coerceIn(-MAX_G, MAX_G)

private val AccelGreen = Color(0xFF1B5E20)
private val BrakeRed = Color(0xFFB71C1C)
private val SteadyGray = Color(0xFF9E9E9E)

@Composable
fun GForceCircleWidget(telemetry: TelemetryPoint, recentPoints: List<TelemetryPoint>) {
    // forward:  positive = speeding up, negative = braking
    val forward = forwardG(telemetry)
    val sideways = sidewaysG(telemetry)

    val trail = recentPoints.takeLast(TRAIL_LENGTH).map { Offset(sidewaysG(it), forwardG(it)) }

    val ringColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.outline
    val labelStyle = MaterialTheme.typography.labelSmall

    val totalG = sqrt(forward * forward + sideways * sideways)   // how hard the car is pulling overall
    val dotColor = when {
        forward > 0.05f -> AccelGreen
        forward < -0.05f -> BrakeRed
        else -> SteadyGray
    }

    Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            // leave some room around the circle for the text labels
            val radius = size.minDimension / 2 - 18.dp.toPx()
            val pixelsPerG = radius / MAX_G

            // Turns "x g sideways, y g forward" into a spot on the screen.
            // Up on the screen is forward, so we SUBTRACT. Left on the screen is left, so we subtract too.
            fun spotOf(side: Float, fwd: Float) =
                Offset(center.x - side * pixelsPerG, center.y - fwd * pixelsPerG)

            // The rings: 0.5 g, 1.0 g and 1.5 g
            for (g in listOf(0.5f, 1.0f, 1.5f)) {
                drawCircle(
                    color = ringColor,
                    radius = g * pixelsPerG,
                    center = center,
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            // The cross through the middle
            drawLine(
                color = ringColor,
                start = Offset(center.x - radius, center.y),
                end = Offset(center.x + radius, center.y),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = ringColor,
                start = Offset(center.x, center.y - radius),
                end = Offset(center.x, center.y + radius),
                strokeWidth = 1.dp.toPx()
            )

            // The trail. Older pieces are fainter, newer pieces are stronger.
            for (i in 1 until trail.size) {
                val from = trail[i - 1]
                val to = trail[i]
                drawLine(
                    color = dotColor.copy(alpha = i / trail.size.toFloat()),
                    start = spotOf(from.x, from.y),
                    end = spotOf(to.x, to.y),
                    strokeWidth = 2.dp.toPx()
                )
            }

            // The dot = where the car is pulling right now
            drawCircle(
                color = dotColor,
                radius = 6.dp.toPx(),
                center = spotOf(sideways, forward)
            )
        }

        // Text labels around the circle
        Text("ACCEL", style = labelStyle, color = labelColor, modifier = Modifier.align(Alignment.TopCenter))
        Text("BRAKE", style = labelStyle, color = labelColor, modifier = Modifier.align(Alignment.BottomCenter))
        Text("LEFT TURN", style = labelStyle, color = labelColor, modifier = Modifier.align(Alignment.CenterStart))
        Text("RIGHT TURN", style = labelStyle, color = labelColor, modifier = Modifier.align(Alignment.CenterEnd))

        // Title in the top left, total g-force in the top right
        Text("G-FORCE", style = labelStyle, color = labelColor, modifier = Modifier.align(Alignment.TopStart))
        Text(
            text = "${totalG.formatOneDecimal()} g",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.align(Alignment.TopEnd)
        )
    }
}