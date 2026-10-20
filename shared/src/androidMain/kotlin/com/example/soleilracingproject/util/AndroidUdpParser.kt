package com.example.soleilracingproject.util

import com.example.soleilracingproject.model.telemetry.TelemetryPoint
import com.example.soleilracingproject.util.tools.ConversionTools
import java.nio.ByteBuffer
import java.nio.ByteOrder

object AndroidUdpParser {

    fun parseUdpData(data: ByteArray): TelemetryPoint? {
        val buffer = ByteBuffer.wrap(data)
        buffer.order(ByteOrder.LITTLE_ENDIAN)

        return try {
            val time = buffer.int
            val rawAccelX = buffer.float
            val rawAccelY = buffer.float
            val rawAccelZ = buffer.float
            val lat = buffer.float
            val lon = buffer.float
            val speed = buffer.float
            val qw = buffer.float
            val qi = buffer.float
            val qj = buffer.float
            val qk = buffer.float
            val altitude = buffer.float

            TelemetryPoint(
                time = time,
                accelX = ConversionTools.normalizeAccel(rawAccelX),
                accelY = ConversionTools.normalizeAccel(rawAccelY),
                accelZ = ConversionTools.normalizeAccel(rawAccelZ),
                latitude = lat,
                longitude = lon,
                speed = speed,
                qw = qw,
                qi = qi,
                qj = qj,
                qk = qk,
                altitude = altitude
            )
        } catch (_: Exception) {
            null
        }
    }
}
