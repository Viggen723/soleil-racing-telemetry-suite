package com.example.soleilracingproject.features.settings.viewmodel

import com.example.soleilracingproject.model.settings.SpeedUnit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.soleilracingproject.features.settings.data.SettingsRepository
import com.example.soleilracingproject.model.settings.AppSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()
    private val _isSaving = MutableStateFlow(false)
    val isSaving = _isSaving.asStateFlow()
    val settings = repository.settings
        .catch { e ->
            if (e is CancellationException) throw e
            _error.value = "Couldn't load settings. Please reopen this screen to retry."
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    fun clearError() { _error.value = null }
    fun updateDisplayName(value: String, onSaved: () -> Unit = {}) = write(onSaved) {
        repository.updateDisplayName(value)
    }
    fun updateSpeedometerMax(value: String, onSaved: () -> Unit = {}) {
        val parsed = value.trim().toFloatOrNull()
        val unit = settings.value.speedUnit
        val range = unit.fromMph(AppSettings.speedometerRange.start)..unit.fromMph(AppSettings.speedometerRange.endInclusive)
        if (parsed == null || !parsed.isFinite() || parsed !in range) {
            _error.value = "Enter a maximum from ${range.start} to ${range.endInclusive} ${unit.label}."
            return
        }
        // Convert back before saving so the stored max stays in MPH.
        write(onSaved) { repository.updateSpeedometerMax(unit.toMph(parsed).coerceIn(AppSettings.speedometerRange)) }
    }
    fun updateSpeedUnit(value: SpeedUnit) = write({}) { repository.updateSpeedUnit(value) }
    fun updateFocusModeEnabled(value: Boolean, onSaved: () -> Unit = {}) = write(onSaved) {
        repository.updateFocusModeEnabled(value)
    }

    private fun write(onSaved: () -> Unit, operation: suspend () -> Unit) {
        if (_isSaving.value) return
        _isSaving.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                operation()
                onSaved()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = "Couldn't save settings. Please try again."
            } finally {
                _isSaving.value = false
            }
        }
    }
}
