package com.example.soleilracingproject

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.lifecycle.lifecycleScope
import com.example.soleilracingproject.features.disclaimer.ui.DisclaimerView
import com.example.soleilracingproject.features.hud.ui.HudView
import com.example.soleilracingproject.features.hud.ui.HudViewModel
import com.example.soleilracingproject.features.loginpage.ui.LoginpageView
import com.example.soleilracingproject.remote.udp.AndroidUdpSocketListener
import com.example.soleilracingproject.remote.udp.TelemetryPacketParser
import com.example.soleilracingproject.remote.udp.UdpSocketListener
import com.example.soleilracingproject.util.AndroidUdpParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import theme.Theme

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
                // Temporary host-only login success wiring. Replace with real
                // authentication/navigation later; no credentials are validated.
                var showHud by remember { mutableStateOf(false) }
                var showDisclaimer by remember { mutableStateOf(false) }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (showHud) {
                        hudView.Content(viewModel = viewModel)
                        if (showDisclaimer) {
                            DisclaimerView(onAgree = { showDisclaimer = false })
                        }
                    } else {
                        LoginpageView(
                            onLoginClick = {
                                showHud = true
                                showDisclaimer = true
                            }
                        )
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
