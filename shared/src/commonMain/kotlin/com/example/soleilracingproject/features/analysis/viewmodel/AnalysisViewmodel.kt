package com.example.soleilracingproject.features.analysis.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.soleilracingproject.features.analysis.data.AnalysisRepository
import com.example.soleilracingproject.model.analysis.KnownTracks
import com.example.soleilracingproject.model.analysis.Session
import com.example.soleilracingproject.util.tools.lapLogic.LapLogic
import com.example.soleilracingproject.util.tools.parseCsvSession.CsvTelemetryParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AnalysisViewModel(
    private val repository: AnalysisRepository? = null,
) : ViewModel() {

    val sessionsState: StateFlow<List<Session>> = (repository?.getAllSessions() ?: flowOf(emptyList()))
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    fun loadCsvSession(fileName: String, csvContent: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val parsedPoints = CsvTelemetryParser.parse(csvContent)

            if (parsedPoints.size < 2) return@launch

            val now = kotlin.time.Clock.System.now().toEpochMilliseconds()
            val uniqueSessionId = "imported_session_$now"

            val matchedTrack = KnownTracks.findMatchingTrack(parsedPoints)
            val gateToUse = matchedTrack?.startFinishGate

            val laps = LapLogic.splitSessionIntoLaps(
                sessionId = uniqueSessionId,
                points = parsedPoints,
                finishGate = gateToUse,
            )

            val newSession = Session(
                id = uniqueSessionId,
                title = fileName.replace(".csv", "", ignoreCase = true),
                vehicleId = "Unknown Vehicle",
                track = matchedTrack?.trackName ?: "Imported Track",
                dateEpoch = now,
                isSynced = false,
                laps = laps,
            )

            insertSession(newSession)
        }
    }

    fun deleteSession(session: Session) {
        viewModelScope.launch {
            repository?.deleteSession(session)
        }
    }

    fun insertSession(session: Session) {
        viewModelScope.launch {
            repository?.addSession(session)
        }
    }
}
