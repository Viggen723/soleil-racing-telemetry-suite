package com.example.soleilracingproject.util.tools.parseCsvSession

import com.example.soleilracingproject.model.telemetry.TelemetryPoint
import com.example.soleilracingproject.util.tools.ConversionTools
import kotlin.math.abs

object CsvTelemetryParser {

    fun parse(csvData: String): List<TelemetryPoint> {
        val lineSequence = csvData.lineSequence().filter { it.isNotBlank() }
        val headerLine = lineSequence.firstOrNull() ?: return emptyList()

        val delimiter = when {
            headerLine.contains(";") -> ";"
            headerLine.contains("\t") -> "\t"
            else -> ","
        }

        val rawHeaders = headerLine.split(delimiter).map {
            it.trim()
                .lowercase()
                .removeSurrounding("\"")
                .replace(" ", "")
                .replace("_", "")
                .replace("-", "")
        }

        val columnIndexMap = TelemetryColumn.entries.associateWith { enumCol ->
            rawHeaders.indexOfFirst { header ->
                when (enumCol) {
                    TelemetryColumn.TIME -> header.contains("time") || header == "t" || header.contains("stamp") ||
                            header.contains("utc") || header.contains("millis") || header.contains("sec") ||
                            header == "ms" || header == "s" || header == "index" || header == "clock"

                    TelemetryColumn.LATITUDE -> header.contains("lat") || header.contains("posy") || header == "y" ||
                            header.contains("latitude")

                    TelemetryColumn.LONGITUDE -> header.contains("lon") || header.contains("lng") || header.contains("long") ||
                            header.contains("posx") || header == "x" || header.contains("longitude")

                    TelemetryColumn.ACCELX -> header.contains("accelx") || header == "ax" || header.contains("latg") ||
                            header == "gx" || header.contains("accx") || header == "g_x" || header == "lat_g"

                    TelemetryColumn.ACCELY -> header.contains("accely") || header == "ay" || header.contains("longg") ||
                            header == "gy" || header.contains("accy") || header == "g_y" || header == "long_g"

                    TelemetryColumn.ACCELZ -> header.contains("accelz") || header == "az" || header.contains("vertg") ||
                            header == "gz" || header.contains("accz") || header == "g_z" || header == "vert_g"

                    TelemetryColumn.SPEED -> header.contains("speed") || header.contains("spd") || header == "v" ||
                            header.contains("velocity") || header.contains("kph") || header.contains("mph")

                    else -> header == enumCol.header.lowercase()
                }
            }
        }

        fun fixCoord(value: Float): Float {
            var v = value
            if (!v.isFinite() || v == 0f) return 0f

            val sign = if (v < 0f) -1f else 1f

            // Microdegrees or large integer scaled format (e.g. 135392830 -> 135.39283)
            while (v * sign > 360f) {
                v /= 10f
            }
            return v
        }

        val rawPoints = lineSequence.drop(1).mapNotNull { line ->
            val row = line.split(delimiter).map {
                it.trim().removeSurrounding("\"").replace(",", ".")
            }

            fun getValue(column: TelemetryColumn): String? {
                val index = columnIndexMap[column] ?: -1
                return if (index in row.indices) row[index] else null
            }

            try {
                val rawLat = getValue(TelemetryColumn.LATITUDE)?.toFloatOrNull() ?: 0f
                val rawLon = getValue(TelemetryColumn.LONGITUDE)?.toFloatOrNull() ?: 0f

                val lat = fixCoord(rawLat)
                val lon = fixCoord(rawLon)

                val rawAccelX = getValue(TelemetryColumn.ACCELX)?.toFloatOrNull() ?: 0f
                val rawAccelY = getValue(TelemetryColumn.ACCELY)?.toFloatOrNull() ?: 0f
                val rawAccelZ = getValue(TelemetryColumn.ACCELZ)?.toFloatOrNull() ?: 0f

                TelemetryPoint(
                    time = getValue(TelemetryColumn.TIME)?.toIntOrNull()
                        ?: getValue(TelemetryColumn.TIME)?.toDoubleOrNull()?.toInt()
                        ?: 0,
                    accelX = rawAccelX,
                    accelY = rawAccelY,
                    accelZ = rawAccelZ,
                    latitude = lat,
                    longitude = lon,
                    speed = getValue(TelemetryColumn.SPEED)?.toFloatOrNull() ?: 0f,
                    qw = getValue(TelemetryColumn.QW)?.toFloatOrNull() ?: 1f,
                    qi = getValue(TelemetryColumn.QI)?.toFloatOrNull() ?: 0f,
                    qj = getValue(TelemetryColumn.QJ)?.toFloatOrNull() ?: 0f,
                    qk = getValue(TelemetryColumn.QK)?.toFloatOrNull() ?: 0f,
                    altitude = getValue(TelemetryColumn.ALTITUDE)?.toFloatOrNull() ?: 0f,
                )
            } catch (_: Exception) {
                null
            }
        }.sortedBy { it.time }.toList()

        if (rawPoints.isEmpty()) return emptyList()

        // Time fallback: if time column was missing or timestamps are mostly 0, synthesize 10 Hz timestamps (100ms per row)
        val nonZeroTimeCount = rawPoints.count { it.time != 0 }
        val pointsWithTime = if (nonZeroTimeCount < rawPoints.size / 2) {
            rawPoints.mapIndexed { idx, pt -> pt.copy(time = idx * 100) }
        } else {
            rawPoints
        }

        // Auto-detect acceleration units: if max acceleration magnitude > 3.5, values are in m/s² (gravity = 9.80665 m/s²); convert to Gs.
        val maxAccelMagnitude = pointsWithTime.maxOfOrNull { maxOf(abs(it.accelX), abs(it.accelY)) } ?: 0f
        val needsAccelNormalization = maxAccelMagnitude > 3.5f

        val normalizedAccelPoints = if (needsAccelNormalization) {
            pointsWithTime.map { pt ->
                pt.copy(
                    accelX = ConversionTools.normalizeAccel(pt.accelX),
                    accelY = ConversionTools.normalizeAccel(pt.accelY),
                    accelZ = ConversionTools.normalizeAccel(pt.accelZ),
                )
            }
        } else {
            pointsWithTime
        }

        // Normalize time units: if timestamps are in seconds (e.g., max time < 10,000 for a long session), scale to ms
        val rawMaxTime = normalizedAccelPoints.last().time
        val isTimeInSeconds = (rawMaxTime < 10000) && (normalizedAccelPoints.size > 100)

        return if (isTimeInSeconds) {
            normalizedAccelPoints.map { pt -> pt.copy(time = pt.time * 1000) }
        } else {
            normalizedAccelPoints
        }
    }
}
