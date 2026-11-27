package com.example.soleilracingproject.di

import com.example.soleilracingproject.features.analysis.data.AnalysisRepository
import com.example.soleilracingproject.features.analysis.data.backup.BackupAccount
import com.example.soleilracingproject.features.analysis.data.backup.FirebaseBackupAccount
import com.example.soleilracingproject.features.analysis.data.backup.FirebaseBackupTransport
import com.example.soleilracingproject.features.analysis.data.backup.SessionBackupRepository
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
    viewModel {
        val recording = get<RecordingSessionOwner>()
        LoginpageViewmodel(get<LoginpageRepository>()) { recording.isRecording.value }
    }
    single { SettingsRepository(get()) }
    viewModel { SettingsViewModel(get()) }
    single { HudRepository(get(), get(), get(), get()) }
    single<BackupAccount> { FirebaseBackupAccount() }
    single {
        val account = get<BackupAccount>()
        AnalysisRepository(get()) {
            runCatching { account.currentUid() }.getOrNull()
        }
    }
    single(createdAtStart = true) {
        SessionBackupRepository(get(), get(), FirebaseBackupTransport())
    } onClose { it?.close() }
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
    viewModel { AnalysisViewModel(getOrNull(), getOrNull(), get(), getOrNull(), get()) }
    viewModel { GarageViewmodel(get()) }
    viewModel { com.example.soleilracingproject.features.customtrack.viewmodel.CustomTrackViewModel(get(), get()) }
}
