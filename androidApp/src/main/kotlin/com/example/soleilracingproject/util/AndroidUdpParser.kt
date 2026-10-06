package com.example.soleilracingproject.util

import com.example.soleilracingproject.model.telemetry.TelemetryPoint
import java.nio.ByteBuffer
import java.nio.ByteOrder

object AndroidUdpParser {

    fun parseUdpData(data: ByteArray) : TelemetryPoint? // Can return null, so ? is needed
    {
        val buffer = ByteBuffer.wrap(data)

        // Because ESP32 sends in Big Endian we need to sort in Little
        buffer.order(ByteOrder.LITTLE_ENDIAN)

        return try
        {
            TelemetryPoint(
                buffer.int,
                buffer.float,
                buffer.float,
                buffer.float,
                buffer.float,
                buffer.float,
                buffer.float,
                buffer.float,
                buffer.float,
                buffer.float,
                buffer.float,
                buffer.float
            )
        }
        catch (e: Exception)
        {
            null
        }
    }
}