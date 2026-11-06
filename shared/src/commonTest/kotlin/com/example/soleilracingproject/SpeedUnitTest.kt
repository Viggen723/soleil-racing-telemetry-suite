package com.example.soleilracingproject

import com.example.soleilracingproject.model.settings.AppSettings
import com.example.soleilracingproject.model.settings.SpeedUnit
import com.example.soleilracingproject.util.tools.ConversionTools
import com.example.soleilracingproject.util.tools.parseCsvSession.CsvTelemetryParser
import com.example.soleilracingproject.features.analysis.ui.TelemetryMetric
import kotlin.test.*

class SpeedUnitTest {
    @Test fun conversions() {
        assertEquals(96.56064f, ConversionTools.mphToKph(60f), 0.0001f)
        assertEquals(62.1371f, ConversionTools.kphToMph(100f), 0.0001f)
        assertEquals(60f, SpeedUnit.MPH.fromMph(60f))
        assertEquals(60f, SpeedUnit.MPH.toMph(60f))
        assertEquals(96.56064f, SpeedUnit.KPH.fromMph(60f), 0.0001f)
        assertEquals(62.1371f, SpeedUnit.KPH.toMph(100f), 0.0001f)
    }

    @Test fun maximumKeepsItsPhysicalRange() {
        val unit = SpeedUnit.KPH
        assertEquals(289.68192f, unit.fromMph(AppSettings().speedometerMax), 0.0001f)
        for (mph in listOf(10f, 180f, 500f)) {
            assertEquals(mph, unit.toMph(unit.fromMph(mph)), 0.0001f)
        }
    }

    @Test fun csvNormalizesOnlyExplicitMetricHeaders() {
        val mphHeaders = listOf("speedmph", "mph", "velocitymph", "Speed_MPH")
        val kphHeaders = listOf("speedkph", "speedkmh", "kph", "kmh", "velocitykph", "velocitykmh", "Speed_KMH", "speed-kph")
        for (header in mphHeaders) {
            assertEquals(60f, CsvTelemetryParser.parse("time,$header\n0,60").single().speed)
        }
        for (header in kphHeaders) {
            assertEquals(62.1371f, CsvTelemetryParser.parse("time,$header\n0,100").single().speed, 0.0001f)
        }
        for (header in listOf("speed", "spd", "velocity", "v")) {
            assertEquals(80f, CsvTelemetryParser.parse("time,$header\n0,80").single().speed)
        }
    }

    @Test fun analysisFormatsCanonicalSpeedsAndDifferences() {
        assertEquals("96.6 KM/H", TelemetryMetric.SPEED.formatValue(60f, speedUnit = SpeedUnit.KPH))
        assertEquals("97", TelemetryMetric.SPEED.formatAxisLabel(60f, SpeedUnit.KPH))
        assertEquals("16.1 KM/H", TelemetryMetric.SPEED.formatValue(10f, speedUnit = SpeedUnit.KPH))
        assertEquals("Speed (KM/H)", TelemetryMetric.SPEED.displayTitle(SpeedUnit.KPH))
    }
}
