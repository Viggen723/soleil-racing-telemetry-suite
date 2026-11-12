package com.example.soleilracingproject.features.hud.data

// Lets the shared HUD tell the platform "a recording started" or "a recording stopped".
// On Android this shows a "Recording in progress" notification.
// Other platforms don't provide one yet, so nothing happens there.
interface RecordingNotifier {
    fun recordingStarted()
    fun recordingStopped()
}