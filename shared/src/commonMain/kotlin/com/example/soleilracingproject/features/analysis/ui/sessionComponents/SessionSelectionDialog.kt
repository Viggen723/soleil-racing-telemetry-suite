package com.example.soleilracingproject.features.analysis.ui.sessionComponents

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.soleilracingproject.model.analysis.Session

@Composable
fun SessionSelectionDialog(
    sessions: List<Session>,
    onDismissRequest: () -> Unit,
    onSessionSelected: (Session) -> Unit,
    onDeleteSession: (Session) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(text = "Select a Session", fontWeight = FontWeight.Bold)
        },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(sessions) { session ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSessionSelected(session) }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = session.title,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = session.track,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        IconButton(
                            onClick = { onDeleteSession(session) },
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Session",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                    HorizontalDivider(thickness = 0.5.dp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Close")
            }
        },
    )
}
