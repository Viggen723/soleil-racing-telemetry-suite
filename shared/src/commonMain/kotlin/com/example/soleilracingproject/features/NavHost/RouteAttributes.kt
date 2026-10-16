package com.example.soleilracingproject.features.NavHost

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Dataset
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.ui.graphics.vector.ImageVector

data class NavItem(
    val route: Route,
    val title: String,
    val icon: ImageVector,
)

val navItems = listOf(
    NavItem(Route.Hud, "HUD", Icons.Filled.Dataset),
    NavItem(Route.Analysis, "Analysis", Icons.Filled.Analytics),
    NavItem(Route.Garage, "Garage", Icons.Filled.DirectionsCar)
)
