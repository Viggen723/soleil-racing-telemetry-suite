package com.example.soleilracingproject.features.loginpage.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.soleilracingproject.features.loginpage.data.LoginpageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginpageViewmodel(
    private val repository: LoginpageRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(repository.currentUser != null)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    init {
        viewModelScope.launch {
            repository.currentUserFlow.collect { user ->
                _isAuthenticated.value = user != null
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
        viewModelScope.launch {
            repository.logout()
        }
    }

    fun clearError() {
        _error.value = null
    }
}