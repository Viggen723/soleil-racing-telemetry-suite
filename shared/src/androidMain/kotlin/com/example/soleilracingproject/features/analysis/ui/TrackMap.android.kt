package com.example.soleilracingproject.features.analysis.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.soleilracingproject.model.telemetry.TelemetryPoint
import com.example.soleilracingproject.util.tools.lapLogic.LapLogic
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sqrt

actual val isMapAvailable: Boolean = true

private val SoleilRed = Color(0xFFD71344)
private val ComparisonCyan = Color(0xFF00E5FF)

// A reading has a GPS fix when it is not exactly (0, 0)
private fun hasFix(p: TelemetryPoint) = p.latitude != 0f || p.longitude != 0f

// GPS reading - map position. normalizeLon adds the missing minus sign for western longitudes.
private fun toLatLng(p: TelemetryPoint): LatLng {
    val lat = p.latitude.toDouble()
    return LatLng(lat, LapLogic.normalizeLon(p.longitude.toDouble(), lat))
}

// How many meters one dp of screen covers at this zoom level (the standard Google Maps formula)
private fun metersPerDpAt(latitude: Double, zoom: Float): Double =
    156543.03392 * cos(latitude * PI / 180.0) / 2.0.pow(zoom.toDouble())

@Composable
actual fun TrackMap(
    lapPoints: List<TelemetryPoint>,
    comparisonPoints: List<TelemetryPoint>,
    selectedIndex: Int?,
    onPointSelected: (Int?) -> Unit,
    recenterCount: Int,
    modifier: Modifier
) {
    // Which readings of the main lap have a GPS fix, and where they are on the map.
    // We keep the reading numbers so a tap on the map can be turned back into a reading.
    val validIndexes = remember(lapPoints) { lapPoints.indices.filter { hasFix(lapPoints[it]) } }
    val lapPositions = remember(lapPoints) { validIndexes.map { toLatLng(lapPoints[it]) } }
    val comparisonPositions = remember(comparisonPoints) {
        comparisonPoints.filter { hasFix(it) }.map { toLatLng(it) }
    }

    val cameraPositionState = rememberCameraPositionState()
    var mapLoaded by remember { mutableStateOf(false) }

    // Zoom so the whole lap fits on screen. Runs when the map is ready, when the lap changes,
    // and when the recenter button is pressed.
    LaunchedEffect(mapLoaded, lapPositions, comparisonPositions, recenterCount) {
        val everything = lapPositions + comparisonPositions
        if (mapLoaded && everything.size >= 2) {
            val bounds = LatLngBounds.builder().apply { everything.forEach { include(it) } }.build()
            cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 100))
        }
    }

    // How many meters one dp of screen is right now. We use it to keep the dots a steady size
    // on screen and to decide how close a tap has to be to the line.
    val oneDp = metersPerDpAt(lapPositions.firstOrNull()?.latitude ?: 0.0, cameraPositionState.position.zoom)

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        properties = MapProperties(mapType = MapType.SATELLITE),
        uiSettings = MapUiSettings(
            zoomControlsEnabled = false,
            mapToolbarEnabled = false,
            tiltGesturesEnabled = false,
        ),
        onMapLoaded = { mapLoaded = true },
        onMapClick = { tap ->
            // Find the reading closest to the tap
            var bestPosition = -1
            var bestMeters = Double.MAX_VALUE
            for (i in lapPositions.indices) {
                val dx = (lapPositions[i].longitude - tap.longitude) * cos(tap.latitude * PI / 180.0) * 111_320.0
                val dy = (lapPositions[i].latitude - tap.latitude) * 111_320.0
                val meters = sqrt(dx * dx + dy * dy)
                if (meters < bestMeters) {
                    bestMeters = meters
                    bestPosition = i
                }
            }
            // Select it only if the tap was within 40 dp of the line, otherwise clear the selection
            if (bestPosition >= 0 && bestMeters < 40 * oneDp) {
                onPointSelected(validIndexes[bestPosition])
            } else {
                onPointSelected(null)
            }
        },
    ) {
        // The comparison lap goes underneath, the main lap on top
        if (comparisonPositions.size >= 2) {
            Polyline(points = comparisonPositions, color = ComparisonCyan, width = 8f)
        }
        if (lapPositions.size >= 2) {
            Polyline(points = lapPositions, color = SoleilRed, width = 10f, zIndex = 1f)
        }

        // Start of the lap: a white dot with a black ring
        lapPositions.firstOrNull()?.let { start ->
            Circle(
                center = start, radius = oneDp * 5,
                fillColor = Color.White, strokeColor = Color.Black, strokeWidth = 4f, zIndex = 2f
            )
        }

        // The selected moment: a dot on each lap (white outline, colored middle)
        selectedIndex?.let { index ->
            comparisonPoints.getOrNull(index)?.takeIf { hasFix(it) }?.let { point ->
                Circle(
                    center = toLatLng(point), radius = oneDp * 9,
                    fillColor = Color.White, strokeColor = Color.White, strokeWidth = 1f, zIndex = 3f
                )
                Circle(
                    center = toLatLng(point), radius = oneDp * 5,
                    fillColor = ComparisonCyan, strokeColor = ComparisonCyan, strokeWidth = 1f, zIndex = 4f
                )
            }
            lapPoints.getOrNull(index)?.takeIf { hasFix(it) }?.let { point ->
                Circle(
                    center = toLatLng(point), radius = oneDp * 10,
                    fillColor = Color.White, strokeColor = Color.White, strokeWidth = 1f, zIndex = 5f
                )
                Circle(
                    center = toLatLng(point), radius = oneDp * 6,
                    fillColor = SoleilRed, strokeColor = SoleilRed, strokeWidth = 1f, zIndex = 6f
                )
            }
        }
    }
}