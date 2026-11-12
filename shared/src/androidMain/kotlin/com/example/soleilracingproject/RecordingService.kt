package com.example.soleilracingproject

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder

// Shows the "Recording in progress" notification. While this service runs, Android treats
// the app as important and is less likely to stop it when the user opens another app.
// The recording itself still happens in HudViewModel; this service only keeps the app alive.
class RecordingService : Service() {

    override fun onCreate() {
        super.onCreate()
        createChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isRunning = true
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        isRunning = false
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        val builder = if (Build.VERSION.SDK_INT >= 26) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            Notification.Builder(this)
        }
        return builder
            .setContentTitle("Soleil Racing")
            .setContentText("Recording in progress")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .setContentIntent(openAppIntent(this, 0))
            .setWhen(System.currentTimeMillis())
            .setUsesChronometer(true)
            .build()
    }

    // Two channels: a quiet one for the ongoing notification, a loud one for the reminder banner
    private fun createChannels() {
        if (Build.VERSION.SDK_INT >= 26) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Recording", NotificationManager.IMPORTANCE_LOW)
            )
            manager.createNotificationChannel(
                NotificationChannel(ALERT_CHANNEL_ID, "Recording reminder", NotificationManager.IMPORTANCE_HIGH)
            )
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID = "recording"
        private const val ALERT_CHANNEL_ID = "recording_alert"
        private const val NOTIFICATION_ID = 1
        private const val ALERT_NOTIFICATION_ID = 2

        // true while the service is running, so the activity knows whether a recording is going
        @Volatile
        var isRunning = false
            private set

        fun start(context: Context) {
            val intent = Intent(context, RecordingService::class.java)
            if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(intent) else context.startService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, RecordingService::class.java))
            clearReminder(context)
        }

        // Tapping a notification opens the app again
        private fun openAppIntent(context: Context, requestCode: Int): PendingIntent {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: Intent()
            return PendingIntent.getActivity(context, requestCode, launchIntent, PendingIntent.FLAG_IMMUTABLE)
        }

        // The banner is shown when the user leaves the app while recording. It goes away by itself.
        fun remindUser(context: Context) {
            if (Build.VERSION.SDK_INT < 26) return          // banners need the channels from Android 8
            val notification = Notification.Builder(context, ALERT_CHANNEL_ID)
                .setContentTitle("Soleil Racing")
                .setContentText("Recording in progress. Tap to return.")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setContentIntent(openAppIntent(context, 1))
                .setAutoCancel(true)
                .setTimeoutAfter(6000)                      // disappears after 6 seconds
                .build()
            context.getSystemService(NotificationManager::class.java).notify(ALERT_NOTIFICATION_ID, notification)
        }

        fun clearReminder(context: Context) {
            context.getSystemService(NotificationManager::class.java).cancel(ALERT_NOTIFICATION_ID)
        }
    }
}