package com.example.soleilracingproject.util.tools

import com.example.soleilracingproject.model.telemetry.EulerAngles
import kotlin.math.roundToInt
import kotlin.math.atan2
import kotlin.math.asin
import kotlin.math.PI

object ConversionTools {

    // Formatters designed to avoid java.lang.String.format in KMP commonMain
    fun Float.formatOneDecimal(): String {
        val rounded = (this * 10).roundToInt() / 10.0f
        return rounded.toString()
    }

    fun Float.formatTwoDecimals(): String {
        val rounded = (this * 100).roundToInt() / 100.0f
        return rounded.toString()
    }

    // Assumes a unit quaternion; returns roll, pitch, and yaw in degrees.
    fun quaternionToEuler(
        x: Double,
        y: Double,
        z: Double,
        w: Double
    ): EulerAngles {
        val roll = atan2(2.0 * (w * x + y * z), 1.0 - 2.0 * (x * x + y * y))
        val pitch = asin((2.0 * (w * y - z * x)).coerceIn(-1.0, 1.0))
        val yaw = atan2(2.0 * (w * z + x * y), 1.0 - 2.0 * (y * y + z * z))
        val radiansToDegrees = 180.0 / PI
        return EulerAngles(
            roll = roll * radiansToDegrees,
            pitch = pitch * radiansToDegrees,
            yaw = yaw * radiansToDegrees
        )
    }
}
