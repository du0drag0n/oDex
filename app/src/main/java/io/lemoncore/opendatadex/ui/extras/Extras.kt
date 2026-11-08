package io.lemoncore.opendatadex.ui.extras

import androidx.compose.ui.res.stringResource
import io.lemoncore.opendatadex.R
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.lemoncore.opendatadex.AppContainer
import io.lemoncore.opendatadex.data.Dex
import io.lemoncore.opendatadex.data.EvolutionNode
import io.lemoncore.opendatadex.data.EvolutionKind
import io.lemoncore.opendatadex.data.HuntMethod
import io.lemoncore.opendatadex.data.shinyOdds
import io.lemoncore.opendatadex.ui.components.PokemonArtwork
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.runtime.saveable.rememberSaveable
import io.lemoncore.opendatadex.data.Game
import io.lemoncore.opendatadex.data.Images
import io.lemoncore.opendatadex.ui.components.generationLabel
import io.lemoncore.opendatadex.ui.components.gameName
import io.lemoncore.opendatadex.ui.components.copyTitle
import io.lemoncore.opendatadex.ui.components.copyDetails
import io.lemoncore.opendatadex.ui.games.GameCopyDialog
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Icon
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import io.lemoncore.opendatadex.data.user.GameCopyEntity
import androidx.compose.ui.res.pluralStringResource
import io.lemoncore.opendatadex.ui.components.EmptyNote
import io.lemoncore.opendatadex.ui.components.Loading
import io.lemoncore.opendatadex.ui.components.LocalWidthClass
import io.lemoncore.opendatadex.ui.components.PokemonCard
import io.lemoncore.opendatadex.ui.components.cardRows
import io.lemoncore.opendatadex.ui.detail.EvolutionChain
import io.lemoncore.opendatadex.ui.dexes.DexScaffold
import io.lemoncore.opendatadex.ui.dexes.GameChip
import io.lemoncore.opendatadex.ui.theme.LocalDexColors
import io.lemoncore.opendatadex.ui.theme.DexType
import io.lemoncore.opendatadex.ui.theme.wash
import io.lemoncore.opendatadex.ui.theme.glow
import androidx.compose.ui.geometry.Size
import io.lemoncore.opendatadex.ui.components.Bar
import io.lemoncore.opendatadex.ui.components.Overline
import io.lemoncore.opendatadex.ui.pokedex.GameCodeSwatch
import io.lemoncore.opendatadex.ui.pokedex.gameWash
import androidx.compose.foundation.border
import androidx.compose.material3.IconButton
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private fun pad(p: PaddingValues) = PaddingValues(start = 12.dp, end = 12.dp, top = p.calculateTopPadding() + 8.dp, bottom = 24.dp)

/** A progress line: label, "n / total" and a bar. */
@Composable
private fun Progress(label: String, done: Int, total: Int, color: Color, modifier: Modifier = Modifier) {
    val fraction = if (total == 0) 0f else done.toFloat() / total
    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontSize = 16.sp, modifier = Modifier.weight(1f))
            Text("$done / $total  ·  ${"%.1f".format(fraction * 100)}%", color = LocalDexColors.current.subtleText, fontSize = 14.sp)
        }
        LinearProgressIndicator(
            progress = { fraction }, color = color, trackColor = color.copy(alpha = 0.2f),
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp).height(8.dp).clip(RoundedCornerShape(50)),
        )
    }
}

// ---- Shiny Dex ----------------------------------------------------------------------------------------------------------------

/** dataDex 4's Shiny Dex: the game's dex in shiny colours, with a shiny-caught checklist kept apart from the normal one. */
@Composable
fun ShinyDexScreen(
    container: AppContainer, dex: Dex, game: Game, onGame: (Game) -> Unit,
    onOpenDrawer: () -> Unit, onOpenPokemon: (Int, Int) -> Unit, onOpenHunt: (Long) -> Unit = {},
) {
    val shinyFlow = remember(game) { container.shiny(game) }
    val shiny by shinyFlow.collectAsState(initial = emptySet())
    val all by produceState(emptyList(), game) { value = withContext(Dispatchers.Default) { dex.pokemonFor(game) } }
    val hunts by container.activeHunts.collectAsState(initial = emptyList())
    DexScaffold(stringResource(R.string.nav_shiny_dex), onOpenDrawer, isRoot = true, chips = { GameChip(game, onGame, Modifier.weight(1f)) }) { query, padding ->
        val shown = all.filter { query.isBlank() || it.name.contains(query.trim(), true) }
        // Hunts in this game, so a Pokémon opened from here shows its counter (the Pokémon screen follows the same game).
        val gameHunts = hunts.filter { it.game == game.key }
        LazyColumn(contentPadding = pad(padding), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                if (game.dataGen == 1) EmptyNote(stringResource(R.string.shiny_gen2))
                else Surface(shape = RoundedCornerShape(14.dp), color = LocalDexColors.current.sectionCard, shadowElevation = 1.dp) {
                    Progress(if (game.isNational) stringResource(R.string.shinies_caught_any) else stringResource(R.string.shinies_caught_in, gameName(game)), shiny.count { id -> all.any { it.id == id } }, all.size,
                        Color(0xFFE6A100), Modifier.padding(14.dp))
                }
            }
            if (gameHunts.isNotEmpty()) item(key = "hunts") {
                Surface(shape = RoundedCornerShape(14.dp), color = LocalDexColors.current.sectionCard, shadowElevation = 1.dp) {
                    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                        Text(stringResource(R.string.your_hunts), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 14.dp))
                        val numbers = NumberFormat.getIntegerInstance(LocalConfiguration.current.locales[0])
                        gameHunts.forEach { h ->
                            val method = HuntMethod.fromName(h.method)
                            Row(Modifier.fillMaxWidth().clickable { onOpenHunt(h.id) }.padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                PokemonArtwork(-h.formId, Images.shinyArtwork(h.formId), Modifier.size(44.dp), null)
                                Column(Modifier.padding(start = 12.dp)) {
                                    Text(dex.speciesOf(h.speciesId)?.name.orEmpty(), fontSize = 17.sp)
                                    Text(stringResource(R.string.hunt_row, numbers.format(h.count), stringResource(method.unit),
                                        numbers.format(shinyOdds(game, method, h.shinyCharm).oneIn)), color = LocalDexColors.current.subtleText, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
            items(shown, key = { it.id }) { p ->
                PokemonCard(
                    p, p.types.map { dex.type(it)?.name ?: "" }, Images.shinyArtwork(p.formId), false, p.id in shiny,
                    onFavorite = null, onCaught = { container.setShiny(game, p.id, p.id !in shiny) },
                    onClick = { onOpenPokemon(p.id, p.formId) }, colorKey = -p.formId, shinyToggle = true,
                )
            }
        }
    }
}

// ---- Evolution Dex ----------------------------------------------------------------------------------------------------------------

@Composable
fun EvolutionDexScreen(dex: Dex, game: Game, onGame: (Game) -> Unit, artwork: Map<Int, String>, onOpenDrawer: () -> Unit, onOpenPokemon: (Int) -> Unit) {
    val families by produceState<List<EvolutionNode>?>(null, game) { value = withContext(Dispatchers.Default) { dex.evolutionFamilies(game) } }
    // dataDex 4's filter: families with an evolution by level, trade, item or something else.
    var kind by rememberSaveable { mutableStateOf<EvolutionKind?>(null) }
    DexScaffold(stringResource(R.string.nav_evolution_dex), onOpenDrawer, isRoot = true, chips = { GameChip(game, onGame, Modifier.weight(1f)) }) { query, padding ->
        val list = families ?: return@DexScaffold Loading()
        fun EvolutionNode.names(): List<String> = listOf(pokemon.name) + next.flatMap { it.names() }
        val shown = list.filter { f -> (kind == null || kind in f.kinds) && (query.isBlank() || f.names().any { it.contains(query.trim(), true) }) }
        LazyColumn(contentPadding = pad(padding), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Text(if (game.isNational) pluralStringResource(R.plurals.evolution_families, list.size, list.size) else pluralStringResource(R.plurals.evolution_families_in, list.size, list.size, gameName(game)),
                    color = LocalDexColors.current.subtleText, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth().padding(8.dp))
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(kind == null, { kind = null }, label = { Text(stringResource(R.string.evolve_all_methods)) })
                    EvolutionKind.entries.forEach { k ->
                        FilterChip(kind == k, { kind = if (kind == k) null else k }, label = { Text(stringResource(k.label)) })
                    }
                }
            }
            if (shown.isEmpty()) item { EmptyNote(stringResource(R.string.no_families_match)) }
            items(shown, key = { it.pokemon.id }) { family ->
                Surface(shape = RoundedCornerShape(14.dp), color = LocalDexColors.current.sectionCard, shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
                    Box(Modifier.padding(10.dp)) { EvolutionChain(family, artwork, currentId = -1, onOpen = onOpenPokemon) }
                }
            }
        }
    }
}

// ---- My Progress ------------------------------------------------------------------------------------------------------------------

/**
 * Progress in the trainer's own games (My Games): one card per copy, each opening its checklist grid, plus species
 * caught or shiny anywhere. Games are added from here as well as from My Games.
 */
@Composable
fun GlobalChecklistScreen(
    container: AppContainer, dex: Dex, currentGame: Game, onOpenDrawer: () -> Unit, onOpenMap: () -> Unit, onOpenCopy: (GameCopyEntity) -> Unit,
) {
    val caught by container.allCaught.collectAsState(initial = null)
    val copies by container.gameCopies.collectAsState(initial = null)
    val shiny by container.allShiny.collectAsState(initial = emptyList())
    var adding by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val activeFlow = remember(currentGame) { container.activeChecklist(currentGame) }
    val activeCopy = activeFlow.collectAsState(initial = null).value?.copy?.id
    val totals = remember(dex) { HashMap<Pair<Game, Boolean>, Set<Int>>() }
    fun idsOf(g: Game, national: Boolean) = totals.getOrPut(g to national) { dex.pokemonFor(g, national).mapTo(HashSet()) { it.id } }
    val national = remember(dex) { dex.pokemonFor(Game.NATIONAL).size }
    val dexColors = LocalDexColors.current
    DexScaffold(
        stringResource(R.string.nav_my_progress), onOpenDrawer, isRoot = true, filterable = false,
        actions = {
            IconButton(onClick = onOpenMap) { Icon(Icons.Filled.Map, stringResource(R.string.nav_map)) }
            IconButton(onClick = { adding = true }) { Icon(Icons.Filled.Add, stringResource(R.string.add_game_copy)) }
        },
    ) { _, padding ->
        val c = caught ?: return@DexScaffold Loading()
        val list = copies ?: return@DexScaffold Loading()
        val byKey = c.groupBy({ it.game }, { it.speciesId })
        val perRow = LocalWidthClass.current.cardsPerRow
        LazyColumn(
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = padding.calculateTopPadding() + 8.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile(stringResource(R.string.caught_any_game), c.mapTo(HashSet()) { it.speciesId }.size, national, dexColors.text, null, Modifier.weight(1f))
                    StatTile(stringResource(R.string.shiny_any_game), shiny.mapTo(HashSet()) { it.speciesId }.size, national, dexColors.gold, dexColors.gold, Modifier.weight(1f))
                }
            }
            item { Overline(stringResource(R.string.my_games_overline), Modifier.padding(start = 4.dp, top = 10.dp)) }
            if (list.isEmpty()) item { EmptyNote(stringResource(R.string.checklist_intro)) }
            cardRows(list, perRow, { it.id }) { copy, modifier ->
                val g = Game.fromKey(copy.game)
                val ids = idsOf(g, copy.nationalDex)
                CopyProgressCard(copy, g, byKey[copy.key].orEmpty().count { it in ids }, ids.size, active = copy.id == activeCopy, modifier) { onOpenCopy(copy) }
            }
        }
    }
    if (adding) GameCopyDialog(null, if (currentGame.isNational) Game.EMERALD else currentGame, onDismiss = { adding = false }) { copy ->
        adding = false
        scope.launch { container.createGameCopy(copy) }
    }
}

/** A big number with a caption above and "of 1025 · 60%" below; [glow] lights the corner (Shinies). */
@Composable
private fun StatTile(caption: String, count: Int, of: Int, color: Color, glow: Color?, modifier: Modifier) {
    val c = LocalDexColors.current
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier.clip(shape).background(c.surface)
            .then(if (glow != null) Modifier.glow(glow.copy(alpha = 0.18f), end = 1f) { Offset(it.width, 0f) to Size(it.width, it.width) } else Modifier)
            .border(1.dp, c.line, shape).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(caption, color = c.textSubtle, style = DexType.caption.copy(fontSize = 11.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(count.toString(), color = color, style = DexType.screenTitle.copy(fontSize = 26.sp))
        Text(stringResource(R.string.of_total_pct, of, if (of == 0) 0 else (count * 100f / of).roundToInt()), color = c.textSubtle, style = DexType.monoMeta.copy(fontWeight = FontWeight.Medium))
    }
}

/** A game copy's completion: its game's wash, code, name and save line, the percentage and a bar. */
@Composable
private fun CopyProgressCard(copy: GameCopyEntity, game: Game, caught: Int, total: Int, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val c = LocalDexColors.current
    val w = gameWash(game)
    val fraction = if (total == 0) 0f else caught.toFloat() / total
    val done = total > 0 && caught == total
    Column(
        modifier.wash(w, RoundedCornerShape(16.dp), border = if (active) c.accent else w.border).clickable(onClick = onClick).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GameCodeSwatch(game)
            Column(Modifier.weight(1f)) {
                Text(copyTitle(copy), color = c.text, style = DexType.itemTitle.copy(fontSize = 15.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                val details = copyDetails(copy)
                if (details.isNotEmpty()) Text(details, color = c.textMuted, style = DexType.caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text("${(fraction * 100).roundToInt()}%", color = if (done) c.gold else c.text, style = DexType.screenTitle.copy(fontSize = 18.sp, letterSpacing = 0.sp))
        }
        Bar(fraction, if (done) c.gold else c.accent, Modifier.fillMaxWidth(), track = if (c.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFF14141E).copy(alpha = 0.08f))
        Text("$caught/$total", color = c.textSubtle, style = DexType.monoMeta.copy(fontWeight = FontWeight.Medium))
    }
}
