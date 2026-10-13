package com.example.soleilracingproject.di

import HudViewModel
import com.example.soleilracingproject.features.analysis.data.AnalysisRepository
import com.example.soleilracingproject.features.analysis.viewmodel.AnalysisViewModel

import org.koin.dsl.module

// https://www.youtube.com/watch?v=TAKZy3uQTdE for a video on dependency injection (di)
val sharedModule = module {
    factory { HudViewModel(get(), get(), getOrNull(), getOrNull()) }
    single { AnalysisRepository(get(), get()) }
    factory { AnalysisViewModel(getOrNull()) }
}
