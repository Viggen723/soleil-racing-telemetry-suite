package com.example.soleilracingproject.di

import com.example.soleilracingproject.features.analysis.data.AnalysisRepository
import com.example.soleilracingproject.features.analysis.viewmodel.AnalysisViewModel
import com.example.soleilracingproject.features.garage.data.GarageRepository
import com.example.soleilracingproject.features.garage.viewmodel.GarageViewmodel
import com.example.soleilracingproject.features.hud.data.HudRepository
import com.example.soleilracingproject.features.hud.viewmodel.HudViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

// https://www.youtube.com/watch?v=TAKZy3uQTdE for a video on dependency injection (di)
val sharedModule = module {
    single { HudRepository(get(), get(), get(), get()) }
    viewModel { HudViewModel(get()) }
    single { AnalysisRepository(get(), get(), get()) }
    viewModel { AnalysisViewModel(getOrNull(), getOrNull()) }
    single { GarageRepository(get()) }
    viewModel { GarageViewmodel(get()) }
}