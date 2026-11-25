package com.example.soleilracingproject.di

import com.example.soleilracingproject.data.local.AppDatabase
import com.example.soleilracingproject.data.local.getDatabaseBuilder
import com.example.soleilracingproject.data.local.getRoomDatabase
import com.example.soleilracingproject.data.remote.udp.IosUdpSocketListener
import com.example.soleilracingproject.data.remote.udp.UdpSocketListener
import com.example.soleilracingproject.data.remote.udp.UdpTelemetryParser
import com.example.soleilracingproject.model.telemetry.TelemetryPoint
import org.koin.dsl.module

private val iosModule = module {
    single<AppDatabase> { getRoomDatabase(getDatabaseBuilder()) }
    single { get<AppDatabase>().sessionDao() }
    single { get<AppDatabase>().lapDao() }
    single { get<AppDatabase>().telemetryDao() }
    single { get<AppDatabase>().carDao() }
    single { get<AppDatabase>().widgetLayoutDao() }
    single { get<AppDatabase>().settingsDao() }
    single { get<AppDatabase>().trackDao() }

    single<UdpSocketListener> { IosUdpSocketListener() }
    single<(ByteArray) -> TelemetryPoint?> { UdpTelemetryParser::parseUdpData }
}

private val iosKoin by lazy {
    initKoin { modules(iosModule) }
}

internal fun initIosKoin() = iosKoin
