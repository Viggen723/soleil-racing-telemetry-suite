package com.example.soleilracingproject.features.customtrack.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.soleilracingproject.features.customtrack.viewmodel.CustomTrackStep
import com.example.soleilracingproject.features.customtrack.viewmodel.CustomTrackViewModel

@Composable
fun CustomTrackView(viewModel: CustomTrackViewModel) {
    val telemetry by viewModel.currentTelemetry.collectAsStateWithLifecycle()
    val step by viewModel.step.collectAsStateWithLifecycle()
    val customTracks by viewModel.customTracks.collectAsStateWithLifecycle()
    var trackName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Custom Track Setup", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (telemetry != null) {
                    Text("GPS Signal OK: Lat ${telemetry?.latitude}, Lon ${telemetry?.longitude}", color = MaterialTheme.colorScheme.primary)
                } else {
                    Text("Waiting for GPS signal...", color = MaterialTheme.colorScheme.error)
                }

                Spacer(modifier = Modifier.height(16.dp))

                when (step) {
                    CustomTrackStep.WAITING_P1 -> {
                        Button(
                            onClick = { viewModel.setPoint1() },
                            modifier = Modifier.fillMaxWidth().height(64.dp),
                            enabled = telemetry != null
                        ) {
                            Text("SET START/FINISH POINT 1")
                        }
                    }
                    CustomTrackStep.WAITING_P2 -> {
                        Button(
                            onClick = { viewModel.setPoint2() },
                            modifier = Modifier.fillMaxWidth().height(64.dp),
                            enabled = telemetry != null
                        ) {
                            Text("SET START/FINISH POINT 2")
                        }
                    }
                    CustomTrackStep.READY_TO_SAVE -> {
                        OutlinedTextField(
                            value = trackName,
                            onValueChange = { trackName = it },
                            label = { Text("Track Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { viewModel.reset(); trackName = "" },
                                modifier = Modifier.weight(1f).height(50.dp)
                            ) {
                                Text("Cancel")
                            }
                            Button(
                                onClick = { 
                                    viewModel.saveTrack(trackName) {
                                        trackName = ""
                                    }
                                },
                                modifier = Modifier.weight(1f).height(50.dp)
                            ) {
                                Text("Complete!")
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Your Custom Tracks", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(customTracks) { track ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(track.trackName, style = MaterialTheme.typography.bodyLarge)
                        IconButton(onClick = { viewModel.deleteTrack(track.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
            if (customTracks.isEmpty()) {
                item {
                    Text("No custom tracks saved yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
