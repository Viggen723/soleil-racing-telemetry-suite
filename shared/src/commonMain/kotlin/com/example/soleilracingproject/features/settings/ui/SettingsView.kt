package com.example.soleilracingproject.features.settings.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.soleilracingproject.features.settings.viewmodel.SettingsViewModel
import com.example.soleilracingproject.util.tools.ConversionTools.formatOneDecimal

@Composable
fun SettingsView(viewModel: SettingsViewModel, modifier: Modifier = Modifier) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    var editing by rememberSaveable { mutableStateOf<String?>(null) }
    // Only the unsaved dialog text lives here; Room owns the actual preferences.
    var draft by rememberSaveable { mutableStateOf("") }
    var confirmDisable by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)
        Text("Profile", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        SettingsRow("Display Name", settings.displayName.ifBlank { "Not set" }, !isSaving) {
            viewModel.clearError()
            draft = settings.displayName
            editing = "name"
        }
        Text("Display", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        SettingsRow("Speedometer Maximum", "${settings.speedometerMax.formatOneDecimal()} mph", !isSaving) {
            viewModel.clearError()
            draft = settings.speedometerMax.formatOneDecimal()
            editing = "maximum"
        }
        Text("Driving", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Focus Mode", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                    Switch(
                        checked = settings.focusModeEnabled,
                        enabled = !isSaving,
                        onCheckedChange = { enabled ->
                            viewModel.clearError()
                            if (enabled) viewModel.updateFocusModeEnabled(true) else confirmDisable = true
                        },
                    )
                }
                Text("Automatically simplifies the HUD while recording at speed.", style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (error != null && editing == null && !confirmDisable) {
            Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
        }
    }

    if (editing != null) {
        val isMaximum = editing == "maximum"
        AlertDialog(
            onDismissRequest = { if (!isSaving) { editing = null; viewModel.clearError() } },
            title = { Text(if (isMaximum) "Speedometer Maximum" else "Display Name") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it; viewModel.clearError() },
                        enabled = !isSaving,
                        singleLine = true,
                        label = { Text(if (isMaximum) "Maximum (mph)" else "Display Name") },
                        keyboardOptions = KeyboardOptions(keyboardType = if (isMaximum) KeyboardType.Decimal else KeyboardType.Text),
                        isError = error != null,
                    )
                    Text(if (isMaximum) "Choose a value from 10 to 500 mph." else "Stored only on this device. A blank name is fine.", style = MaterialTheme.typography.bodySmall)
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(enabled = !isSaving, onClick = {
                    if (isMaximum) viewModel.updateSpeedometerMax(draft) { editing = null }
                    else viewModel.updateDisplayName(draft) { editing = null }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(enabled = !isSaving, onClick = { editing = null; viewModel.clearError() }) { Text("Cancel") } },
        )
    }

    if (confirmDisable) {
        AlertDialog(
            onDismissRequest = { if (!isSaving) { confirmDisable = false; viewModel.clearError() } },
            title = { Text("Disable Focus Mode?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Focus Mode reduces available controls while moving. Only interact with the app when it is safe to do so.")
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = { TextButton(enabled = !isSaving, onClick = {
                viewModel.updateFocusModeEnabled(false) { confirmDisable = false }
            }) { Text("Disable Anyway") } },
            dismissButton = { TextButton(enabled = !isSaving, onClick = { confirmDisable = false; viewModel.clearError() }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun SettingsRow(title: String, value: String, enabled: Boolean, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Edit $title")
        }
    }
}
