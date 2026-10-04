package com.example.soleilracingproject.remote.udp

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.SocketException
import java.net.SocketTimeoutException

class AndroidUdpSocketListener : UdpSocketListener {

    private val _packetFlow = MutableSharedFlow<ByteArray>(
        extraBufferCapacity = 64 // Buffer incoming high-frequency telemetry packets
    )

    private var socket: DatagramSocket? = null
    private var listener: Job? = null // Job is for managing the coroutine
    private val scope = CoroutineScope(Dispatchers.IO)

    override suspend fun beginListening(port: Int) = withContext(Dispatchers.IO) {
        // Stop any existing socket/job before starting a new session
        stopListening()

        try {
            socket = DatagramSocket(port).apply {
                reuseAddress = true
                soTimeout = 1000 // 1 second timeout so the loop can check cancellation
            }

            listener = scope.launch {
                val buffer = ByteArray(2048) // Sized for incoming ESP32 datagrams

                while (socket?.isClosed == false) {
                    try {
                        val packet = DatagramPacket(buffer, buffer.size)
                        socket?.receive(packet)

                        // Extract exact payload bytes and emit to flow
                        val payload = packet.data.copyOfRange(0, packet.length)
                        _packetFlow.emit(payload)

                    } catch (_: SocketTimeoutException) {
                        // Expected timeout to check if socket was closed or job cancelled
                    } catch (e: SocketException) {
                        // Socket was closed explicitly by stopListening()
                        break
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun stopListening() {
        listener?.cancel()
        listener = null
        socket?.close()
        socket = null
    }

    override fun observePackets(): Flow<ByteArray> = _packetFlow.asSharedFlow()
}