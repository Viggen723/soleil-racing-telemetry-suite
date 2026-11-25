package com.example.soleilracingproject.di

import com.example.soleilracingproject.features.analysis.data.AnalysisRepository
import com.example.soleilracingproject.features.analysis.viewmodel.AnalysisViewModel
import com.example.soleilracingproject.features.garage.data.GarageRepository
import com.example.soleilracingproject.features.garage.viewmodel.GarageViewmodel
import com.example.soleilracingproject.features.hud.data.HudRepository
import com.example.soleilracingproject.features.hud.data.RecordingSessionOwner
import com.example.soleilracingproject.features.analysis.data.TrackRepository
import com.example.soleilracingproject.features.hud.viewmodel.HudViewModel
import com.example.soleilracingproject.features.loginpage.data.LoginpageRepository
import com.example.soleilracingproject.features.loginpage.viewmodel.LoginpageViewmodel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.koin.dsl.onClose
import com.example.soleilracingproject.features.settings.data.SettingsRepository
import com.example.soleilracingproject.features.settings.viewmodel.SettingsViewModel

// https://www.youtube.com/watch?v=TAKZy3uQTdE for a video on dependency injection (di)
val sharedModule = module {
    single { LoginpageRepository() }
    viewModel { LoginpageViewmodel(get()) }
    single { SettingsRepository(get()) }
    viewModel { SettingsViewModel(get()) }
    single { HudRepository(get(), get(), get(), get()) }
    single { AnalysisRepository(get()) }
    single { GarageRepository(get()) }
    single { TrackRepository(get()) }
    single { RecordingSessionOwner(get(), getOrNull(), getOrNull()) } onClose { it?.close() }
    viewModel {
        HudViewModel(
            get(),
            get(),
            get(),
            getOrNull()
        )
    }
    viewModel { AnalysisViewModel(getOrNull(), getOrNull(), get(), getOrNull()) }
    viewModel { GarageViewmodel(get()) }
    viewModel { com.example.soleilracingproject.features.customtrack.viewmodel.CustomTrackViewModel(get(), get()) }
}
