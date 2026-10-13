package com.example.soleilracingproject

import android.app.Application
import com.example.soleilracingproject.di.initKoin
import com.example.soleilracingproject.di.appModule

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        initKoin {
            modules(appModule)
        }
    }
}