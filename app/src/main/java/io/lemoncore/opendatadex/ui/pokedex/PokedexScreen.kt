package io.lemoncore.opendatadex.ui.pokedex

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.lemoncore.opendatadex.R
import io.lemoncore.opendatadex.data.Dex
import io.lemoncore.opendatadex.data.Game
import io.lemoncore.opendatadex.data.Images
import io.lemoncore.opendatadex.data.PokemonSummary
import io.lemoncore.opendatadex.data.SortOrder
import io.lemoncore.opendatadex.data.TabletList
import io.lemoncore.opendatadex.data.user.GameCopyEntity
import io.lemoncore.opendatadex.ui.components.EmptyNote
import io.lemoncore.opendatadex.ui.components.GameChip
import io.lemoncore.opendatadex.ui.components.HeaderChip
import io.lemoncore.opendatadex.ui.components.Loading
import io.lemoncore.opendatadex.ui.components.LocalWidthClass
import io.lemoncore.opendatadex.ui.components.PokemonGridCard
import io.lemoncore.opendatadex.ui.components.ReadableWidth
import io.lemoncore.opendatadex.ui.components.OdexWordmark
import io.lemoncore.opendatadex.ui.components.Overline
import io.lemoncore.opendatadex.ui.components.PokemonCard
import io.lemoncore.opendatadex.ui.components.ProgressLine
import io.lemoncore.opendatadex.ui.components.SegmentedButtons
import io.lemoncore.opendatadex.ui.components.TypeTag
import io.lemoncore.opendatadex.ui.components.copyDetails
import io.lemoncore.opendatadex.ui.components.copyTitle
import io.lemoncore.opendatadex.ui.components.gameChipLabel
import io.lemoncore.opendatadex.ui.components.gameCode
import io.lemoncore.opendatadex.ui.components.gameColor
import io.lemoncore.opendatadex.ui.components.gameName
import io.lemoncore.opendatadex.ui.components.generationLabel
import io.lemoncore.opendatadex.ui.theme.DexType
import io.lemoncore.opendatadex.ui.theme.JetBrainsMono
import io.lemoncore.opendatadex.ui.theme.LocalDexColors
import io.lemoncore.opendatadex.ui.theme.TypeColors
import io.lemoncore.opendatadex.ui.theme.WashTones
import io.lemoncore.opendatadex.ui.theme.oklchHue
import io.lemoncore.opendatadex.ui.theme.wash
import kotlinx.coroutines.flow.first

private enum class Sheet { NONE, GAME, GEN, TYPE, SORT, MORE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PokedexScreen(
    vm: PokedexViewModel,
    artwork: Map<Int, String>,
    onOpenDrawer: () -> Unit,
    onOpenPokemon: (Int) -> Unit,
    onSearchAll: () -> Unit,
    onSettings: () -> Unit,
    /** Opens a game copy's PC boxes. */
    onOpenBoxes: (Long) -> Unit,
    /** The Pokémon open in the tablet's side panel, outlined in the list; null on a phone. */
    selected: Int? = null,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val st = state ?: return Loading()
    val c = LocalDexColors.current
    var sheet by remember { mutableStateOf(Sheet.NONE) }
    var confirmClear by remember { mutableStateOf(false) }
    val listState = rememberLazyGridState()
    val game = st.settings.game
    val dex = st.dex
    val f = st.filters
    val large = LocalWidthClass.current.large
    val grid = large && st.settings.tabletList == TabletList.GRID

    // Brings [id]'s card a third of the way down if it isn't in view (wholly, or with [partly], at all).
    suspend fun reveal(id: Int, partly: Boolean = false) {
        val index = listIndexOf(st, id) ?: return
        val layout = snapshotFlow { listState.layoutInfo }.first { it.totalItemsCount > 0 }
        val inView = layout.visibleItemsInfo.any {
            it.index == index && (partly || it.offset.y >= 0 && it.offset.y + it.size.height <= layout.viewportEndOffset)
        }
        if (!inView) listState.scrollToItem(index, -layout.viewportEndOffset / 3)
    }
    // Back to the top when the game or a filter changes, but not when the screen returns from a Pokémon.
    val filterKey = listOf(game.key, f.gen, f.type, f.eggGroup, f.ability, f.move, f.show, st.active.nationalDex).toString()
    var lastFilterKey by rememberSaveable { mutableStateOf(filterKey) }
    LaunchedEffect(filterKey) {
        if (filterKey != lastFilterKey) {
            lastFilterKey = filterKey
            listState.scrollToItem(0)
        }
    }
    // Returning from a Pokémon (perhaps swiped to another): bring its card into view so its artwork lands there.
    val listShown = vm.opened.collectAsStateWithLifecycle().value == null
    LaunchedEffect(listShown) {
        if (!listShown) return@LaunchedEffect
        reveal(vm.takeLastViewed())
    }

    Scaffold(
        containerColor = c.bg,
        topBar = {
            Column(Modifier.background(c.bg.copy(alpha = 0.95f)).statusBarsPadding().padding(bottom = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (f.name != null) {
                    NameFilterBar(f.name, onChange = vm::setNameFilter, onClose = { vm.setNameFilter(null) })
                } else {
                    HomeTopBar(
                        st, onOpenDrawer, onSort = { sheet = Sheet.SORT }, onFilterName = { vm.setNameFilter("") },
                        onClear = { confirmClear = true }, onSettings = onSettings, onOpenBoxes = onOpenBoxes, vm = vm,
                    )
                }
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically,
                ) {
                    GameChip(game, st.active.copy?.let { copyTitle(it) } ?: gameChipLabel(game), { sheet = Sheet.GAME }, Modifier.weight(1f, fill = false), shrink = true)
                    HeaderChip(f.gen?.let { stringResource(R.string.gen_short, it) } ?: stringResource(R.string.chip_gens), f.gen != null, { sheet = Sheet.GEN })
                    val typeTones = f.type?.let { TypeColors.tones(it, c.isDark) }
                    HeaderChip(
                        f.type?.let { dex.type(it)?.name } ?: stringResource(R.string.chip_types), f.type != null, { sheet = Sheet.TYPE },
                        activeColors = typeTones?.let { Triple(it.bg, it.dot, it.text) },
                    )
                    HeaderChip(
                        stringResource(R.string.more) + if (f.extraCount > 0) " · ${f.extraCount}" else "",
                        f.extraCount > 0, { sheet = Sheet.MORE },
                    )
                }
                ExtraFilterChips(dex, game, f, vm)
                SegmentedButtons(
                    listOf(stringResource(R.string.show_filter_all), stringResource(R.string.favorites), stringResource(R.string.show_caught), stringResource(R.string.show_missing)),
                    f.show.ordinal, { vm.setShow(ShowFilter.entries[it]) }, Modifier.padding(horizontal = 12.dp).widthIn(max = ReadableWidth), counts = st.showCounts,
                )
                if (!game.isNational) ProgressLine(st.caught.size, st.total, Modifier.padding(horizontal = 12.dp).widthIn(max = ReadableWidth))
            }
        },
        floatingActionButton = { SearchFab(onSearchAll) },
    ) { padding ->
        if (st.shown.isEmpty()) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyNote(
                    when (f.show) {
                        ShowFilter.CAUGHT -> stringResource(R.string.empty_checklist, gameName(game))
                        ShowFilter.FAVORITES -> stringResource(R.string.empty_favorites)
                        else -> stringResource(R.string.empty_filters)
                    },
                    Modifier.padding(32.dp),
                )
            }
        } else BoxWithConstraints(Modifier.fillMaxSize()) {
            val side = if (large) 20.dp else 12.dp
            val room = maxWidth - side * 2
            // Columns from the room the list has (after the rail, and the panel when it pushes), so foldables and split
            // screen work too: rows of about a phone's width, at most two; square cards of about 210-300 dp.
            val columns = when {
                !large -> 1
                grid -> (room / 210.dp).toInt().coerceIn(2, 4)
                else -> (room / 320.dp).toInt().coerceIn(1, 2)
            }
            // The open Pokémon stays in view when the columns change, and when the panel moves on to another.
            LaunchedEffect(selected, columns) { selected?.let { reveal(it, partly = true) } }
            LazyVerticalGrid(
                GridCells.Fixed(columns),
                state = listState,
                contentPadding = PaddingValues(start = side, end = side, top = padding.calculateTopPadding(), bottom = 96.dp),
                horizontalArrangement = Arrangement.spacedBy(if (grid) 16.dp else 12.dp),
                verticalArrangement = Arrangement.spacedBy(if (grid) 16.dp else if (large) 10.dp else 8.dp),
            ) {
                val full: LazyGridItemSpanScope.() -> GridItemSpan = { GridItemSpan(maxLineSpan) }
                // Games with several dexes (Kalos, Galar and its DLC, Paldea and its DLC) get a heading per dex.
                val sections = game.pokedexIds.size > 1 && !st.active.nationalDex && st.settings.sort == SortOrder.NUMBER && !st.settings.sortDescending
                if (st.active.nationalDex) item(key = "national", span = full) {
                    Overline(stringResource(R.string.national_pokedex), Modifier.fillMaxWidth(), align = TextAlign.Center)
                }
                st.shown.forEachIndexed { i, p ->
                    if (sections && (i == 0 || st.shown[i - 1].dexIndex != p.dexIndex)) {
                        item(key = "dex-${p.dexIndex}", span = full) {
                            Overline(
                                stringResource(R.string.dex_heading, dex.pokedexName(game.pokedexIds[p.dexIndex])), align = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(top = if (i == 0) 0.dp else 12.dp),
                            )
                        }
                    }
                    item(key = p.id) {
                        PokemonListCard(p, dex, artwork, st, vm, onOpenPokemon, grid, p.id == selected, if (large) Modifier.animateItem() else Modifier)
                    }
                }
            }
        }
    }
    PokedexSheets(sheet, { sheet = it }, st, vm, confirmClear, { confirmClear = it })
}

@Composable
private fun HomeTopBar(
    st: PokedexUiState, onOpenDrawer: () -> Unit, onSort: () -> Unit, onFilterName: () -> Unit, onClear: () -> Unit,
    onSettings: () -> Unit, onOpenBoxes: (Long) -> Unit, vm: PokedexViewModel,
) {
    val c = LocalDexColors.current
    val game = st.settings.game
    var menu by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onOpenDrawer) { Icon(Icons.Filled.Menu, stringResource(R.string.open_menu), tint = c.text, modifier = Modifier.size(22.dp)) }
        OdexWordmark(Modifier.weight(1f).padding(start = 4.dp))
        st.active.copy?.let { copy -> HeaderChip(stringResource(R.string.tab_boxes), false, { onOpenBoxes(copy.id) }, caret = false) }
        Box {
            IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, stringResource(R.string.more), tint = c.textSecondary, modifier = Modifier.size(21.dp)) }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text(stringResource(R.string.sort_by_ellipsis)) }, onClick = { menu = false; onSort() })
                DropdownMenuItem(text = { Text(stringResource(R.string.filter_by_name)) }, onClick = { menu = false; onFilterName() })
                if (game.hasNationalDex) DropdownMenuItem(
                    text = { Text(stringResource(R.string.national_pokedex)) },
                    trailingIcon = { Checkbox(checked = st.active.nationalDex, onCheckedChange = null) },
                    onClick = { menu = false; vm.setNationalDex(!st.active.nationalDex) },
                )
                DropdownMenuItem(text = { Text(stringResource(R.string.mark_shown_caught)) }, onClick = { menu = false; vm.markShownCaught() })
                DropdownMenuItem(text = { Text(stringResource(R.string.clear_game_checklist, gameName(game))) }, onClick = { menu = false; onClear() })
                DropdownMenuItem(text = { Text(stringResource(R.string.nav_settings)) }, onClick = { menu = false; onSettings() })
            }
        }
    }
}

/** The one floating button: Search all. */
@Composable
private fun SearchFab(onClick: () -> Unit) {
    val c = LocalDexColors.current
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier.navigationBarsPadding().padding(bottom = 4.dp)
            .shadow(14.dp, shape, ambientColor = c.accent, spotColor = c.accent)
            .clip(shape).background(c.accent).clickable(onClick = onClick)
            .height(56.dp).padding(start = 16.dp, end = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Search, null, tint = c.onAccent, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.search_all), color = c.onAccent, style = DexType.fab)
    }
}

/** Where [id]'s card sits in the list, counting the headings drawn above cards (see the LazyColumn below). */
private fun listIndexOf(st: PokedexUiState, id: Int): Int? {
    if (id == 0) return null
    val game = st.settings.game
    val sections = game.pokedexIds.size > 1 && !st.active.nationalDex && st.settings.sort == SortOrder.NUMBER && !st.settings.sortDescending
    var index = if (st.active.nationalDex) 1 else 0
    st.shown.forEachIndexed { i, p ->
        if (sections && (i == 0 || st.shown[i - 1].dexIndex != p.dexIndex)) index++
        if (p.id == id) return index
        index++
    }
    return null
}

@Composable
private fun PokemonListCard(
    p: PokemonSummary, dex: Dex, artwork: Map<Int, String>, st: PokedexUiState, vm: PokedexViewModel, onOpenPokemon: (Int) -> Unit,
    grid: Boolean, selected: Boolean, modifier: Modifier,
) {
    val typeNames = p.types.map { dex.type(it)?.name ?: "" }
    val badge = if (p.id in st.eventOnly) stringResource(R.string.event_badge) else null
    if (grid) PokemonGridCard(
        p, typeNames, Images.artwork(p.formId, artwork), favorite = p.id in st.favorites, caught = p.id in st.caught,
        onFavorite = { vm.toggleFavorite(p.id) }, onCaught = { vm.toggleCaught(p.id) }, onClick = { onOpenPokemon(p.id) },
        modifier = modifier, badge = badge, selected = selected,
    ) else PokemonCard(
        pokemon = p,
        typeNames = typeNames,
        artworkUrl = Images.artwork(p.formId, artwork),
        favorite = p.id in st.favorites,
        caught = p.id in st.caught,
        onFavorite = { vm.toggleFavorite(p.id) },
        onCaught = { vm.toggleCaught(p.id) },
        onClick = { onOpenPokemon(p.id) },
        modifier = modifier,
        badge = badge,
        selected = selected,
    )
}

@Composable
private fun PokedexSheets(
    sheet: Sheet, setSheet: (Sheet) -> Unit, st: PokedexUiState, vm: PokedexViewModel,
    confirmClear: Boolean, setConfirmClear: (Boolean) -> Unit,
) {
    val game = st.settings.game
    val dex = st.dex
    when (sheet) {
        Sheet.GAME -> {
            val progress by vm.copyProgress.collectAsStateWithLifecycle()
            GamePickerSheet(
                game, onPick = { vm.setGame(it); setSheet(Sheet.NONE) }, onDismiss = { setSheet(Sheet.NONE) },
                copies = st.copies, currentCopy = st.active.copy?.id, onPickCopy = { vm.useCopy(it); setSheet(Sheet.NONE) },
                copyProgress = progress,
            )
        }
        Sheet.GEN -> OptionSheet(
            title = stringResource(R.string.select_generation),
            options = listOf<Pair<Int?, String>>(null to stringResource(R.string.all_gens)) + (1..game.speciesGen).map { it to generationLabel(it) },
            onPick = { vm.setGen(it); setSheet(Sheet.NONE) }, onDismiss = { setSheet(Sheet.NONE) }, selected = st.filters.gen,
        )
        Sheet.TYPE -> TypePickerSheet(
            types = dex.typesIn(game).map { it.id to it.name }, selected = st.filters.type,
            onPick = { vm.setType(it); setSheet(Sheet.NONE) }, onDismiss = { setSheet(Sheet.NONE) },
        )
        Sheet.SORT -> SortDialog(st.settings.sort, st.settings.sortDescending, onPick = { o, d -> vm.setSort(o, d); setSheet(Sheet.NONE) }, onDismiss = { setSheet(Sheet.NONE) })
        Sheet.MORE -> MoreFiltersSheet(dex, game, st.active.nationalDex, st.filters, vm, onDismiss = { setSheet(Sheet.NONE) })
        Sheet.NONE -> Unit
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { setConfirmClear(false) },
            title = { Text(stringResource(R.string.clear_checklist_title)) },
            text = { Text(stringResource(R.string.clear_checklist_body, gameName(game))) },
            confirmButton = { TextButton(onClick = { setConfirmClear(false); vm.clearChecklist() }) { Text(stringResource(R.string.clear)) } },
            dismissButton = { TextButton(onClick = { setConfirmClear(false) }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun NameFilterBar(text: String, onChange: (String) -> Unit, onClose: () -> Unit) {
    val c = LocalDexColors.current
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    // The field keeps its own text: waiting for the filter to come back through the view model loses fast keystrokes.
    var typed by remember { mutableStateOf(text) }
    Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.stop_filtering), tint = c.text) }
        Box(Modifier.weight(1f).padding(horizontal = 4.dp), contentAlignment = Alignment.CenterStart) {
            val style = DexType.body.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium, color = c.text)
            if (typed.isEmpty()) Text(stringResource(R.string.filter_by_name), style = style.copy(color = c.textFaint))
            BasicTextField(
                value = typed, onValueChange = { typed = it; onChange(it) }, singleLine = true, textStyle = style,
                cursorBrush = SolidColor(c.accent), keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
            )
        }
        if (typed.isNotEmpty()) IconButton(onClick = { typed = ""; onChange("") }) { Icon(Icons.Filled.Close, stringResource(R.string.clear), tint = c.textSecondary) }
    }
}

/** A bottom sheet in the app's style: raised surface, a slim handle and a left-aligned title. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DexSheet(
    title: String?, onDismiss: () -> Unit, sheetState: SheetState = rememberModalBottomSheetState(),
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = LocalDexColors.current
    ModalBottomSheet(
        onDismissRequest = onDismiss, sheetState = sheetState, containerColor = c.bgRaised, scrimColor = Color.Black.copy(alpha = 0.62f),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(Modifier.padding(top = 10.dp, bottom = 2.dp).size(36.dp, 4.dp).clip(RoundedCornerShape(2.dp))
                .background(if (c.isDark) Color(0xFF2A2B31) else c.lineStrong))
        },
    ) {
        if (title != null) Text(title, color = c.text, style = DexType.sheetTitle, modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 6.dp))
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamePickerSheet(
    current: Game, onPick: (Game) -> Unit, onDismiss: () -> Unit, includeNational: Boolean = true,
    /** The trainer's game copies, offered first (the Pokédex's picker only). */
    copies: List<GameCopyEntity> = emptyList(), currentCopy: Long? = null, onPickCopy: (GameCopyEntity) -> Unit = {},
    /** Caught and total per copy, for the percentage on each. */
    copyProgress: Map<Long, Pair<Int, Int>> = emptyMap(),
) {
    DexSheet(stringResource(R.string.choose_game), onDismiss) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 12.dp, end = 12.dp, top = 4.dp).navigationBarsPadding().padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (copies.isNotEmpty()) {
                Overline(stringResource(R.string.your_games), Modifier.padding(start = 4.dp, top = 8.dp, bottom = 2.dp))
                copies.forEach { copy ->
                    val (caught, total) = copyProgress[copy.id] ?: (0 to 0)
                    CopyRow(copy, caught, total, copy.id == currentCopy) { onPickCopy(copy) }
                }
            }
            if (includeNational) {
                Spacer(Modifier.height(6.dp))
                GameButton(Game.NATIONAL, current == Game.NATIONAL, Modifier.fillMaxWidth(), label = stringResource(R.string.national_pokedex)) { onPick(Game.NATIONAL) }
            }
            Game.byGeneration().forEach { (gen, games) ->
                Overline(stringResource(R.string.gen_short, gen), Modifier.padding(start = 4.dp, top = 12.dp, bottom = 2.dp))
                // Paired versions share a row; a third version gets a full-width row, as in dataDex.
                gamePairs(games).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { g -> GameButton(g, g == current && currentCopy == null, Modifier.weight(1f)) { onPick(g) } }
                    }
                }
            }
        }
    }
}

/** Rows of a generation's games: versions released as a pair share a row (Emerald, Platinum and Legends get their own). */
fun gamePairs(games: List<Game>): List<List<Game>> = games.groupBy { it.versionGroup }.values.flatMap { group -> group.chunked(2) }

/** The wash colours of a game, from its own colour. */
@Composable
fun gameWash(game: Game): WashTones {
    val dark = LocalDexColors.current.isDark
    return remember(game, dark) { WashTones.from(Color(game.color).oklchHue(), dark) }
}

/** A game's square with its short code ("FR", "LGE"). */
@Composable
fun GameCodeSwatch(game: Game, size: androidx.compose.ui.unit.Dp = 40.dp) {
    val w = gameWash(game)
    Box(
        Modifier.size(size).clip(RoundedCornerShape(10.dp)).background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(w.swatchStart, w.swatchEnd))),
        contentAlignment = Alignment.Center,
    ) {
        Text(gameCode(game), color = Color.White.copy(alpha = 0.85f), fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 10.sp, maxLines = 1)
    }
}

/** One of the trainer's game copies in the picker: the game's wash, its code, name, save line and completion. */
@Composable
private fun CopyRow(copy: GameCopyEntity, caught: Int, total: Int, active: Boolean, onClick: () -> Unit) {
    val c = LocalDexColors.current
    val game = Game.fromKey(copy.game)
    val w = gameWash(game)
    val pct = if (total == 0) 0 else Math.round(caught * 100f / total)
    Row(
        Modifier.fillMaxWidth().height(64.dp).wash(w, RoundedCornerShape(14.dp), border = if (active) c.accent else w.border)
            .clickable(onClick = onClick).padding(start = 10.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        GameCodeSwatch(game)
        Column(Modifier.weight(1f)) {
            Text(copyTitle(copy), color = c.text, style = DexType.itemTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val details = copyDetails(copy)
            if (details.isNotEmpty()) Text(details, color = c.textMuted, style = DexType.caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text("$pct%", color = if (total > 0 && caught == total) c.gold else c.text, style = DexType.monoMeta)
        if (active) Icon(Icons.Filled.CheckCircle, null, tint = c.accent, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun GameButton(game: Game, selected: Boolean, modifier: Modifier, label: String = gameName(game), onClick: () -> Unit) {
    val c = LocalDexColors.current
    Row(
        modifier.height(48.dp).clip(RoundedCornerShape(12.dp)).background(if (c.isDark) Color(0xFF121316) else c.surface)
            .border(1.dp, if (selected) c.accent else c.line, RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(10.dp, 28.dp).clip(RoundedCornerShape(3.dp)).background(gameColor(game)))
        Spacer(Modifier.width(10.dp))
        Text(label, color = c.text, style = DexType.itemTitle.copy(fontSize = 13.sp), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        if (selected) Icon(Icons.Filled.CheckCircle, null, tint = c.accent, modifier = Modifier.size(16.dp))
    }
}

/** A sheet listing options as full-width rows; [selected] is ticked. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> OptionSheet(title: String, options: List<Pair<T, String>>, onPick: (T) -> Unit, onDismiss: () -> Unit, selected: T? = null) {
    val c = LocalDexColors.current
    DexSheet(title, onDismiss) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 12.dp).navigationBarsPadding().padding(top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            options.forEach { (value, label) ->
                val on = value == selected
                Row(
                    Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(12.dp))
                        .background(if (on) c.accentTint else if (c.isDark) Color(0xFF121316) else c.surface)
                        .border(1.dp, if (on) c.accent.copy(alpha = 0.5f) else c.line, RoundedCornerShape(12.dp))
                        .clickable { onPick(value) }.padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(label, color = if (on) c.accentText else c.text, style = DexType.itemTitle.copy(fontSize = 13.sp), maxLines = 1,
                        overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    if (on) Icon(Icons.Filled.CheckCircle, null, tint = c.accent, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TypePickerSheet(types: List<Pair<Int, String>>, selected: Int?, onPick: (Int?) -> Unit, onDismiss: () -> Unit) {
    val c = LocalDexColors.current
    DexSheet(stringResource(R.string.select_type), onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp).navigationBarsPadding().padding(top = 4.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            HeaderChip(stringResource(R.string.all_types), selected == null, { onPick(null) }, Modifier.fillMaxWidth().height(40.dp), caret = false)
            types.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { (id, name) ->
                        val t = TypeColors.tones(id, c.isDark)
                        Box(
                            Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(10.dp)).background(t.bg)
                                .border(1.dp, if (id == selected) t.dot else Color.Transparent, RoundedCornerShape(10.dp)).clickable { onPick(id) },
                            contentAlignment = Alignment.Center,
                        ) { TypeTag(id, name) }
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun SortDialog(order: SortOrder, descending: Boolean, onPick: (SortOrder, Boolean) -> Unit, onDismiss: () -> Unit) {
    var o by remember { mutableStateOf(order) }
    var d by remember { mutableStateOf(descending) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.sort_by)) },
        text = {
            Column {
                SortOrder.entries.forEach { opt ->
                    Row(Modifier.fillMaxWidth().clickable { o = opt }, verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = o == opt, onClick = { o = opt })
                        Text(stringResource(opt.label))
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.descending), Modifier.weight(1f))
                    Switch(checked = d, onCheckedChange = { d = it })
                }
            }
        },
        confirmButton = { TextButton(onClick = { onPick(o, d) }) { Text(stringResource(R.string.apply)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
