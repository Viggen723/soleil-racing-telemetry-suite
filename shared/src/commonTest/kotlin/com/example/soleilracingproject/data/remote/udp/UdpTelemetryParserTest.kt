package com.example.soleilracingproject.data.remote.udp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal object UdpTelemetryFixtures {
    fun validPacket(): ByteArray = (
        "78563412" +                 // Timestamp 0x12345678
            "0ae81c410ae89cc10ae89c40" + // Acceleration 1g, -2g, 0.5g in m/s²
            "00c00e4200e00bc3" +         // Latitude, longitude
            "0080f742" +                 // Speed 123.75 MPH
            "0000003f000080be0000003e000080bd" + // Quaternion W, I, J, K
            "00002942"                   // Altitude 42.25
        ).hexToByteArray()

    fun negativeTimestampPacket(): ByteArray = validPacket().also {
        "feffffff".hexToByteArray().copyInto(it)
    }

    fun southernEasternPacket(): ByteArray = validPacket().also {
        "00c00ec200e00b43".hexToByteArray().copyInto(it, destinationOffset = 16)
    }

    fun nonfinitePacket(): ByteArray = validPacket().also {
        "3412c07f0000807f000080ff".hexToByteArray().copyInto(it, destinationOffset = 4)
        "000080ff3412c07f0000807f".hexToByteArray().copyInto(it, destinationOffset = 16)
    }

    fun outOfRangeCoordinatesPacket(): ByteArray = validPacket().also {
        "0000b742004035c3".hexToByteArray().copyInto(it, destinationOffset = 16)
    }
}

class UdpTelemetryParserTest {
    @Test
    fun decodesLittleEndianPacketAndPreservesFieldOrder() {
        val packet = UdpTelemetryFixtures.validPacket()
        assertEquals(48, packet.size)
        val point = assertNotNull(UdpTelemetryParser.parseUdpData(packet))

        assertEquals(0x12345678, point.time)
        assertEquals(1f, point.accelX, 0.000001f)
        assertEquals(-2f, point.accelY, 0.000001f)
        assertEquals(0.5f, point.accelZ, 0.000001f)
        assertEquals(35.6875f, point.latitude)
        assertEquals(-139.875f, point.longitude)
        assertEquals(123.75f, point.speed)
        assertEquals(0.5f, point.qw)
        assertEquals(-0.25f, point.qi)
        assertEquals(0.125f, point.qj)
        assertEquals(-0.0625f, point.qk)
        assertEquals(42.25f, point.altitude)
    }

    @Test
    fun preservesNegativeSignedTimestamp() {
        val point = assertNotNull(UdpTelemetryParser.parseUdpData(UdpTelemetryFixtures.negativeTimestampPacket()))
        assertEquals(-2, point.time)
    }

    @Test
    fun preservesNegativeLatitudeAndPositiveLongitude() {
        val point = assertNotNull(UdpTelemetryParser.parseUdpData(UdpTelemetryFixtures.southernEasternPacket()))
        assertEquals(-35.6875f, point.latitude)
        assertEquals(139.875f, point.longitude)
    }

    @Test
    fun rejectsEveryPacketLengthBelow48Bytes() {
        val packet = UdpTelemetryFixtures.validPacket()
        for (length in 0 until packet.size) {
            assertNull(UdpTelemetryParser.parseUdpData(packet.copyOf(length)), "Packet length $length")
        }
    }

    @Test
    fun ignoresTrailingBytes() {
        val packet = UdpTelemetryFixtures.validPacket()
        val expected = assertNotNull(UdpTelemetryParser.parseUdpData(packet))
        assertEquals(expected, UdpTelemetryParser.parseUdpData(packet + byteArrayOf(-1, 42)))
    }

    @Test
    fun acceptsNanAndSignedInfinities() {
        val point = assertNotNull(UdpTelemetryParser.parseUdpData(UdpTelemetryFixtures.nonfinitePacket()))
        assertTrue(point.accelX.isNaN())
        assertEquals(Float.POSITIVE_INFINITY, point.accelY)
        assertEquals(Float.NEGATIVE_INFINITY, point.accelZ)
        assertEquals(Float.NEGATIVE_INFINITY, point.latitude)
        assertTrue(point.longitude.isNaN())
        assertEquals(Float.POSITIVE_INFINITY, point.speed)
    }

    @Test
    fun preservesOutOfRangeCoordinates() {
        val point = assertNotNull(UdpTelemetryParser.parseUdpData(UdpTelemetryFixtures.outOfRangeCoordinatesPacket()))
        assertEquals(91.5f, point.latitude)
        assertEquals(-181.25f, point.longitude)
    }
}
