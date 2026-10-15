package com.example.soleilracingproject.features.analysis.ui.SessionComponents

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.soleilracingproject.model.analysis.Lap
import com.example.soleilracingproject.model.analysis.Session
import com.example.soleilracingproject.util.tools.ConversionTools.formatLapTime
import com.example.soleilracingproject.util.tools.ConversionTools.formatOneDecimal

@Composable
fun SessionHighlightsCard(
    selectedSession: Session?,
    displayLap: Lap?,
    onChangeSessionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "SESSION HIGHLIGHTS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("Current Session", style = MaterialTheme.typography.labelSmall)
                    Text(
                        text = selectedSession?.title ?: "No Session",
                        fontWeight = FontWeight.Bold,
                    )
                }

                Button(
                    onClick = onChangeSessionClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
                ) {
                    Text("Change Session")
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SessionStatItem("Recorded Laps", (selectedSession?.laps?.size ?: 0).toString())
                SessionStatItem("Active Lap", "Lap #${displayLap?.lapNumber ?: 1}")
                SessionStatItem("Lap Time", displayLap?.lapTime?.formatLapTime() ?: "N/A")
                SessionStatItem("Top Speed", "${displayLap?.topSpeedKmh?.formatOneDecimal() ?: "N/A"} km/h")
            }
        }
    }
}
