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
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.soleilracingproject.features.navhost.AppNavigationBar
import theme.Theme
import android.Manifest
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts

class MainActivity : ComponentActivity() {
    // The pop-up that asks "Allow Soleil to send you notifications?"
    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // Android 13 and newer needs the user's OK to show notifications
        if (Build.VERSION.SDK_INT >= 33) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // Hide OS Navigation Bar
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.hide(WindowInsetsCompat.Type.navigationBars())

        setContent {
            Theme.AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    AppNavigationBar(modifier = Modifier)
                }
            }
        }
    }
    // Called when the app is no longer on screen (Home button, another app, locked screen).
    override fun onStop() {
        super.onStop()
        // isChangingConfigurations is true when the screen is only rotating, so we skip that case
        if (!isChangingConfigurations && RecordingService.isRunning) {
            RecordingService.remindUser(this)
        }
    }

    // The app is on screen again, so the reminder is no longer needed
    override fun onStart() {
        super.onStart()
        RecordingService.clearReminder(this)
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