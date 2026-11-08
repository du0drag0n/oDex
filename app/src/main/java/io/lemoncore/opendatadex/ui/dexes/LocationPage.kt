package io.lemoncore.opendatadex.ui.dexes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Phishing
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.lemoncore.opendatadex.R
import io.lemoncore.opendatadex.data.Dex
import io.lemoncore.opendatadex.data.Encounter
import io.lemoncore.opendatadex.data.Game
import io.lemoncore.opendatadex.data.Images
import io.lemoncore.opendatadex.data.PokemonSummary
import io.lemoncore.opendatadex.ui.components.EmptyNote
import io.lemoncore.opendatadex.ui.components.PokemonArtwork
import io.lemoncore.opendatadex.ui.components.animatedWash
import io.lemoncore.opendatadex.ui.components.gameName
import io.lemoncore.opendatadex.ui.components.levelRange
import io.lemoncore.opendatadex.ui.theme.DexType
import io.lemoncore.opendatadex.ui.theme.wash
import io.lemoncore.opendatadex.ui.theme.halo
import io.lemoncore.opendatadex.ui.theme.LocalDexColors
import kotlinx.coroutines.launch
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.first

/** One Pokémon found one way at the place, merged across the versions shown. */
private data class Spot(val pokemon: PokemonSummary, val minLevel: Int, val maxLevel: Int, val rarity: Int, val games: List<Game>)

private sealed interface Line {
    data class Method(val name: String, val methodId: Int) : Line
    data class Conditions(val text: String) : Line
    /** The heading over Pokémon found here only in other games. */
    data object OtherGames : Line
    data class Row(val spot: Spot, val otherGame: Boolean = false) : Line
}

/**
 * A location in dataDex 4's style: the place and region, a version badge per game of the selected game's version group
 * (tap one to add or remove that version), then the Pokémon grouped by how they are found. Rows only carry a game name
 * when it isn't the game being played (the paired version's exclusives), or every game in the national view.
 * [highlight] is the species the trainer came from: its rows are outlined and a bar at the bottom steps between them.
 */
@Composable
fun LocationDetailScreen(
    dex: Dex, areaId: Int, game: Game, artwork: Map<Int, String>, highlight: Int, onBack: () -> Unit, onOpenPokemon: (Int, Int) -> Unit,
) {
    val area = dex.area(areaId) ?: return
    val versions = if (game.isNational) emptyList() else Game.playable.filter { it.versionGroup == game.versionGroup }
    var shownKeys by rememberSaveable { mutableStateOf(listOf(game.key)) }
    val shown = versions.filter { it.key in shownKeys }
    val lines = remember(areaId, game, shownKeys) { locationLines(dex, areaId, game, shown) }
    val speciesCount = lines.filterIsInstance<Line.Row>().filter { !it.otherGame }.map { it.spot.pokemon.id }.distinct().size
    var focus by rememberSaveable { mutableIntStateOf(highlight) }
    val focusRows = lines.indices.filter { (lines[it] as? Line.Row)?.spot?.pokemon?.id == focus }
    var focusAt by rememberSaveable { mutableIntStateOf(0) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    // Lines start after the two header items.
    fun scrollTo(i: Int) = scope.launch { focusRows.getOrNull(i)?.let { listState.animateScrollToItem(it + 2, -200) } }
    // Bring the first highlighted row into view, unless it is already on screen (then the title stays in sight).
    LaunchedEffect(lines) {
        if (focusRows.isEmpty()) return@LaunchedEffect
        focusAt = 0
        val target = focusRows.first() + 2
        val layout = snapshotFlow { listState.layoutInfo }.first { it.totalItemsCount > 0 }
        val fullyShown = layout.visibleItemsInfo.any { it.index == target && it.offset + it.size <= layout.viewportEndOffset - 220 }
        if (!fullyShown) listState.animateScrollToItem(target, -200)
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(state = listState, contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 120.dp)) {
            item(key = "top") {
                Row(Modifier.statusBarsPadding().fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) }
                    Spacer(Modifier.weight(1f))
                    versions.forEach { v ->
                        VersionBadge(v, v.key in shownKeys) {
                            shownKeys = if (v.key in shownKeys) (shownKeys - v.key).ifEmpty { shownKeys } else shownKeys + v.key
                        }
                    }
                }
            }
            item(key = "title") {
                Column(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(dex.placeName(area), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onBackground)
                    Text(
                        listOfNotNull(dex.regionName(area.region), stringResource(R.string.location_pokemon_count, speciesCount)).joinToString(" · "),
                        color = LocalDexColors.current.subtleText, fontSize = 18.sp, modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            if (lines.isEmpty()) item(key = "none") { EmptyNote(stringResource(R.string.no_wild_here, gameName(game))) }
            itemsIndexed(lines, key = { i, _ -> i }) { _, line ->
                when (line) {
                    is Line.Method -> MethodHeader(line)
                    is Line.Conditions -> Text(line.text, color = LocalDexColors.current.subtleText, fontSize = 14.sp, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp))
                    Line.OtherGames -> Column(Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.SwapHoriz, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                            Text("  " + stringResource(R.string.in_other_games), color = MaterialTheme.colorScheme.primary, fontSize = 18.sp)
                        }
                        Text(stringResource(R.string.in_other_games_note, gameName(game)), color = LocalDexColors.current.subtleText, fontSize = 14.sp,
                            textAlign = TextAlign.Center, modifier = Modifier.padding(top = 2.dp))
                    }
                    is Line.Row -> SpotRow(line.spot, game, artwork, focused = line.spot.pokemon.id == focus) {
                        onOpenPokemon(line.spot.pokemon.id, line.spot.pokemon.formId)
                    }
                }
            }
        }
        if (focusRows.isNotEmpty()) {
            val p = (lines[focusRows.first()] as Line.Row).spot.pokemon
            FocusBar(p, artwork, focusAt, focusRows.size,
                onPrev = { focusAt = (focusAt - 1 + focusRows.size) % focusRows.size; scrollTo(focusAt) },
                onNext = { focusAt = (focusAt + 1) % focusRows.size; scrollTo(focusAt) },
                onClose = { focus = 0 },
                modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}

/** Groups the place's encounters by method (then time or other conditions) and merges each Pokémon across versions. */
private fun locationLines(dex: Dex, areaId: Int, game: Game, shown: List<Game>): List<Line> {
    val found = dex.encountersAt(areaId, Game.NATIONAL).filter { game.isNational || it.game in shown }
    val out = ArrayList<Line>()
    // Walking first, then the rods, surfing and the rest, as the game lists them.
    found.groupBy { it.method to it.methodId }.toSortedMap(compareBy({ it.second }, { it.first })).forEach { (method, byMethod) ->
        out += Line.Method(method.first, method.second)
        val byConditions = byMethod.groupBy { it.conditions.joinToString(" / ") }
        byConditions.forEach { (conditions, list) ->
            if (conditions.isNotEmpty()) out += Line.Conditions(conditions)
            list.groupBy { it.pokemonId }.mapNotNull { (pokemonId, es) -> spot(dex, pokemonId, es, game) }
                .sortedByDescending { it.rarity }
                .forEach { out += Line.Row(it) }
        }
    }
    // Pokémon found at this place in other games (Emerald or Omega Ruby for a Ruby player), but not in the versions
    // shown: one row each, naming the games to get it from.
    if (!game.isNational) {
        val here = found.mapTo(HashSet()) { it.speciesId }
        val elsewhere = dex.encountersAt(areaId, Game.NATIONAL).filter { it.game.versionGroup != game.versionGroup && it.speciesId !in here }
        val spots = elsewhere.groupBy { it.pokemonId }.mapNotNull { (pokemonId, es) -> spot(dex, pokemonId, es, game)?.copy(rarity = 0) }
            .sortedBy { it.pokemon.id }
        if (spots.isNotEmpty()) {
            out += Line.OtherGames
            spots.forEach { out += Line.Row(it, otherGame = true) }
        }
    }
    return out
}

private fun spot(dex: Dex, pokemonId: Int, es: List<Encounter>, game: Game): Spot? {
    val summary = dex.summaryOfForm(pokemonId, if (game.isNational) es.first().game else game) ?: return null
    // The rate in the game being played when it has one, else the other version's.
    val rate = (es.filter { it.game == game }.ifEmpty { es }).maxOf { it.rarity }
    return Spot(summary, es.minOf { it.minLevel }, es.maxOf { it.maxLevel }, rate, es.map { it.game }.distinct().sortedBy { it.ordinal })
}

private fun methodIcon(methodId: Int): ImageVector = when (methodId) {
    1 -> Icons.AutoMirrored.Filled.DirectionsWalk
    2, 3, 4 -> Icons.Filled.Phishing
    5 -> Icons.Filled.Waves
    6 -> Icons.Filled.Landscape
    7 -> Icons.Filled.Park
    18, 19, 20, 36 -> Icons.Filled.CardGiftcard
    else -> Icons.Filled.Place
}

@Composable
private fun MethodHeader(m: Line.Method) {
    Row(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 6.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Icon(methodIcon(m.methodId), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Text("  " + m.name, color = MaterialTheme.colorScheme.primary, fontSize = 18.sp, textAlign = TextAlign.Center)
    }
}

/** "R", "S", "HG", "OR", "LGP": a game's initials for its badge. */
private fun initials(name: String): String {
    val words = name.split(' ', '-', ':', ',').map { it.trim('!', '\'') }.filter { it.isNotEmpty() }
    if (words.size > 1) return words.joinToString("") { it.take(1) }.take(3).uppercase()
    val caps = name.filter { it.isUpperCase() }
    return if (caps.length >= 2) caps.take(3) else name.take(1).uppercase()
}

@Composable
private fun VersionBadge(game: Game, on: Boolean, onClick: () -> Unit) {
    val color = Color(game.color)
    Box(
        Modifier.padding(end = 6.dp).size(44.dp).clip(RoundedCornerShape(10.dp)).background(if (on) color else Color.Transparent)
            .border(2.dp, color, RoundedCornerShape(10.dp)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(initials(gameName(game)), color = if (on) Color.White else color, fontSize = 15.sp, maxLines = 1)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SpotRow(s: Spot, game: Game, artwork: Map<Int, String>, focused: Boolean, onClick: () -> Unit) {
    val p = s.pokemon
    val c = LocalDexColors.current
    val dark = c.isDark
    val tones = animatedWash(p.formId, p.colorId)
    val shape = RoundedCornerShape(16.dp)
    // A row found in the game being played needs no label; one only in the other version names it (the national view names every game).
    val labels = when {
        game.isNational -> s.games
        game in s.games -> emptyList()
        else -> s.games
    }
    Box(
        Modifier.fillMaxWidth().padding(vertical = 4.dp).wash(tones, shape, border = if (focused) c.accent else tones.border)
            .clickable(onClick = onClick),
    ) {
        Row(Modifier.padding(start = 16.dp, end = 10.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(p.name, color = c.text, style = DexType.itemTitle.copy(fontSize = 16.sp),
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Text(levelRange(s.minLevel, s.maxLevel), color = c.textSecondary, style = DexType.monoValue.copy(fontSize = 12.sp), modifier = Modifier.padding(horizontal = 8.dp))
                    if (s.rarity > 0) RatePill(s.rarity, dark)
                }
                if (labels.isNotEmpty()) FlowRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp), maxItemsInEachRow = 3) {
                    labels.forEach { g ->
                        Box(Modifier.weight(1f).clip(RoundedCornerShape(50)).background(LocalDexColors.current.sectionCard).padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center) {
                            Text(gameName(g), color = Color(g.color), fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            Box(Modifier.padding(start = 10.dp).size(68.dp).halo(tones), contentAlignment = Alignment.Center) {
                PokemonArtwork(p.formId, Images.artwork(p.formId, artwork), Modifier.size(60.dp), p.name)
            }
        }
    }
}

/** The encounter rate, coloured from common (teal) to rare (orange). */
@Composable
private fun RatePill(rate: Int, dark: Boolean) {
    val color = when {
        rate >= 25 -> if (dark) Color(0xFF4DB6AC) else Color(0xFF00796B)
        rate >= 15 -> if (dark) Color(0xFFAED581) else Color(0xFF558B2F)
        rate >= 10 -> if (dark) Color(0xFFFFD54F) else Color(0xFFB28704)
        else -> if (dark) Color(0xFFFFB74D) else Color(0xFFE65100)
    }
    Box(Modifier.clip(RoundedCornerShape(50)).background(LocalDexColors.current.sectionCard).padding(horizontal = 12.dp, vertical = 5.dp)) {
        Text("$rate%", color = color, fontSize = 15.sp)
    }
}

/** dataDex 4's finder: the Pokémon the trainer came from, which of its rows is in view, and arrows between them. */
@Composable
private fun FocusBar(
    p: PokemonSummary, artwork: Map<Int, String>, at: Int, count: Int,
    onPrev: () -> Unit, onNext: () -> Unit, onClose: () -> Unit, modifier: Modifier,
) {
    Surface(
        shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceVariant, shadowElevation = 6.dp,
        modifier = modifier.navigationBarsPadding().padding(bottom = 16.dp),
    ) {
        Row(Modifier.padding(horizontal = 6.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrev) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.previous)) }
            Box(Modifier.size(40.dp).clip(CircleShape).background(LocalDexColors.current.chip), contentAlignment = Alignment.Center) {
                PokemonArtwork(p.formId, Images.artwork(p.formId, artwork), Modifier.size(36.dp), p.name)
            }
            Text("  ${p.name} · ${at + 1} / $count", fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            IconButton(onClick = onNext) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.next)) }
            Box(Modifier.width(1.dp).height(32.dp).background(LocalDexColors.current.divider))
            IconButton(onClick = onClose) { Icon(Icons.Filled.Close, stringResource(R.string.close)) }
        }
    }
}
