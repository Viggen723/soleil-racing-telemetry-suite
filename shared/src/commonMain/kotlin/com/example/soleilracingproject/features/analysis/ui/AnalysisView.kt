package com.example.soleilracingproject.features.analysis.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.soleilracingproject.features.analysis.viewmodel.AnalysisViewModel
import com.example.soleilracingproject.model.analysis.Session

@Composable
fun AnalysisView(
    viewModel: AnalysisViewModel,
    modifier: Modifier = Modifier
) {
    val sessions by viewModel.sessionsState.collectAsStateWithLifecycle()
    val activeSessions = sessions

    AnalysisContent(sessions = activeSessions, modifier = modifier)
}

@Composable
fun AnalysisContent(
    sessions: List<Session>,
    modifier: Modifier = Modifier
) {
    val displayLap = sessions.flatMap { it.laps }
        .firstOrNull { it.telemetryPoints.size >= 2 }

    // This allows the map and the graph to share the exact same selected point
    var globalSelectedIndex by remember { mutableStateOf<Int?>(null) }
    var currentMetric by remember { mutableStateOf(TelemetryMetric.SPEED) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Session Summary Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "SESSION HIGHLIGHTS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SessionStatItem("Recorded Sessions", "${sessions.size}")
                    SessionStatItem("Displayed Lap", "1:24.310")
                    SessionStatItem("Top Speed", "${displayLap?.topSpeedKmh ?: "N/A"} km/h")
                }
            }
        }

        // The Map Widget
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            AnalysisMapWidget(
                lap = displayLap,
                selectedIndex = globalSelectedIndex,
                onPointSelected = { globalSelectedIndex = it },
                modifier = Modifier.fillMaxSize()
            )
        }

        // 2. Metric Selector Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TelemetryMetric.entries.forEach { metric ->
                Button(
                    onClick = { currentMetric = metric },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (currentMetric == metric) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (currentMetric == metric) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(metric.title)
                }
            }
        }

        // Graph Widget
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            AnalysisGraphWidget(
                lap = displayLap,
                metric = currentMetric,
                selectedIndex = globalSelectedIndex,
                onPointSelected = { globalSelectedIndex = it },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}