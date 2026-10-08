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
import com.example.soleilracingproject.features.hud.ui.HudView
import com.example.soleilracingproject.features.hud.ui.HudViewModel
import com.example.soleilracingproject.features.loginpage.ui.LoginpageView
import com.example.soleilracingproject.data.remote.udp.AndroidUdpSocketListener
import com.example.soleilracingproject.util.AndroidUdpParser
import theme.Theme

val SHOW_LOGIN_PREVIEW = true

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val udp = AndroidUdpSocketListener()
        val viewModel = HudViewModel(
            udpListener = udp,
            parseUdpData = AndroidUdpParser::parseUdpData
        )

        val hudView = HudView()

        setContent {
            Theme.AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (SHOW_LOGIN_PREVIEW) {
                        LoginpageView()
                    } else {
                        hudView.Content(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
