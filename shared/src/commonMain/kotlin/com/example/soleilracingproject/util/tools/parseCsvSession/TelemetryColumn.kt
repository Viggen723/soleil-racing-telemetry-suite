package com.example.soleilracingproject.util.tools.parseCsvSession

enum class TelemetryColumn(override val header: String) : Column {
    TIME("time"),
    ACCELX("accelX"),
    ACCELY("accelY"),
    ACCELZ("accelZ"),
    LATITUDE("lat"),
    LONGITUDE("lon"),
    SPEED("speed"),
    QW("qw"),
    QI("qi"),
    QJ("qj"),
    QK("qk"),
    ALTITUDE("altitude")
}