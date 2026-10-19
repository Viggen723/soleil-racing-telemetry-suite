package com.example.soleilracingproject.features.hud.ui

import HudViewModel
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.soleilracingproject.features.hud.ui.widget.*
import com.example.soleilracingproject.model.telemetry.TelemetryPoint
import com.example.soleilracingproject.util.tools.lapLogic.LapDeltas

// How much bigger the main (center) widget is than each side widget
private const val MAIN_SIZE = 1.5f

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
    val lapDeltas by viewModel.lapDeltas.collectAsStateWithLifecycle()

    // Which widget is being replaced right now (null = the list is closed)
    var replacingWidgetId by remember { mutableStateOf<String?>(null) }

    // Focus mode (full screen): only while recording AND moving, and never while editing.
    // Before, it switched on whenever speed was above 10. That hid the Edit button,
    // so nobody could edit while data was coming in.
    val isFocusMode = isRecording && !isEditMode && (telemetry?.speed ?: 0f) > 10f
    LaunchedEffect(isFocusMode) {
        onFocusModeChange(isFocusMode)
    }

    // Top bar: just the Edit / Done button now
    LaunchedEffect(isEditMode) {
        onUpdateTopBarActions {
            IconButton(onClick = viewModel::toggleEditMode) {
                Text(if (isEditMode) "Done" else "Edit")
            }
        }
    }

    // Clear top bar actions only when leaving the HUD screen
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
        replacingWidgetId = replacingWidgetId,
        onReplaceClick = { id -> replacingWidgetId = id },
        onDismissDialog = { replacingWidgetId = null },
        onReplaceSelected = { content ->
            replacingWidgetId?.let { viewModel.replaceWidget(it, content) }
            replacingWidgetId = null
        },
        onMakeMain = viewModel::makeMain,
        onStartRecording = viewModel::startRecording,
        onStopRecording = viewModel::stopRecording,
        modifier = modifier,
        lapDeltas = lapDeltas
    )
}

// Separated content composable for modular layout & previews
@Composable
fun HudContent(
    telemetry: TelemetryPoint?,
    widgets: List<WidgetConfig>,
    isEditMode: Boolean,
    isRecording: Boolean,
    replacingWidgetId: String? = null,
    onReplaceClick: (id: String) -> Unit = {},
    onDismissDialog: () -> Unit = {},
    onReplaceSelected: (WidgetContent) -> Unit = {},
    onMakeMain: (index: Int) -> Unit = {},
    onStartRecording: () -> Unit = {},
    onStopRecording: () -> Unit = {},
    modifier: Modifier = Modifier,
    lapDeltas: LapDeltas = LapDeltas(),
) {
    // One widget card. index 0 = the main widget, index 1 and 2 = the side widgets.
    // We write it once here so we don't repeat it for portrait and landscape.
    val widgetCard: @Composable (Int, Modifier) -> Unit = { index, cardModifier ->
        val widget = widgets.getOrNull(index)
        if (widget != null) {
            Card(modifier = cardModifier) {
                Box(modifier = Modifier.fillMaxSize()) {
                    WidgetRenderer(
                        content = widget.content,
                        telemetry = telemetry,
                        isRecording = isRecording,
                        onStartRecording = onStartRecording,
                        onStopRecording = onStopRecording,
                        lapDeltas = lapDeltas,
                    )

                    if (isEditMode) {
                        WidgetControlOverlay(
                            isMain = index == 0,
                            canReplace = widget.content != WidgetContent.RECORDING_BUTTON,
                            onReplace = { onReplaceClick(widget.id) },
                            onMakeMain = { onMakeMain(index) }
                        )
                    }
                }
            }
        }
    }

    // BoxWithConstraints tells us how big the screen is, so we know if it is wide or tall
    BoxWithConstraints(modifier = modifier.fillMaxSize().padding(12.dp)) {
        val isLandscape = maxWidth > maxHeight

        if (isLandscape) {
            // Landscap screen: side | main | side, left to right
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                widgetCard(1, Modifier.weight(1f).fillMaxHeight())
                widgetCard(0, Modifier.weight(MAIN_SIZE).fillMaxHeight())
                widgetCard(2, Modifier.weight(1f).fillMaxHeight())
            }
        } else {
            // Potrait screen: side | main | side, top to bottom
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                widgetCard(1, Modifier.weight(1f).fillMaxWidth())
                widgetCard(0, Modifier.weight(MAIN_SIZE).fillMaxWidth())
                widgetCard(2, Modifier.weight(1f).fillMaxWidth())
            }
        }
    }

    // The "pick a widget" list, shown only after Replace was pressed
    if (replacingWidgetId != null) {
        AddWidgetDialog(
            alreadyAdded = widgets.map { it.content },
            onDismiss = onDismissDialog,
            onWidgetSelected = onReplaceSelected
        )
    }
}

@Composable
private fun WidgetControlOverlay(
    isMain: Boolean,
    canReplace: Boolean,
    onReplace: () -> Unit,
    onMakeMain: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (canReplace) SmallButton("Replace", onReplace)
            // the main widget can't be made main again
            if (!isMain) SmallButton("Make main", onMakeMain)
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
        title = { Text("Choose a widget") },
        text = {
            Column {
                WidgetContent.entries.forEach { option ->
                    // only show widgets that are not on the screen already
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
            isRecording = false
        )
    }
}