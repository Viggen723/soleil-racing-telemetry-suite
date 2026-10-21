package com.example.soleilracingproject.features.analysis.ui.sessionComponents

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.soleilracingproject.model.analysis.Lap
import com.example.soleilracingproject.model.analysis.Session
import com.example.soleilracingproject.model.garage.Car
import com.example.soleilracingproject.util.tools.ConversionTools.formatLapTime
import com.example.soleilracingproject.util.tools.ConversionTools.formatOneDecimal

@Composable
fun SessionHighlightsCard(
    selectedSession: Session?,
    displayLap: Lap?,
    onChangeSessionClick: () -> Unit,
    onRenameSession: (Session, String) -> Unit,
    modifier: Modifier = Modifier,
    onUpdateVehicle: (Session, String) -> Unit = { _, _ -> },
    garageCars: List<Car> = emptyList(),
) {
    var showRenameDialog by remember { mutableStateOf(false) }
    var showVehicleDialog by remember { mutableStateOf(false) }

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
                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Current Session", style = MaterialTheme.typography.labelSmall)
                    Text(
                        text = selectedSession?.title ?: "No Session",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.clickable(enabled = selectedSession != null) {
                            showRenameDialog = true
                        },
                    )
                    if (selectedSession != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .clickable { showVehicleDialog = true },
                        ) {
                            Text(
                                text = "Vehicle: ${selectedSession.vehicleId.ifBlank { "Unassigned" }}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = "Change Vehicle",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(start = 4.dp)
                                    .size(14.dp),
                            )
                        }
                    }
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
                SessionStatItem("Top Speed", "${displayLap?.topSpeedKmh?.formatOneDecimal() ?: "N/A"} mph")
            }
        }
    }

    if (showRenameDialog && selectedSession != null) {
        RenameSessionDialog(
            currentTitle = selectedSession.title,
            onDismissRequest = { showRenameDialog = false },
            onConfirm = { newTitle ->
                onRenameSession(selectedSession, newTitle)
                showRenameDialog = false
            },
        )
    }

    if (showVehicleDialog && selectedSession != null) {
        ChangeVehicleDialog(
            currentVehicle = selectedSession.vehicleId,
            garageCars = garageCars,
            onDismissRequest = { showVehicleDialog = false },
            onConfirm = { newVehicle ->
                onUpdateVehicle(selectedSession, newVehicle)
                showVehicleDialog = false
            },
        )
    }
}

@Composable
private fun RenameSessionDialog(
    currentTitle: String,
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember { mutableStateOf(currentTitle) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Rename Session") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Session Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(text) },
                enabled = text.isNotBlank(),
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun ChangeVehicleDialog(
    currentVehicle: String,
    garageCars: List<Car>,
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var selectedName by remember { mutableStateOf(currentVehicle) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Assign Vehicle to Session") },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (garageCars.isEmpty()) {
                    Text(
                        text = "No vehicles found in Garage. Add a vehicle in the Garage tab first.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    garageCars.forEach { car ->
                        val carDisplayName = listOfNotNull(car.year?.toString(), car.make, car.model, "(${car.name})")
                            .filter { it.isNotBlank() }
                            .joinToString(" ")

                        val isSelected = selectedName == carDisplayName || selectedName == car.name

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedName = carDisplayName
                                    onConfirm(carDisplayName)
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    selectedName = carDisplayName
                                    onConfirm(carDisplayName)
                                }
                            )
                            Column {
                                Text(
                                    text = car.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                Text(
                                    text = listOfNotNull(car.year?.toString(), car.make, car.model).joinToString(" "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
}