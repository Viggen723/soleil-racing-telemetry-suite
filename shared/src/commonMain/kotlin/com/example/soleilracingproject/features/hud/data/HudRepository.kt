package com.example.soleilracingproject.features.hud.data

import com.example.soleilracingproject.data.remote.udp.UdpSocketListener
import com.example.soleilracingproject.features.analysis.data.AnalysisRepository
import com.example.soleilracingproject.features.garage.data.GarageRepository
import com.example.soleilracingproject.model.analysis.Session
import com.example.soleilracingproject.model.garage.Car
import com.example.soleilracingproject.model.telemetry.TelemetryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch

class HudRepository(
    private val udpListener: UdpSocketListener,
    private val parseUdpData: (ByteArray) -> TelemetryPoint?,
    private val analysisRepository: AnalysisRepository,
    private val garageRepository: GarageRepository,
    telemetryScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
) {
    private val reconnectRequests = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    private val telemetry = channelFlow {
        // Subscribe before binding so the first datagram has a consumer.
        launch(start = CoroutineStart.UNDISPATCHED) {
            udpListener.observePackets().mapNotNull(parseUdpData).collect { send(it) }
        }
        try {
            reconnectRequests.onSubscription { udpListener.beginListening() }.collect {
                udpListener.stopListening()
                currentCoroutineContext().ensureActive()
                udpListener.beginListening()
            }
        } finally {
            udpListener.stopListening()
        }
    }
        .buffer(0)
        .shareIn(telemetryScope, SharingStarted.WhileSubscribed(), replay = 0)

    fun observeTelemetry() = telemetry

    fun reconnectUdp() {
        reconnectRequests.tryEmit(Unit)
    }

    fun getActiveCar(): Car? {
        return garageRepository.getActiveCar()
    }

    internal fun captureBackupOwner() = analysisRepository.captureBackupOwner()

    suspend fun saveSession(session: Session, ownerUid: String?) {
        analysisRepository.addSession(session, ownerUid)
    }
}
