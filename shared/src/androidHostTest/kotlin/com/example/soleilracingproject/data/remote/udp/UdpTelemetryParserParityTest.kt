package com.example.soleilracingproject.data.remote.udp

import com.example.soleilracingproject.util.AndroidUdpParser
import kotlin.test.Test
import kotlin.test.assertEquals

class UdpTelemetryParserParityTest {
    @Test
    fun matchesAndroidParserForAllFixtures() {
        val validPacket = UdpTelemetryFixtures.validPacket()
        val cases = mapOf(
            "valid" to validPacket,
            "negative timestamp" to UdpTelemetryFixtures.negativeTimestampPacket(),
            "southern and eastern coordinates" to UdpTelemetryFixtures.southernEasternPacket(),
            "empty" to byteArrayOf(),
            "truncated" to validPacket.copyOf(47),
            "trailing bytes" to (validPacket + byteArrayOf(-1, 42)),
            "nonfinite values" to UdpTelemetryFixtures.nonfinitePacket(),
            "out-of-range coordinates" to UdpTelemetryFixtures.outOfRangeCoordinatesPacket(),
        )

        for ((name, packet) in cases) {
            assertEquals(AndroidUdpParser.parseUdpData(packet), UdpTelemetryParser.parseUdpData(packet), name)
        }
    }
}
