package com.example.soleilracingproject.features.hud.ui.widget.widgetTypes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.example.soleilracingproject.util.tools.lapLogic.LapDeltas
import kotlin.math.abs
import kotlin.math.roundToInt

// The kinds of delta the widget can show. To add a new one, add it here and in the "when" below.
enum class DeltaMode(val title: String) {
    BEST_LAP("VS BEST LAP"),
    LAST_LAP("VS LAST LAP")
}

// 0.35 turns into "+0.35"     -1.2 turns into "-1.20"
private fun formatDelta(seconds: Float): String {
    val hundredths = (abs(seconds) * 100).roundToInt()
    val sign = if (seconds < 0f && hundredths != 0) "-" else "+"
    return sign + (hundredths / 100) + "." + (hundredths % 100).toString().padStart(2, '0')
}

@Composable
fun DeltaWidget(deltas: LapDeltas, isRecording: Boolean, mode: DeltaMode, onNextMode: () -> Unit) {

    val delta = when (mode) {
        DeltaMode.BEST_LAP -> deltas.vsBestSeconds
        DeltaMode.LAST_LAP -> deltas.vsLastSeconds
    }

    val numberColor = when {
        delta == null -> MaterialTheme.colorScheme.outline
        delta < 0f -> Color(0xFF1B5E20)      // ahead of the reference = green
        delta > 0f -> Color(0xFFB71C1C)      // behind the reference = red
        else -> MaterialTheme.colorScheme.onSurface
    }

    val hint = when {
        delta != null -> "SECONDS"
        isRecording -> "FINISH A LAP FIRST"
        else -> "PRESS START"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onNextMode() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = mode.title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )
        Text(
            text = if (delta != null) formatDelta(delta) else "--",
            style = MaterialTheme.typography.displayLarge.copy(fontSize = 32.sp, fontWeight = FontWeight.Bold),
            color = numberColor
        )
        Text(
            text = hint,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center
        )
        Text(
            text = "TAP TO CHANGE",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}