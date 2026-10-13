package com.example.soleilracingproject.features.NavHost

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.soleilracingproject.features.analysis.ui.AnalysisView
import com.example.soleilracingproject.features.hud.ui.HudView
import com.example.soleilracingproject.features.hud.ui.HudViewModel
import org.koin.compose.koinInject

@Composable
fun MainNavHost(
    navHostController: NavHostController,
    startDestination: Route = Route.Hud,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navHostController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable<Route.Hud> {
            val hudViewModel: HudViewModel = koinInject()
            HudView(viewModel = hudViewModel)
        }

        composable<Route.Analysis> {
            AnalysisView()
        }
    }
}