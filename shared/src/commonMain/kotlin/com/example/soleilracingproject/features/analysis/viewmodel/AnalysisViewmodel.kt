package com.example.soleilracingproject.features.analysis.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.soleilracingproject.features.analysis.data.AnalysisRepository
import com.example.soleilracingproject.model.analysis.Session
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class AnalysisViewModel(
    private val repository: AnalysisRepository? = null
) : ViewModel() {

    val sessionsState: StateFlow<List<Session>> = repository?.getAllSessions()
        ?.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        ) ?: MutableStateFlow(emptyList())
}
