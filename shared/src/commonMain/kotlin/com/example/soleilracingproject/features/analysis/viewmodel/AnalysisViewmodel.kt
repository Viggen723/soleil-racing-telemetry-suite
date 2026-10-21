package com.example.soleilracingproject.features.analysis.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.soleilracingproject.features.analysis.data.AnalysisRepository
import com.example.soleilracingproject.features.garage.data.GarageRepository
import com.example.soleilracingproject.model.analysis.KnownTracks
import com.example.soleilracingproject.model.analysis.Session
import com.example.soleilracingproject.model.garage.Car
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
    private val garageRepository: GarageRepository? = null,
) : ViewModel() {

    val sessionsState: StateFlow<List<Session>> = (repository?.getAllSessions() ?: flowOf(emptyList()))
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    val garageCars: StateFlow<List<Car>> = (garageRepository?.cars ?: flowOf(emptyList()))
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
            val presetGate = matchedTrack?.startFinishGate
            val autoGate = LapLogic.autoGenerateGate(parsedPoints)
            val gateToUse = presetGate ?: autoGate

            var laps = LapLogic.splitSessionIntoLaps(
                sessionId = uniqueSessionId,
                points = parsedPoints,
                finishGate = gateToUse,
            )

            if (laps.size <= 1 && autoGate != null && parsedPoints.size > 200) {
                val autoLaps = LapLogic.splitSessionIntoLaps(
                    sessionId = uniqueSessionId,
                    points = parsedPoints,
                    finishGate = autoGate,
                )
                if (autoLaps.size > 1) {
                    laps = autoLaps
                }
            }

            val activeCar = garageRepository?.getActiveCar()
            val defaultVehicleName = activeCar?.let { car ->
                listOfNotNull(car.year?.toString(), car.make, car.model, "(${car.name})")
                    .filter { it.isNotBlank() }
                    .joinToString(" ")
            }?.ifBlank { null } ?: "Unknown Vehicle"

            val newSession = Session(
                id = uniqueSessionId,
                title = fileName.replace(".csv", "", ignoreCase = true),
                vehicleId = defaultVehicleName,
                track = matchedTrack?.trackName ?: "Imported Track",
                dateEpoch = now,
                isSynced = false,
                laps = laps,
            )

            insertSession(newSession)
        }
    }

    fun renameSession(session: Session, newTitle: String) {
        val trimmed = newTitle.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            repository?.updateSession(session.copy(title = trimmed))
        }
    }

    fun updateSessionVehicle(session: Session, newVehicleName: String) {
        val trimmed = newVehicleName.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            repository?.updateSession(session.copy(vehicleId = trimmed))
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