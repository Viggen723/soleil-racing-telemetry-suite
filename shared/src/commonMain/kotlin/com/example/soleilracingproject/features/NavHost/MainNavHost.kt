package com.example.soleilracingproject.features.NavHost

import HudViewModel
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.soleilracingproject.features.analysis.ui.AnalysisView
import com.example.soleilracingproject.features.hud.ui.HudView
import com.example.soleilracingproject.features.garage.ui.GarageView
import com.example.soleilracingproject.features.garage.viewmodel.GarageViewmodel
import org.koin.compose.koinInject

@Composable
fun MainNavHost(
    navHostController: NavHostController,
    startDestination: Route = Route.Hud,
    onUpdateTopBarActions: ((@Composable RowScope.() -> Unit)?) -> Unit = {},
    onFocusModeChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navHostController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable<Route.Hud> {
            val hudViewModel: HudViewModel = koinInject()
            HudView(
                viewModel = hudViewModel,
                onUpdateTopBarActions = onUpdateTopBarActions,
                onFocusModeChange = onFocusModeChange
            )
        }

        composable<Route.Analysis> {
            val analysisViewModel: com.example.soleilracingproject.features.analysis.viewmodel.AnalysisViewModel = koinInject()
            LaunchedEffect(Unit) {
                onUpdateTopBarActions(null)
            }
            AnalysisView(viewModel = analysisViewModel)
        }
        composable<Route.Garage> {
            val garageViewmodel: GarageViewmodel = koinInject()
            LaunchedEffect(Unit) {
                onUpdateTopBarActions(null)
                onFocusModeChange(false)
            }
            GarageView(viewModel = garageViewmodel)
        }
    }
}
