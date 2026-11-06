package com.example.soleilracingproject.model.settings

import com.example.soleilracingproject.util.tools.ConversionTools

enum class SpeedUnit(val label: String) {
    MPH("MPH"),
    KPH("KM/H");

    fun fromMph(value: Float): Float = when (this) {
        MPH -> value
        KPH -> ConversionTools.mphToKph(value)
    }

    fun toMph(value: Float): Float = when (this) {
        MPH -> value
        KPH -> ConversionTools.kphToMph(value)
    }
}
