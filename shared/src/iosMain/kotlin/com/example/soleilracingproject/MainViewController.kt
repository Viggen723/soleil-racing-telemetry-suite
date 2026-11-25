package com.example.soleilracingproject

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController
import com.example.soleilracingproject.di.initIosKoin
import com.example.soleilracingproject.features.navHost.AppNavigationBar
import theme.Theme

fun MainViewController(): platform.UIKit.UIViewController {
    initIosKoin()
    return ComposeUIViewController {
        App()
        Theme.AppTheme {
            Surface(modifier = Modifier.fillMaxSize()) {
                AppNavigationBar(modifier = Modifier)
            }
        }
    }
}
