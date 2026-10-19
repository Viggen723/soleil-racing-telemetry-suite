package com.example.soleilracingproject.di

import HudViewModel
import com.example.soleilracingproject.features.analysis.data.AnalysisRepository
import com.example.soleilracingproject.features.analysis.viewmodel.AnalysisViewModel
import com.example.soleilracingproject.features.garage.data.GarageRepository
import com.example.soleilracingproject.features.garage.viewmodel.GarageViewmodel

import org.koin.dsl.module

// https://www.youtube.com/watch?v=TAKZy3uQTdE for a video on dependency injection (di)
val sharedModule = module {
    single { HudViewModel(get(), get(), getOrNull(), getOrNull()) }
    single { AnalysisRepository(get(), get(), get()) }
    factory { AnalysisViewModel(getOrNull()) }
    single { GarageRepository(get()) }
    factory { GarageViewmodel(get()) }
}
