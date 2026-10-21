package com.example.soleilracingproject.features.garage.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.soleilracingproject.features.garage.viewmodel.GarageViewmodel
import com.example.soleilracingproject.model.garage.Car

@Composable
fun GarageView(viewModel: GarageViewmodel, modifier: Modifier = Modifier) {
    val cars by viewModel.cars.collectAsStateWithLifecycle()
    val selectedCarId by viewModel.selectedCarId.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    LaunchedEffect(cars) {
        if (selectedCarId == null && cars.isNotEmpty()) {
            viewModel.selectCar(cars.first().id)
        }
    }

    GarageContent(
        cars = cars,
        selectedCarId = selectedCarId,
        isSaving = isSaving,
        error = error,
        onSelectCar = viewModel::selectCar,
        onAddCar = viewModel::addCar,
        onEditCar = viewModel::editCar,
        onDeleteCar = viewModel::deleteCar,
        modifier = modifier
    )
}

@Composable
fun GarageContent(
    cars: List<Car>,
    selectedCarId: String?,
    isSaving: Boolean,
    error: String?,
    onSelectCar: (String?) -> Unit,
    onAddCar: (Car, () -> Unit) -> Unit,
    onEditCar: (Car, () -> Unit) -> Unit,
    onDeleteCar: (String, () -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    var showForm by rememberSaveable { mutableStateOf(false) }
    var showSelectDialog by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var deletingId by rememberSaveable { mutableStateOf<String?>(null) }
    val editingCar = cars.firstOrNull { it.id == editingId }
    val deletingCar = cars.firstOrNull { it.id == deletingId }
    val activeVehicle = cars.firstOrNull { it.id == selectedCarId }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Garage",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Button(
                        enabled = !isSaving,
                        onClick = { editingId = null; showForm = true }
                    ) {
                        Text("Add Car")
                    }
                }

                if (cars.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        FilterChip(
                            selected = activeVehicle != null,
                            onClick = { showSelectDialog = true },
                            label = {
                                Text(
                                    text = if (activeVehicle != null) "In Use: ${activeVehicle.name}" else "Select Active Vehicle",
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.DirectionsCar,
                                    contentDescription = "Active Vehicle",
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }
        }

        if (error != null) item { Text(error, color = MaterialTheme.colorScheme.error) }

        if (cars.isEmpty()) item {
            Text(
                "No cars yet. Add your first car to get started.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(cars, key = { it.id }) { car ->
            val isCurrent = car.id == selectedCarId

            Card(
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isCurrent) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                border = if (isCurrent) CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary)) else null
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            car.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )

                        if (isCurrent) {
                            FilterChip(
                                selected = true,
                                onClick = {},
                                label = { Text("In Use", fontWeight = FontWeight.Bold) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "In Use",
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        } else {
                            OutlinedButton(
                                onClick = { onSelectCar(car.id) }
                            ) {
                                Text("Select for Use")
                            }
                        }
                    }

                    Text(
                        listOfNotNull(car.year?.toString(), car.make, car.model).joinToString(" "),
                        style = MaterialTheme.typography.bodyMedium
                    )

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

    if (showSelectDialog) {
        AlertDialog(
            onDismissRequest = { showSelectDialog = false },
            title = { Text("Select Active Vehicle for Use") },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    cars.forEach { car ->
                        val isSelected = car.id == selectedCarId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectCar(car.id)
                                    showSelectDialog = false
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    onSelectCar(car.id)
                                    showSelectDialog = false
                                }
                            )
                            Column {
                                Text(
                                    text = car.name,
                                    style = MaterialTheme.typography.bodyLarge,
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
            },
            confirmButton = {
                TextButton(onClick = { showSelectDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showForm) {
        CarFormDialog(
            car = editingCar,
            isSaving = isSaving,
            error = error,
            onDismiss = { showForm = false },
            onSave = { car ->
                val onSaved = { showForm = false }
                if (editingId == null) onAddCar(car, onSaved) else onEditCar(car, onSaved)
            }
        )
    }

    if (deletingCar != null) {
        AlertDialog(
            onDismissRequest = { if (!isSaving) deletingId = null },
            title = { Text("Delete car?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Delete ${deletingCar.name} from your Garage?")
                    if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !isSaving,
                    onClick = {
                        onDeleteCar(deletingCar.id) { deletingId = null }
                    }
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(
                    enabled = !isSaving,
                    onClick = { deletingId = null }
                ) { Text("Cancel") }
            }
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
                OutlinedTextField(
                    name, { name = it }, label = { Text("Name *") }, singleLine = true,
                    enabled = !isSaving, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    make, { make = it }, label = { Text("Make *") }, singleLine = true,
                    enabled = !isSaving, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    model, { model = it }, label = { Text("Model *") }, singleLine = true,
                    enabled = !isSaving, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    year, { year = it }, label = { Text("Year (optional)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = invalidYear, supportingText = { if (invalidYear) Text("Enter a positive whole year.") },
                    enabled = !isSaving, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    specifications, { specifications = it }, label = { Text("Specifications (optional)") },
                    enabled = !isSaving, modifier = Modifier.fillMaxWidth(), minLines = 2
                )
                OutlinedTextField(
                    modifications, { modifications = it }, label = { Text("Modifications (optional)") },
                    enabled = !isSaving, modifier = Modifier.fillMaxWidth(), minLines = 2
                )
                if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            Button(
                enabled = valid && !isSaving,
                onClick = {
                    onSave(
                        Car(
                            id = car?.id.orEmpty(),
                            name = name.trim(),
                            make = make.trim(),
                            model = model.trim(),
                            year = yearValue,
                            specifications = specifications.trim().takeIf { it.isNotEmpty() },
                            modifications = modifications.trim().takeIf { it.isNotEmpty() },
                            photoPath = car?.photoPath
                        )
                    )
                }
            ) { Text(if (isSaving) "Saving…" else "Save") }
        },
        dismissButton = { TextButton(enabled = !isSaving, onClick = onDismiss) { Text("Cancel") } }
    )
}