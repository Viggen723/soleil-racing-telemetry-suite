package com.example.soleilracingproject.features.navhost

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.soleilracingproject.features.NavHost.MainNavHost
import com.example.soleilracingproject.features.NavHost.Route
import com.example.soleilracingproject.features.NavHost.navItems
import com.example.soleilracingproject.features.shared.SoleilTopBar

// https://developer.android.com/develop/ui/compose/components/navigation-bar?hl=ja
@Composable
fun AppNavigationBar(
    modifier: Modifier = Modifier,
    isFocusModeParam: Boolean = false,
) {
    val navController = rememberNavController()
    var currentRoute by remember { mutableStateOf<Route>(Route.Hud) }
    var isFocusMode by remember { mutableStateOf(isFocusModeParam) }
    var topBarActions by remember { mutableStateOf<(@Composable RowScope.() -> Unit)?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            AnimatedVisibility(
                visible = !isFocusMode,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                SoleilTopBar(
                    actions = { topBarActions?.invoke(this) }
                )
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = !isFocusMode,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                NavigationBar {
                    navItems.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = {
                                if (currentRoute != item.route) {
                                    currentRoute = item.route
                                    navController.navigate(item.route) {
                                        launchSingleTop = true
                                    }
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.title) },
                            label = { Text(item.title) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary,
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        MainNavHost(
            navHostController = navController,
            onUpdateTopBarActions = { actions -> topBarActions = actions },
            onFocusModeChange = { focus -> isFocusMode = focus },
            modifier = Modifier.padding(padding)
        )
    }
}