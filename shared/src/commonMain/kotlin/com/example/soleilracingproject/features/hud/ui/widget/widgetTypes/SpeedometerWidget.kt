package com.example.soleilracingproject.features.hud.ui.widget.widgetTypes

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.soleilracingproject.model.telemetry.TelemetryPoint
import com.example.soleilracingproject.util.tools.ConversionTools.formatOneDecimal
import kotlin.math.min

private val GaugeOrange = Color(0xFFFF8F00)
private val GaugeRed = Color(0xFFD50000)

@Composable
fun SpeedometerWidget(telemetry: TelemetryPoint?, maxSpeed: Float = 180f) {
    // No data yet (logger not connected) means we show 0
    val speed = telemetry?.speed ?: 0f

    // How full the arc is: 0 = empty, 1 = full.
    // animateFloatAsState makes the arc glide to the new value instead of jumping.
    val fraction by animateFloatAsState(
        targetValue = (speed / maxSpeed).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 150)
    )

    val trackColor = MaterialTheme.colorScheme.outlineVariant

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().padding(8.dp),
        contentAlignment = Alignment.Center
    ) {

        val numberSize = (min(maxWidth.value, maxHeight.value) * 0.22f).sp

        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = size.minDimension * 0.08f           // how thick the arc is
            val diameter = size.minDimension - strokeWidth        // the circle the arc sits on
            val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
            val arcSize = Size(diameter, diameter)
            val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)

            drawArc(
                color = trackColor,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke
            )

            if (fraction > 0f) {
                rotate(degrees = 135f, pivot = center) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            0f to GaugeOrange,
                            0.75f to GaugeRed,
                            1f to GaugeRed,
                            center = center
                        ),
                        startAngle = 0f,
                        sweepAngle = 270f * fraction,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = stroke
                    )
                }
            }
        }

        // The text in the middle of the gauge
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "SPEED",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
            Text(
                text = speed.formatOneDecimal(),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = numberSize,
                    lineHeight = numberSize * 1.1f,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "mph",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}
