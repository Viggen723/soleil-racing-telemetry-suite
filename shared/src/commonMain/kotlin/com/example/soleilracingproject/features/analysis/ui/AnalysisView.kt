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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.soleilracingproject.features.analysis.viewmodel.AnalysisViewModel
import com.example.soleilracingproject.model.analysis.Lap
import com.example.soleilracingproject.model.analysis.Session
import com.example.soleilracingproject.util.tools.ConversionTools.formatLapTime
import com.example.soleilracingproject.util.tools.ConversionTools.formatOneDecimal

@Composable
fun AnalysisView(
    viewModel: AnalysisViewModel,
    modifier: Modifier = Modifier
) {
    val sessions by viewModel.sessionsState.collectAsStateWithLifecycle()
    AnalysisContent(sessions = sessions, modifier = modifier)
}

@Composable
fun AnalysisContent(
    sessions: List<Session>,
    modifier: Modifier = Modifier
) {
    val sampleLap = sessions.firstOrNull()?.laps?.firstOrNull() ?: Lap(
        id = "sample_lap_1",
        sessionId = "session_1",
        lapNumber = 1,
        lapTime = 84310L, // 1:24.310
        topSpeedKmh = 214.5f
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Dashboard Title
        Text(
            text = "Track Analysis & Map Overlay",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        // Track Map Overlay Widget
        AnalysisMapWidget(
            lap = sampleLap,
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
        )

        // Session Overview Summary Card
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
                    StatSummaryItem("Recorded Sessions", "${sessions.size}")
                    StatSummaryItem("Best Lap", sampleLap.lapTime.formatLapTime())
                    StatSummaryItem("Top Speed", "${sampleLap.topSpeedKmh.formatOneDecimal()} km/h")
                }
            }
        }
    }
}

@Composable
private fun StatSummaryItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
