package com.example.soleilracingproject.features.navhost

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
                androidx.compose.material3.Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 3.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .height(50.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        navItems.forEach { item ->
                            val isSelected = currentRoute == item.route

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable(
                                        onClick = {
                                            if (currentRoute != item.route) {
                                                currentRoute = item.route
                                                navController.navigate(item.route) {
                                                    launchSingleTop = true
                                                }
                                            }
                                        }
                                    )
                                    .padding(vertical = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
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