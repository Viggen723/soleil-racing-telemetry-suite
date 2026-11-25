package com.example.soleilracingproject.data.remote.udp

import com.example.soleilracingproject.model.telemetry.TelemetryPoint
import com.example.soleilracingproject.util.tools.ConversionTools

object UdpTelemetryParser {
    private const val PAYLOAD_SIZE = 48

    /**
     * Decodes the first 48 bytes. Like the Android parser, this accepts trailing bytes,
     * nonfinite floats, and out-of-range coordinates.
     */
    fun parseUdpData(data: ByteArray): TelemetryPoint? {
        if (data.size < PAYLOAD_SIZE) return null

        return TelemetryPoint(
            time = data.readInt32(0),
            accelX = ConversionTools.normalizeAccel(data.readFloat32(4)),
            accelY = ConversionTools.normalizeAccel(data.readFloat32(8)),
            accelZ = ConversionTools.normalizeAccel(data.readFloat32(12)),
            latitude = data.readFloat32(16),
            longitude = data.readFloat32(20),
            speed = data.readFloat32(24),
            qw = data.readFloat32(28),
            qi = data.readFloat32(32),
            qj = data.readFloat32(36),
            qk = data.readFloat32(40),
            altitude = data.readFloat32(44),
        )
    }

    private fun ByteArray.readInt32(offset: Int): Int =
        (this[offset].toInt() and 0xff) or
            ((this[offset + 1].toInt() and 0xff) shl 8) or
            ((this[offset + 2].toInt() and 0xff) shl 16) or
            ((this[offset + 3].toInt() and 0xff) shl 24)

    private fun ByteArray.readFloat32(offset: Int): Float = Float.fromBits(readInt32(offset))
}
