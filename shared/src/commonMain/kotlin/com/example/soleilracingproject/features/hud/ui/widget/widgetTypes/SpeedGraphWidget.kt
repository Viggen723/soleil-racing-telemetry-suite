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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
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

// Gives every point on the line a color, based on how fast the speed is changing
private fun segmentColors(speeds: List<Float>): List<Color> =
    speeds.indices.map { i ->
        if (i < LOOKBACK) {
            SteadyColor   // if we don't have enough history yet
        } else {
            val change = speeds[i] - speeds[i - LOOKBACK]
            val rate = change * SAMPLES_PER_SECOND / LOOKBACK   // km/h gained or lost per second
            val strength = ((abs(rate) - DEAD_ZONE) / (HARD_CHANGE - DEAD_ZONE)).coerceIn(0f, 1f)
            when {
                rate > DEAD_ZONE -> lerp(AccelLight, AccelDark, strength)   // speeding up: green
                rate < -DEAD_ZONE -> lerp(BrakeLight, BrakeDark, strength)  // braking: pink to red
                else -> SteadyColor
            }
        }
    }

@Composable
fun SpeedGraphWidget(telemetry: TelemetryPoint) {
    // The widget's notebook of past speeds
    val history = remember { mutableStateListOf<Float>() }

    // This runs every time a new telemetry reading arrives
    LaunchedEffect(telemetry) {
        history.add(telemetry.speed)
        if (history.size > 200) {
            history.removeAt(0)
        }
    }

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
                text = "SPEED · LAST 20 SECONDS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
            Text(
                text = "NOW ${telemetry.speed.formatOneDecimal()} km/h",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        LineChart(
            values = history,
            lineColor = MaterialTheme.colorScheme.primary,
            segmentColors = segmentColors(history),
            xLabels = listOf("-20s", "-10s", "Now"),
            yAxisTitle = "km/h",
            xAxisTitle = "Time (seconds ago)",
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(top = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            LegendDot(AccelDark, "Speeding up")
            LegendDot(BrakeDark, "Braking")
            LegendDot(SteadyColor, "Steady")
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