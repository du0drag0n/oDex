package io.lemoncore.opendatadex.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** How wide the window is: a phone, a tablet in portrait (or an open foldable), or a tablet in landscape. */
enum class WidthClass {
    COMPACT, MEDIUM, EXPANDED;

    /** Wide enough for the navigation rail and the side panel. */
    val large get() = this != COMPACT

    /** How many cards a list of them (games, catch lists, teams) shows side by side. */
    val cardsPerRow get() = if (large) 2 else 1

    companion object {
        fun of(width: Dp) = when {
            width < 600.dp -> COMPACT
            width < 840.dp -> MEDIUM
            else -> EXPANDED
        }
    }
}

/** The window's [WidthClass], set once at the top of the app. */
val LocalWidthClass = staticCompositionLocalOf { WidthClass.COMPACT }

/** The widest a phone-style screen (lists, settings, a Pokémon full screen) is drawn on a tablet; it sits centred. */
val ReadableWidth = 720.dp

/** [cards] in rows of [perRow], each card as tall as the tallest in its row; [card] draws one with the modifier it's given. */
fun <T> LazyListScope.cardRows(cards: List<T>, perRow: Int, cardKey: (T) -> Any, card: @Composable (T, Modifier) -> Unit) {
    items(cards.chunked(perRow), key = { cardKey(it.first()) }) { row ->
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            row.forEach { key(cardKey(it)) { card(it, Modifier.weight(1f).fillMaxHeight()) } }
            repeat(perRow - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}
