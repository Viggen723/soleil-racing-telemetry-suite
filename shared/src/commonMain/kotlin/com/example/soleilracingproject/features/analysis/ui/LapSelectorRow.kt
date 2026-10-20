package com.example.soleilracingproject.features.analysis.ui

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.soleilracingproject.model.analysis.Lap
import com.example.soleilracingproject.util.tools.ConversionTools.formatLapTime

@Composable
fun LapSelectorRow(
    laps: List<Lap>,
    selectedLapNumber: Int?,
    comparisonLapNumber: Int?,
    onLapSelected: (Int) -> Unit,
    onComparisonLapSelected: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (laps.isEmpty()) return

    val primaryColor = Color(0xFFD71344) // Soleil Red
    val comparisonColor = Color(0xFF00E5FF) // Cyan

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "SELECT LAP  (LONG PRESS TO COMPARE)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )

            if (comparisonLapNumber != null) {
                TextButton(
                    onClick = { onComparisonLapSelected(null) },
                    modifier = Modifier.padding(0.dp),
                ) {
                    Text(
                        text = "Stop Comparing",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            laps.forEach { lap ->
                val isPrimary = lap.lapNumber == selectedLapNumber
                val isComparison = lap.lapNumber == comparisonLapNumber
                val formattedTime = lap.lapTime.formatLapTime()

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when {
                        isPrimary -> primaryColor
                        isComparison -> comparisonColor
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    contentColor = when {
                        isPrimary -> Color.White
                        isComparison -> Color.Black
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.combinedClickable(
                        onClick = {
                            if (isComparison) {
                                onComparisonLapSelected(null)
                            }
                            onLapSelected(lap.lapNumber)
                        },
                        onLongClick = {
                            if (!isPrimary) {
                                if (isComparison) {
                                    onComparisonLapSelected(null)
                                } else {
                                    onComparisonLapSelected(lap.lapNumber)
                                }
                            }
                        },
                    ),
                ) {
                    Text(
                        text = when {
                            isPrimary -> "Lap ${lap.lapNumber} ($formattedTime) [Primary]"
                            isComparison -> "Lap ${lap.lapNumber} ($formattedTime) [VS]"
                            else -> "Lap ${lap.lapNumber} ($formattedTime)"
                        },
                        fontWeight = if (isPrimary || isComparison) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}
