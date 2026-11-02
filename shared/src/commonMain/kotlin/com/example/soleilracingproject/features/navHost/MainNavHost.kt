package com.example.soleilracingproject.features.navHost

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
import com.example.soleilracingproject.features.loginpage.ui.LoginpageView
import com.example.soleilracingproject.features.loginpage.viewmodel.LoginpageViewmodel
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.soleilracingproject.features.settings.ui.SettingsView
import com.example.soleilracingproject.features.settings.viewmodel.SettingsViewModel

@Composable
fun MainNavHost(
    navHostController: NavHostController,
    startDestination: Route = Route.Login,
    onUpdateTopBarActions: ((@Composable RowScope.() -> Unit)?) -> Unit = {},
    onFocusModeChange: (Boolean) -> Unit = {},
    onRecordingTimeChange: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val hudViewModel: HudViewModel = koinViewModel()
    val settings by hudViewModel.settings.collectAsStateWithLifecycle()
    // Also clear the nav's remembered focus state when HUD isn't on screen.
    LaunchedEffect(settings.focusModeEnabled) {
        if (!settings.focusModeEnabled) onFocusModeChange(false)
    }

    NavHost(
        navController = navHostController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable<Route.Login> {
            val loginViewModel: LoginpageViewmodel = koinViewModel()
            LaunchedEffect(Unit) {
                onUpdateTopBarActions(null)
                onFocusModeChange(false)
            }
            LoginpageView(
                viewModel = loginViewModel,
                onNavigateSuccess = {
                    navHostController.navigate(Route.Hud) {
                        popUpTo(Route.Login) { inclusive = true }
                    }
                }
            )
        }

        composable<Route.Settings> {
            val settingsViewModel: SettingsViewModel = koinViewModel()
            LaunchedEffect(Unit) {
                onUpdateTopBarActions(null)
                onFocusModeChange(false)
            }
            SettingsView(settingsViewModel)
        }
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
