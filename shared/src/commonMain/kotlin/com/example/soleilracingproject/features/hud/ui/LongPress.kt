package com.example.soleilracingproject.features.hud.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput

// Calls onLongPress when a finger stays down on this element for about half a second.
// It watches the touch before the widgets inside get it (PointerEventPass.Initial),
// and it does not take the touch, so normal taps (like the Start button) still work.
fun Modifier.onLongPress(onLongPress: () -> Unit): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        // A finger touches down. We only watch it.
        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)

        // Wait for the long-press time. If the finger lifts before then, it was just a tap.
        val liftedEarly = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
            do {
                val event = awaitPointerEvent(PointerEventPass.Initial)
            } while (event.changes.any { it.pressed })
            true
        }

        // Time ran out and the finger is still down: this is a long press.
        if (liftedEarly == null) {
            onLongPress()
            // Eat the rest of this touch, so lifting the finger doesn't also click the widget.
            do {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                event.changes.forEach { it.consume() }
            } while (event.changes.any { it.pressed })
        }
    }
}