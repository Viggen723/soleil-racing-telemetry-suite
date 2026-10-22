package com.example.soleilracingproject.features.NavHost

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.soleilracingproject.features.analysis.ui.AnalysisView
import com.example.soleilracingproject.features.analysis.viewmodel.AnalysisViewModel
import com.example.soleilracingproject.features.garage.ui.GarageView
import com.example.soleilracingproject.features.garage.viewmodel.GarageViewmodel
import com.example.soleilracingproject.features.hud.ui.HudView
import com.example.soleilracingproject.features.hud.viewmodel.HudViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MainNavHost(
    navHostController: NavHostController,
    startDestination: Route = Route.Hud,
    onUpdateTopBarActions: ((@Composable RowScope.() -> Unit)?) -> Unit = {},
    onFocusModeChange: (Boolean) -> Unit = {},
    onRecordingTimeChange: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val hudViewModel: HudViewModel = koinViewModel()

    NavHost(
        navController = navHostController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable<Route.Hud> {
            HudView(
                viewModel = hudViewModel,
                onUpdateTopBarActions = onUpdateTopBarActions,
                onFocusModeChange = onFocusModeChange,
                onRecordingTimeChange = onRecordingTimeChange
            )
        }

        composable<Route.Analysis> {
            val analysisViewModel: AnalysisViewModel = koinViewModel()
            LaunchedEffect(Unit) {
                onUpdateTopBarActions(null)
            }
            AnalysisView(viewModel = analysisViewModel)
        }

        composable<Route.Garage> {
            val garageViewmodel: GarageViewmodel = koinViewModel()
            LaunchedEffect(Unit) {
                onUpdateTopBarActions(null)
                onFocusModeChange(false)
            }
            GarageView(viewModel = garageViewmodel)
        }
    }
}