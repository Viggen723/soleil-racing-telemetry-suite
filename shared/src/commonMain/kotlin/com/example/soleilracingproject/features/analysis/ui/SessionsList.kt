package com.example.soleilracingproject.features.analysis.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.soleilracingproject.features.analysis.ui.SessionStatItem
import com.example.soleilracingproject.model.analysis.Lap
import com.example.soleilracingproject.model.analysis.Session

@Composable
fun SessionLazyColumn(sessions: List<Session>)
{
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    )
    {
        items(sessions.size) { index ->
            SessionCard(sessions[index])
        }
    }
}

@Composable
fun SessionCard(session: Session)
{
    Card(
        modifier = Modifier,
        content = { SessionInfo(session) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    )
}

@Composable
fun SessionInfo(session: Session)
{
    Column(
        modifier = Modifier.padding(5.dp))
    {
        Text(
            modifier = Modifier.offset(x = 15.dp),
            textAlign = TextAlign.Left,
            text = "Session at ${session.track}",
            color = MaterialTheme.colorScheme.primary)
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        )
        {
            SessionStatItem("Track", "${session.track}")
            SessionStatItem("Laps", "${session.laps.size}")
            SessionStatItem("Best Lap", "${session.bestTimeFormatted}")
        }
    }

}

@Composable
fun SessionStatItem(label: String, value: String)
{
    Column(
        modifier = Modifier,
        verticalArrangement = Arrangement.spacedBy(5.dp),
        horizontalAlignment = Alignment.Start
    )
    {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 12.sp,
        )

        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }

}



@Preview
@Composable
fun SessionCardPreview()
{
    SessionCard(Session(
        "test",
        "test",
        "test",
        "test",
        1,
        "2",
        "1",
        true))
}