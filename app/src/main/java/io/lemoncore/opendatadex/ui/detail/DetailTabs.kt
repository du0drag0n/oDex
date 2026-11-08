package io.lemoncore.opendatadex.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Phishing
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Grass
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.em
import io.lemoncore.opendatadex.ui.components.GameSwatch
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import io.lemoncore.opendatadex.R
import io.lemoncore.opendatadex.data.Dex
import io.lemoncore.opendatadex.data.EvolutionNode
import io.lemoncore.opendatadex.data.Game
import io.lemoncore.opendatadex.data.Images
import io.lemoncore.opendatadex.data.LearnMethod
import io.lemoncore.opendatadex.data.LearnedMove
import io.lemoncore.opendatadex.data.Obtain
import io.lemoncore.opendatadex.data.ObtainKind
import io.lemoncore.opendatadex.data.StatLine
import io.lemoncore.opendatadex.data.catchPlan
import io.lemoncore.opendatadex.data.seed.SeedPokemon
import io.lemoncore.opendatadex.data.seed.SeedSpecies
import io.lemoncore.opendatadex.ui.components.CategoryBadge
import io.lemoncore.opendatadex.ui.components.EmptyNote
import io.lemoncore.opendatadex.ui.components.Loading
import io.lemoncore.opendatadex.ui.components.MonoTag
import io.lemoncore.opendatadex.ui.components.PokemonArtwork
import io.lemoncore.opendatadex.ui.components.PokemonTile
import io.lemoncore.opendatadex.ui.components.Section
import io.lemoncore.opendatadex.ui.components.SectionTitle
import io.lemoncore.opendatadex.ui.components.SegmentedButtons
import io.lemoncore.opendatadex.ui.components.StatBar
import io.lemoncore.opendatadex.ui.components.TypeTag
import io.lemoncore.opendatadex.ui.components.ValueBox
import io.lemoncore.opendatadex.ui.components.animatedWash
import io.lemoncore.opendatadex.ui.components.formatMoveValue
import io.lemoncore.opendatadex.ui.components.gameName
import io.lemoncore.opendatadex.ui.components.generationLabel
import io.lemoncore.opendatadex.ui.components.levelRange
import io.lemoncore.opendatadex.ui.components.obtainText
import io.lemoncore.opendatadex.ui.components.rowCard
import io.lemoncore.opendatadex.ui.pokedex.GamePickerSheet
import io.lemoncore.opendatadex.ui.pokedex.gamePairs
import io.lemoncore.opendatadex.ui.theme.DexType
import io.lemoncore.opendatadex.ui.theme.LocalDexColors
import io.lemoncore.opendatadex.ui.theme.WashTones
import io.lemoncore.opendatadex.ui.theme.multiplierColor
import io.lemoncore.opendatadex.ui.theme.wash
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.roundToInt

// ---- Info ------------------------------------------------------------------------------------------------------------------

/** Base stats, evolution and damage taken first; then the entry, abilities, held items, training and breeding. */
@Composable
internal fun InfoTab(
    dex: Dex, s: SeedSpecies, p: SeedPokemon, game: Game, tones: WashTones, artwork: Map<Int, String>, nav: DetailNav,
    onCry: () -> Unit, onSpeak: () -> Unit, canSpeak: Boolean,
) {
    BaseStats(dex, p, game, tones)
    Evolution(dex, s, game, artwork, nav)
    DamageTaken(dex, p, game, nav)
    Species(dex, s, p, game, onCry, onSpeak, canSpeak)
    Abilities(dex, p, game, nav)
    if (game.dataGen >= 2) HeldItems(dex, s, p, game)
    Training(dex, s, p, game)
    Breeding(dex, s, game)
}

/** Short stat names for the stat rows ("Atk", "SpA"; "Spc" for Generation I's Special). */
@androidx.annotation.StringRes
private fun shortStat(st: StatLine) = when (st.statId) {
    1 -> R.string.stat_short_hp; 2 -> R.string.stat_short_attack; 3 -> R.string.stat_short_defense; 4 -> R.string.stat_short_sp_attack
    5 -> R.string.stat_short_sp_defense; 6 -> R.string.stat_short_speed; else -> R.string.stat_short_special
}

@Composable
private fun BaseStats(dex: Dex, p: SeedPokemon, game: Game, tones: WashTones) {
    val c = LocalDexColors.current
    var mode by rememberSaveable { mutableIntStateOf(0) }
    Section(stringResource(R.string.section_base_stats), trailing = {
        SegmentedButtons(
            listOf(stringResource(R.string.stat_base), stringResource(R.string.stat_min), stringResource(R.string.stat_max)), mode, { mode = it },
            Modifier.width(150.dp), height = 26.dp,
        )
    }) {
        val stats = dex.baseStats(p, game)
        val values = stats.map { when (mode) { 1 -> it.min; 2 -> it.max; else -> it.base } }
        // Base stats fill their bar at 180; min and max at level 100 at 500.
        val scale = if (mode == 0) 180 else 500
        val top = values.maxOrNull() ?: 0
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            stats.forEachIndexed { i, st ->
                StatBar(stringResource(shortStat(st)), values[i], scale, tones.statBar, highest = values[i] == top)
            }
        }
        Box(Modifier.fillMaxWidth().padding(top = 10.dp).height(1.dp).background(c.divider))
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.stats_total), color = c.textSubtle, style = DexType.caption, modifier = Modifier.weight(1f))
            Text(values.sum().toString(), color = c.text, style = DexType.monoValue.copy(fontWeight = FontWeight.SemiBold))
        }
        if (mode != 0) Note(stringResource(if (game.dataGen <= 2) R.string.stat_range_gen12 else R.string.stat_range))
        if (game.dataGen == 1) Note(stringResource(R.string.special_stat_note))
    }
}

@Composable
private fun Note(text: String) {
    Text(text, color = LocalDexColors.current.textSubtle, style = DexType.caption.copy(fontWeight = FontWeight.Medium), modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
}

/** The family as one wash card per member, stage by stage, each with how it evolves. */
@Composable
private fun Evolution(dex: Dex, s: SeedSpecies, game: Game, artwork: Map<Int, String>, nav: DetailNav) {
    val tree = dex.evolutionTree(s, game)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(stringResource(R.string.section_evolution))
        if (tree == null) {
            EmptyNote(if (game.isNational) stringResource(R.string.no_evolution, s.name) else stringResource(R.string.no_evolution_in, s.name, gameName(game)))
            return@Column
        }
        val stages = generateSequence(listOf(tree)) { stage -> stage.flatMap { it.next }.takeIf { it.isNotEmpty() } }.flatten()
        val base = stringResource(R.string.evo_base)
        stages.forEach { node -> EvolutionCard(node, node.method ?: base, artwork, current = node.pokemon.id == s.id) { nav.openPokemon(node.pokemon.id) } }
    }
}

@Composable
private fun EvolutionCard(node: EvolutionNode, method: String, artwork: Map<Int, String>, current: Boolean, onClick: () -> Unit) {
    val c = LocalDexColors.current
    val p = node.pokemon
    val tones = animatedWash(p.formId, p.colorId)
    Row(
        Modifier.fillMaxWidth().height(64.dp).wash(tones, RoundedCornerShape(14.dp), border = if (current) c.accent else c.line)
            .clickable(enabled = !current, onClick = onClick).padding(start = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(p.name, color = c.text, style = DexType.itemTitle.copy(fontSize = 15.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(method, color = c.textMuted, style = DexType.monoMeta, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        PokemonArtwork(p.formId, Images.artwork(p.formId, artwork), Modifier.padding(end = 10.dp).size(60.dp), p.name, colorId = p.colorId)
    }
}

/** One row per multiplier that has types: 4×, 2×, ½×, ¼×, 0×. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DamageTaken(dex: Dex, p: SeedPokemon, game: Game, nav: DetailNav) {
    val c = LocalDexColors.current
    val groups = dex.damageTaken(dex.typesOf(p, game), game).filter { it.first != 100 }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle(stringResource(R.string.section_damage_taken))
        groups.forEach { (factor, types) ->
            Row(Modifier.padding(horizontal = 4.dp)) {
                val label = when (factor) { 25 -> "¼×"; 50 -> "½×"; 0 -> "0×"; else -> "${factor / 100}×" }
                Text(label, color = multiplierColor(factor, c.isDark), style = DexType.monoValue.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.width(34.dp).padding(top = 3.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    types.forEach { t -> TypeTag(t.id, t.name, large = true, dot = false) { nav.openType(t.id) } }
                }
            }
        }
        if (game.dataGen == 1) GameNote(stringResource(R.string.type_chart_gen1))
        if (game.dataGen in 2..5) GameNote(stringResource(R.string.type_chart_gen2_5, generationLabel(game.dataGen)))
    }
}

@Composable
private fun Species(dex: Dex, s: SeedSpecies, p: SeedPokemon, game: Game, onCry: () -> Unit, onSpeak: () -> Unit, canSpeak: Boolean) {
    val c = LocalDexColors.current
    Section(stringResource(R.string.section_species)) {
        val flavor = dex.flavorText(s, game)
        if (flavor != null) {
            Text(flavor.first, color = c.text, style = DexType.body.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium, lineHeight = 20.sp))
            Text(
                if (game.isNational || flavor.second == null) stringResource(R.string.entry_latest) else stringResource(R.string.entry_from, flavor.second!!),
                color = c.textSubtle, style = DexType.caption, modifier = Modifier.padding(top = 6.dp, bottom = 12.dp),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ValueBox(heightText(p.height), stringResource(R.string.height), Modifier.weight(1f))
            ValueBox(weightText(p.weight), stringResource(R.string.weight), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconBox(Icons.AutoMirrored.Filled.VolumeUp, stringResource(R.string.read_aloud), Modifier.weight(1f), enabled = canSpeak, onClick = onSpeak)
            IconBox(Icons.Filled.MusicNote, stringResource(R.string.cry), Modifier.weight(1f), onClick = onCry)
        }
    }
}

@Composable
private fun Abilities(dex: Dex, p: SeedPokemon, game: Game, nav: DetailNav) {
    val c = LocalDexColors.current
    var showAnyway by rememberSaveable { mutableStateOf(false) }
    Section(stringResource(R.string.section_abilities), trailing = { if (!game.hasAbilities) MonoTag(stringResource(R.string.not_in_this_game)) }) {
        if (!game.hasAbilities && !showAnyway) {
            Text(stringResource(R.string.no_abilities_in, gameName(game)), color = c.textSubtle, style = DexType.body.copy(fontWeight = FontWeight.Medium))
            TextButton(onClick = { showAnyway = true }) { Text(stringResource(R.string.show_abilities_anyway), color = c.accentText, style = DexType.chip) }
        } else {
            dex.abilitiesOf(p, if (game.hasAbilities) game else Game.NATIONAL).forEach { a ->
                Row(Modifier.fillMaxWidth().clickable { nav.openAbility(a.ability.id) }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(a.ability.name, color = c.text, style = DexType.itemTitle)
                            if (a.hidden) MonoTag(stringResource(R.string.hidden_ability), Modifier.padding(start = 8.dp))
                        }
                        Text(a.ability.short, color = c.textSubtle, style = DexType.caption.copy(fontWeight = FontWeight.Medium))
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = c.textFaint)
                }
            }
        }
    }
}

@Composable
private fun HeldItems(dex: Dex, s: SeedSpecies, p: SeedPokemon, game: Game) {
    val c = LocalDexColors.current
    Section(stringResource(R.string.section_held_items)) {
        val held = dex.heldItems(p, game)
        if (held.isEmpty()) Text(
            if (!dex.hasHeldItemData(game)) stringResource(R.string.held_items_no_data, gameName(game))
            else if (game.isNational) stringResource(R.string.held_items_none, s.name) else stringResource(R.string.held_items_none_in, s.name, gameName(game)),
            color = c.textSubtle, style = DexType.body.copy(fontWeight = FontWeight.Medium),
        )
        held.groupBy { it.item.id }.values.forEach { rows ->
            val h = rows.first()
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(Images.item(h.item.key), null, Modifier.size(30.dp))
                Text(h.item.name, color = c.text, style = DexType.itemTitle, modifier = Modifier.padding(start = 10.dp).weight(1f))
                Text(
                    if (game.isNational) rows.map { "${gameName(it.game)} ${it.rarity}%" }.joinToString(", ") else "${h.rarity}%",
                    color = c.textSubtle, style = DexType.monoMeta, textAlign = TextAlign.End, modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun Training(dex: Dex, s: SeedSpecies, p: SeedPokemon, game: Game) {
    Section(stringResource(R.string.section_training)) {
        val ev = dex.evYield(p)
        ValueBox(ev.map { "${it.second} ${stringResource(it.first)}" }.joinToString(", ").ifEmpty { "—" }, stringResource(if (game.dataGen <= 2) R.string.ev_yield_gen3 else R.string.ev_yield))
        val pct = (s.capture / 3.0) / 255.0 * 100
        ValueBox(stringResource(R.string.catch_rate_value, s.capture, pct), stringResource(R.string.catch_rate))
        dex.growthRate(s)?.let { ValueBox(stringResource(R.string.growth_rate_value, it.name.replaceFirstChar(Char::titlecase), it.exp100), stringResource(R.string.growth_rate)) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ValueBox(p.baseExp.toString(), stringResource(R.string.base_experience), Modifier.weight(1f))
            ValueBox(if (game.dataGen < 2) "—" else s.happiness.toString(), stringResource(R.string.base_friendship), Modifier.weight(1f))
        }
    }
}

@Composable
private fun Breeding(dex: Dex, s: SeedSpecies, game: Game) {
    val c = LocalDexColors.current
    Section(stringResource(R.string.section_breeding)) {
        val note = if (game.dataGen == 1) stringResource(R.string.breeding_gen2) else if (!game.hasBreeding) stringResource(R.string.no_breeding_in, gameName(game)) else null
        if (note != null) Text(note, color = c.textSubtle, style = DexType.body.copy(fontWeight = FontWeight.Medium), modifier = Modifier.padding(bottom = 10.dp))
        ValueBox(if (s.genderRate < 0) stringResource(R.string.genderless) else genderText(s.genderRate), stringResource(R.string.gender))
        ValueBox(dex.eggGroupNames(s).joinToString(", ").ifEmpty { "—" }, stringResource(R.string.egg_groups))
        ValueBox(stringResource(R.string.egg_cycles_value, s.hatch, (s.hatch + 1) * 256), stringResource(R.string.egg_cycles))
    }
}

private fun genderText(rate: Int) = run {
    val female = rate * 12.5
    "%.1f%% ♂ · %.1f%% ♀".format(Locale.US, 100 - female, female)
}

@Composable
private fun IconBox(icon: androidx.compose.ui.graphics.vector.ImageVector, caption: String, modifier: Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    val c = LocalDexColors.current
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(12.dp)).background(c.surface2).border(1.dp, c.line, RoundedCornerShape(12.dp))
                .clickable(enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, caption, tint = if (enabled) c.text else c.textFaint, modifier = Modifier.size(20.dp)) }
        Text(caption, color = c.textSubtle, style = DexType.caption, modifier = Modifier.padding(top = 4.dp))
    }
}

private fun heightText(dm: Int): String {
    val m = dm / 10.0
    val inches = (m * 39.3701).roundToInt()
    return "%d'%d\" (%.2f m)".format(Locale.US, inches / 12, inches % 12, m)
}

private fun weightText(hg: Int): String {
    val kg = hg / 10.0
    return "%.1f lbs (%.1f kg)".format(Locale.US, kg * 2.20462, kg)
}

// ---- Evolution chains elsewhere (Evolution Dex) ----------------------------------------------------------------------------

private fun EvolutionNode.isLinear(): Boolean = next.size <= 1 && next.all { it.isLinear() }

private fun EvolutionNode.depth(): Int = 1 + (next.maxOfOrNull { it.depth() } ?: 0)

/** An evolution family: across the card width when it has no branches, else a scrollable branching row. */
@Composable
internal fun EvolutionChain(tree: EvolutionNode, artwork: Map<Int, String>, currentId: Int, onOpen: (Int) -> Unit) {
    if (tree.isLinear() && tree.depth() <= 3) LinearEvolution(tree, artwork, onOpen, currentId)
    else Row(Modifier.horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
        EvolutionRow(tree, artwork, onOpen, currentId)
    }
}

/**
 * A chain without branches fits the card width. Tiles are always sized for three stages, so a two-stage family keeps
 * normal-sized tiles and gives the method text the extra room.
 */
@Composable
private fun LinearEvolution(root: EvolutionNode, artwork: Map<Int, String>, onOpen: (Int) -> Unit, currentId: Int) {
    val stages = generateSequence(root) { it.next.firstOrNull() }.toList()
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val tile = (maxWidth - 112.dp) / 3
        val arrow = if (stages.size > 1) (maxWidth - tile * stages.size) / (stages.size - 1) else 0.dp
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            stages.forEachIndexed { i, node ->
                if (i > 0) EvolutionArrow(node.method, Modifier.width(arrow))
                PokemonTile(node.pokemon, Images.artwork(node.pokemon.formId, artwork), label = null, modifier = Modifier.width(tile),
                    onClick = { if (node.pokemon.id != currentId) onOpen(node.pokemon.id) })
            }
        }
    }
}

@Composable
private fun EvolutionArrow(method: String?, modifier: Modifier) {
    val c = LocalDexColors.current
    Column(modifier.padding(horizontal = 2.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = c.textFaint, modifier = Modifier.size(18.dp))
        Text(method ?: "", textAlign = TextAlign.Center, style = DexType.monoMeta.copy(fontWeight = FontWeight.Medium, lineHeight = 13.sp), color = c.textMuted)
    }
}

@Composable
private fun EvolutionRow(node: EvolutionNode, artwork: Map<Int, String>, onOpen: (Int) -> Unit, currentId: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        PokemonTile(node.pokemon, Images.artwork(node.pokemon.formId, artwork), label = null, modifier = Modifier.width(104.dp),
            onClick = { if (node.pokemon.id != currentId) onOpen(node.pokemon.id) })
        if (node.next.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                node.next.forEach { child ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        EvolutionArrow(child.method, Modifier.width(92.dp))
                        EvolutionRow(child, artwork, onOpen, currentId)
                    }
                }
            }
        }
    }
}

// ---- Gallery helpers ---------------------------------------------------------------------------------------------------------

@Composable
internal fun LookTile(name: String, url: String, fallback: String, types: List<Int>, dex: Dex, modifier: Modifier) {
    val c = LocalDexColors.current
    var failed by remember(url) { mutableStateOf(false) }
    Column(modifier.rowCard(c, 14.dp).padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        AsyncImage(if (failed) fallback else url, contentDescription = name, modifier = Modifier.size(72.dp), onError = { failed = true })
        Text(name, style = DexType.caption, textAlign = TextAlign.Center, maxLines = 2, color = c.textMuted)
        types.forEach { t -> TypeTag(t, dex.type(t)?.name ?: "", Modifier.padding(top = 4.dp)) }
    }
}

/** A game sprite; forms and species missing from a game's sprite set fall back to their Pokémon HOME render. */
@Composable
internal fun SpriteColumn(label: String, url: String?, fallback: String?) {
    var failed by remember(url) { mutableStateOf(false) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        AsyncImage(
            if (failed && fallback != null) fallback else url, contentDescription = label, modifier = Modifier.size(120.dp),
            filterQuality = if (failed) FilterQuality.Low else FilterQuality.None,
            onError = { failed = true },
        )
        Text(label, color = LocalDexColors.current.textMuted, style = DexType.chip)
    }
}

// ---- Locations ---------------------------------------------------------------------------------------------------------------

/**
 * Where to catch it in the selected game: each place with how and at what levels. When it isn't found there, a card
 * says how to get it instead (trade from the other version, evolve, breed, an event or a transfer).
 */
@Composable
internal fun LocationsTab(dex: Dex, s: SeedSpecies, game: Game, nav: DetailNav) {
    val c = LocalDexColors.current
    val obtain by produceState<Obtain?>(null, s.id, game) {
        // Pokémon outside the regional dex (Mew in Emerald) are planned from the game's National Pokédex.
        value = if (game.isNational) null else withContext(Dispatchers.Default) {
            dex.catchPlan(game)[s.id] ?: if (game.hasNationalDex) dex.catchPlan(game, national = true)[s.id] else null
        }
    }
    val encounters = dex.encountersOf(s, game)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!dex.hasEncounterData(game)) {
            EmptyNote(stringResource(R.string.locations_no_data, gameName(game)))
            return@Column
        }
        if (encounters.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GameOverline(game, if (game.isNational) stringResource(R.string.locations_all_games) else stringResource(R.string.locations_in_game, gameName(game)),
                    Modifier.weight(1f))
                Row(
                    Modifier.clip(RoundedCornerShape(16.dp)).background(c.accentTint).clickable { nav.openMap(s.id) }.padding(start = 10.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Icons.Filled.Map, null, tint = c.accentText, modifier = Modifier.size(16.dp))
                    Text(stringResource(R.string.show_on_map), color = c.accentText, style = DexType.chip)
                }
            }
            Column(Modifier.fillMaxWidth().locationCard()) {
                encounters.groupBy { it.areaId }.values.forEachIndexed { i, list ->
                    if (i > 0) Box(Modifier.padding(start = 66.dp).fillMaxWidth().height(1.dp).background(if (c.isDark) Color(0xFF1A1B1F) else c.line))
                    PlaceRow(list, game) { nav.openLocation(list.first().areaId, s.id) }
                }
            }
            dex.encountersBorrowedFrom(game)?.let { EmptyNote(stringResource(R.string.locations_borrowed, gameName(game), gameName(it))) }
        } else if (!game.isNational) {
            GameOverline(game, stringResource(R.string.locations_cant_get, gameName(game)), Modifier.padding(top = 6.dp))
            CantGetCard(s, game, obtain)
        } else {
            EmptyNote(stringResource(R.string.locations_none_any, s.name))
        }
    }
}

@Composable
private fun GameOverline(game: Game, text: String, modifier: Modifier = Modifier) {
    Row(modifier.padding(start = 8.dp, end = 8.dp, bottom = 0.dp), verticalAlignment = Alignment.CenterVertically) {
        GameSwatch(game, 10.dp, radius = 3.dp)
        Spacer(Modifier.width(8.dp))
        Text(text.uppercase(), color = LocalDexColors.current.textSubtle, style = DexType.overline.copy(letterSpacing = 0.1.em))
    }
}

@Composable
private fun Modifier.locationCard(): Modifier {
    val c = LocalDexColors.current
    val shape = RoundedCornerShape(16.dp)
    return clip(shape).background(if (c.isDark) Color(0xFF0C0C0F) else Color.White).border(1.dp, if (c.isDark) Color(0xFF1A1B1F) else c.line, shape)
}

/** One place: an icon for how it's found, the place, the method and levels, and the encounter rate. */
@Composable
private fun PlaceRow(list: List<io.lemoncore.opendatadex.data.Encounter>, game: Game, onClick: () -> Unit) {
    val c = LocalDexColors.current
    val e = list.first()
    val gameNames = list.map { it.game }.distinct().map { gameName(it) }
    val games = if (game.isNational) gameNames.joinToString("/") + " · " else ""
    val rate = list.maxOf { it.rarity }
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(c.accentTint), contentAlignment = Alignment.Center) {
            Icon(methodIcon(e), null, tint = c.accentText, modifier = Modifier.size(19.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(e.place, color = c.text, style = DexType.itemTitle.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold))
            Text(
                "$games${e.method} · ${levelRange(list.minOf { it.minLevel }, list.maxOf { it.maxLevel })}",
                color = c.textSubtle, style = DexType.caption.copy(fontWeight = FontWeight.Medium),
            )
        }
        if (rate > 0) Text("$rate%", color = c.accentText, style = DexType.monoValue.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold))
    }
}

/** An icon for how a Pokémon is found: grass, surfing, fishing, rocks, a gift, a trade or a one-off encounter. */
private fun methodIcon(e: io.lemoncore.opendatadex.data.Encounter): ImageVector {
    val m = e.method.lowercase()
    return when {
        e.methodId in 2..4 || "rod" in m || "fish" in m -> Icons.Filled.Phishing
        e.methodId == 5 || "surf" in m -> Icons.Filled.Waves
        e.methodId == 6 || "rock" in m -> Icons.Filled.Landscape
        e.methodId in 18..19 || "gift" in m -> Icons.Filled.CardGiftcard
        e.methodId == 36 || "trade" in m -> Icons.Filled.SwapHoriz
        e.methodId == 20 || "only" in m || "static" in m -> Icons.Filled.AutoAwesome
        else -> Icons.Filled.Grass
    }
}

/** Not found in [game]: the Pokémon's sprite on its wash, with how to get it instead. */
@Composable
private fun CantGetCard(s: SeedSpecies, game: Game, obtain: Obtain?) {
    val c = LocalDexColors.current
    val tones = animatedWash(s.id, s.color)
    Row(
        Modifier.fillMaxWidth().wash(tones, RoundedCornerShape(16.dp)).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PokemonArtwork(s.id, Images.pixelSprite(s.id), Modifier.size(56.dp), s.name, colorId = s.color)
        Column(Modifier.weight(1f)) {
            Text(s.name, color = c.text, style = DexType.itemTitle.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold))
            Text(
                obtain?.takeIf { it.kind != ObtainKind.UNKNOWN && it.kind != ObtainKind.WILD }?.let { obtainText(it, game, living = false) }
                    ?: stringResource(R.string.locations_not_in, gameName(game)),
                color = c.textMuted, style = DexType.caption.copy(fontWeight = FontWeight.Medium),
            )
        }
        Icon(Icons.Filled.SwapHoriz, null, tint = c.textMuted, modifier = Modifier.size(18.dp))
    }
}

// ---- Moves ------------------------------------------------------------------------------------------------------------------

@Composable
internal fun MovesTab(dex: Dex, s: SeedSpecies, p: SeedPokemon, game: Game, nav: DetailNav, onPickGame: (Game) -> Unit) {
    val c = LocalDexColors.current
    var picker by remember { mutableStateOf(false) }
    if (game.isNational) {
        Section(stringResource(R.string.section_moves)) {
            Text(stringResource(R.string.moves_pick_game, s.name), color = c.textSubtle, style = DexType.body, modifier = Modifier.padding(bottom = 10.dp))
            GameChoiceRow(onPick = onPickGame)
        }
        return
    }
    var method by rememberSaveable { mutableIntStateOf(0) }
    val methods = LearnMethod.entries
    val learnsets by produceState(dex.learnsetsIfLoaded(game), game) { value = withContext(Dispatchers.Default) { dex.learnsets(game) } }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SegmentedButtons(methods.map { stringResource(dex.methodLabel(it, game)) }, method, { method = it })
        Text(
            stringResource(R.string.showing_game_change, gameName(game)), color = c.accentText, style = DexType.chip,
            modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { picker = true }.padding(horizontal = 4.dp, vertical = 6.dp),
        )
        val ls = learnsets ?: return@Column Loading(Modifier.height(160.dp))
        val moves = dex.movesLearned(p, game, methods[method], ls)
        if (moves.isEmpty()) {
            EmptyNote(
                when {
                    ls.pokemonIds.isEmpty() -> stringResource(R.string.moves_no_data, gameName(game))
                    ls.rows(p.id) == null && ls.rows(s.forms.first()) == null -> stringResource(R.string.moves_no_data_for, s.name, gameName(game))
                    methods[method] == LearnMethod.EGG && game.dataGen == 1 -> stringResource(R.string.breeding_gen2)
                    methods[method] == LearnMethod.EGG && !game.hasBreeding -> stringResource(R.string.no_breeding_in, gameName(game))
                    else -> stringResource(R.string.moves_none_this_way, s.name, gameName(game))
                },
            )
        } else BoxWithConstraints {
            // Accuracy and PP fit on wider screens.
            val wide = maxWidth >= 380.dp
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                moves.forEach { m -> MoveRow(dex, m, methods[method], wide) { nav.openMove(m.move.id) } }
            }
        }
    }
    if (picker) GamePickerSheet(game, onPick = { onPickGame(it); picker = false }, onDismiss = { picker = false }, includeNational = false)
}

@Composable
internal fun GameChoiceRow(onPick: (Game) -> Unit) {
    val c = LocalDexColors.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Game.byGeneration().forEach { (_, games) ->
            gamePairs(games).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { g ->
                        Row(
                            Modifier.weight(1f).height(44.dp).rowCard(c, 12.dp).clickable { onPick(g) }.padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.size(8.dp, 24.dp).clip(RoundedCornerShape(3.dp)).background(Color(g.color)))
                            Spacer(Modifier.width(10.dp))
                            Text(gameName(g), color = c.text, style = DexType.itemTitle.copy(fontSize = 13.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MoveRow(dex: Dex, m: LearnedMove, method: LearnMethod, wide: Boolean, onClick: () -> Unit) {
    val c = LocalDexColors.current
    val first = when (method) {
        LearnMethod.LEVEL_UP -> if (m.level <= 0) stringResource(R.string.evo_move) else m.level.toString()
        LearnMethod.MACHINE -> m.machine ?: stringResource(R.string.method_tm)
        else -> ""
    }
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).rowCard(c).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (first.isNotEmpty() || method == LearnMethod.LEVEL_UP || method == LearnMethod.MACHINE) {
            Text(first, color = c.textSubtle, style = DexType.monoMeta.copy(fontWeight = FontWeight.Medium), maxLines = 1, modifier = Modifier.width(34.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(m.move.name, color = c.text, style = DexType.itemTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
            CategoryBadge(m.move.category)
        }
        TypeTag(m.move.type, dex.type(m.move.type)?.name ?: "", dot = false)
        Text(formatMoveValue(m.move.power), color = c.text, style = DexType.monoValue, textAlign = TextAlign.End, modifier = Modifier.width(30.dp))
        if (wide) {
            Text(formatMoveValue(m.move.accuracy), color = c.textMuted, style = DexType.monoValue, textAlign = TextAlign.End, modifier = Modifier.width(30.dp))
            Text(formatMoveValue(m.move.pp), color = c.textMuted, style = DexType.monoValue, textAlign = TextAlign.End, modifier = Modifier.width(24.dp))
        }
    }
}
