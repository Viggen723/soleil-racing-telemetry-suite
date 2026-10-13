package com.example.soleilracingproject.features.hud.ui

import androidx.compose.foundation.Image
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
import com.example.soleilracingproject.features.SharedFeatureUi.SoleilTopBar
import com.example.soleilracingproject.features.hud.ui.widget.*
import com.example.soleilracingproject.model.telemetry.TelemetryPoint
import org.jetbrains.compose.resources.painterResource
import soleilracingproject.shared.generated.resources.Res
import soleilracingproject.shared.generated.resources.soleil_trimmed

@Composable
fun HudView(
    viewModel: HudViewModel,
    modifier: Modifier = Modifier
) {
    val telemetry by viewModel.telemetryState.collectAsStateWithLifecycle()
    val widgets by viewModel.widgetLayout.collectAsStateWithLifecycle()
    val isEditMode by viewModel.isEditMode.collectAsStateWithLifecycle()

    HudContent(
        telemetry = telemetry,
        widgets = widgets,
        isEditMode = isEditMode,
        onToggleEditMode = viewModel::toggleEditMode,
        onAddWidget = viewModel::addWidget,
        onMoveWidget = viewModel::moveWidget,
        onToggleWidgetSpan = viewModel::toggleWidgetSpan,
        onAdjustWidgetHeight = viewModel::adjustWidgetHeight,
        onRemoveWidget = viewModel::removeWidget,
        modifier = modifier
    )
}
// Had to separate the content to make it passable through the NavHost
@Composable
fun HudContent(
    telemetry: TelemetryPoint?,
    widgets: List<WidgetConfig>,
    isEditMode: Boolean,
    onToggleEditMode: () -> Unit,
    onAddWidget: (WidgetContent) -> Unit,
    onMoveWidget: (index: Int, offset: Int) -> Unit,
    onToggleWidgetSpan: (id: String) -> Unit,
    onAdjustWidgetHeight: (id: String, delta: Int) -> Unit,
    onRemoveWidget: (id: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { SoleilTopBar() },
                actions = {
                    if (isEditMode) {
                        Button(
                            onClick = { showAddDialog = true },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("+ Add Widget")
                        }
                    }
                    IconButton(onClick = onToggleEditMode) {
                        Text(if (isEditMode) "Done" else "Edit")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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
                            telemetry = telemetry
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
                onDismiss = { showAddDialog = false },
                onWidgetSelected = { content ->
                    onAddWidget(content)
                    showAddDialog = false
                }
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
    onDismiss: () -> Unit,
    onWidgetSelected: (WidgetContent) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Widget") },
        text = {
            Column {
                WidgetContent.entries.forEach { option ->
                    TextButton(
                        onClick = { onWidgetSelected(option) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(option.name)
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
            onToggleEditMode = {},
            onAddWidget = {},
            onMoveWidget = { _, _ -> },
            onToggleWidgetSpan = {},
            onAdjustWidgetHeight = { _, _ -> },
            onRemoveWidget = {}
        )
    }
}