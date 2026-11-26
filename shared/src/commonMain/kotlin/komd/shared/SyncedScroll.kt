package komd.shared

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.launch

/**
 * Mirrors the scroll position between editor and preview as a fraction of each pane's scroll
 * range. Only the pane the user is actually scrolling drives the other one; while we apply the
 * mirrored position we ignore the echo it produces, so the two panes never fight.
 *
 * Proportional mapping is approximate when one pane has much taller content in a section
 * (e.g. a big image); a source-line ↔ block anchor map can refine it later.
 */
@Composable
internal fun SyncedScrollEffect(enabled: Boolean, first: ScrollState, second: ScrollState) {
    if (!enabled) return
    val mirroring = remember { mutableStateOf(false) }
    LaunchedEffect(first, second) {
        launch { mirrorScroll(from = first, to = second, mirroring) }
        launch { mirrorScroll(from = second, to = first, mirroring) }
        // Align once when sync is (re)enabled, using the editor as the reference.
        second.scrollTo(scaledScroll(first, second))
    }
}

private suspend fun mirrorScroll(from: ScrollState, to: ScrollState, mirroring: MutableState<Boolean>) {
    snapshotFlow { from.value to from.isScrollInProgress }
        .collect { (_, inProgress) ->
            if (!inProgress || mirroring.value) return@collect
            mirroring.value = true
            try {
                to.scrollTo(scaledScroll(from, to))
            } finally {
                mirroring.value = false
            }
        }
}

internal fun scaledScroll(fromValue: Int, fromMax: Int, toMax: Int): Int {
    if (fromMax <= 0 || toMax <= 0) return 0
    return (fromValue.toLong() * toMax / fromMax).toInt().coerceIn(0, toMax)
}

private fun scaledScroll(from: ScrollState, to: ScrollState): Int = scaledScroll(from.value, from.maxValue, to.maxValue)

