package com.example.soleilracingproject.features.SharedFeatureUi

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import soleilracingproject.shared.generated.resources.Res
import soleilracingproject.shared.generated.resources.soleil_trimmed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoleilTopBar() {
    TopAppBar(
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Image(
                    painter = painterResource(Res.drawable.soleil_trimmed),
                    contentDescription = "Soleil logo",
                    modifier = Modifier.size(100.dp)
                )
            }
        }
    )
}