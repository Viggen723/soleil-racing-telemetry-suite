package com.example.soleilracingproject.features.hud.ui

import HudViewModel
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.soleilracingproject.features.hud.ui.widget.*
import com.example.soleilracingproject.model.telemetry.TelemetryPoint

@Composable
fun HudView(
    viewModel: HudViewModel,
    onUpdateTopBarActions: ((@Composable RowScope.() -> Unit)?) -> Unit = {},
    onFocusModeChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val telemetry by viewModel.telemetryState.collectAsStateWithLifecycle()
    val widgets by viewModel.widgetLayout.collectAsStateWithLifecycle()
    val isEditMode by viewModel.isEditMode.collectAsStateWithLifecycle()
    val isRecording by viewModel.isRecording.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }

    // Focus mode!
    val isFocusMode = (telemetry?.speed ?: 0f) > 10f
    LaunchedEffect(isFocusMode) {
        onFocusModeChange(isFocusMode)
    }

    // Set top bar actions only when isEditMode changes, avoiding parent state mutations on telemetry recompositions
    LaunchedEffect(isEditMode) {
        onUpdateTopBarActions {
            if (isEditMode) {
                Button(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text("+ Add Widget")
                }
            }
            IconButton(onClick = viewModel::toggleEditMode) {
                Text(if (isEditMode) "Done" else "Edit")
            }
        }
    }

    // Clear top bar actions only when leaving HudView navigation screen
    DisposableEffect(Unit) {
        onDispose {
            onUpdateTopBarActions(null)
        }
    }

    HudContent(
        telemetry = telemetry,
        widgets = widgets,
        isEditMode = isEditMode,
        isRecording = isRecording,
        showAddDialog = showAddDialog,
        onDismissAddDialog = { showAddDialog = false },
        onAddWidgetSelected = { content ->
            viewModel.addWidget(content)
            showAddDialog = false
        },
        onMoveWidget = viewModel::moveWidget,
        onToggleWidgetSpan = viewModel::toggleWidgetSpan,
        onAdjustWidgetHeight = viewModel::adjustWidgetHeight,
        onRemoveWidget = viewModel::removeWidget,
        onStartRecording = viewModel::startRecording,
        onStopRecording = viewModel::stopRecording,
        modifier = modifier
    )
}

// Separated content composable for modular layout & previews
@Composable
fun HudContent(
    telemetry: TelemetryPoint?,
    widgets: List<WidgetConfig>,
    isEditMode: Boolean,
    isRecording: Boolean,
    showAddDialog: Boolean = false,
    onDismissAddDialog: () -> Unit = {},
    onAddWidgetSelected: (WidgetContent) -> Unit = {},
    onMoveWidget: (index: Int, offset: Int) -> Unit = { _, _ -> },
    onToggleWidgetSpan: (id: String) -> Unit = {},
    onAdjustWidgetHeight: (id: String, delta: Int) -> Unit = { _, _ -> },
    onRemoveWidget: (id: String) -> Unit = {},
    onStartRecording: () -> Unit = {},
    onStopRecording: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(
                items = widgets,
                key = { _, item -> item.id },
                span = { _, item -> GridItemSpan(item.span.spanCount) }
            ) { index, widget ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(widget.heightDp.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        WidgetRenderer(
                            content = widget.content,
                            telemetry = telemetry,
                            isRecording = isRecording,
                            onStartRecording = onStartRecording,
                            onStopRecording = onStopRecording
                        )

                        if (isEditMode) {
                            WidgetControlOverlay(
                                widget = widget,
                                canMoveUp = index > 0,
                                canMoveDown = index < widgets.size - 1,
                                onMoveUp = { onMoveWidget(index, -1) },
                                onMoveDown = { onMoveWidget(index, 1) },
                                onToggleWidth = { onToggleWidgetSpan(widget.id) },
                                onIncreaseHeight = { onAdjustWidgetHeight(widget.id, 30) },
                                onDecreaseHeight = { onAdjustWidgetHeight(widget.id, -30) },
                                onDelete = { onRemoveWidget(widget.id) }
                            )
                        }
                    }
                }
            }
        }

        if (showAddDialog) {
            AddWidgetDialog(
                alreadyAdded = widgets.map { it.content },
                onDismiss = onDismissAddDialog,
                onWidgetSelected = onAddWidgetSelected
            )
        }
    }
}
@Composable
private fun WidgetControlOverlay(
    widget: WidgetConfig,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onToggleWidth: () -> Unit,
    onIncreaseHeight: () -> Unit,
    onDecreaseHeight: () -> Unit,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f))
            .padding(6.dp)
    ) {
        Row(
            modifier = Modifier.align(Alignment.TopStart),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (canMoveUp) SmallButton("▲", onMoveUp)
            if (canMoveDown) SmallButton("▼", onMoveDown)
        }

        SmallButton("X", onDelete, modifier = Modifier.align(Alignment.TopEnd))

        Row(
            modifier = Modifier.align(Alignment.BottomEnd),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SmallButton(if (widget.span == WidgetSpan.FULL) "1/2 Width" else "Full Width", onToggleWidth)
            SmallButton("-H", onDecreaseHeight)
            SmallButton("+H", onIncreaseHeight)
        }
    }
}

@Composable
private fun SmallButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(32.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun AddWidgetDialog(
    alreadyAdded: List<WidgetContent> = emptyList(),
    onDismiss: () -> Unit,
    onWidgetSelected: (WidgetContent) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Widget") },
        text = {
            Column {
                WidgetContent.entries.forEach { option ->
                    if (option !in alreadyAdded) {
                        TextButton(
                        onClick = { onWidgetSelected(option) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(option.name)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Preview
@Composable
fun HudContentPreview() {
    MaterialTheme {
        HudContent(
            telemetry = null,
            widgets = emptyList(),
            isEditMode = true,
            isRecording = false,
            onDismissAddDialog = {},
            onAddWidgetSelected = {},
            onMoveWidget = { _, _ -> },
            onToggleWidgetSpan = {},
            onAdjustWidgetHeight = { _, _ -> },
            onRemoveWidget = {},
            onStartRecording = {},
            onStopRecording = {}
        )
    }
}