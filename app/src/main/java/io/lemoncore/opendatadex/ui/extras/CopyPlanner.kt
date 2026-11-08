package io.lemoncore.opendatadex.ui.extras

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.lemoncore.opendatadex.R
import io.lemoncore.opendatadex.data.Dex
import io.lemoncore.opendatadex.data.Game
import io.lemoncore.opendatadex.data.Images
import io.lemoncore.opendatadex.data.PokemonSummary
import io.lemoncore.opendatadex.data.TradePlan
import io.lemoncore.opendatadex.data.boxSize
import io.lemoncore.opendatadex.data.tradePlan
import io.lemoncore.opendatadex.data.user.GameCopyEntity
import io.lemoncore.opendatadex.ui.components.EmptyNote
import io.lemoncore.opendatadex.ui.components.Loading
import io.lemoncore.opendatadex.ui.components.LocalWidthClass
import io.lemoncore.opendatadex.ui.components.PokemonArtwork
import io.lemoncore.opendatadex.ui.components.cardRows
import io.lemoncore.opendatadex.ui.components.copyTitle
import io.lemoncore.opendatadex.ui.components.dexNumber
import io.lemoncore.opendatadex.ui.components.obtainText
import io.lemoncore.opendatadex.ui.theme.LocalDexColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val CaughtGreen = Color(0xFF43A047)

/**
 * A living dex laid out in PC boxes: the Pokédex in order, [Game.boxSize] to a box, so the trainer can sort their real
 * boxes to match and see which boxes still have gaps. Tapping a slot opens the Pokémon; holding it marks it caught.
 * With no [onToggle], [caught] is what an imported save holds, so slots can't be marked by hand.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BoxesView(
    game: Game, all: List<PokemonSummary>, caught: Set<Int>, events: Set<Int>, artwork: Map<Int, String>,
    onToggle: ((Int) -> Unit)?, onOpen: (PokemonSummary, List<PokemonSummary>) -> Unit,
) {
    val size = game.boxSize
    val columns = if (size == 30) 6 else 5
    var gapsOnly by rememberSaveable { mutableStateOf(false) }
    val boxes = all.chunked(size).withIndex().filter { (_, box) -> !gapsOnly || box.any { it.id !in caught } }
    val dexColors = LocalDexColors.current
    val haptics = LocalHapticFeedback.current
    val perRow = LocalWidthClass.current.cardsPerRow

    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
        item {
            Text(stringResource(if (onToggle == null) R.string.boxes_intro_save else R.string.boxes_intro, size), color = dexColors.subtleText,
                style = MaterialTheme.typography.bodyMedium)
            Row(Modifier.fillMaxWidth().clickable { gapsOnly = !gapsOnly }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.boxes_gaps_only), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                Switch(gapsOnly, { gapsOnly = it })
            }
        }
        if (boxes.isEmpty()) item { EmptyNote(stringResource(R.string.nothing_left)) }
        cardRows(boxes, perRow, { it.index }) { (index, box), modifier ->
            val done = box.count { it.id in caught }
            Column(
                modifier.clip(RoundedCornerShape(16.dp)).background(dexColors.sectionCard)
                    .border(1.dp, dexColors.divider, RoundedCornerShape(16.dp)).padding(10.dp),
            ) {
                Row(Modifier.padding(start = 4.dp, end = 4.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.box_title, index + 1), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Text("  " + stringResource(R.string.box_range, dexNumber(box.first().dexNumber), dexNumber(box.last().dexNumber)),
                        color = dexColors.subtleText, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    if (done == box.size) Icon(Icons.Filled.CheckCircle, null, tint = CaughtGreen, modifier = Modifier.size(18.dp).padding(end = 4.dp))
                    Text(stringResource(R.string.box_count, done, size), color = dexColors.subtleText, fontSize = 14.sp)
                }
                // Empty slots after the last Pokémon keep the box its real shape.
                val slots = box.map<PokemonSummary, PokemonSummary?> { it } + List(size - box.size) { null }
                slots.chunked(columns).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(bottom = 4.dp)) {
                        row.forEach { p ->
                            Box(
                                Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(10.dp))
                                    .background(if (p != null && p.id in caught) dexColors.chip else dexColors.sectionCard)
                                    .border(1.dp, dexColors.divider, RoundedCornerShape(10.dp))
                                    .then(
                                        if (p == null) Modifier else Modifier.combinedClickable(
                                            onClick = { onOpen(p, all) },
                                            onLongClick = onToggle?.let { toggle -> { haptics.performHapticFeedback(HapticFeedbackType.LongPress); toggle(p.id) } },
                                        ),
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (p != null) {
                                    val have = p.id in caught
                                    PokemonArtwork(p.formId, Images.artwork(p.formId, artwork),
                                        Modifier.fillMaxSize().padding(4.dp).graphicsLayer { alpha = if (have) 1f else 0.25f }, p.name)
                                    if (have) Icon(Icons.Filled.CheckCircle, null, tint = CaughtGreen,
                                        modifier = Modifier.align(Alignment.TopEnd).padding(2.dp).size(12.dp))
                                    // Event-only in this game: a gift marks the slot so its gap isn't a surprise.
                                    else if (p.id in events) Icon(Icons.Filled.CardGiftcard, stringResource(R.string.event_badge), tint = dexColors.subtleText,
                                        modifier = Modifier.align(Alignment.BottomEnd).padding(2.dp).size(12.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * What this copy is still missing and which of the trainer's other games can send it, grouped by game: Pokémon
 * already caught there first (ready to send), then ones that game can catch, evolve or breed. Trade evolutions and
 * what no other game can provide follow.
 */
@Composable
fun TradesView(
    dex: Dex, copy: GameCopyEntity, copies: List<GameCopyEntity>, caught: Map<String, Set<Int>>, events: Set<Int>, artwork: Map<Int, String>,
    onOpen: (PokemonSummary, List<PokemonSummary>) -> Unit,
) {
    val plan by produceState<TradePlan?>(null, copy, copies, caught) {
        value = withContext(Dispatchers.Default) { dex.tradePlan(copy, copies, caught) }
    }
    val p = plan ?: return Loading()
    val dexColors = LocalDexColors.current
    var expanded by rememberSaveable { mutableStateOf(setOf<String>()) }
    fun toggle(key: String) { expanded = if (key in expanded) expanded - key else expanded + key }

    // Each card is split into lazy items (header, one per Pokémon, footer) so a game with hundreds to send stays smooth.
    LazyColumn(contentPadding = PaddingValues(12.dp), modifier = Modifier.fillMaxSize()) {
        item { Text(stringResource(R.string.trades_intro), color = dexColors.subtleText, style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 12.dp)) }
        when {
            p.missing == 0 -> item { EmptyNote(stringResource(R.string.nothing_left)) }
            copies.none { it.id != copy.id } -> item { EmptyNote(stringResource(R.string.trades_add_game)) }
            p.groups.isEmpty() && p.tradeEvolutions.isEmpty() -> item { EmptyNote(stringResource(R.string.trades_none)) }
        }
        p.groups.forEach { group ->
            val key = "g" + group.copy.id
            val open = key in expanded
            val game = Game.fromKey(group.copy.game)
            val order = group.pokemon.map { it.first }
            item(key = "$key-head") {
                val ready = group.pokemon.count { it.second.caught }
                val link = stringResource(group.link.label) + if (group.link.twoWay) "" else " · " + stringResource(R.string.link_one_way)
                CardHeader(stringResource(R.string.trades_from, copyTitle(group.copy)),
                    stringResource(R.string.trades_count, group.pokemon.size, ready) + " · " + link, Color(game.color))
            }
            items(if (open) group.pokemon else group.pokemon.take(PREVIEW), key = { "$key-" + it.first.id }) { (mon, source) ->
                val text = if (source.caught) stringResource(R.string.trades_ready) else source.obtain?.let { obtainText(it, game, living = false) }
                PlanRow(mon, artwork, text, ready = source.caught) { onOpen(mon, order) }
            }
            item(key = "$key-foot") {
                CardFooter { if (group.pokemon.size > PREVIEW) ShowToggle(open, group.pokemon.size) { toggle(key) } }
            }
        }
        if (p.tradeEvolutions.isNotEmpty()) {
            item(key = "evo-head") { CardHeader(stringResource(R.string.trade_evolutions), null, Color(Game.fromKey(copy.game).color)) }
            items(p.tradeEvolutions, key = { "evo-" + it.pokemon.id }) { evo ->
                PlanRow(
                    evo.pokemon, artwork,
                    stringResource(R.string.trade_evolve_line, evo.from) + "\n" + if (evo.partners.isEmpty()) stringResource(R.string.trade_no_partner)
                    else stringResource(R.string.trade_partners, evo.partners.map { copyTitle(it) }.joinToString(", ")),
                    ready = false,
                ) { onOpen(evo.pokemon, p.tradeEvolutions.map { it.pokemon }) }
            }
            item(key = "evo-foot") { CardFooter {} }
        }
        if (p.unsourced.isNotEmpty() && p.missing > 0 && copies.any { it.id != copy.id }) {
            val open = "unsourced" in expanded
            item(key = "none-head") {
                CardHeader(stringResource(R.string.trades_unsourced, p.unsourced.size), null, dexColors.subtleText, onClick = { toggle("unsourced") })
            }
            if (open) items(p.unsourced, key = { "none-" + it.id }) { mon ->
                PlanRow(mon, artwork, if (mon.id in events) stringResource(R.string.plan_event) else null, ready = false) { onOpen(mon, p.unsourced) }
            }
            item(key = "none-foot") { CardFooter { ShowToggle(open, p.unsourced.size) { toggle("unsourced") } } }
        }
    }
}

private const val PREVIEW = 6

/** The top of a card drawn across several lazy items: rounded above, with a coloured bar by its title. */
@Composable
private fun CardHeader(title: String, subtitle: String?, accent: Color, onClick: (() -> Unit)? = null) {
    val dexColors = LocalDexColors.current
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)).background(dexColors.sectionCard)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(5.dp).height(if (subtitle != null) 40.dp else 24.dp).clip(RoundedCornerShape(3.dp)).background(accent))
        Column(Modifier.padding(start = 12.dp)) {
            Text(title, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) Text(subtitle, color = dexColors.subtleText, fontSize = 14.sp)
        }
    }
}

@Composable
private fun CardFooter(content: @Composable () -> Unit) {
    Column(Modifier.padding(bottom = 12.dp)) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                .background(LocalDexColors.current.sectionCard).padding(bottom = 6.dp),
        ) { content() }
    }
}

@Composable
private fun PlanRow(p: PokemonSummary, artwork: Map<Int, String>, text: String?, ready: Boolean, onClick: () -> Unit) {
    val dexColors = LocalDexColors.current
    Row(
        Modifier.fillMaxWidth().background(dexColors.sectionCard).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(48.dp).clip(CircleShape).background(dexColors.chip), contentAlignment = Alignment.Center) {
            PokemonArtwork(p.formId, Images.artwork(p.formId, artwork), Modifier.size(42.dp), p.name)
        }
        Column(Modifier.padding(start = 12.dp).weight(1f)) {
            Text(dexNumber(p.dexNumber) + "  " + p.name, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (text != null) Text(text, color = if (ready) CaughtGreen else dexColors.subtleText, fontSize = 13.sp, maxLines = 3)
        }
        if (ready) Icon(Icons.Filled.CheckCircle, null, tint = CaughtGreen, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun ShowToggle(open: Boolean, total: Int, onToggle: () -> Unit) {
    Text(
        if (open) stringResource(R.string.show_less) else stringResource(R.string.show_all, total),
        color = MaterialTheme.colorScheme.primary, fontSize = 15.sp,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(horizontal = 14.dp, vertical = 10.dp),
    )
}
