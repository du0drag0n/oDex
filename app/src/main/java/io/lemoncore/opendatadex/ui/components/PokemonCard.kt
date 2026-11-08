package io.lemoncore.opendatadex.ui.components

import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.lemoncore.opendatadex.R
import io.lemoncore.opendatadex.data.PokemonSummary
import io.lemoncore.opendatadex.ui.theme.DexType
import io.lemoncore.opendatadex.ui.theme.LocalDexColors
import io.lemoncore.opendatadex.ui.theme.halo
import io.lemoncore.opendatadex.ui.theme.tileWash
import io.lemoncore.opendatadex.ui.theme.wash

private val CardShape = RoundedCornerShape(16.dp)

/**
 * The Pokémon list card: a dark (or white) card with a wash of the artwork's colour glowing in from the right, the
 * artwork on a soft halo, and the dex number as a pill that toggles caught. Swiping right also toggles caught.
 */
@Composable
fun PokemonCard(
    pokemon: PokemonSummary,
    typeNames: List<String>,
    artworkUrl: String,
    favorite: Boolean,
    caught: Boolean,
    /** Favourite on or off, offered in the long-press menu (the card has no star button). */
    onFavorite: (() -> Unit)?,
    onCaught: (() -> Unit)?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 92.dp,
    subtitle: String? = null,
    /** Key the card colour is learned under; the Shiny Dex uses its own so shiny artwork gets its own colours. */
    colorKey: Int = pokemon.formId,
    /** Gold number pill with a sparkle (Shiny Dex). */
    shinyToggle: Boolean = false,
    /** A short tag after the name, such as "Event" for a Pokémon only an event gives in this game. */
    badge: String? = null,
    /** The Pokémon open in the tablet's side panel: outlined in the accent colour. */
    selected: Boolean = false,
) {
    val c = LocalDexColors.current
    val tones = animatedWash(colorKey, pokemon.colorId)
    val markLabel = stringResource(if (caught) R.string.mark_not_caught else R.string.mark_caught)
    var menu by remember { mutableStateOf(false) }
    val card: @Composable () -> Unit = {
        @OptIn(ExperimentalFoundationApi::class)
        Box(
            Modifier.fillMaxWidth().heightIn(min = height)
                .then(if (!c.isDark) Modifier.shadow(1.dp, CardShape, ambientColor = Color(0x0A14141E), spotColor = Color(0x0A14141E)) else Modifier)
                .wash(tones, CardShape)
                .then(if (selected) Modifier.border(2.dp, c.accent, CardShape) else Modifier)
                .combinedClickable(onClick = onClick, onLongClick = if (onCaught != null || onFavorite != null) ({ menu = true }) else null)
                .semantics {
                    if (onCaught != null) customActions = listOf(CustomAccessibilityAction(markLabel) { onCaught(); true })
                },
        ) {
            Box(Modifier.align(Alignment.CenterEnd).padding(end = 6.dp).size(96.dp).halo(tones))
            PokemonArtwork(
                colorKey, artworkUrl, contentDescription = pokemon.name, colorId = pokemon.colorId,
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp).sharedArtwork(pokemon.formId).size(minOf(82.dp, height - 10.dp)),
            )
            Column(
                // Grows with large text instead of cutting the type tags off.
                Modifier.heightIn(min = height).padding(start = 16.dp, end = 110.dp, top = 6.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                // Nudged left so the number's text lines up with the name. Compose widens the pill's touch area to 48 dp.
                NumberPill(pokemon.dexNumber, caught, onCaught, shiny = shinyToggle, modifier = Modifier.offset(x = (-10).dp))
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            pokemon.name, color = c.text, style = DexType.cardName, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (favorite) Icon(Icons.Filled.Star, null, tint = c.gold, modifier = Modifier.padding(start = 6.dp).size(15.dp))
                        if (badge != null) Text(
                            badge, color = c.textMuted, style = DexType.caption.copy(fontSize = 11.sp), maxLines = 1,
                            modifier = Modifier.padding(start = 6.dp).border(1.dp, c.textFaint, RoundedCornerShape(6.dp))
                                .padding(horizontal = 5.dp, vertical = 1.dp),
                        )
                    }
                    if (subtitle != null) {
                        Text(subtitle, color = c.textMuted, style = DexType.caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            pokemon.types.zip(typeNames).forEach { (id, name) -> TypeTag(id, name) }
                        }
                    }
                }
            }
            DropdownMenu(menu, { menu = false }) {
                if (onFavorite != null) DropdownMenuItem(
                    { Text(stringResource(if (favorite) R.string.remove_favorite else R.string.add_favorite)) },
                    { menu = false; onFavorite() },
                )
                if (onCaught != null) DropdownMenuItem({ Text(markLabel) }, { menu = false; onCaught() })
                DropdownMenuItem({ Text(stringResource(R.string.open)) }, { menu = false; onClick() })
            }
        }
    }
    // With Swipe to mark off, only the Poké Ball chip toggles caught.
    if (onCaught == null || !LocalSettings.current.swipeToMark) {
        Box(modifier) { card() }
        return
    }
    SwipeToCatch(caught, onCaught, modifier, card)
}

/** How far right (a share of the card's width) the finger must go for a swipe to count. */
private const val SWIPE_THRESHOLD = 0.38f

/** Swipe right to toggle caught; the card springs back rather than leaving the list. */
@Composable
private fun SwipeToCatch(caught: Boolean, onToggle: () -> Unit, modifier: Modifier, content: @Composable () -> Unit) {
    val c = LocalDexColors.current
    val feel = LocalFeel.current
    val wasCaught by rememberUpdatedState(caught)
    val toggle by rememberUpdatedState(onToggle)
    // The box only moves the card and springs it back (it never settles at the end). Whether a swipe counts is decided
    // here, once, when the finger lifts: past the buzz, it toggles. Leaving it to the box went wrong both ways: it asks
    // to confirm up to three times a swipe (at the edge, on release and while springing back), which could mark and
    // unmark in one go, and it judges the release by the finger's last speed, so a swipe that slowed or wobbled back
    // at the end didn't count however far it went.
    @Suppress("DEPRECATION")
    val state = rememberSwipeToDismissBoxState(confirmValueChange = { it != SwipeToDismissBoxValue.StartToEnd })
    SwipeToDismissBox(
        state = state, enableDismissFromEndToStart = false,
        // Watches the finger without taking the touch from the box or the list.
        modifier = modifier.pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                var past = false
                do {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val finger = event.changes.firstOrNull { it.id == down.id } ?: break
                    // The card must have moved too, so a scroll of the list never counts.
                    val moved = runCatching { state.requireOffset() }.getOrDefault(0f) > 0f
                    val now = moved && finger.position.x - down.position.x >= size.width * SWIPE_THRESHOLD
                    if (now && !past) feel.threshold()
                    past = now
                } while (finger.pressed)
                if (past) {
                    if (wasCaught) feel.unmarked() else feel.caught()
                    toggle()
                }
            }
        },
        backgroundContent = {
            Row(
                Modifier.fillMaxSize().clip(CardShape).background(if (caught) c.swipeUndo else c.accent).padding(start = 22.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val fg = if (!caught) c.onAccent else if (!c.isDark) c.text else Color.White
                if (caught) Icon(Icons.AutoMirrored.Filled.Undo, null, tint = fg, modifier = Modifier.size(22.dp))
                else PokeballIcon(caught = false, muted = fg, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(if (caught) R.string.swipe_unmark else R.string.swipe_caught), color = fg, style = DexType.itemTitle.copy(fontSize = 13.sp))
            }
        },
    ) { content() }
}

/**
 * The square card of the tablet's Grid list: the artwork large on its halo, the number pill (which toggles caught) at
 * the top, and the name and types at the bottom. Long-press for favourite and caught, as on the row card.
 */
@Composable
fun PokemonGridCard(
    pokemon: PokemonSummary,
    typeNames: List<String>,
    artworkUrl: String,
    favorite: Boolean,
    caught: Boolean,
    onFavorite: () -> Unit,
    onCaught: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: String? = null,
    /** The Pokémon open in the side panel: ringed in the accent colour, a little out from the card. */
    selected: Boolean = false,
) {
    val c = LocalDexColors.current
    val tones = animatedWash(pokemon.formId, pokemon.colorId)
    val markLabel = stringResource(if (caught) R.string.mark_not_caught else R.string.mark_caught)
    var menu by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(24.dp)
    @OptIn(ExperimentalFoundationApi::class)
    BoxWithConstraints(
        modifier.aspectRatio(1f / 1.08f)
            .drawWithContent {
                drawContent()
                if (selected) {
                    val stroke = 2.dp.toPx()
                    val out = 3.dp.toPx() + stroke / 2
                    drawRoundRect(
                        c.accent, Offset(-out, -out), Size(size.width + 2 * out, size.height + 2 * out),
                        CornerRadius(24.dp.toPx() + out), style = Stroke(stroke),
                    )
                }
            }
            .tileWash(tones, shape)
            .combinedClickable(onClick = onClick, onLongClick = { menu = true })
            .semantics { customActions = listOf(CustomAccessibilityAction(markLabel) { onCaught(); true }) },
    ) {
        // The artwork takes 64% of the width, centred 43% of the way down.
        val art = maxWidth * 0.64f
        val haloSize = maxWidth * 0.82f
        val centreY = maxHeight * 0.43f
        Box(Modifier.offset((maxWidth - haloSize) / 2, centreY - haloSize / 2).size(haloSize).halo(tones))
        PokemonArtwork(
            pokemon.formId, artworkUrl, contentDescription = pokemon.name, colorId = pokemon.colorId,
            modifier = Modifier.offset((maxWidth - art) / 2, centreY - art / 2).sharedArtwork(pokemon.formId).size(art),
        )
        NumberPill(
            pokemon.dexNumber, caught, onCaught, modifier = Modifier.padding(start = 8.dp, top = 8.dp),
            offBackground = if (c.isDark) Color(0xFF0B0B0D).copy(alpha = 0.55f) else Color.White.copy(alpha = 0.75f),
        )
        if (favorite) Icon(Icons.Filled.Star, null, tint = c.gold, modifier = Modifier.align(Alignment.TopEnd).padding(14.dp).size(18.dp))
        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicText(
                    pokemon.name, maxLines = 1, style = DexType.cardName.copy(color = c.text),
                    autoSize = TextAutoSize.StepBased(minFontSize = 12.sp, maxFontSize = 18.sp),
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (badge != null) Text(
                    badge, color = c.textMuted, style = DexType.caption.copy(fontSize = 11.sp), maxLines = 1,
                    modifier = Modifier.padding(start = 6.dp).border(1.dp, c.textFaint, RoundedCornerShape(6.dp)).padding(horizontal = 5.dp, vertical = 1.dp),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                pokemon.types.zip(typeNames).forEach { (id, name) -> TypeTag(id, name) }
            }
        }
        DropdownMenu(menu, { menu = false }) {
            DropdownMenuItem({ Text(stringResource(if (favorite) R.string.remove_favorite else R.string.add_favorite)) }, { menu = false; onFavorite() })
            DropdownMenuItem({ Text(markLabel) }, { menu = false; onCaught() })
            DropdownMenuItem({ Text(stringResource(R.string.open)) }, { menu = false; onClick() })
        }
    }
}

/** Small tile for grids of Pokémon on other screens: a wash tile with the artwork on its halo and the name below. */
@Composable
fun PokemonTile(pokemon: PokemonSummary, artworkUrl: String, label: String?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = LocalDexColors.current
    val tones = animatedWash(pokemon.formId, pokemon.colorId)
    Column(
        modifier.wash(tones, RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(78.dp).halo(tones))
            PokemonArtwork(pokemon.formId, artworkUrl, Modifier.size(64.dp).aspectRatio(1f), pokemon.name, colorId = pokemon.colorId)
        }
        Text(dexNumber(pokemon.dexNumber), color = c.textSubtle, style = DexType.monoMeta)
        BasicText(
            pokemon.name, maxLines = 1, style = DexType.itemTitle.copy(color = c.text),
            autoSize = TextAutoSize.StepBased(minFontSize = 9.sp, maxFontSize = 14.sp), modifier = Modifier.padding(horizontal = 4.dp),
        )
        if (label != null) Text(label, color = c.textMuted, style = DexType.caption, maxLines = 2, modifier = Modifier.padding(horizontal = 4.dp))
    }
}

