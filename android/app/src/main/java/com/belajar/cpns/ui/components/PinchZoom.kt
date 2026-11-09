package com.belajar.cpns.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import kotlin.math.abs

/**
 * Menangkap cubit dua jari untuk zoom tanpa mengganggu gulir satu jari.
 * Event dua jari diambil di pass Initial, sebelum sampai ke verticalScroll di dalamnya.
 */
fun Modifier.pinchToZoom(onZoom: (Float) -> Unit): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        do {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val pressed = event.changes.filter { it.pressed }
            if (pressed.size >= 2) {
                val zoom = event.calculateZoom()
                if (abs(zoom - 1f) > 0.002f) onZoom(zoom)
                event.changes.forEach { it.consume() }
            }
        } while (event.changes.any { it.pressed })
    }
}

/** Mengalikan ukuran huruf semua Text di dalamnya tanpa mengubah dp layout. */
@Composable
fun ScaledText(scale: Float, content: @Composable () -> Unit) {
    val base = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(base.density, base.fontScale * scale), content = content)
}
