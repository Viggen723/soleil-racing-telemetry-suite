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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.soleilracingproject.features.analysis.ui.SessionComponents.SessionHighlightsCard
import com.example.soleilracingproject.features.analysis.ui.SessionComponents.SessionSelectionDialog
import com.example.soleilracingproject.features.analysis.ui.widgets.AnalysisGraphWidget
import com.example.soleilracingproject.features.analysis.ui.widgets.AnalysisMapWidget
import com.example.soleilracingproject.features.analysis.viewmodel.AnalysisViewModel
import com.example.soleilracingproject.model.analysis.Session
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import kotlinx.coroutines.launch

@Composable
fun AnalysisView(
    viewModel: AnalysisViewModel,
    modifier: Modifier = Modifier,
) {
    val sessions by viewModel.sessionsState.collectAsStateWithLifecycle()

    AnalysisContent(
        sessions = sessions,
        viewModel = viewModel,
        modifier = modifier,
    )
}

@Composable
fun AnalysisContent(
    sessions: List<Session>,
    viewModel: AnalysisViewModel,
    modifier: Modifier = Modifier,
) {
    var selectedSession by remember { mutableStateOf<Session?>(null) }
    var showSessionDialog by remember { mutableStateOf(false) }

    var selectedLapNumber by remember(selectedSession) {
        mutableStateOf(selectedSession?.laps?.firstOrNull()?.lapNumber)
    }

    LaunchedEffect(sessions) {
        selectedSession = if (sessions.isNotEmpty()) {
            if ((selectedSession == null) || sessions.none { it.id == selectedSession?.id }) {
                sessions.first()
            } else {
                sessions.firstOrNull { it.id == selectedSession?.id }
            }
        } else {
            null
        }
    }

    val displayLap = selectedSession?.laps?.firstOrNull { it.lapNumber == selectedLapNumber }
        ?: selectedSession?.laps?.firstOrNull()

    var globalSelectedIndex by remember { mutableStateOf<Int?>(null) }
    var currentMetric by remember { mutableStateOf(TelemetryMetric.SPEED) }

    val coroutineScope = rememberCoroutineScope()

    val launcher = rememberFilePickerLauncher(
        type = PickerType.File(extensions = listOf("csv")),
        mode = PickerMode.Single,
        title = "Select Telemetry CSV",
    ) { platformFile ->
        if (platformFile != null) {
            coroutineScope.launch {
                try {
                    val fileBytes = platformFile.readBytes()
                    val csvString = fileBytes.decodeToString()

                    viewModel.loadCsvSession(
                        fileName = platformFile.name,
                        csvContent = csvString,
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    if (showSessionDialog) {
        SessionSelectionDialog(
            sessions = sessions,
            onDismissRequest = { showSessionDialog = false },
            onSessionSelected = { chosenSession ->
                selectedSession = chosenSession
                selectedLapNumber = chosenSession.laps.firstOrNull()?.lapNumber
                showSessionDialog = false
                globalSelectedIndex = null
            },
            onDeleteSession = { sessionToDelete ->
                viewModel.deleteSession(sessionToDelete)

                if (selectedSession?.id == sessionToDelete.id) {
                    selectedSession = sessions.firstOrNull { it.id != sessionToDelete.id }
                    selectedLapNumber = selectedSession?.laps?.firstOrNull()?.lapNumber
                    globalSelectedIndex = null
                }
            },
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // --- HEADER ROW (Title & CSV Import) ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Analysis",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Button(
                onClick = { launcher.launch() },
                shape = RoundedCornerShape(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Upload CSV",
                    modifier = Modifier.padding(end = 4.dp),
                )
                Text("Import CSV")
            }
        }

        // Session Highlights Card
        SessionHighlightsCard(
            selectedSession = selectedSession,
            displayLap = displayLap,
            onChangeSessionClick = { showSessionDialog = true },
        )

        // Lap Selector Row
        selectedSession?.let { session ->
            LapSelectorRow(
                laps = session.laps,
                selectedLapNumber = displayLap?.lapNumber,
                onLapSelected = { lapNum ->
                    selectedLapNumber = lapNum
                    globalSelectedIndex = null
                },
            )
        }

        // Map Widget
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
        ) {
            AnalysisMapWidget(
                lap = displayLap,
                selectedIndex = globalSelectedIndex,
                onPointSelected = { globalSelectedIndex = it },
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Metric Selector Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TelemetryMetric.entries.forEach { metric ->
                Button(
                    onClick = { currentMetric = metric },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (currentMetric == metric) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (currentMetric == metric) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    modifier = Modifier.weight(1f),
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
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
        ) {
            AnalysisGraphWidget(
                lap = displayLap,
                metric = currentMetric,
                selectedIndex = globalSelectedIndex,
                onPointSelected = { globalSelectedIndex = it },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
