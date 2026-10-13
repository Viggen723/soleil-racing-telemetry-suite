package com.example.soleilracingproject.features.analysis.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.soleilracingproject.model.analysis.Lap
import com.example.soleilracingproject.model.telemetry.TelemetryPoint
import kotlin.time.Duration

@Composable
fun AnalysisView(modifier: Modifier = Modifier)
{
    AnalysisContent(modifier)
}

@Composable
fun AnalysisContent(modifier: Modifier = Modifier)
{
    Scaffold(modifier = Modifier.fillMaxSize())
    {

    }

}