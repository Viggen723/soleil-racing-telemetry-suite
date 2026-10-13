package com.example.soleilracingproject

import android.app.Application
import android.content.Context
import com.example.soleilracingproject.di.initKoin
import com.example.soleilracingproject.di.appModule
import com.example.soleilracingproject.di.sharedModule
import org.koin.dsl.module

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        initKoin {
            modules(
                module { single<Context> { this@MainApplication } },
                appModule,
                sharedModule
            )
        }
    }
}