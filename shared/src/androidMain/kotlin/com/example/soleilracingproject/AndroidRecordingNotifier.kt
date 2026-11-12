package com.example.soleilracingproject

import android.content.Context
import com.example.soleilracingproject.features.hud.data.RecordingNotifier

// The Android side of RecordingNotifier: it starts and stops the notification service.
class AndroidRecordingNotifier(private val context: Context) : RecordingNotifier {
    override fun recordingStarted() = RecordingService.start(context)
    override fun recordingStopped() = RecordingService.stop(context)
}