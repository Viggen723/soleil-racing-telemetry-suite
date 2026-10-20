package com.example.soleilracingproject.features.analysis.ui.widgets.graph

import kotlin.math.roundToInt

fun formatGraphTime(millis: Long): String {
    if (millis <= 0) return "0.0s"
    val totalSeconds = millis / 1000f
    return if (totalSeconds < 60f) {
        val wholeSec = totalSeconds.toInt()
        val tenths = ((totalSeconds - wholeSec) * 10).roundToInt() % 10
        "$wholeSec.${tenths}s"
    } else {
        val minutes = (totalSeconds / 60).toInt()
        val remainingSec = totalSeconds % 60
        val secInt = remainingSec.toInt()
        val tenths = ((remainingSec - secInt) * 10).roundToInt() % 10
        val secStr = if (secInt < 10) "0$secInt" else secInt.toString()
        "$minutes:$secStr.${tenths}s"
    }
}