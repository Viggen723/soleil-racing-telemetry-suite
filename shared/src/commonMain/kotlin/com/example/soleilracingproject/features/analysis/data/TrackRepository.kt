package com.example.soleilracingproject.features.analysis.data

import com.example.soleilracingproject.data.local.dao.TrackDao
import com.example.soleilracingproject.data.mapper.toDomain
import com.example.soleilracingproject.model.analysis.TrackLayout
import com.example.soleilracingproject.data.mapper.toEntity
import com.example.soleilracingproject.model.analysis.TrackLayoutDto
import com.example.soleilracingproject.model.analysis.toDomain
import com.example.soleilracingproject.model.telemetry.TelemetryPoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.ExperimentalResourceApi
import soleilracingproject.shared.generated.resources.Res

class TrackRepository(private val trackDao: TrackDao) {

    private val jsonConfig = Json {
        ignoreUnknownKeys = true
    }

    fun getAllTracks(): Flow<List<TrackLayout>> {
        return trackDao.getAllTracks().map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun saveTrack(track: TrackLayout, isCustom: Boolean = true) {
        trackDao.insertTrack(track.toEntity(isCustom))
    }

    suspend fun deleteTrack(id: String) {
        trackDao.deleteTrackById(id)
    }

    suspend fun findMatchingTrack(points: List<TelemetryPoint>): TrackLayout? {
        val validPoints = points.filter { (it.latitude != 0f) || (it.longitude != 0f) }
        if (validPoints.isEmpty()) return null

        val allTracks = trackDao.getAllTracks().firstOrNull() ?: return null

        return allTracks.firstOrNull { track ->
            val trackLat = (track.sfLat1 + track.sfLat2) / 2.0
            val trackLon = (track.sfLon1 + track.sfLon2) / 2.0

            validPoints.any { pt ->
                val ptLat = pt.latitude.toDouble()
                val ptLon = com.example.soleilracingproject.util.tools.lapLogic.LapLogic.normalizeLon(pt.longitude.toDouble(), trackLon)

                val dLat = kotlin.math.abs(ptLat - trackLat)
                val dLon = kotlin.math.abs(ptLon - trackLon)
                (dLat < 0.03) && (dLon < 0.03) // Within ~3km of start/finish line
            }
        }?.toDomain()
    }

    /**
     * Call this once on startup. It will load the predefined tracks into the database
     * without overwriting user's custom ones.
     */
    @OptIn(ExperimentalResourceApi::class)
    suspend fun seedInitialTracksIfNeeded() {
        val existingTracks = trackDao.getAllTracks().firstOrNull() ?: emptyList()
        if (existingTracks.isNotEmpty()) {
            return // Database already seeded
        }

        try {
            val jsonBytes = Res.readBytes("files/seeded_tracks.json")
            val jsonString = jsonBytes.decodeToString()
            val trackDtos = jsonConfig.decodeFromString<List<TrackLayoutDto>>(jsonString)
            
            val initialTracks = trackDtos.map { it.toDomain().toEntity(isCustom = false) }
            // Insert in batch
            trackDao.insertTracks(initialTracks)
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
