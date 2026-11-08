package io.lemoncore.opendatadex.ui.pokedex

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import io.lemoncore.opendatadex.ui.theme.LocalDexColors

private val PanelEasing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)

/**
 * The tablet's Pokédex: the list, with the Pokémon tapped in it open in a panel on the right. In landscape ([pushes])
 * the panel takes its room from the list, which reflows into fewer columns; in portrait it covers the right of the
 * list, which can still be scrolled and tapped to show another Pokémon.
 */
@Composable
fun PokedexWithPanel(
    opened: Int?,
    pushes: Boolean,
    list: @Composable (selected: Int?) -> Unit,
    panel: @Composable (Int) -> Unit,
) {
    val c = LocalDexColors.current
    // The panel keeps showing its Pokémon while it slides away.
    val shown = remember { IntArray(1) }
    opened?.let { shown[0] = it }
    val sheet: @Composable () -> Unit = {
        Box(
            Modifier.width(if (pushes) 432.dp else 420.dp).fillMaxHeight()
                .then(if (pushes) Modifier else Modifier.shadow(24.dp, RectangleShape))
                .background(c.bg)
                .drawBehind { drawLine(c.line, Offset.Zero, Offset(0f, size.height), 1.dp.toPx()) }
                // Touches stop here rather than reaching the list underneath.
                .pointerInput(Unit) {},
        ) {
            // Another Pokémon picked while the panel is open fades in rather than sliding in again.
            Crossfade(shown[0], animationSpec = tween(200), label = "panel") { id -> panel(id) }
        }
    }
    Box(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) { list(opened) }
            if (pushes) AnimatedVisibility(
                opened != null,
                enter = expandHorizontally(tween(320, easing = PanelEasing), expandFrom = Alignment.End),
                exit = shrinkHorizontally(tween(320, easing = PanelEasing), shrinkTowards = Alignment.End),
            ) { sheet() }
        }
        if (!pushes) AnimatedVisibility(
            opened != null, Modifier.align(Alignment.CenterEnd),
            enter = slideInHorizontally(tween(320, easing = PanelEasing)) { it } + fadeIn(tween(320)),
            exit = slideOutHorizontally(tween(320, easing = PanelEasing)) { it } + fadeOut(tween(320)),
        ) { sheet() }
    }
}
