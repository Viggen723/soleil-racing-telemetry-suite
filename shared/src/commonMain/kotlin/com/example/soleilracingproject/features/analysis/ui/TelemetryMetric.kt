package com.example.soleilracingproject.features.analysis.ui

import com.example.soleilracingproject.model.settings.SpeedUnit
import com.example.soleilracingproject.model.telemetry.TelemetryPoint
import com.example.soleilracingproject.util.tools.ConversionTools.formatOneDecimal
import kotlin.math.roundToInt

enum class TelemetryMetric(
    val title: String,
    val unit: String,
    val extractValue: (TelemetryPoint) -> Float,
) {
    SPEED("Speed (MPH)", "MPH", { point -> point.speed }),
    ACCEL_X("X-Accel (G)", "G", { point -> point.accelX }),
    ACCEL_Y("Y-Accel (G)", "G", { point -> point.accelY });

    fun displayTitle(speedUnit: SpeedUnit) = if (this == SPEED) "Speed (${speedUnit.label})" else title

    fun formatValue(value: Float, includeUnit: Boolean = true, speedUnit: SpeedUnit = SpeedUnit.MPH): String {
        val formatted = when (this) {
            SPEED -> speedUnit.fromMph(value).formatOneDecimal()
            ACCEL_X, ACCEL_Y -> {
                val rounded = (value * 100).roundToInt() / 100.0f
                val sign = if (rounded > 0) "+" else ""
                "$sign$rounded"
            }
        }
        return if (includeUnit) "$formatted ${if (this == SPEED) speedUnit.label else unit}" else formatted
    }

    fun formatAxisLabel(value: Float, speedUnit: SpeedUnit = SpeedUnit.MPH): String {
        return when (this) {
            SPEED -> speedUnit.fromMph(value).roundToInt().toString()
            ACCEL_X, ACCEL_Y -> {
                val rounded = (value * 10).roundToInt() / 10.0f
                val sign = if (rounded > 0) "+" else ""
                "$sign$rounded"
            }
        }
    }
}