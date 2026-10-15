package com.example.soleilracingproject.di

import android.content.Context
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.example.soleilracingproject.data.local.AppDatabase
import com.example.soleilracingproject.data.local.getDatabaseBuilder
import com.example.soleilracingproject.data.remote.udp.AndroidUdpSocketListener
import com.example.soleilracingproject.data.remote.udp.UdpSocketListener
import com.example.soleilracingproject.model.telemetry.TelemetryPoint
import com.example.soleilracingproject.util.AndroidUdpParser
import org.koin.dsl.module

val appModule = module {

    single<UdpSocketListener> { AndroidUdpSocketListener() }
    single<(ByteArray) -> TelemetryPoint?> { AndroidUdpParser::parseUdpData }

    single {
        getDatabaseBuilder(get<Context>())
            .setDriver(BundledSQLiteDriver())
            .build()
    }
    single { get<AppDatabase>().sessionDao() }
    single { get<AppDatabase>().lapDao() }
    single { get<AppDatabase>().telemetryDao() }
}