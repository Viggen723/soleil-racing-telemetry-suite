package com.example.soleilracingproject.features.loginpage.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.soleilracingproject.features.loginpage.data.LoginAuthentication
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginpageViewmodel(
    private val repository: LoginAuthentication,
    private val isRecording: () -> Boolean,
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _authenticatedUserId = MutableStateFlow(repository.currentUserId)
    val authenticatedUserId = _authenticatedUserId.asStateFlow()

    private val _isLoggingOut = MutableStateFlow(false)
    val isLoggingOut = _isLoggingOut.asStateFlow()

    private val _logoutError = MutableStateFlow<String?>(null)
    val logoutError = _logoutError.asStateFlow()

    init {
        viewModelScope.launch {
            repository.userIdChanges.collect { uid ->
                // A local sign-out event can arrive before the operation returns.
                if (!_isLoggingOut.value) _authenticatedUserId.value = uid
            }
        }
    }

    fun login(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _error.value = "Email and Password cannot be empty."
            return
        }
        _isLoading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                repository.login(email, pass)
            } catch (e: Exception) {
                _error.value = e.message ?: "Login failed."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun register(email: String, pass: String, passConfirm: String) {
        if (email.isBlank() || pass.isBlank()) {
            _error.value = "Email and Password cannot be empty."
            return
        }
        if (pass != passConfirm) {
            _error.value = "Passwords do not match."
            return
        }
        _isLoading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                repository.register(email, pass)
            } catch (e: Exception) {
                _error.value = e.message ?: "Registration failed."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun logout() {
        if (!_isLoggingOut.compareAndSet(expect = false, update = true)) return
        _logoutError.value = null
        viewModelScope.launch {
            try {
                if (isRecording()) {
                    _logoutError.value = "Stop recording before logging out."
                    return@launch
                }
                repository.logout()
                _error.value = null
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _logoutError.value = "Could not log out. Please try again."
            } finally {
                _authenticatedUserId.value = repository.currentUserId
                _isLoggingOut.value = false
            }
        }
    }

    fun clearLogoutError() {
        _logoutError.value = null
    }

    fun clearError() {
        _error.value = null
    }
}
