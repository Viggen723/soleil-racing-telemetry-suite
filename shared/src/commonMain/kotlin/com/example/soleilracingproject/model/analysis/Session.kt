package com.example.soleilracingproject.model.analysis

import kotlin.time.Duration

data class Session(
    val id: String,
    val title: String,
    val vehicleId: String, // Will contain which vehicle was used.
    val track: String,
    val dateEpoch: Long, // In ms
    val rawTelemetryPath: String? = null,
    val remoteStorageUrl: String? = null, // For the Firebase-stored location
    val isSynced: Boolean = false,
    val laps: List<Lap> = emptyList() // Each is saved to the Session via the sessionId var.
) {
    // Can put values that wll be calculated dynamically
    // here like getting the bestLap formatted time (See values to see the different ways. Pretty nifty

    val lapCount: Int
        get() = laps.size

    // A formatted best lap time that uses the get() function
    val bestTimeFormatted: Duration?
        get() = laps.filter {!it.isDnf}.minOfOrNull { it.lapTime }
}


