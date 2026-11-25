package com.example.soleilracingproject.features.hud.ui

import com.example.soleilracingproject.model.settings.SpeedUnit
import com.example.soleilracingproject.features.hud.viewmodel.HudViewModel
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.soleilracingproject.features.hud.ui.widget.*
import com.example.soleilracingproject.model.telemetry.TelemetryPoint
import com.example.soleilracingproject.util.tools.lapLogic.LapDeltas
import com.example.soleilracingproject.features.hud.ui.widget.widgetTypes.DeltaMode
import com.example.soleilracingproject.model.analysis.Lap
import kotlinx.coroutines.launch

// How much bigger the main (center) widget is than each side widget
private const val MAIN_SIZE = 1.5f

@Composable
fun HudView(
    viewModel: HudViewModel,
    onUpdateTopBarActions: ((@Composable RowScope.() -> Unit)?) -> Unit = {},
    onFocusModeChange: (Boolean) -> Unit = {},
    onRecordingTimeChange: (Long) -> Unit = {},
    onNavigateToCustomTracks: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val telemetry by viewModel.telemetryState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val widgets by viewModel.widgetLayout.collectAsStateWithLifecycle()
    val isEditMode by viewModel.isEditMode.collectAsStateWithLifecycle()
    val isRecording by viewModel.isRecording.collectAsStateWithLifecycle()
    val lapDeltas by viewModel.lapDeltas.collectAsStateWithLifecycle()
    val recentPoints by viewModel.recentPoints.collectAsStateWithLifecycle()
    val deltaMode by viewModel.deltaMode.collectAsStateWithLifecycle()
    val laps by viewModel.lapsState.collectAsStateWithLifecycle()
    val recordingSeconds by viewModel.recordingSeconds.collectAsStateWithLifecycle()
    val isLoggerConnected by viewModel.isLoggerConnected.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }    // holds the little message
    val scope = rememberCoroutineScope()
    LaunchedEffect(recordingSeconds) {
        onRecordingTimeChange(recordingSeconds)    // tell the navigation bar new time
    }

    // Which widget is being replaced right now (null = the list is closed)
    var replacingWidgetId by remember { mutableStateOf<String?>(null) }

    // Focus mode (full screen): only while recording and moving, and never while editing.
    val isFocusMode = settings.focusModeEnabled && isRecording && !isEditMode && (telemetry?.speed ?: 0f) > 10f
    LaunchedEffect(isFocusMode) {
        onFocusModeChange(isFocusMode)
    }

    // Top bar: Dropdown Menu with "Edit Layout" and "Custom Tracks"
    var showMenu by remember { mutableStateOf(false) }
    LaunchedEffect(isEditMode, showMenu) {
        onUpdateTopBarActions {
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options"
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(if (isEditMode) "Done Editing" else "Edit Layout") },
                        onClick = {
                            viewModel.toggleEditMode()
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Custom Tracks") },
                        onClick = {
                            showMenu = false
                            onNavigateToCustomTracks()
                        }
                    )
                }
            }
        }
    }

    // Clear top bar actions only when leaving the HUD screen
    DisposableEffect(Unit) {
        viewModel.reconnectUdp()
        onDispose {
            onUpdateTopBarActions(null)
            onFocusModeChange(false)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
    HudContent(
        telemetry = if (isLoggerConnected) telemetry else null,
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
        isLoggerConnected = isLoggerConnected,
        onStartRecording = {
            if (isLoggerConnected) {
                viewModel.startRecording()
            } else {
                // If there is no logger connected don't start, tell the user instead
                scope.launch {
                    snackbarHostState.currentSnackbarData?.dismiss()    // replace an older message
                    snackbarHostState.showSnackbar("Logger disconnected. Connect the logger to start recording.")
                }
            }
        },
        onStopRecording = viewModel::stopRecording,
        lapDeltas = lapDeltas,
        recentPoints = recentPoints,
        deltaMode = deltaMode,
        onNextDeltaMode = viewModel::nextDeltaMode,
        laps = laps,
        speedometerMax = settings.speedometerMax,
        speedUnit = settings.speedUnit,
        onLongPressWidget = {
            // Same rule as the edit button: never while the screen is in full-screen driving mode
            if (!isEditMode && !isFocusMode) viewModel.toggleEditMode()
        },
    )
        SnackbarHost(                                                  //where the message appears
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
        )
}
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
    isLoggerConnected: Boolean = true,
    modifier: Modifier = Modifier,
    lapDeltas: LapDeltas = LapDeltas(),
    recentPoints: List<TelemetryPoint> = emptyList(),
    deltaMode: DeltaMode = DeltaMode.BEST_LAP,
    onNextDeltaMode: () -> Unit = {},
    laps: List<Lap> = emptyList(),
    onLongPressWidget: () -> Unit = {},
    speedometerMax: Float = 180f,
    speedUnit: SpeedUnit = SpeedUnit.MPH,
) {
    // One widget card. index 0 = the main widget, index 1 and 2 = the side widgets.
    // We write it once here so we don't repeat it for portrait and landscape.
    val widgetCard: @Composable (Int, Modifier) -> Unit = { index, cardModifier ->
        val widget = widgets.getOrNull(index)
        if (widget != null) {
            Card(modifier = cardModifier.onLongPress(onLongPressWidget)) {
                Box(modifier = Modifier.fillMaxSize()) {
                    WidgetRenderer(
                        content = widget.content,
                        telemetry = telemetry,
                        isRecording = isRecording,
                        onStartRecording = onStartRecording,
                        onStopRecording = onStopRecording,
                        lapDeltas = lapDeltas,
                        recentPoints = recentPoints,
                        deltaMode = deltaMode,
                        onNextDeltaMode = onNextDeltaMode,
                        laps = laps,
                        speedometerMax = speedometerMax,
                        speedUnit = speedUnit,
                        isLoggerConnected = isLoggerConnected,
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
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f))
            .pointerInput(Unit) { detectTapGestures { } },
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
                            Text(option.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() })
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
