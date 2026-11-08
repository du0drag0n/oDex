package io.lemoncore.opendatadex.ui

import androidx.compose.ui.res.stringResource
import io.lemoncore.opendatadex.R
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import io.lemoncore.opendatadex.ui.components.LocalWidthClass
import io.lemoncore.opendatadex.ui.components.OdexMark
import io.lemoncore.opendatadex.ui.components.ReadableWidth
import io.lemoncore.opendatadex.ui.components.WidthClass
import io.lemoncore.opendatadex.ui.pokedex.PokedexWithPanel
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CatchingPokemon
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsMma
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Backpack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import io.lemoncore.opendatadex.ui.extras.EvolutionDexScreen
import io.lemoncore.opendatadex.ui.catchlist.CatchListScreen
import io.lemoncore.opendatadex.ui.catchlist.CatchListsScreen
import androidx.compose.material.icons.automirrored.filled.PlaylistAddCheck
import io.lemoncore.opendatadex.ui.extras.GlobalChecklistScreen
import io.lemoncore.opendatadex.ui.extras.CopyChecklistScreen
import io.lemoncore.opendatadex.ui.extras.ShinyDexScreen
import io.lemoncore.opendatadex.ui.team.TeamEditorScreen
import io.lemoncore.opendatadex.ui.team.TeamListScreen
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material.icons.filled.Map
import io.lemoncore.opendatadex.ui.extras.FindOnMapScreen
import io.lemoncore.opendatadex.ui.extras.RegionMapScreen
import io.lemoncore.opendatadex.ui.extras.ShinyHuntScreen
import io.lemoncore.opendatadex.ui.extras.Milestone
import io.lemoncore.opendatadex.ui.extras.MilestoneDialog
import io.lemoncore.opendatadex.ui.extras.milestoneCrossed
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalResources
import io.lemoncore.opendatadex.ui.components.Feel
import io.lemoncore.opendatadex.ui.components.LocalFeel
import io.lemoncore.opendatadex.ui.components.LocalSettings
import io.lemoncore.opendatadex.ui.components.dexNumber
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import io.lemoncore.opendatadex.AppContainer
import io.lemoncore.opendatadex.data.Dex
import io.lemoncore.opendatadex.data.Game
import io.lemoncore.opendatadex.data.SearchHit
import io.lemoncore.opendatadex.data.Settings
import io.lemoncore.opendatadex.ui.components.Loading
import io.lemoncore.opendatadex.ui.components.gameName
import io.lemoncore.opendatadex.ui.components.copyTitle
import io.lemoncore.opendatadex.data.user.GameCopyEntity
import io.lemoncore.opendatadex.ui.components.LocalScreenAnimation
import io.lemoncore.opendatadex.ui.components.LocalArtworkShown
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.key
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import io.lemoncore.opendatadex.ui.components.LocalSharedTransition
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import io.lemoncore.opendatadex.ui.games.MyGamesScreen
import io.lemoncore.opendatadex.data.ActiveChecklist
import androidx.compose.material.icons.filled.SportsEsports
import androidx.annotation.StringRes
import io.lemoncore.opendatadex.ui.detail.DetailNav
import io.lemoncore.opendatadex.ui.detail.PokemonDetailScreen
import io.lemoncore.opendatadex.ui.dexes.AbilityDetailScreen
import io.lemoncore.opendatadex.ui.dexes.AbilityDexScreen
import io.lemoncore.opendatadex.ui.dexes.ItemDexScreen
import io.lemoncore.opendatadex.ui.dexes.LocationDetailScreen
import io.lemoncore.opendatadex.ui.dexes.LocationDexScreen
import io.lemoncore.opendatadex.ui.dexes.MoveDetailScreen
import io.lemoncore.opendatadex.ui.dexes.MoveDexScreen
import io.lemoncore.opendatadex.ui.dexes.NatureDexScreen
import io.lemoncore.opendatadex.ui.dexes.TypeDetailScreen
import io.lemoncore.opendatadex.ui.dexes.TypeDexScreen
import io.lemoncore.opendatadex.ui.settings.AboutScreen
import io.lemoncore.opendatadex.ui.settings.SupportScreen
import io.lemoncore.opendatadex.ui.misc.SearchScreen
import io.lemoncore.opendatadex.ui.settings.MenuSettingsScreen
import io.lemoncore.opendatadex.ui.settings.SettingsScreen
import io.lemoncore.opendatadex.ui.pokedex.PokedexScreen
import io.lemoncore.opendatadex.ui.pokedex.PokedexViewModel
import io.lemoncore.opendatadex.ui.theme.DexType
import io.lemoncore.opendatadex.ui.theme.LocalDexColors
import io.lemoncore.opendatadex.ui.theme.oklch
import io.lemoncore.opendatadex.ui.theme.glow
import androidx.compose.ui.geometry.Size
import io.lemoncore.opendatadex.ui.theme.oklchHue
import io.lemoncore.opendatadex.ui.components.Bar
import io.lemoncore.opendatadex.ui.components.OdexWordmark
import io.lemoncore.opendatadex.ui.components.Overline
import io.lemoncore.opendatadex.ui.pokedex.GamePickerSheet
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.scale
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable object PokedexRoute
@Serializable object MoveDexRoute
@Serializable object AbilityDexRoute
@Serializable object ItemDexRoute
@Serializable object LocationDexRoute
@Serializable object TypeDexRoute
@Serializable object NatureDexRoute
@Serializable object ChecklistRoute
@Serializable object RegionMapRoute
/** Where [species] lives, lit up on the selected game's map. */
@Serializable data class FindOnMapRoute(val species: Int)
@Serializable data class ShinyHuntRoute(val id: Long)
@Serializable object MyGamesRoute
@Serializable data class CopyChecklistRoute(val id: Long, val boxes: Boolean = false)
@Serializable object CatchListsRoute
@Serializable data class CatchListRoute(val id: Long)
@Serializable object ShinyDexRoute
@Serializable object EvolutionDexRoute
@Serializable object TeamsRoute
@Serializable data class TeamRoute(val id: Long)
@Serializable object SearchRoute
@Serializable object SettingsRoute
@Serializable object AboutRoute
@Serializable object SupportRoute
@Serializable object MenuSettingsRoute
/** [form] is the Pokémon (form) id to open on, or 0 for the form the selected game shows. */
@Serializable data class PokemonRoute(val id: Int, val form: Int = 0)
@Serializable data class MoveRoute(val id: Int)
@Serializable data class AbilityRoute(val id: Int)
/** [highlight] is the species to point out on the page (the Pokémon the trainer came from), or 0. */
@Serializable data class LocationRoute(val id: Int, val highlight: Int = 0)
@Serializable data class TypeRoute(val id: Int)

/** A side menu entry; [key] is what Settings → Side menu saves its order and hidden entries by. */
internal data class DrawerEntry(val key: String, @StringRes val label: Int, val icon: ImageVector, val route: Any)

/** The side menu's screens in their default order. Pokédex always comes first and can't be hidden. */
internal val menuEntries = listOf(
    DrawerEntry(MENU_POKEDEX, R.string.nav_pokedex, Icons.Filled.CatchingPokemon, PokedexRoute),
    DrawerEntry("my_games", R.string.nav_my_games, Icons.Filled.SportsEsports, MyGamesRoute),
    DrawerEntry("my_progress", R.string.nav_my_progress, Icons.Filled.Checklist, ChecklistRoute),
    DrawerEntry("map", R.string.nav_map, Icons.Filled.Map, RegionMapRoute),
    DrawerEntry("catch_lists", R.string.nav_catch_lists, Icons.AutoMirrored.Filled.PlaylistAddCheck, CatchListsRoute),
    DrawerEntry("shiny_dex", R.string.nav_shiny_dex, Icons.Filled.AutoAwesome, ShinyDexRoute),
    DrawerEntry("evolution_dex", R.string.nav_evolution_dex, Icons.AutoMirrored.Filled.TrendingUp, EvolutionDexRoute),
    DrawerEntry("move_dex", R.string.nav_move_dex, Icons.Filled.SportsMma, MoveDexRoute),
    DrawerEntry("ability_dex", R.string.nav_ability_dex, Icons.Filled.Bolt, AbilityDexRoute),
    DrawerEntry("item_dex", R.string.nav_item_dex, Icons.Filled.Backpack, ItemDexRoute),
    DrawerEntry("location_dex", R.string.nav_location_dex, Icons.Filled.Place, LocationDexRoute),
    DrawerEntry("type_dex", R.string.nav_type_dex, Icons.AutoMirrored.Filled.Label, TypeDexRoute),
    DrawerEntry("nature_dex", R.string.nav_nature_dex, Icons.Filled.Spa, NatureDexRoute),
    DrawerEntry("team_builder", R.string.nav_team_builder, Icons.Filled.Groups, TeamsRoute),
)
internal const val MENU_POKEDEX = "pokedex"

/**
 * The menu in the trainer's order: saved entries first as saved, then any the saved order doesn't know (a screen added
 * since) at their default place, with Pokédex kept at the top.
 */
internal fun orderedMenu(order: List<String>): List<DrawerEntry> {
    val byKey = menuEntries.associateBy { it.key }
    val out = order.mapNotNull { byKey[it] }.distinct().toMutableList()
    menuEntries.forEachIndexed { i, e -> if (e !in out) out.add(minOf(i, out.size), e) }
    out.remove(menuEntries.first())
    return listOf(menuEntries.first()) + out
}

private val appEntries = listOf(
    DrawerEntry("support", R.string.support_odex, Icons.Filled.Favorite, SupportRoute),
    DrawerEntry("settings", R.string.nav_settings, Icons.Filled.Settings, SettingsRoute),
    DrawerEntry("about", R.string.nav_about, Icons.Filled.Info, AboutRoute),
)
private val rootRoutes = menuEntries.map { it.route::class.qualifiedName }

@Composable
fun DexApp(container: AppContainer, settings: Settings) {
    val dex by container.dex.collectAsStateWithLifecycle()
    val seedError by container.seedError.collectAsStateWithLifecycle()
    seedError?.let { return io.lemoncore.opendatadex.ui.components.EmptyNote(stringResource(R.string.seed_error) + "\n$it",Modifier.statusBarsPadding().padding(24.dp)) }
    val loaded = dex ?: return Loading()
    val nav = rememberNavController()
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val atRoot = rootRoutes.any { it != null && currentRoute?.startsWith(it) == true }
    val openDrawer: () -> Unit = { scope.launch { drawer.open() } }
    val headerCaughtFlow = remember(settings.game) { container.caught(settings.game) }
    val headerCaught by headerCaughtFlow.collectAsStateWithLifecycle(initialValue = emptySet())
    val activeFlow = remember(settings.game) { container.activeChecklist(settings.game) }
    val active by activeFlow.collectAsStateWithLifecycle(initialValue = ActiveChecklist())
    val headerTotal = remember(loaded, settings.game, active.nationalDex) { loaded.pokemonFor(settings.game, active.nationalDex).size }
    val pokedexVm: PokedexViewModel = viewModel { PokedexViewModel(container) }
    val pokemonOpen = pokedexVm.opened.collectAsStateWithLifecycle().value != null
    var pickingGame by remember { mutableStateOf(false) }
    val c = LocalDexColors.current
    val view = LocalView.current
    val feel = remember(view, settings.haptics, settings.sounds) { Feel(view, container.sounds, settings.haptics, settings.sounds) }
    val snackbar = remember { SnackbarHostState() }
    val res = LocalResources.current
    val undoOn by rememberUpdatedState(settings.undoUnmark)
    LaunchedEffect(Unit) {
        container.saveLinks.updated.collect {
            snackbar.showSnackbar(res.getString(R.string.save_link_updated, it.fileName, it.newCatches), duration = SnackbarDuration.Short)
        }
    }
    // Unmarking a catch anywhere offers Undo for a few seconds, which puts it back with its original date.
    LaunchedEffect(Unit) {
        container.unmarked.collect { row ->
            if (!undoOn) return@collect
            val name = loaded.speciesOf(row.speciesId)?.name ?: dexNumber(row.speciesId)
            snackbar.currentSnackbarData?.dismiss()
            launch {
                val result = snackbar.showSnackbar(res.getString(R.string.unmarked, name), res.getString(R.string.undo), duration = SnackbarDuration.Short)
                if (result == SnackbarResult.ActionPerformed) container.restoreCaught(row)
            }
        }
    }

    // A checklist crossing 25, 50, 75 or 100% by one catch shows the milestone card, once per checklist and milestone
    // (bulk changes such as Mark shown as caught or a restore never do).
    val keyFlow = remember(settings.game) { container.checklistKey(settings.game) }
    val checklistKey by keyFlow.collectAsStateWithLifecycle(initialValue = null)
    val counts = remember { HashMap<String, Int>() }
    var milestone by remember { mutableStateOf<Milestone?>(null) }
    val shown by rememberUpdatedState(settings.milestonesShown)
    val gameTitle = gameName(settings.game)
    LaunchedEffect(checklistKey, headerCaught, headerTotal) {
        val key = checklistKey ?: return@LaunchedEffect
        val now = headerCaught.size
        val before = counts.put(key, now)
        if (before == null || now != before + 1) return@LaunchedEffect
        val percent = milestoneCrossed(before, now, headerTotal) ?: return@LaunchedEffect
        if ("$key:$percent" in shown) return@LaunchedEffect
        container.settings.addMilestoneShown("$key:$percent")
        val name = active.copy?.label?.takeIf { it.isNotBlank() } ?: active.list?.name
        milestone = Milestone(key, percent, now, headerTotal, if (name != null) "$gameTitle · $name" else gameTitle, settings.game.hue)
        if (percent == 100) feel.complete()
    }

    val widthClass = WidthClass.of(LocalConfiguration.current.screenWidthDp.dp)
    val menuShown = remember(settings.menuOrder, settings.menuHidden) {
        orderedMenu(settings.menuOrder).filter { it.key == MENU_POKEDEX || it.key !in settings.menuHidden }
    }
    val isCurrent = { entry: DrawerEntry -> currentRoute?.startsWith(entry.route::class.qualifiedName ?: "-") == true }
    val go = { entry: DrawerEntry ->
        pokedexVm.closePokemon()
        if (!isCurrent(entry)) nav.navigateRoot(entry.route)
    }
    // An inner screen keeps its section lit: the one beneath it, else the Pokédex it was opened from.
    val section = remember(backStack, menuShown) {
        (menuShown + appEntries).firstOrNull { it.route != PokedexRoute && nav.holds(it.route) }?.route ?: PokedexRoute
    }
    val isSection = { entry: DrawerEntry -> entry.route == section }

    CompositionLocalProvider(LocalFeel provides feel, LocalSettings provides settings, LocalWidthClass provides widthClass) {
    Box(Modifier.fillMaxSize()) {
    ModalNavigationDrawer(
        drawerState = drawer,
        scrimColor = Color.Black.copy(alpha = 0.62f),
        // A Pokémon open over the list keeps the edge swipe for itself.
        gesturesEnabled = (atRoot && !pokemonOpen) || drawer.isOpen,
        drawerContent = {
            val shape = RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp)
            ModalDrawerSheet(
                drawerContainerColor = c.bgRaised, drawerShape = shape,
                modifier = Modifier.width(316.dp).border(1.dp, c.line, shape),
            ) {
                Column(Modifier.statusBarsPadding().verticalScroll(rememberScrollState()).padding(start = 12.dp, end = 12.dp, top = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    OdexWordmark(Modifier.padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 14.dp), large = true)
                    TrackingCard(settings.game, active.copy, headerCaught.size, headerTotal) {
                        scope.launch { drawer.close() }
                        pickingGame = true
                    }
                    (menuShown + appEntries).forEachIndexed { i, entry ->
                        if (i == menuShown.size) HorizontalDivider(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), color = c.line)
                        NavigationDrawerItem(
                            label = { Text(stringResource(entry.label), style = DexType.drawerItem) },
                            icon = { Icon(entry.icon, null, Modifier.size(20.dp)) },
                            selected = isSection(entry),
                            onClick = {
                                scope.launch { drawer.close() }
                                go(entry)
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedTextColor = c.accentText, selectedIconColor = c.accentText, selectedContainerColor = c.accentTint,
                                unselectedTextColor = c.textSecondary, unselectedIconColor = c.textSecondary, unselectedContainerColor = Color.Transparent,
                            ),
                            modifier = Modifier.height(48.dp),
                        )
                    }
                }
            }
        },
    ) {
        // A tablet has a rail of the first menu screens down the left; More opens the full menu.
        Row(Modifier.fillMaxSize().background(c.bg)) {
            if (widthClass.large) DexRail(menuShown.take(RAIL_SCREENS) + appEntries.first { it.route == SettingsRoute }, isSection, go, onMore = openDrawer)
            Box(Modifier.weight(1f)) {
                AppNavHost(container, loaded, settings, nav, pokedexVm, openDrawer) { pickingGame = true }
            }
        }
    }
    SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 72.dp))
    }
    }
    milestone?.let { m ->
        val art by container.openDexArtwork.collectAsStateWithLifecycle()
        MilestoneDialog(container, m, art) { milestone = null }
    }
    if (pickingGame) {
        val copies by container.gameCopies.collectAsStateWithLifecycle(initialValue = emptyList())
        val progress by pokedexVm.copyProgress.collectAsStateWithLifecycle()
        GamePickerSheet(
            settings.game, onPick = { pokedexVm.setGame(it); pickingGame = false }, onDismiss = { pickingGame = false },
            copies = copies, currentCopy = active.copy?.id, onPickCopy = { pokedexVm.useCopy(it); pickingGame = false }, copyProgress = progress,
        )
    }
}

private fun NavHostController.holds(route: Any) = runCatching { getBackStackEntry(route) }.isSuccess

private fun NavHostController.navigateRoot(route: Any) {
    // Going to a screen already beneath this one is going back to it: navigating to it would restore the screens just
    // saved over it, leaving the trainer where they were. The Pokédex is the start screen, so it's always beneath.
    if (popBackStack(route, inclusive = false) || route == PokedexRoute) return
    navigate(route) {
        popUpTo(PokedexRoute) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** The game (or copy) the Pokédex is tracking, with a glow of its colour and its progress. Tap to choose another. */
@Composable
private fun TrackingCard(game: Game, copy: GameCopyEntity?, caught: Int, total: Int, onClick: () -> Unit) {
    val c = LocalDexColors.current
    val hue = remember(game) { Color(game.color).oklchHue() }
    val glow = if (game.isNational) c.textFaint else Color(game.color)
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier.fillMaxWidth().padding(bottom = 10.dp).clip(shape)
            .background(if (c.isDark) Color(0xFF121316) else c.surface)
            // A glow of the game's colour from the top-right corner.
            .glow(glow.copy(alpha = 0.3f), end = 0.6f) { Offset(it.width, 0f) to Size(it.width * 1.2f, it.height * 1.4f) }
            .border(1.dp, if (c.isDark) Color(0xFF222328) else c.line, shape)
            .clickable(onClick = onClick).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Overline(stringResource(R.string.tracking), color = if (game.isNational) c.textSubtle else if (c.isDark) oklch(0.78f, 0.09f, hue) else oklch(0.45f, 0.10f, hue))
        Text(gameName(game), color = c.text, style = DexType.itemTitle.copy(fontSize = 16.sp), maxLines = 1)
        Text(
            if (copy != null && copy.label.isNotBlank()) stringResource(R.string.tracking_line, copy.label, caught, total) else stringResource(R.string.tracking_caught, caught, total),
            color = c.textMuted, style = DexType.caption, maxLines = 1,
        )
        Bar(if (total == 0) 0f else caught.toFloat() / total, c.accent, Modifier.fillMaxWidth(), track = if (c.isDark) Color(0xFF222328) else c.track)
    }
}

/** How many of the menu's screens the tablet's rail shows: the trainer's first five. The rest are under More. */
private const val RAIL_SCREENS = 5

/**
 * The tablet's navigation rail: the oDex mark, the first menu screens, More (the full menu), and the last of [entries]
 * (Settings) at the foot.
 */
@Composable
private fun DexRail(entries: List<DrawerEntry>, isCurrent: (DrawerEntry) -> Boolean, onPick: (DrawerEntry) -> Unit, onMore: () -> Unit) {
    val c = LocalDexColors.current
    Column(
        Modifier.width(88.dp).fillMaxHeight().background(if (c.isDark) Color(0xFF09090B) else Color(0xFFECECE9))
            .drawBehind { drawLine(c.line, Offset(size.width, 0f), Offset(size.width, size.height), 1.dp.toPx()) }
            .statusBarsPadding().navigationBarsPadding().padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        OdexMark(30.dp, Modifier.padding(bottom = 12.dp), color = c.text)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            entries.dropLast(1).forEach { e -> RailItem(e.icon, stringResource(e.label), isCurrent(e)) { onPick(e) } }
            RailItem(Icons.Filled.MoreHoriz, stringResource(R.string.more), false, onMore)
        }
        val foot = entries.last()
        RailItem(foot.icon, stringResource(foot.label), isCurrent(foot)) { onPick(foot) }
    }
}

/** A rail entry: the icon in a pill (filled with the accent tint when it's the screen showing) over its name. */
@Composable
private fun RailItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val c = LocalDexColors.current
    val fg = if (selected) c.accentText else c.textSecondary
    Column(
        Modifier.width(80.dp).clip(RoundedCornerShape(12.dp)).selectable(selected, onClick = onClick, role = Role.Tab).padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            Modifier.size(56.dp, 32.dp).clip(RoundedCornerShape(16.dp)).background(if (selected) c.accentTint else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = fg, modifier = Modifier.size(22.dp)) }
        Text(
            label, color = fg, maxLines = 2, textAlign = TextAlign.Center,
            style = DexType.caption.copy(fontSize = 12.sp, lineHeight = 14.sp, fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold),
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun AppNavHost(
    container: AppContainer, dex: Dex, settings: Settings, nav: NavHostController, pokedexVm: PokedexViewModel, openDrawer: () -> Unit,
    pickGame: () -> Unit,
) {
    val artwork by container.openDexArtwork.collectAsStateWithLifecycle()
    val favorites by container.favorites.collectAsStateWithLifecycle()
    val caughtFlow = remember(settings.game) { container.caught(settings.game) }
    val caught by caughtFlow.collectAsStateWithLifecycle(initialValue = emptySet())
    val scope = rememberCoroutineScope()
    val setGame: (Game) -> Unit = { g -> scope.launch { container.settings.setGame(g) } }
    val back: () -> Unit = { nav.popBackStack() }
    val openPokemon: (Int) -> Unit = { nav.navigate(PokemonRoute(it)) }
    val openForm: (Int, Int) -> Unit = { species, form -> nav.navigate(PokemonRoute(species, form)) }
    fun detailNav(back: () -> Unit) = DetailNav(
        openPokemon = openPokemon,
        home = { pokedexVm.closePokemon(); nav.popBackStack(PokedexRoute, inclusive = false) },
        openMove = { nav.navigate(MoveRoute(it)) },
        openAbility = { nav.navigate(AbilityRoute(it)) },
        openLocation = { area, species -> nav.navigate(LocationRoute(area, species)) },
        openType = { nav.navigate(TypeRoute(it)) },
        openTeam = { nav.navigate(TeamRoute(it)) },
        back = back,
        openHunt = { nav.navigate(ShinyHuntRoute(it)) },
        openMap = { nav.navigate(FindOnMapRoute(it)) },
    )

    // Quick fades into a screen (Navigation's default is a slow 700 ms crossfade) and none going back, which is instant;
    // a Pokémon's page zooms in from the list while its artwork flies there. Predictive back is off in the manifest:
    // it played the fade along with the back swipe, so the old screen seemed to hang before it went.
    SharedTransitionLayout {
    CompositionLocalProvider(LocalSharedTransition provides this) {
    NavHost(
        nav, startDestination = PokedexRoute,
        enterTransition = { fadeIn(tween(220)) }, exitTransition = { fadeOut(tween(180)) },
        popEnterTransition = { EnterTransition.None }, popExitTransition = { ExitTransition.None },
    ) {
        screen<PokedexRoute>(wide = true) {
            // A Pokémon opened from the list shows over it while the list stays composed underneath, so going back
            // only has to animate (rebuilding the list took up to a second, with the old page frozen on top).
            val opened by pokedexVm.opened.collectAsStateWithLifecycle()
            // The page keeps showing its Pokémon while it closes.
            val shown = remember { IntArray(1) }
            opened?.let { shown[0] = it }
            BackHandler(enabled = opened != null) { pokedexVm.closePokemon() }
            val width = LocalWidthClass.current
            // A tablet opens the Pokémon in a panel beside the list. Picking another in the panel (an evolution) swaps
            // it there, and its menu offers the full-screen page.
            if (width.large) PokedexWithPanel(
                opened, pushes = width == WidthClass.EXPANDED,
                list = { selected ->
                    PokedexScreen(pokedexVm, artwork, openDrawer, pokedexVm::openPokemon,
                        onSearchAll = { nav.navigate(SearchRoute) }, onSettings = { nav.navigate(SettingsRoute) },
                        onOpenBoxes = { nav.navigate(CopyChecklistRoute(it, boxes = true)) }, selected = selected)
                },
                panel = { id ->
                    CompositionLocalProvider(LocalSharedTransition provides null) {
                        PokemonDetailScreen(container, dex, id, 0, settings, artwork, favorites, caught,
                            detailNav(back = pokedexVm::closePokemon).copy(openPokemon = pokedexVm::openPokemon, home = pokedexVm::closePokemon),
                            onFullScreen = { nav.navigate(PokemonRoute(id)) })
                    }
                },
            ) else Box(Modifier.fillMaxSize()) {
                CompositionLocalProvider(LocalArtworkShown provides (opened == null)) {
                    Box(if (opened != null) Modifier.clearAndSetSemantics {} else Modifier) {
                        PokedexScreen(pokedexVm, artwork, openDrawer, pokedexVm::openPokemon,
                            onSearchAll = { nav.navigate(SearchRoute) }, onSettings = { nav.navigate(SettingsRoute) },
                            onOpenBoxes = { nav.navigate(CopyChecklistRoute(it, boxes = true)) })
                    }
                }
                AnimatedVisibility(
                    visible = opened != null,
                    enter = fadeIn(tween(260)) + scaleIn(tween(320), initialScale = 0.92f),
                    exit = fadeOut(tween(160)) + scaleOut(tween(200), targetScale = 0.92f),
                ) {
                    CompositionLocalProvider(LocalScreenAnimation provides this, LocalArtworkShown provides null) {
                        // Touches stop here rather than reaching the list underneath.
                        Box(Modifier.fillMaxSize().pointerInput(Unit) {}) {
                            key(shown[0]) {
                                PokemonDetailScreen(container, dex, shown[0], 0, settings, artwork, favorites, caught,
                                    detailNav(back = pokedexVm::closePokemon))
                            }
                        }
                    }
                }
            }
        }
        screen<PokemonRoute>(
            enter = { fadeIn(tween(260)) + scaleIn(tween(320), initialScale = 0.92f) },
        ) { entry ->
            val route = entry.toRoute<PokemonRoute>()
            PokemonDetailScreen(container, dex, route.id, route.form, settings, artwork, favorites, caught, detailNav(back))
        }
        screen<MoveDexRoute> { MoveDexScreen(dex, settings.game, setGame, openDrawer) { nav.navigate(MoveRoute(it)) } }
        screen<MoveRoute> { entry ->
            MoveDetailScreen(dex, entry.toRoute<MoveRoute>().id, settings.game, artwork, favorites, back, openPokemon, container::toggleFavorite)
        }
        screen<AbilityDexRoute> { AbilityDexScreen(dex, settings.game, openDrawer) { nav.navigate(AbilityRoute(it)) } }
        screen<AbilityRoute> { entry -> AbilityDetailScreen(dex, entry.toRoute<AbilityRoute>().id, settings.game, artwork, back, openForm) }
        screen<ItemDexRoute> { ItemDexScreen(dex, settings.game, setGame, openDrawer) }
        screen<LocationDexRoute> { LocationDexScreen(dex, settings.game, setGame, openDrawer) { nav.navigate(LocationRoute(it)) } }
        screen<LocationRoute> { entry ->
            val route = entry.toRoute<LocationRoute>()
            LocationDetailScreen(dex, route.id, settings.game, artwork, route.highlight, back, openForm)
        }
        screen<TypeDexRoute> { TypeDexScreen(dex, settings.game, setGame, openDrawer) { nav.navigate(TypeRoute(it)) } }
        screen<TypeRoute> { entry ->
            TypeDetailScreen(dex, entry.toRoute<TypeRoute>().id, settings.game, artwork, back, openPokemon) { nav.navigate(TypeRoute(it)) }
        }
        screen<NatureDexRoute> { NatureDexScreen(dex, openDrawer) }
        screen<ChecklistRoute>(wide = true) {
            GlobalChecklistScreen(container, dex, settings.game, openDrawer, onOpenMap = { nav.navigate(RegionMapRoute) }) { c -> nav.navigate(CopyChecklistRoute(c.id)) }
        }
        screen<RegionMapRoute>(wide = true) { RegionMapScreen(container, dex, settings.game, back, openForm) }
        screen<FindOnMapRoute>(wide = true) { entry ->
            FindOnMapScreen(dex, settings.game, entry.toRoute<FindOnMapRoute>().species, back) { area, species -> nav.navigate(LocationRoute(area, species)) }
        }
        screen<CopyChecklistRoute>(wide = true) { entry ->
            val route = entry.toRoute<CopyChecklistRoute>()
            CopyChecklistScreen(container, dex, route.id, artwork, back, openPokemon, startOnBoxes = route.boxes)
        }
        screen<MyGamesRoute>(wide = true) {
            MyGamesScreen(container, dex, settings.game, openDrawer, onOpenBoxes = { nav.navigate(CopyChecklistRoute(it, boxes = true)) }) { c ->
                pokedexVm.useCopy(c)
                nav.navigateRoot(PokedexRoute)
            }
        }
        screen<CatchListsRoute>(wide = true) { CatchListsScreen(container, dex, settings.game, openDrawer) { nav.navigate(CatchListRoute(it)) } }
        screen<CatchListRoute> { entry -> CatchListScreen(container, dex, entry.toRoute<CatchListRoute>().id, artwork, back, openPokemon) }
        screen<ShinyDexRoute> { ShinyDexScreen(container, dex, settings.game, setGame, openDrawer, openForm) { nav.navigate(ShinyHuntRoute(it)) } }
        screen<ShinyHuntRoute> { entry -> ShinyHuntScreen(container, dex, entry.toRoute<ShinyHuntRoute>().id, back) }
        screen<EvolutionDexRoute> { EvolutionDexScreen(dex, settings.game, setGame, artwork, openDrawer, openPokemon) }
        screen<TeamsRoute>(wide = true) { TeamListScreen(container, dex, settings.game, artwork, openDrawer) { nav.navigate(TeamRoute(it)) } }
        screen<TeamRoute> { entry -> TeamEditorScreen(container, dex, entry.toRoute<TeamRoute>().id, artwork, back, openForm) }
        screen<SearchRoute> {
            SearchScreen(dex, settings.game, back) { hit ->
                when (hit.kind) {
                    SearchHit.Kind.POKEMON -> nav.navigate(PokemonRoute(hit.id))
                    SearchHit.Kind.MOVE -> nav.navigate(MoveRoute(hit.id))
                    SearchHit.Kind.ABILITY -> nav.navigate(AbilityRoute(hit.id))
                    SearchHit.Kind.ITEM -> nav.navigateRoot(ItemDexRoute)
                    SearchHit.Kind.NATURE -> nav.navigateRoot(NatureDexRoute)
                    SearchHit.Kind.LOCATION -> nav.navigate(LocationRoute(hit.id))
                }
            }
        }
        screen<SettingsRoute> { SettingsScreen(container, settings, back, pickGame, onMenu = { nav.navigate(MenuSettingsRoute) }, onSupport = { nav.navigate(SupportRoute) }, onAbout = { nav.navigate(AboutRoute) }) }
        screen<MenuSettingsRoute> { MenuSettingsScreen(container, settings, back) }
        screen<AboutRoute> { AboutScreen(back) }
        screen<SupportRoute> { SupportScreen(back) }
    }
    }
    }
}

/**
 * A destination whose content can share elements (the Pokémon artwork) with the screens it moves between. On a tablet
 * a phone-style screen is drawn centred at a readable width; [wide] screens (the Pokédex, the maps) use all of it.
 */
private inline fun <reified T : Any> NavGraphBuilder.screen(
    noinline enter: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition?)? = null,
    noinline popExit: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition?)? = null,
    wide: Boolean = false,
    noinline content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit,
) {
    composable<T>(enterTransition = enter, popExitTransition = popExit) { entry ->
        val scope = this
        CompositionLocalProvider(LocalScreenAnimation provides scope) {
            if (wide || !LocalWidthClass.current.large) content(entry)
            else Box(Modifier.fillMaxSize().background(LocalDexColors.current.bg), contentAlignment = Alignment.TopCenter) {
                Box(Modifier.widthIn(max = ReadableWidth).fillMaxHeight()) { scope.content(entry) }
            }
        }
    }
}
