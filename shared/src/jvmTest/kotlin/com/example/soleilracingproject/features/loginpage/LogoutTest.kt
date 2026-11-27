package com.example.soleilracingproject.features.loginpage

import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.Composition
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModelStore
import com.example.soleilracingproject.features.loginpage.data.LoginAuthentication
import com.example.soleilracingproject.features.loginpage.viewmodel.LoginpageViewmodel
import com.example.soleilracingproject.features.navhost.AccountNavigation
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LogoutTest {
    @Test
    fun signOutWaitsForCompletionAndIgnoresAnotherTap() = withLogin { auth, login ->
        auth.signOutGate = CompletableDeferred()
        login.logout()
        login.logout()
        assertEquals(1, auth.signOutCalls)
        assertTrue(login.isLoggingOut.value)
        assertEquals("alice", login.authenticatedUserId.value)
        auth.signOutGate?.complete(Unit)
        login.authenticatedUserId.first { it == null }
        assertFalse(login.isLoggingOut.value)
        assertNull(login.logoutError.value)
    }

    @Test
    fun signOutFailureKeepsAuthenticatedStateAndReportsError() = withLogin { auth, login ->
        auth.failSignOut = true
        login.logout()
        assertEquals(1, auth.signOutCalls)
        assertEquals("alice", login.authenticatedUserId.value)
        assertEquals("Could not log out. Please try again.", login.logoutError.value)
        assertFalse(login.isLoggingOut.value)
    }

    @Test
    fun activeRecordingBlocksSignOut() = withLogin(isRecording = { true }) { auth, login ->
        login.logout()
        assertEquals(0, auth.signOutCalls)
        assertEquals("alice", login.authenticatedUserId.value)
        assertEquals("Stop recording before logging out.", login.logoutError.value)
    }

    @Test
    fun logoutDisposesAuthenticatedCompositionAndLoginAgainBuildsItFresh() = withLogin { auth, login ->
        withContext(TestFrameClock) {
            val recomposer = Recomposer(coroutineContext)
            val runner = launch { recomposer.runRecomposeAndApplyChanges() }
            val composition = Composition(NoUiApplier(), recomposer)
            val authenticatedState = MutableStateFlow<MutableList<String>?>(null)
            val loginVisible = MutableStateFlow(false)
            try {
                composition.setContent {
                    val uid by login.authenticatedUserId.collectAsState()
                    AccountNavigation(uid, signedOut = {
                        DisposableEffect(Unit) {
                            loginVisible.value = true
                            onDispose { loginVisible.value = false }
                        }
                    }) {
                        DisposableEffect(Unit) {
                            authenticatedState.value = mutableListOf("hud")
                            onDispose { authenticatedState.value = null }
                        }
                    }
                }
                val first = authenticatedState.filterNotNull().first()
                first.addAll(listOf("analysis", "settings"))

                login.logout()
                loginVisible.first { it }
                assertNull(authenticatedState.value)

                login.login("alice@example.test", "password")
                val fresh = authenticatedState.filterNotNull().first()
                assertNotSame(first, fresh)
                assertEquals(listOf("hud"), fresh)

                auth.uid.value = null // Firebase reports an external sign-out.
                loginVisible.first { it }
                assertNull(authenticatedState.value)
            } finally {
                composition.dispose()
                recomposer.cancel()
                runner.cancelAndJoin()
            }
        }
    }

    private fun withLogin(
        isRecording: () -> Boolean = { false },
        test: suspend (FakeAuth, LoginpageViewmodel) -> Unit,
    ) = runBlocking {
        withTimeout(10_000) {
            withContext(Dispatchers.Main) {
                val auth = FakeAuth()
                val store = ViewModelStore()
                val login = LoginpageViewmodel(auth, isRecording)
                store.put("login", login)
                try {
                    test(auth, login)
                } finally {
                    store.clear()
                }
            }
        }
    }

    private class FakeAuth : LoginAuthentication {
        val uid = MutableStateFlow<String?>("alice")
        override val currentUserId get() = uid.value
        override val userIdChanges = uid
        var signOutCalls = 0
        var failSignOut = false
        var signOutGate: CompletableDeferred<Unit>? = null

        override suspend fun logout() {
            signOutCalls++
            check(!failSignOut)
            uid.value = null
            signOutGate?.await()
        }

        override suspend fun login(email: String, pass: String): Nothing? {
            uid.value = email.substringBefore('@')
            return null
        }

        override suspend fun register(email: String, pass: String) = login(email, pass)
    }

    private object TestFrameClock : MonotonicFrameClock {
        override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R {
            delay(1)
            return onFrame(System.nanoTime())
        }
    }

    private class NoUiApplier : AbstractApplier<Unit>(Unit) {
        override fun insertTopDown(index: Int, instance: Unit) = Unit
        override fun insertBottomUp(index: Int, instance: Unit) = Unit
        override fun remove(index: Int, count: Int) = Unit
        override fun move(from: Int, to: Int, count: Int) = Unit
        override fun onClear() = Unit
    }
}
