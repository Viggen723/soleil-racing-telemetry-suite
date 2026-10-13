package com.example.soleilracingproject.di

import com.example.soleilracingproject.features.hud.ui.HudViewModel
import org.koin.dsl.module

// https://www.youtube.com/watch?v=TAKZy3uQTdE for a video on dependency injection (di)
val sharedModule = module {
    factory { HudViewModel(get(), get()) }
}
