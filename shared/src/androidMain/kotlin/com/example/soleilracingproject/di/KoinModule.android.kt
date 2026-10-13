package com.example.soleilracingproject.di

import com.example.soleilracingproject.data.remote.udp.AndroidUdpSocketListener
import com.example.soleilracingproject.data.remote.udp.UdpSocketListener
import com.example.soleilracingproject.model.telemetry.TelemetryPoint
import com.example.soleilracingproject.util.AndroidUdpParser
import org.koin.dsl.module

val appModule = module {
    single<UdpSocketListener> { AndroidUdpSocketListener() }
    single<(ByteArray) -> TelemetryPoint?> { AndroidUdpParser::parseUdpData }
}
