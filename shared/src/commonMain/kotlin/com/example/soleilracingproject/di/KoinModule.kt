package com.example.soleilracingproject.di

import com.example.soleilracingproject.features.analysis.data.AnalysisRepository
import com.example.soleilracingproject.features.analysis.viewmodel.AnalysisViewModel
import com.example.soleilracingproject.features.garage.data.GarageRepository
import com.example.soleilracingproject.features.garage.viewmodel.GarageViewmodel
import com.example.soleilracingproject.features.hud.data.HudRepository
import com.example.soleilracingproject.features.hud.viewmodel.HudViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import com.example.soleilracingproject.features.settings.data.SettingsRepository
import com.example.soleilracingproject.features.settings.viewmodel.SettingsViewModel

// https://www.youtube.com/watch?v=TAKZy3uQTdE for a video on dependency injection (di)
val sharedModule = module {
    single { SettingsRepository(get()) }
    viewModel { SettingsViewModel(get()) }
    single { HudRepository(get(), get(), get(), get()) }
    single { AnalysisRepository(get(), get(), get()) }
    single { GarageRepository(get()) }

    viewModel {
        HudViewModel(
            get(),
            get(),
            getOrNull(),
            getOrNull()
        )
    }
    viewModel { AnalysisViewModel(getOrNull(), getOrNull()) }
    viewModel { GarageViewmodel(get()) }
}
