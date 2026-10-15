package com.example.soleilracingproject.util.tools.parseCsvSession

import com.example.soleilracingproject.model.telemetry.TelemetryPoint
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
                    TelemetryColumn.TIME -> header.contains("time") || header == "t" || header.contains("stamp")
                    TelemetryColumn.LATITUDE -> header.contains("lat") || header == "y" || header.contains("posy")
                    TelemetryColumn.LONGITUDE -> header.contains("lon") || header.contains("lng") || header.contains("long") || header == "x" || header.contains("posx")
                    TelemetryColumn.ACCELX -> header.contains("accelx") || header == "ax" || header.contains("latg") || header == "gx"
                    TelemetryColumn.ACCELY -> header.contains("accely") || header == "ay" || header.contains("longg") || header == "gy"
                    TelemetryColumn.ACCELZ -> header.contains("accelz") || header == "az" || header.contains("vertg") || header == "gz"
                    TelemetryColumn.SPEED -> header.contains("speed") || header.contains("spd") || header == "v" || header.contains("velocity")
                    else -> header == enumCol.header.lowercase()
                }
            }
        }

        fun fixCoord(value: Float): Float {
            var v = value
            while (abs(v) > 360f) {
                v /= 1000f
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

                TelemetryPoint(
                    time = getValue(TelemetryColumn.TIME)?.toIntOrNull()
                        ?: getValue(TelemetryColumn.TIME)?.toDoubleOrNull()?.toInt()
                        ?: 0,
                    accelX = getValue(TelemetryColumn.ACCELX)?.toFloatOrNull() ?: 0f,
                    accelY = getValue(TelemetryColumn.ACCELY)?.toFloatOrNull() ?: 0f,
                    accelZ = getValue(TelemetryColumn.ACCELZ)?.toFloatOrNull() ?: 0f,
                    latitude = fixCoord(rawLat),
                    longitude = fixCoord(rawLon),
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

        // Normalize time units: if timestamps are in seconds (e.g., max time < 10,000 for a long session), scale to ms
        val rawMaxTime = rawPoints.last().time
        val isTimeInSeconds = (rawMaxTime < 10000) && (rawPoints.size > 100)

        return if (isTimeInSeconds) {
            rawPoints.map { pt -> pt.copy(time = pt.time * 1000) }
        } else {
            rawPoints
        }
    }
}
