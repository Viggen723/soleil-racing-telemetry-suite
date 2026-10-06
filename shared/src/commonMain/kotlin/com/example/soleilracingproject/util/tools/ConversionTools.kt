package com.example.soleilracingproject.util.tools

import kotlin.math.roundToInt

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
}