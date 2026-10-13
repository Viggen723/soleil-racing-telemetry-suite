package com.example.soleilracingproject

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.example.soleilracingproject.di.appModule
import com.example.soleilracingproject.di.sharedModule
import com.example.soleilracingproject.features.NavHost.AppNavigationBar
import com.example.soleilracingproject.features.NavHost.MainNavHost
import com.example.soleilracingproject.features.NavHost.Route
import theme.Theme
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            Theme.AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    val navController = rememberNavController()

                    AppNavigationBar(modifier = Modifier)
                }
            }
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    Theme.AppTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            AppNavigationBar(modifier = Modifier)
        }
    }
}