package com.example.soleilracingproject

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import com.example.soleilracingproject.features.navHost.MainNavHost
import com.example.soleilracingproject.features.analysis.data.TrackRepository
import org.koin.compose.koinInject

@Composable
fun App() {
    val trackRepository = koinInject<TrackRepository>()

    LaunchedEffect(Unit) {
        trackRepository.seedInitialTracksIfNeeded()
    }
}
