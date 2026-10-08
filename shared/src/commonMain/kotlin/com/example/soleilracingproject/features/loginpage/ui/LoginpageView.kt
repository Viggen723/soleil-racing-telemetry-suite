package com.example.soleilracingproject.features.loginpage.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import soleilracingproject.shared.generated.resources.Res
import soleilracingproject.shared.generated.resources.soleil_full
import soleilracingproject.shared.generated.resources.soleil_trimmed
import theme.Theme

private enum class AuthView {
    LANDING,
    LOGIN,
    CREATE_ACCOUNT
}

@Composable
fun LoginpageView(
    onLoginClick: () -> Unit = {},
    onNewUserClick: () -> Unit = {}
) {
    var authView by remember { mutableStateOf(AuthView.LANDING) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var createAccountUsername by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var createAccountPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    val brandingWeight by animateFloatAsState(
        targetValue = if (authView == AuthView.LANDING) 0.6f else 0.28f,
        animationSpec = tween(400),
        label = "Branding section size"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeContentPadding()
            .imePadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(brandingWeight)
                .padding(start = 32.dp, top = 32.dp, end = 32.dp, bottom = 12.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.BottomCenter
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = authView == AuthView.LANDING,
                    enter = fadeIn(tween(400)),
                    exit = fadeOut(tween(4))
                ) {
                    Image(
                        painter = painterResource(Res.drawable.soleil_full),
                        contentDescription = "Soleil Racing Project",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                androidx.compose.animation.AnimatedVisibility(
                    visible = authView != AuthView.LANDING,
                    enter = fadeIn(tween(400)),
                    exit = fadeOut(tween(4))
                ) {
                    Image(
                        painter = painterResource(Res.drawable.soleil_trimmed),
                        contentDescription = "Soleil",
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    )
                }
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f - brandingWeight),
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 24.dp, top = 16.dp, end = 24.dp, bottom = 24.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = authView == AuthView.LANDING,
                    enter = fadeIn(tween(350)) + slideInVertically(tween(350)) { it / 8 },
                    exit = fadeOut(tween(350)) + slideOutVertically(tween(350)) { it / 8 }
                ) {
                    Column(
                        modifier = Modifier.widthIn(max = 360.dp).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(
                            onClick = { authView = AuthView.LOGIN },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
                        ) {
                            Text("Login", style = MaterialTheme.typography.titleMedium)
                        }
                        OutlinedButton(
                            onClick = { authView = AuthView.CREATE_ACCOUNT },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
                        ) {
                            Text("Create Account", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
                androidx.compose.animation.AnimatedVisibility(
                    visible = authView == AuthView.LOGIN,
                    enter = fadeIn(tween(350)) + slideInVertically(tween(350)) { it / 8 },
                    exit = fadeOut(tween(350)) + slideOutVertically(tween(350)) { it / 8 }
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 360.dp)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Username") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Password") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                        )
                        Button(
                            onClick = onLoginClick,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
                        ) {
                            Text("Login", style = MaterialTheme.typography.titleMedium)
                        }
                        TextButton(onClick = {
                            authView = AuthView.LANDING
                            username = ""
                            password = ""
                        }) {
                            Text("Back")
                        }
                    }
                }
                androidx.compose.animation.AnimatedVisibility(
                    visible = authView == AuthView.CREATE_ACCOUNT,
                    enter = fadeIn(tween(350)) + slideInVertically(tween(350)) { it / 8 },
                    exit = fadeOut(tween(350)) + slideOutVertically(tween(350)) { it / 8 }
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 360.dp)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedTextField(
                            value = createAccountUsername,
                            onValueChange = { createAccountUsername = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Username") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Email") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                        )
                        OutlinedTextField(
                            value = createAccountPassword,
                            onValueChange = { createAccountPassword = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Password") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                        )
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Confirm Password") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                        )
                        Button(
                            onClick = {},
                            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("Create Account", style = MaterialTheme.typography.titleMedium)
                        }
                        TextButton(onClick = {
                            authView = AuthView.LANDING
                            createAccountUsername = ""
                            email = ""
                            createAccountPassword = ""
                            confirmPassword = ""
                        }) {
                            Text("Back")
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun LoginpagePreview() {
    Theme.AppTheme {
        LoginpageView()
    }
}
