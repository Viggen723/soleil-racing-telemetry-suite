package com.example.soleilracingproject.features.analysis.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.soleilracingproject.model.analysis.Lap
import com.example.soleilracingproject.util.tools.ConversionTools.formatLapTime

@Composable
fun LapSelectorRow(
    laps: List<Lap>,
    selectedLapNumber: Int?,
    onLapSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (laps.isEmpty()) return

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "SELECT LAP",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            laps.forEach { lap ->
                val isSelected = lap.lapNumber == selectedLapNumber
                val formattedTime = lap.lapTime.formatLapTime()

                FilterChip(
                    selected = isSelected,
                    onClick = { onLapSelected(lap.lapNumber) },
                    label = { Text("Lap ${lap.lapNumber} ($formattedTime)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                )
            }
        }
    }
}
