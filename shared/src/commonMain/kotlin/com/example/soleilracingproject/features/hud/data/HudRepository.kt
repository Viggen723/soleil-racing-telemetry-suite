package com.example.soleilracingproject.features.hud.data

import com.example.soleilracingproject.data.remote.udp.UdpSocketListener
import com.example.soleilracingproject.features.analysis.data.AnalysisRepository
import com.example.soleilracingproject.features.garage.data.GarageRepository
import com.example.soleilracingproject.model.analysis.Session
import com.example.soleilracingproject.model.garage.Car
import com.example.soleilracingproject.model.telemetry.TelemetryPoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull

class HudRepository(
    private val udpListener: UdpSocketListener,
    private val parseUdpData: (ByteArray) -> TelemetryPoint?,
    private val analysisRepository: AnalysisRepository,
    private val garageRepository: GarageRepository,
) {
    fun observeTelemetry(): Flow<TelemetryPoint> {
        return udpListener.observePackets().mapNotNull { parseUdpData(it) }
    }

    suspend fun beginListening(port: Int = 4120) {
        udpListener.beginListening(port)
    }

    fun stopListening() {
        udpListener.stopListening()
    }

    fun getActiveCar(): Car? {
        return garageRepository.getActiveCar()
    }

    suspend fun saveSession(session: Session) {
        analysisRepository.addSession(session)
    }
}