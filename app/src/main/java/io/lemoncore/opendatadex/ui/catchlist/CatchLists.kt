package io.lemoncore.opendatadex.ui.catchlist

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.lemoncore.opendatadex.AppContainer
import io.lemoncore.opendatadex.data.Dex
import io.lemoncore.opendatadex.data.Game
import io.lemoncore.opendatadex.data.Images
import io.lemoncore.opendatadex.data.Obtain
import io.lemoncore.opendatadex.data.ObtainKind
import io.lemoncore.opendatadex.data.PokemonSummary
import io.lemoncore.opendatadex.data.catchPlan
import io.lemoncore.opendatadex.data.user.CatchListEntity
import io.lemoncore.opendatadex.ui.components.EmptyNote
import io.lemoncore.opendatadex.ui.components.gameName
import io.lemoncore.opendatadex.ui.components.obtainText
import io.lemoncore.opendatadex.ui.components.HeaderChip
import io.lemoncore.opendatadex.ui.components.Loading
import io.lemoncore.opendatadex.ui.components.LocalWidthClass
import io.lemoncore.opendatadex.ui.components.PokemonCard
import io.lemoncore.opendatadex.ui.components.cardRows
import io.lemoncore.opendatadex.ui.components.SegmentedButtons
import io.lemoncore.opendatadex.ui.dexes.DexScaffold
import io.lemoncore.opendatadex.ui.pokedex.GamePickerSheet
import io.lemoncore.opendatadex.ui.theme.LocalDexColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val LIVING = "living"
private const val COMPLETE = "complete"

@Composable
private fun goalLabel(goal: String) = stringResource(if (goal == LIVING) R.string.goal_living else R.string.goal_complete)

@Composable
private fun ListProgress(done: Int, total: Int, color: Color) {
    val fraction = if (total == 0) 0f else done.toFloat() / total
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
        LinearProgressIndicator(
            progress = { fraction }, color = color, trackColor = color.copy(alpha = 0.2f),
            modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(50)),
        )
        Text("  $done / $total", color = LocalDexColors.current.subtleText, fontSize = 14.sp)
    }
}

// ---- all lists --------------------------------------------------------------------------------------------------------------

@Composable
fun CatchListsScreen(container: AppContainer, dex: Dex, currentGame: Game, onOpenDrawer: () -> Unit, onOpenList: (Long) -> Unit) {
    val lists by container.catchLists.collectAsState(initial = null)
    val caught by container.allCaught.collectAsState(initial = emptyList())
    var creating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    DexScaffold(stringResource(R.string.nav_catch_lists), onOpenDrawer, isRoot = true, filterable = false) { _, padding ->
        val all = lists ?: return@DexScaffold Loading()
        val byKey = caught.groupBy({ it.game }, { it.speciesId })
        val perRow = LocalWidthClass.current.cardsPerRow
        LazyColumn(
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = padding.calculateTopPadding() + 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Row(
                    Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.primary).clickable { creating = true },
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.Filled.Add, null, tint = MaterialTheme.colorScheme.onPrimary)
                    Text("  " + stringResource(R.string.new_catch_list), color = MaterialTheme.colorScheme.onPrimary, fontSize = 17.sp)
                }
            }
            if (all.isEmpty()) item {
                EmptyNote(stringResource(R.string.catch_lists_intro))
            }
            cardRows(all, perRow, { it.id }) { list, modifier ->
                val game = Game.fromKey(list.game)
                val total = remember(game, list.nationalDex) { dex.pokemonFor(game, list.nationalDex).map { it.id }.toSet() }
                val active by container.settings.activeList(game).collectAsState(initial = null)
                Surface(shape = RoundedCornerShape(14.dp), color = LocalDexColors.current.sectionCard, shadowElevation = 1.dp,
                    modifier = modifier.clickable { onOpenList(list.id) }) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(list.name, color = MaterialTheme.colorScheme.primary, fontSize = 19.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(goalLabel(list.goal) + if (active == list.key) " · " + stringResource(R.string.used_by_pokedex) else "",
                                    color = LocalDexColors.current.subtleText, fontSize = 13.sp)
                            }
                            Box(Modifier.clip(RoundedCornerShape(50)).background(Color(game.color)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                                Text(gameName(game), color = Color.White, fontSize = 13.sp)
                            }
                        }
                        ListProgress(byKey[list.key].orEmpty().count { it in total }, total.size, Color(game.color))
                    }
                }
            }
        }
    }
    if (creating) NewCatchListDialog(if (currentGame.isNational) Game.RED else currentGame, onDismiss = { creating = false }) { name, game, goal ->
        creating = false
        scope.launch { onOpenList(container.createCatchList(name, game, goal)) }
    }
}

@Composable
private fun NewCatchListDialog(initialGame: Game, onDismiss: () -> Unit, onCreate: (String, Game, String) -> Unit) {
    var game by remember { mutableStateOf(initialGame) }
    var goal by remember { mutableStateOf(LIVING) }
    var name by remember { mutableStateOf("") }
    var picking by remember { mutableStateOf(false) }
    val suggested = stringResource(if (goal == LIVING) R.string.suggested_living_name else R.string.suggested_dex_name, gameName(game))
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.new_catch_list)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                io.lemoncore.opendatadex.ui.components.GameChip(game, gameName(game), { picking = true }, Modifier.fillMaxWidth())
                listOf(LIVING to stringResource(R.string.goal_living_long), COMPLETE to stringResource(R.string.goal_complete_long)).forEach { (value, label) ->
                    Row(Modifier.fillMaxWidth().clickable { goal = value }, verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = goal == value, onClick = { goal = value })
                        Text(label, fontSize = 15.sp)
                    }
                }
                OutlinedTextField(value = name, onValueChange = { name = it.take(40) }, singleLine = true,
                    label = { Text(stringResource(R.string.name)) }, placeholder = { Text(suggested) })
            }
        },
        confirmButton = { TextButton(onClick = { onCreate(name.trim().ifEmpty { suggested }, game, goal) }) { Text(stringResource(R.string.create)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
    if (picking) GamePickerSheet(game, onPick = { game = it; picking = false }, onDismiss = { picking = false }, includeNational = false)
}

// ---- one list ---------------------------------------------------------------------------------------------------------------

private data class PlanRow(val pokemon: PokemonSummary, val obtain: Obtain)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatchListScreen(
    container: AppContainer, dex: Dex, listId: Long, artwork: Map<Int, String>,
    onBack: () -> Unit, onOpenPokemon: (Int) -> Unit,
) {
    val listState by container.catchList(listId).collectAsState(initial = null)
    val list = listState ?: return Loading()
    val game = Game.fromKey(list.game)
    val caughtFlow = remember(list.key) { container.caughtIn(list.key) }
    val caught by caughtFlow.collectAsState(initial = emptySet())
    val active by container.settings.activeList(game).collectAsState(initial = null)
    val rows by produceState<List<PlanRow>?>(null, game, list.goal, list.nationalDex) {
        value = withContext(Dispatchers.Default) {
            val plan = dex.catchPlan(game, list.nationalDex)
            dex.pokemonFor(game, list.nationalDex).mapNotNull { p -> plan[p.id]?.let { PlanRow(p, it) } }
        }
    }
    var view by rememberSaveable { mutableIntStateOf(0) }
    var neededOnly by rememberSaveable { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val kindTitles = ObtainKind.entries.associateWith { stringResource(it.title) }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(list.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } },
            actions = {
                IconButton(onClick = { renaming = true }) { Icon(Icons.Filled.Edit, stringResource(R.string.rename_list)) }
                IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, stringResource(R.string.delete_list)) }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
        )
    }) { padding ->
        val all = rows ?: return@Scaffold Loading()
        val shown = all.filter { !neededOnly || it.pokemon.id !in caught }
        LazyColumn(
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = padding.calculateTopPadding() + 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "header") {
                Surface(shape = RoundedCornerShape(14.dp), color = LocalDexColors.current.sectionCard, shadowElevation = 1.dp) {
                    Column(Modifier.padding(14.dp)) {
                        Text(gameName(game) + " · " + goalLabel(list.goal), color = Color(game.color), fontSize = 16.sp)
                        ListProgress(all.count { it.pokemon.id in caught }, all.size, Color(game.color))
                        Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(R.string.use_in_pokedex), fontSize = 16.sp)
                                Text(
                                    if (active == list.key) stringResource(R.string.use_in_pokedex_on, gameName(game))
                                    else stringResource(R.string.use_in_pokedex_off, gameName(game)),
                                    color = LocalDexColors.current.subtleText, fontSize = 13.sp,
                                )
                            }
                            Switch(checked = active == list.key, onCheckedChange = { on -> container.useCatchList(game, if (on) list else null) })
                        }
                        if (game.hasNationalDex) Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(R.string.national_pokedex), fontSize = 16.sp)
                                Text(stringResource(R.string.national_list_body, game.nationalDexSize), color = LocalDexColors.current.subtleText, fontSize = 13.sp)
                            }
                            Switch(checked = list.nationalDex, onCheckedChange = { on -> container.updateCatchList(list.copy(nationalDex = on)) })
                        }
                        if (list.goal == LIVING) Text(
                            stringResource(R.string.living_dex_note),
                            color = LocalDexColors.current.subtleText, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }
            item(key = "controls") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SegmentedButtons(listOf(stringResource(R.string.view_plan), stringResource(R.string.view_by_number)), view, MaterialTheme.colorScheme.primary, { view = it })
                    HeaderChip(stringResource(if (neededOnly) R.string.still_needed else R.string.showing_all), neededOnly, { neededOnly = !neededOnly }, Modifier.fillMaxWidth())
                }
            }
            if (shown.isEmpty()) item(key = "done") {
                EmptyNote(stringResource(if (neededOnly) R.string.nothing_left else R.string.nothing_to_plan))
            }
            val sections: List<Pair<String, List<PlanRow>>> = if (view == 1) listOf("" to shown) else planSections(shown, kindTitles)
            sections.forEachIndexed { i, (title, sectionRows) ->
                if (title.isNotEmpty()) item(key = "s$i") {
                    Text(title, color = LocalDexColors.current.subtleText, style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                }
                items(sectionRows, key = { "$i-${it.pokemon.id}" }) { r ->
                    val p = r.pokemon
                    PokemonCard(
                        p, p.types.map { dex.type(it)?.name ?: "" }, Images.artwork(p.formId, artwork), false, p.id in caught,
                        onFavorite = null, onCaught = { scope.launch { container.setCaughtIn(list.key, p.id, p.id !in caught) } },
                        onClick = { onOpenPokemon(p.id) }, subtitle = obtainText(r.obtain, game, list.goal == LIVING),
                    )
                }
            }
        }
    }

    if (renaming) {
        var name by rememberSaveable { mutableStateOf(list.name) }
        AlertDialog(
            onDismissRequest = { renaming = false },
            title = { Text(stringResource(R.string.rename_list)) },
            text = { OutlinedTextField(value = name, onValueChange = { name = it.take(40) }, singleLine = true) },
            confirmButton = { TextButton(onClick = { if (name.isNotBlank()) container.updateCatchList(list.copy(name = name.trim())); renaming = false }) { Text(stringResource(R.string.save)) } },
            dismissButton = { TextButton(onClick = { renaming = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_named, list.name)) },
            text = { Text(stringResource(R.string.delete_list_body, gameName(game))) },
            confirmButton = { TextButton(onClick = { confirmDelete = false; container.deleteCatchList(list); onBack() }) { Text(stringResource(R.string.delete)) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

/**
 * The plan view: wild and gift Pokémon grouped by where to find them first, areas in rough game order (lowest
 * levels first), then everything you evolve, breed or trade for.
 */
private fun planSections(rows: List<PlanRow>, kindTitles: Map<ObtainKind, String>): List<Pair<String, List<PlanRow>>> {
    // Event islands (Birth Island, Navel Rock…) go in the Event section rather than among the places you can reach.
    val placed = rows.filter { it.obtain.place != null && it.obtain.kind != ObtainKind.EVENT }
        .groupBy { it.obtain.place!! }
        .toList()
        .sortedBy { (_, list) -> list.minOf { it.obtain.level } }
    val rest = listOf(ObtainKind.EVOLVE, ObtainKind.BREED, ObtainKind.OTHER_VERSION, ObtainKind.OTHER_GAME, ObtainKind.EVENT, ObtainKind.UNKNOWN).map { kind ->
        kindTitles.getValue(kind) to rows.filter { (it.obtain.place == null || kind == ObtainKind.EVENT) && it.obtain.kind == kind }.sortedBy { it.obtain.level }
    }
    return (placed + rest).filter { it.second.isNotEmpty() }
}
