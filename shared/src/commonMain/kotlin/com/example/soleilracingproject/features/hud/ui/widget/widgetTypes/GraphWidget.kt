package com.example.soleilracingproject.features.hud.ui.widget.widgetTypes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.soleilracingproject.features.hud.ui.widget.LineChart
import com.example.soleilracingproject.model.telemetry.TelemetryPoint
import com.example.soleilracingproject.util.tools.ConversionTools.formatOneDecimal
import kotlin.math.abs
import kotlin.math.roundToInt

// Settings we can tune
private const val SAMPLES_PER_SECOND = 10f  // how many readings the logger sends per second
private const val LOOKBACK = 5              // compare with the speed 5 readings ago (= half a second)
private const val DEAD_ZONE = 2f            // below this many km/h per second, we call it "steady"
private const val HARD_CHANGE = 30f         // at this many km/h per second, the color is at its darkest

private val SteadyColor = Color(0xFF9E9E9E)
private val AccelLight = Color(0xFFA5D6A7)
private val AccelDark = Color(0xFF1B5E20)
private val BrakeLight = Color(0xFFF48FB1)
private val BrakeDark = Color(0xFFB71C1C)
private const val HARD_G = 1f   // at this many g the color is darkest

// This gives every point on the line a color, based on how fast the speed is changing
private fun segmentColors(speeds: List<Float>): List<Color> =
    speeds.indices.map { i ->
        if (i < LOOKBACK) {
            SteadyColor   // if we don't have enough history yet
        } else {
            val change = speeds[i] - speeds[i - LOOKBACK]
            val rate = change * SAMPLES_PER_SECOND / LOOKBACK    // km/h gained or lost per second
            val strength = ((abs(rate) - DEAD_ZONE) / (HARD_CHANGE - DEAD_ZONE)).coerceIn(0f, 1f)
            when {
                rate > DEAD_ZONE -> lerp(AccelLight, AccelDark, strength)    // speeding up: green
                rate < -DEAD_ZONE -> lerp(BrakeLight, BrakeDark, strength)  // braking: pink to red
                else -> SteadyColor
            }
        }
    }

// coloring rule for the G graph
private fun colorsBySign(values: List<Float>): List<Color> =
    values.map { v ->
        val strength = (abs(v) / HARD_G).coerceIn(0f, 1f)
        when {
            abs(v) < 0.05f -> SteadyColor
            v > 0f -> lerp(AccelLight, AccelDark, strength)
            else -> lerp(BrakeLight, BrakeDark, strength)
        }
    }

@Composable
fun GraphWidget(
    telemetry: TelemetryPoint,
    title: String,
    unit: String,
    value: Float,
    history: List<Float>,
    colorByChange: Boolean = false,
    colorBySign: Boolean = false,         // green above zero, pink/red below zero
    centerOnZero: Boolean = false,
    roundTo: Float = 10f,
    formatLabel: (Float) -> String = { it.roundToInt().toString() }
) {

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$title ",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
            Text(
                text = "NOW ${value.formatOneDecimal()} $unit",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        LineChart(
            values = history,
            lineColor = MaterialTheme.colorScheme.primary,
            segmentColors = when {
                colorByChange -> segmentColors(history)
                colorBySign -> colorsBySign(history)
                else -> emptyList()
            },
            xLabels = listOf("-20s", "-10s", "Now"),
            yAxisTitle = unit,
            xAxisTitle = "Time (seconds ago)",
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(top = 8.dp),
            centerOnZero = centerOnZero,
            roundTo = roundTo,
            formatLabel = formatLabel
        )

        // The legend only makes sense when the line is colored
        if (colorByChange) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LegendDot(AccelDark, "Accel")
                LegendDot(BrakeDark, "Decel")
                LegendDot(SteadyColor, "Coast")
            }
        }

        if (colorBySign) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LegendDot(AccelDark, "Accelerating")
                LegendDot(BrakeDark, "Braking")
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}