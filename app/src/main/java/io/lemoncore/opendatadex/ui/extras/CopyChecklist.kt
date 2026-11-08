package io.lemoncore.opendatadex.ui.extras

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.unit.Dp
import androidx.compose.runtime.produceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.lemoncore.opendatadex.data.eventOnly
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.lemoncore.opendatadex.AppContainer
import io.lemoncore.opendatadex.R
import io.lemoncore.opendatadex.data.Dex
import io.lemoncore.opendatadex.data.Game
import io.lemoncore.opendatadex.data.Images
import io.lemoncore.opendatadex.data.PokemonSummary
import io.lemoncore.opendatadex.ui.components.CaughtToggle
import io.lemoncore.opendatadex.ui.components.EmptyNote
import io.lemoncore.opendatadex.ui.components.Loading
import io.lemoncore.opendatadex.ui.components.LocalWidthClass
import io.lemoncore.opendatadex.ui.components.PokemonArtwork
import io.lemoncore.opendatadex.ui.components.ReadableWidth
import io.lemoncore.opendatadex.ui.components.SegmentedButtons
import io.lemoncore.opendatadex.ui.components.animatedWash
import io.lemoncore.opendatadex.ui.components.copyTitle
import io.lemoncore.opendatadex.ui.components.dexNumber
import io.lemoncore.opendatadex.ui.theme.DexType
import io.lemoncore.opendatadex.ui.theme.wash
import io.lemoncore.opendatadex.ui.theme.halo
import io.lemoncore.opendatadex.ui.theme.LocalDexColors
import java.text.DateFormat
import java.util.Date

private enum class Show(@StringRes val label: Int) { ALL(R.string.filter_all), CAUGHT(R.string.filter_caught), MISSING(R.string.filter_missing) }

/**
 * One game copy's checklist in dataDex 4's Shiny Dex style: a progress bar, then a grid of tiles where caught Pokémon
 * are in colour with their catch date and missing ones are faded. The tick marks or clears a catch; the tile opens the
 * Pokémon, and swiping there steps through the grid.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CopyChecklistScreen(
    container: AppContainer, dex: Dex, copyId: Long, artwork: Map<Int, String>, onBack: () -> Unit, onOpenPokemon: (Int) -> Unit,
    startOnBoxes: Boolean = false,
) {
    val copies by container.gameCopies.collectAsState(initial = null)
    val list = copies ?: return Loading()
    val copy = list.firstOrNull { it.id == copyId }
    // Deleted from My Games while open: nothing to show.
    LaunchedEffect(copy == null) { if (copy == null) onBack() }
    if (copy == null) return
    val game = Game.fromKey(copy.game)
    val all = remember(game, copy.nationalDex) { dex.pokemonFor(game, copy.nationalDex) }
    val datesFlow = remember(copy.key) { container.caughtDates(copy.key) }
    val dates by datesFlow.collectAsState(initial = emptyMap())
    var show by rememberSaveable { mutableStateOf(Show.ALL) }
    val shown = all.filter { p ->
        when (show) {
            Show.ALL -> true
            Show.CAUGHT -> p.id in dates
            Show.MISSING -> p.id !in dates
        }
    }
    val done = all.count { it.id in dates }
    val locale = LocalConfiguration.current.locales[0]
    val dateFormat = remember(locale) { DateFormat.getDateInstance(DateFormat.MEDIUM, locale) }
    val scope = rememberCoroutineScope()
    var tab by rememberSaveable { mutableStateOf(if (startOnBoxes) PlannerTab.BOXES else PlannerTab.CHECKLIST) }
    val importFlow = remember(copyId) { container.saveImport(copyId) }
    val saved by importFlow.collectAsState(initial = null)
    val savedPokemonFlow = remember(copyId) { container.savedPokemon(copyId) }
    val savedPokemon by savedPokemonFlow.collectAsState(initial = emptyList())
    val inSave = remember(savedPokemon) { savedPokemon.filter { !it.egg }.mapTo(HashSet()) { it.speciesId } }
    // With a save imported, the Boxes tab shows the real boxes first; the living dex plan is one tap away.
    var showPlan by rememberSaveable { mutableStateOf(false) }
    val allCaught by container.allCaught.collectAsState(initial = emptyList())
    val caughtByKey = remember(allCaught) { allCaught.groupBy({ it.game }, { it.speciesId }).mapValues { it.value.toSet() } }
    val events by produceState(emptySet<Int>(), game, copy.nationalDex) {
        value = withContext(Dispatchers.Default) { dex.eventOnly(game, copy.nationalDex) }
    }
    val open: (PokemonSummary, List<PokemonSummary>) -> Unit = { p, order ->
        container.browseOrder.value = order.map { it.id }
        onOpenPokemon(p.id)
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(copyTitle(copy), maxLines = 1, overflow = TextOverflow.Ellipsis) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
        )
    }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            ProgressStrip(done, all.size, Color(game.color))
            PlannerTabs(tab, Color(game.color)) { tab = it }
            when (tab) {
                PlannerTab.BOXES -> Column {
                    val import = saved
                    if (import != null) SegmentedButtons(
                        listOf(stringResource(R.string.save_view), stringResource(R.string.plan_view)), if (showPlan) 1 else 0, Color(game.color),
                        { showPlan = it == 1 }, Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp),
                    )
                    // With a save, the plan shows what's really in the game: anything else still has to be brought in.
                    if (import != null && !showPlan) SaveBoxesView(dex, game, import, savedPokemon, all, dates.keys, onOpenPokemon)
                    else if (import != null) BoxesView(game, all, inSave, events, artwork, onToggle = null, onOpen = open)
                    else BoxesView(game, all, dates.keys, events, artwork,
                        onToggle = { id -> scope.launch { container.setCaughtIn(copy.key, id, id !in dates) } }, onOpen = open)
                }
                // One long list, so on a tablet it keeps a readable width.
                PlannerTab.TRADES -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                    Box(Modifier.widthIn(max = ReadableWidth)) { TradesView(dex, copy, list, caughtByKey, events, artwork, onOpen = open) }
                }
                PlannerTab.CHECKLIST -> ChecklistGrid(shown, show, { show = it }, game, artwork, dates, dateFormat,
                    eventMissing = events.count { it !in dates }, events = events,
                    onToggle = { id -> scope.launch { container.setCaughtIn(copy.key, id, id !in dates) } }, onOpen = open)
            }
        }
    }
}

/** A game copy's three views: its checklist, its PC boxes and what its other games can send it. */
private enum class PlannerTab(@StringRes val label: Int) { CHECKLIST(R.string.tab_checklist), BOXES(R.string.tab_boxes), TRADES(R.string.tab_trades) }

@Composable
private fun PlannerTabs(tab: PlannerTab, accent: Color, onTab: (PlannerTab) -> Unit) {
    PrimaryTabRow(
        selectedTabIndex = tab.ordinal, containerColor = Color.Transparent, divider = {},
        indicator = { TabRowDefaults.PrimaryIndicator(Modifier.tabIndicatorOffset(tab.ordinal, matchContentSize = true), width = Dp.Unspecified, color = accent) },
    ) {
        PlannerTab.entries.forEach { t ->
            Tab(selected = tab == t, onClick = { onTab(t) }, text = { Text(stringResource(t.label), fontSize = 16.sp, maxLines = 1) },
                unselectedContentColor = LocalDexColors.current.subtleText)
        }
    }
}

@Composable
private fun ChecklistGrid(
    shown: List<PokemonSummary>, show: Show, onShow: (Show) -> Unit, game: Game, artwork: Map<Int, String>, dates: Map<Int, Long>,
    dateFormat: DateFormat, eventMissing: Int, events: Set<Int>, onToggle: (Int) -> Unit, onOpen: (PokemonSummary, List<PokemonSummary>) -> Unit,
) {
    Column {
        SegmentedButtons(Show.entries.map { stringResource(it.label) }, show.ordinal, Color(game.color), { onShow(Show.entries[it]) },
            Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp))
        // So the trainer knows which of the missing they can't reach in the game itself.
        if (eventMissing > 0) Text(stringResource(R.string.event_missing_note, eventMissing), color = LocalDexColors.current.subtleText,
            style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 8.dp))
        if (shown.isEmpty()) EmptyNote(stringResource(if (show == Show.CAUGHT) R.string.checklist_none_caught else R.string.nothing_left), Modifier.padding(24.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(3 * LocalWidthClass.current.cardsPerRow),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(shown, key = { it.id }) { p ->
                ChecklistTile(
                    p, artwork, dates[p.id]?.let { dateFormat.format(Date(it)) }, event = p.id in events,
                    onToggle = { onToggle(p.id) },
                    onClick = { onOpen(p, shown) },
                )
            }
        }
    }
}

/** dataDex 4's progress strip: the done share filled in the game's colour, with the percentage and the count. */
@Composable
private fun ProgressStrip(done: Int, total: Int, color: Color) {
    val fraction = if (total == 0) 0f else done.toFloat() / total
    Box(Modifier.fillMaxWidth().height(48.dp).background(LocalDexColors.current.chip)) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(fraction).background(color.copy(alpha = 0.35f)))
        Row(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.TaskAlt, null, tint = LocalDexColors.current.chipText, modifier = Modifier.size(20.dp))
            Text("  " + "%.1f%%".format(fraction * 100), color = LocalDexColors.current.chipText, fontSize = 16.sp)
            Spacer(Modifier.weight(1f))
            Text(stringResource(R.string.checklist_count, done, total), color = LocalDexColors.current.chipText, fontSize = 16.sp)
        }
    }
}

@Composable
private fun ChecklistTile(p: PokemonSummary, artwork: Map<Int, String>, caughtOn: String?, event: Boolean, onToggle: () -> Unit, onClick: () -> Unit) {
    val tones = animatedWash(p.formId, p.colorId)
    val dexColors = LocalDexColors.current
    val caught = caughtOn != null
    val shape = RoundedCornerShape(14.dp)
    Box(
        Modifier.then(if (caught) Modifier.wash(tones, shape) else Modifier.clip(shape).background(dexColors.surface).border(1.dp, dexColors.line, shape))
            .clickable(onClick = onClick),
    ) {
        Column(Modifier.fillMaxWidth().padding(start = 6.dp, end = 6.dp, top = 12.dp, bottom = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(72.dp).then(if (caught) Modifier.halo(tones) else Modifier), contentAlignment = Alignment.Center) {
                PokemonArtwork(p.formId, Images.artwork(p.formId, artwork), Modifier.size(64.dp).graphicsLayer { alpha = if (caught) 1f else 0.35f }, p.name)
            }
            Text(dexNumber(p.dexNumber), color = if (caught) dexColors.accentText else dexColors.textSubtle, style = DexType.monoMeta,
                modifier = Modifier.padding(top = 6.dp))
            BasicText(
                p.name, maxLines = 1,
                style = DexType.itemTitle.copy(color = if (caught) dexColors.text else dexColors.textSubtle, textAlign = TextAlign.Center),
                autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = 15.sp), modifier = Modifier.padding(horizontal = 2.dp),
            )
            Text(caughtOn ?: if (event) stringResource(R.string.event_badge) else " ",
                color = dexColors.textMuted, style = DexType.caption.copy(fontSize = 11.sp), maxLines = 1)
        }
        Box(Modifier.align(Alignment.TopEnd)) {
            CaughtToggle(caught, dexColors.textFaint, onToggle, size = 20.dp)
        }
    }
}
