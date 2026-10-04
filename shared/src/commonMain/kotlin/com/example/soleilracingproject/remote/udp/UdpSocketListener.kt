package com.example.soleilracingproject.remote.udp

import kotlinx.coroutines.flow.Flow

// We need an interface as iOS has to use another way to process the packet (KTOR socket vs DatagramSocket)
interface UdpSocketListener {
    suspend fun beginListening(port: Int = 4120)

    fun stopListening()

    fun observePackets(): Flow<ByteArray>
}