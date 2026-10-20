package com.example.soleilracingproject.features.hud.ui.widget.widgetTypes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.soleilracingproject.model.analysis.Lap

private fun lapTimeText(ms: Long): String {
    val hundredths = ms / 10
    val minutes = hundredths / 6000
    val seconds = (hundredths / 100) % 60
    val rest = hundredths % 100
    return "$minutes:${seconds.toString().padStart(2, '0')}.${rest.toString().padStart(2, '0')}"
}

@Composable
fun LapTimesWidget(laps: List<Lap>, isRecording: Boolean) {
    // the fastest lap so far (null when there are no laps yet)
    val bestLap = laps.filter { !it.isDnf }.minByOrNull { it.lapTime }

    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "LAP TIMES",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )

        if (laps.isEmpty()) {
            // nothing to show yet
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (isRecording) "FINISH A LAP FIRST" else "PRESS START",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        } else {
            // newest lap on top, and only the last 5 so they fit on the card
            laps.takeLast(5).reversed().forEach { lap ->
                val isBest = lap.lapNumber == bestLap?.lapNumber
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isBest) "LAP ${lap.lapNumber}  BEST" else "LAP ${lap.lapNumber}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = lapTimeText(lap.lapTime),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = if (isBest) FontWeight.Bold else FontWeight.Normal
                        ),
                        color = if (isBest) Color(0xFF1B5E20) else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}