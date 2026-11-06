package com.example.soleilracingproject.model.settings

data class AppSettings(
    val displayName: String = "",
    val speedometerMax: Float = 180f,
    val focusModeEnabled: Boolean = true,
    val speedUnit: SpeedUnit = SpeedUnit.MPH,
) {
    companion object {
        // Plenty of room for race vehicles, without accidental huge values.
        val speedometerRange = 10f..500f
        fun isValidSpeedometerMax(value: Float) = value.isFinite() && value in speedometerRange
    }
}
