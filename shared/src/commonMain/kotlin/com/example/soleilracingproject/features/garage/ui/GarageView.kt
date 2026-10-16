package com.example.soleilracingproject.features.garage.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.soleilracingproject.features.garage.viewmodel.GarageViewmodel
import com.example.soleilracingproject.model.garage.Car

@Composable
fun GarageView(viewModel: GarageViewmodel, modifier: Modifier = Modifier) {
    val cars by viewModel.cars.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    GarageContent(
        cars = cars,
        isSaving = isSaving,
        error = error,
        onAddCar = viewModel::addCar,
        onEditCar = viewModel::editCar,
        onDeleteCar = viewModel::deleteCar,
        modifier = modifier
    )
}

@Composable
fun GarageContent(
    cars: List<Car>,
    isSaving: Boolean,
    error: String?,
    onAddCar: (Car, () -> Unit) -> Unit,
    onEditCar: (Car, () -> Unit) -> Unit,
    onDeleteCar: (String, () -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    var showForm by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var deletingId by rememberSaveable { mutableStateOf<String?>(null) }
    val editingCar = cars.firstOrNull { it.id == editingId }
    val deletingCar = cars.firstOrNull { it.id == deletingId }

    // The app already supplies the scaffold and navigation bars.
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Garage", style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary)
                Button(enabled = !isSaving, onClick = { editingId = null; showForm = true }) {
                    Text("Add Car")
                }
            }
        }
        if (error != null) item { Text(error, color = MaterialTheme.colorScheme.error) }
        if (cars.isEmpty()) item {
            Text("No cars yet. Add your first car to get started.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items(cars, key = { it.id }) { car ->
            Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(car.name, style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary)
                    Text(listOfNotNull(car.year?.toString(), car.make, car.model).joinToString(" "),
                        style = MaterialTheme.typography.bodyMedium)
                    car.specifications?.takeIf { it.isNotBlank() }?.let {
                        Text("Specifications: $it", style = MaterialTheme.typography.bodyMedium)
                    }
                    car.modifications?.takeIf { it.isNotBlank() }?.let {
                        Text("Modifications: $it", style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(enabled = !isSaving, onClick = { editingId = car.id; showForm = true }) { Text("Edit") }
                        TextButton(enabled = !isSaving, onClick = { deletingId = car.id }) { Text("Delete") }
                    }
                }
            }
        }
    }

    if (showForm) {
        CarFormDialog(car = editingCar, isSaving = isSaving, error = error,
            onDismiss = { showForm = false },
            onSave = { car ->
                val onSaved = { showForm = false }
                if (editingId == null) onAddCar(car, onSaved) else onEditCar(car, onSaved)
            })
    }
    if (deletingCar != null) {
        AlertDialog(
            onDismissRequest = { if (!isSaving) deletingId = null },
            title = { Text("Delete car?") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Delete ${deletingCar.name} from your Garage?")
                if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
            } },
            confirmButton = { TextButton(enabled = !isSaving, onClick = {
                onDeleteCar(deletingCar.id) { deletingId = null }
            }) { Text("Delete") } },
            dismissButton = { TextButton(enabled = !isSaving, onClick = { deletingId = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun CarFormDialog(
    car: Car?,
    isSaving: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (Car) -> Unit
) {
    // One draft form serves both Add and Edit; existing IDs and photo paths stay intact.
    var name by rememberSaveable(car?.id) { mutableStateOf(car?.name.orEmpty()) }
    var make by rememberSaveable(car?.id) { mutableStateOf(car?.make.orEmpty()) }
    var model by rememberSaveable(car?.id) { mutableStateOf(car?.model.orEmpty()) }
    var year by rememberSaveable(car?.id) { mutableStateOf(car?.year?.toString().orEmpty()) }
    var specifications by rememberSaveable(car?.id) { mutableStateOf(car?.specifications.orEmpty()) }
    var modifications by rememberSaveable(car?.id) { mutableStateOf(car?.modifications.orEmpty()) }
    val yearValue = year.trim().toIntOrNull()
    val invalidYear = year.isNotBlank() && (yearValue == null || yearValue <= 0)
    val valid = name.isNotBlank() && make.isNotBlank() && model.isNotBlank() && !invalidYear

    AlertDialog(
        modifier = Modifier.imePadding(),
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text(if (car == null) "Add Car" else "Edit Car") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Name, make, and model are required.", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(name, { name = it }, label = { Text("Name *") }, singleLine = true,
                    enabled = !isSaving, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(make, { make = it }, label = { Text("Make *") }, singleLine = true,
                    enabled = !isSaving, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(model, { model = it }, label = { Text("Model *") }, singleLine = true,
                    enabled = !isSaving, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(year, { year = it }, label = { Text("Year (optional)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = invalidYear, supportingText = { if (invalidYear) Text("Enter a positive whole year.") },
                    enabled = !isSaving, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(specifications, { specifications = it }, label = { Text("Specifications (optional)") },
                    enabled = !isSaving, modifier = Modifier.fillMaxWidth(), minLines = 2)
                OutlinedTextField(modifications, { modifications = it }, label = { Text("Modifications (optional)") },
                    enabled = !isSaving, modifier = Modifier.fillMaxWidth(), minLines = 2)
                if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = { Button(enabled = valid && !isSaving, onClick = {
            onSave(Car(
                id = car?.id.orEmpty(), name = name.trim(), make = make.trim(), model = model.trim(),
                year = yearValue, specifications = specifications.trim().takeIf { it.isNotEmpty() },
                modifications = modifications.trim().takeIf { it.isNotEmpty() }, photoPath = car?.photoPath
            ))
        }) { Text(if (isSaving) "Saving…" else "Save") } },
        dismissButton = { TextButton(enabled = !isSaving, onClick = onDismiss) { Text("Cancel") } }
    )
}
