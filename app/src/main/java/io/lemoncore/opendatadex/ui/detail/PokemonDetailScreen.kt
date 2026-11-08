package io.lemoncore.opendatadex.ui.detail

import android.media.AudioAttributes
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.graphics.GraphicsLayerScope
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.platform.LocalDensity
import io.lemoncore.opendatadex.ui.components.dexNumber
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.lemoncore.opendatadex.AppContainer
import io.lemoncore.opendatadex.R
import io.lemoncore.opendatadex.data.Dex
import io.lemoncore.opendatadex.data.Game
import io.lemoncore.opendatadex.data.Images
import io.lemoncore.opendatadex.data.Settings
import io.lemoncore.opendatadex.data.seed.SeedPokemon
import io.lemoncore.opendatadex.data.seed.SeedSpecies
import io.lemoncore.opendatadex.ui.components.FavoriteToggle
import io.lemoncore.opendatadex.ui.components.GameChip
import io.lemoncore.opendatadex.ui.components.NumberPill
import io.lemoncore.opendatadex.ui.components.PokemonArtwork
import io.lemoncore.opendatadex.ui.components.TypeTag
import io.lemoncore.opendatadex.ui.components.animatedWash
import io.lemoncore.opendatadex.ui.components.gameChipLabel
import io.lemoncore.opendatadex.ui.components.sharedArtwork
import io.lemoncore.opendatadex.ui.pokedex.GamePickerSheet
import io.lemoncore.opendatadex.ui.theme.DexType
import io.lemoncore.opendatadex.ui.theme.LocalDexColors
import io.lemoncore.opendatadex.ui.theme.WashTones
import io.lemoncore.opendatadex.ui.theme.halo
import io.lemoncore.opendatadex.ui.theme.headerWash
import kotlinx.coroutines.launch
import io.lemoncore.opendatadex.data.hasShinies
import java.util.Locale
import kotlin.math.abs

/** What a Pokémon screen can open. */
data class DetailNav(
    val openPokemon: (Int) -> Unit,
    val home: () -> Unit,
    val openMove: (Int) -> Unit,
    val openAbility: (Int) -> Unit,
    /** Opens a place (area id), pointing out a species there. */
    val openLocation: (Int, Int) -> Unit,
    val openType: (Int) -> Unit,
    val openTeam: (Long) -> Unit,
    val back: () -> Unit,
    /** Opens a shiny hunt's full-screen counter. */
    val openHunt: (Long) -> Unit = {},
    /** Lights up where a species lives on the game's region map. */
    val openMap: (Int) -> Unit = {},
)

/** The Pokémon screen's tabs: a text tab row under the header. */
private enum class DetailTab(@androidx.annotation.StringRes val label: Int) {
    INFO(R.string.tab_info), MOVES(R.string.section_moves), LOCATIONS(R.string.section_locations), GALLERY(R.string.tab_gallery), TEAMS(R.string.tab_teams)
}

/**
 * A Pokémon's screen. Swiping sideways steps through the list the trainer came from (the browse order, fixed when the
 * screen opens), with each page sliding in under the finger; the tab and game chosen carry over from page to page.
 */
@Composable
fun PokemonDetailScreen(
    container: AppContainer,
    dex: Dex,
    speciesId: Int,
    /** Pokémon (form) id to open on, or 0 for the form the game's dex shows. */
    initialForm: Int,
    settings: Settings,
    artwork: Map<Int, String>,
    favorites: Set<Int>,
    caught: Set<Int>,
    nav: DetailNav,
    /**
     * Set when the page is the tablet's side panel, beside the list: it opens the Pokémon full screen. The panel shows
     * one Pokémon (the list beside it picks another), and its back button closes it.
     */
    onFullScreen: (() -> Unit)? = null,
) {
    val order = remember(speciesId) {
        if (onFullScreen != null) listOf(speciesId) else container.browseOrder.value.takeIf { speciesId in it } ?: listOf(speciesId)
    }
    val pager = rememberPagerState(initialPage = order.indexOf(speciesId)) { order.size }
    val scope = rememberCoroutineScope()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    // The screen can look at another game than the list (the game chip in the header).
    var gameKey by rememberSaveable { mutableStateOf(settings.game.key) }
    val game = Game.fromKey(gameKey)
    val context = LocalContext.current

    val cry = remember { CryPlayer() }
    DisposableEffect(Unit) { onDispose { Thread { cry.release() }.start() } }
    LaunchedEffect(pager.settledPage) {
        container.lastViewed.value = order[pager.settledPage]
        if (settings.autoPlayCry) dex.speciesOf(order[pager.settledPage])?.let { s ->
            val p = dex.displayForm(s, game)
            cry.play(Images.cry(p.id, game), Images.latestCry(p.id))
        }
    }
    val tts = remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(settings.dataLanguage) {
        var engine: TextToSpeech? = null
        var disposed = false
        // Read entries in the data language; fall back to English when the device has no voice for it.
        engine = TextToSpeech(context) { status ->
            // An engine replaced (the data language changed) before it finished starting is never used.
            if (disposed) return@TextToSpeech
            if (status == TextToSpeech.SUCCESS) {
                val wanted = Locale.forLanguageTag(settings.dataLanguage.locale)
                val ok = engine?.setLanguage(wanted)?.let { it >= TextToSpeech.LANG_AVAILABLE } == true
                if (!ok) engine?.language = Locale.US
                tts.value = engine
            } else tts.value = null
        }
        // Shutting the speech engine down can take a moment; do it off the main thread so going back stays smooth.
        onDispose {
            disposed = true
            tts.value = null
            engine?.let { e -> Thread { e.stop(); e.shutdown() }.start() }
        }
    }
    var pickingGame by remember { mutableStateOf(false) }

    HorizontalPager(pager, key = { order[it] }, modifier = Modifier.fillMaxSize().background(LocalDexColors.current.bg)) { page ->
        val id = order[page]
        val s = dex.speciesOf(id) ?: return@HorizontalPager
        PokemonPage(
            container, dex, s, if (id == speciesId) initialForm else 0, game, settings, artwork, id in favorites, id in caught, nav,
            tab = tab, onTab = { tab = it }, onPickGame = { pickingGame = true },
            // How far this page is from the middle of the screen: -1 (left) to 1 (right), for the artwork's transition.
            pageOffset = { (pager.currentPage - page) + pager.currentPageOffsetFraction },
            onCry = { p -> cry.play(Images.cry(p.id, game), Images.latestCry(p.id)) },
            onSpeak = { text -> tts.value?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "dex-$id") },
            canSpeak = tts.value != null,
            onStep = if (order.size > 1) { step -> scope.launch { pager.animateScrollToPage((page + step).coerceIn(0, order.lastIndex)) } } else null,
            onFullScreen = onFullScreen,
        )
    }
    if (pickingGame) GamePickerSheet(game, onPick = { gameKey = it.key; pickingGame = false }, onDismiss = { pickingGame = false })
}

@Composable
private fun PokemonPage(
    container: AppContainer, dex: Dex, s: SeedSpecies, initialForm: Int, game: Game, settings: Settings, artwork: Map<Int, String>,
    favorite: Boolean, caught: Boolean, nav: DetailNav,
    tab: Int, onTab: (Int) -> Unit, onPickGame: () -> Unit, pageOffset: () -> Float,
    onCry: (SeedPokemon) -> Unit, onSpeak: (String) -> Unit, canSpeak: Boolean, onStep: ((Int) -> Unit)?, onFullScreen: (() -> Unit)?,
) {
    val c = LocalDexColors.current
    val forms = dex.formsIn(s, game)
    var formChoice by rememberSaveable { mutableIntStateOf(initialForm) }
    val p = forms.firstOrNull { it.id == formChoice } ?: dex.displayForm(s, game)
    val summary = dex.summary(s, game, p)
    val tones = animatedWash(p.id, s.color)
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val statusPx = WindowInsets.statusBars.getTop(density)
    // Height of the slim bar the header collapses into: the status bar plus 56 dp.
    val barPx = statusPx + with(density) { 56.dp.roundToPx() }
    // How far the header has scrolled, and how far it has collapsed into the bar: 0 at the top, 1 once the tab row
    // meets the bar. From then on the bar and a copy of the tab row stay at the top.
    val scrolled by remember {
        derivedStateOf { listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == "header" }?.let { -it.offset.toFloat() } }
    }
    val collapse by remember {
        derivedStateOf {
            val header = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == "header" }
            if (header == null) 1f else (-header.offset.toFloat() / (header.size - barPx).coerceAtLeast(1)).coerceIn(0f, 1f)
        }
    }
    val collapsed by remember { derivedStateOf { collapse >= 1f } }
    // On a new tab, keep the tab row pinned under the bar if it was; otherwise leave the page as it is.
    LaunchedEffect(tab) {
        if (collapsed) {
            listState.scrollToItem(1)
            listState.scrollBy(-barPx.toFloat())
        }
    }
    val name = if (p.isDefault) s.name else p.name
    val speak = { onSpeak(listOfNotNull(name, s.genus, dex.flavorText(s, game)?.first).joinToString(". ")) }
    // Where the header shows the name; the name itself is drawn by FloatingName, which carries it up into the bar.
    var page by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var nameSpot by remember { mutableStateOf<NameSpot?>(null) }
    // Fades for the header's other parts as it collapses (v4 fades them out early).
    val fadeEarly = { (1f - collapse * 2.2f).coerceIn(0f, 1f) }
    val huntScope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize().onGloballyPositioned { page = it }) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 40.dp)) {
            item(key = "header") {
                Column(Modifier.fillMaxWidth().headerWash(tones, c.bg).statusBarsPadding().padding(start = 4.dp, end = 4.dp, bottom = 16.dp)) {
                    TopRow(
                        game, nav, onPickGame, onCry = { onCry(p) }, onSpeak = speak.takeIf { canSpeak }, onStep = onStep, alpha = fadeEarly,
                        onHunt = if (game.hasShinies) ({ huntScope.launch { nav.openHunt(container.openHunt(game, s.id, p.id)) } }) else null,
                        onFullScreen = onFullScreen,
                    )
                    Identity(
                        name, s.genus, summary.dexNumber, summary.types.map { it to (dex.type(it)?.name ?: "") }, p.id, Images.artwork(p.id, artwork), s.color, tones,
                        pageOffset, favorite = favorite, caught = caught,
                        onFavorite = { container.toggleFavorite(s.id) },
                        onCaught = { container.setCaught(settings.game, s.id, !caught) },
                        fade = fadeEarly,
                        // The artwork drifts down against the scroll and fades as the header collapses.
                        artLayer = { scrolled?.let { translationY = it * 0.45f }; alpha *= (1f - collapse * 1.3f).coerceIn(0f, 1f) },
                        onNamePlaced = { text, slot -> page?.let { pg -> nameSpot = NameSpot(pg.localPositionOf(text, Offset.Zero), text.size.height, slot) } },
                        onArtworkTap = if (settings.playCry) ({ onCry(p) }) else null,
                    )
                    if (forms.size > 1) Box(Modifier.graphicsLayer { alpha = fadeEarly() }) { FormChips(s.name, forms, p.id) { formChoice = it } }
                }
            }
            item(key = "tabs") { DetailTabRow(tab, onTab) }
            item(key = "content") {
                // The new tab slides in from the side it sits on in the tab row.
                AnimatedContent(
                    targetState = tab,
                    transitionSpec = {
                        val dir = if (targetState > initialState) 1 else -1
                        (slideInHorizontally { it / 6 * dir } + fadeIn()) togetherWith (slideOutHorizontally { -it / 6 * dir } + fadeOut())
                    },
                    label = "tab",
                    modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 16.dp),
                ) { shown ->
                    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        when (DetailTab.entries[shown]) {
                            DetailTab.INFO -> InfoTab(dex, s, p, game, tones, artwork, nav, onCry = { onCry(p) }, onSpeak = speak, canSpeak = canSpeak)
                            DetailTab.MOVES -> MovesTab(dex, s, p, game, nav, onPickGame = { onPickGame() })
                            DetailTab.LOCATIONS -> LocationsTab(dex, s, game, nav)
                            DetailTab.GALLERY -> GalleryTab(container, dex, s, p, game, tones, artwork, nav.openHunt)
                            DetailTab.TEAMS -> TeamsTab(container, dex, s, p, game, artwork, nav)
                        }
                    }
                }
            }
        }
        var numberWidth by remember { mutableIntStateOf(0) }
        Column {
            CollapseBar(
                summary.dexNumber, name, p.id, Images.artwork(p.id, artwork), tones, { collapse }, nav.back,
                closes = onFullScreen != null,
                onNumberWidth = { numberWidth = it },
                onTap = if (collapsed) ({ scope.launch { listState.animateScrollToItem(0) } }) else null,
            )
            if (collapsed) DetailTabRow(tab, onTab)
        }
        val spot = nameSpot
        val pageWidth = page?.size?.width ?: 0
        if (pageWidth > 0 && (spot != null || collapse >= 1f)) FloatingName(name, spot, { collapse }, statusPx, numberWidth, pageWidth)
    }
}

/** Where the header lays out the name: its top-left in the page, its height, and the width it may take. */
private data class NameSpot(val at: Offset, val height: Int, val slot: Int)

/**
 * The name, carried from the header up into the bar as the header collapses (as dataDex 4 does): it scrolls with the
 * page until it reaches the bar, then stays there, sliding across to sit after the number and shrinking to the bar's size.
 */
@Composable
private fun FloatingName(name: String, spot: NameSpot?, collapse: () -> Float, statusPx: Int, numberWidth: Int, pageWidth: Int) {
    val c = LocalDexColors.current
    val density = LocalDensity.current
    val f = collapse()
    // The name stays in line with the header at first, then slides across and reaches its place in the bar before the
    // number fades in beside it.
    val t = ((f - 0.15f) / 0.6f).coerceIn(0f, 1f)
    val across = t * t * (3f - 2f * t)
    with(density) {
        // In the bar: after the back button and the number, before the small artwork.
        val barX = 56.dp.toPx() + numberWidth + 10.dp.toPx()
        val barWidth = pageWidth - barX - 64.dp.toPx()
        val barCenter = statusPx + 28.dp.toPx()
        val boxHeight = androidx.compose.ui.util.lerp(spot?.height?.toFloat() ?: 0f, 28.dp.toPx(), f)
        val x = if (spot == null) barX else androidx.compose.ui.util.lerp(spot.at.x, barX, across)
        val centre = if (spot == null) barCenter else maxOf(spot.at.y + spot.height / 2f, barCenter)
        val width = if (spot == null) barWidth else androidx.compose.ui.util.lerp(spot.slot.toFloat(), barWidth, across)
        val maxSize = androidx.compose.ui.util.lerp(30f, 18f, f)
        Box(
            Modifier.offset { IntOffset(x.roundToInt(), (centre - boxHeight / 2).roundToInt()) }
                .size(width.coerceAtLeast(1f).toDp(), boxHeight.coerceAtLeast(1f).toDp()),
            contentAlignment = Alignment.CenterStart,
        ) {
            BasicText(
                name, maxLines = 1, style = DexType.detailName.copy(color = c.text, lineHeight = androidx.compose.ui.unit.TextUnit.Unspecified),
                autoSize = TextAutoSize.StepBased(minFontSize = 14.sp, maxFontSize = maxSize.sp),
            )
        }
    }
}

/**
 * The bar the header collapses into. The back button is always here; the bar's colour, the number and a small round
 * artwork fade in as the header collapses. [onTap] (once collapsed) scrolls back to the top. In the tablet's side
 * panel ([closes]) the back button is a close button.
 */
@Composable
private fun CollapseBar(
    number: Int, name: String, artworkId: Int, artworkUrl: String, tones: WashTones, collapse: () -> Float, onBack: () -> Unit,
    closes: Boolean, onNumberWidth: (Int) -> Unit, onTap: (() -> Unit)?,
) {
    val c = LocalDexColors.current
    // The number and the small artwork arrive over the last part of the collapse.
    val late = { ((collapse() - 0.7f) / 0.3f).coerceIn(0f, 1f) }
    Box(Modifier.fillMaxWidth().then(if (onTap != null) Modifier.clickable(onClick = onTap) else Modifier)) {
        Box(Modifier.matchParentSize().graphicsLayer { alpha = collapse() }.background(tones.headerTop))
        Row(Modifier.statusBarsPadding().fillMaxWidth().height(56.dp).padding(start = 4.dp, end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                if (closes) Icon(Icons.Filled.Close, stringResource(R.string.close), tint = c.text, modifier = Modifier.size(22.dp))
                else Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), tint = c.text, modifier = Modifier.size(22.dp))
            }
            Text(
                dexNumber(number), color = c.textMuted, style = DexType.monoValue,
                modifier = Modifier.padding(start = 4.dp).onSizeChanged { onNumberWidth(it.width) }.graphicsLayer { alpha = late() },
            )
            Spacer(Modifier.weight(1f))
            Box(
                Modifier.size(44.dp).graphicsLayer { alpha = late(); scaleX = 0.6f + 0.4f * late(); scaleY = scaleX }
                    .clip(CircleShape).background(c.bg.copy(alpha = 0.35f)).halo(tones),
                contentAlignment = Alignment.Center,
            ) { PokemonArtwork(artworkId, artworkUrl, Modifier.size(38.dp), name) }
        }
    }
}

/** The game the screen shows (tap to look at another game) and a menu; the back button lives in the bar above. */
@Composable
private fun TopRow(
    game: Game, nav: DetailNav, onPickGame: () -> Unit, onCry: () -> Unit, onSpeak: (() -> Unit)?, onStep: ((Int) -> Unit)?, alpha: () -> Float,
    onHunt: (() -> Unit)? = null, onFullScreen: (() -> Unit)? = null,
) {
    val c = LocalDexColors.current
    var menu by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().height(56.dp).graphicsLayer { this.alpha = alpha() }, verticalAlignment = Alignment.CenterVertically) {
        Spacer(Modifier.weight(1f))
        GameChip(
            game, gameChipLabel(game), onPickGame, Modifier.padding(end = 4.dp).widthIn(max = 220.dp),
            background = if (c.isDark) Color(0xFF0B0B0D).copy(alpha = 0.7f) else Color.White.copy(alpha = 0.8f),
        )
        Box {
            IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, stringResource(R.string.more), tint = c.textSecondary, modifier = Modifier.size(21.dp)) }
            DropdownMenu(menu, { menu = false }) {
                if (onFullScreen != null) DropdownMenuItem({ Text(stringResource(R.string.open_full_screen)) }, { menu = false; onFullScreen() })
                DropdownMenuItem({ Text(stringResource(R.string.cry)) }, { menu = false; onCry() })
                if (onHunt != null) DropdownMenuItem({ Text(stringResource(R.string.start_shiny_hunt)) }, { menu = false; onHunt() })
                if (onSpeak != null) DropdownMenuItem({ Text(stringResource(R.string.read_aloud)) }, { menu = false; onSpeak() })
                if (onStep != null) {
                    DropdownMenuItem({ Text(stringResource(R.string.previous)) }, { menu = false; onStep(-1) })
                    DropdownMenuItem({ Text(stringResource(R.string.next)) }, { menu = false; onStep(1) })
                }
                DropdownMenuItem({ Text(stringResource(R.string.nav_pokedex)) }, { menu = false; nav.home() })
            }
        }
    }
}

/**
 * Number pill, name and favourite, genus and types on the left; the artwork on its halo on the right. The name is only
 * laid out here (FloatingName draws it); [onNamePlaced] reports where, with the width the name may take.
 */
@Composable
private fun Identity(
    name: String, genus: String, number: Int, types: List<Pair<Int, String>>, artworkId: Int, artworkUrl: String, colorId: Int, tones: WashTones,
    pageOffset: () -> Float, favorite: Boolean, caught: Boolean, onFavorite: () -> Unit, onCaught: () -> Unit,
    fade: () -> Float, artLayer: GraphicsLayerScope.() -> Unit, onNamePlaced: (LayoutCoordinates, Int) -> Unit,
    /** Tapping the artwork plays the cry (Settings → Feel); null when that is off. */
    onArtworkTap: (() -> Unit)? = null,
) {
    val c = LocalDexColors.current
    val star = with(LocalDensity.current) { 44.dp.roundToPx() }
    var slot by remember { mutableIntStateOf(0) }
    Row(Modifier.fillMaxWidth().heightIn(min = 200.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberPill(
                number, caught, onCaught, large = true, modifier = Modifier.graphicsLayer { alpha = fade() },
                offBackground = if (c.isDark) Color(0xFF0B0B0D).copy(alpha = 0.6f) else Color.White.copy(alpha = 0.75f),
            )
            Row(Modifier.onSizeChanged { slot = it.width - star }, verticalAlignment = Alignment.CenterVertically) {
                BasicText(
                    name, maxLines = 1, style = DexType.detailName.copy(color = Color.Transparent),
                    autoSize = TextAutoSize.StepBased(minFontSize = 18.sp, maxFontSize = 30.sp),
                    modifier = Modifier.weight(1f, fill = false).onGloballyPositioned { onNamePlaced(it, slot) },
                )
                Box(Modifier.graphicsLayer { alpha = fade() }) { FavoriteToggle(favorite, c.textMuted, onFavorite, size = 20.dp) }
            }
            Column(Modifier.graphicsLayer { alpha = fade() }, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (genus.isNotEmpty()) Text(genus, color = if (c.isDark) Color(0xFFB4B5BD) else c.textMuted, style = DexType.body, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { types.forEach { (id, n) -> TypeTag(id, n, large = true) } }
            }
        }
        Box(
            Modifier.size(190.dp).graphicsLayer(artLayer)
                .then(if (onArtworkTap != null) Modifier.clickable(interactionSource = null, indication = null, onClick = onArtworkTap) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.fillMaxSize().halo(tones, detail = true))
            // While swiping, the artwork shrinks, fades and lags behind its page, so the next Pokémon grows into place.
            Crossfade(artworkId to artworkUrl, animationSpec = tween(200), label = "art") { (id, url) ->
                PokemonArtwork(id, url, Modifier.sharedArtwork(id).size(170.dp).graphicsLayer {
                    val o = pageOffset().coerceIn(-1f, 1f)
                    val a = abs(o)
                    scaleX = 1f - 0.35f * a
                    scaleY = 1f - 0.35f * a
                    alpha = 1f - 0.6f * a
                    translationX = -o * size.width * 0.5f
                }, name, colorId = colorId)
            }
        }
    }
}

/** One chip per form that exists in the game: "Charizard", "Mega X", "Mega Y". */
@Composable
private fun FormChips(speciesName: String, forms: List<SeedPokemon>, selected: Int, onPick: (Int) -> Unit) {
    val c = LocalDexColors.current
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(start = 12.dp, end = 12.dp, top = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        forms.forEach { f ->
            val full = if (f.isDefault) f.form.ifEmpty { speciesName } else f.form.ifEmpty { f.name }
            // "Mega Charizard X" reads as "Mega X" next to "Charizard".
            val label = if (full.startsWith("Mega $speciesName")) "Mega" + full.removePrefix("Mega $speciesName") else full
            val on = f.id == selected
            val bg = if (on) c.accent.copy(alpha = if (c.isDark) 0.14f else 0.10f) else if (c.isDark) Color(0xFF0F1013).copy(alpha = 0.7f) else Color.White.copy(alpha = 0.7f)
            Box(
                Modifier.height(32.dp).clip(RoundedCornerShape(10.dp)).background(bg)
                    .border(1.dp, if (on) c.accent else c.lineStrong, RoundedCornerShape(10.dp)).clickable { onPick(f.id) }.padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center,
            ) { Text(label, color = if (on) c.accentText else if (c.isDark) c.textMuted else Color(0xFF4A4B52), style = DexType.chip, maxLines = 1) }
        }
    }
}

/** Five equal tabs; the chosen one is underlined in red across its width. */
@Composable
private fun DetailTabRow(tab: Int, onTab: (Int) -> Unit) {
    val c = LocalDexColors.current
    Column(Modifier.fillMaxWidth().background(c.bg)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp)) {
            DetailTab.entries.forEachIndexed { i, t ->
                val on = tab == i
                val fg by animateColorAsState(if (on) c.text else c.textFaint, tween(200), label = "tab")
                val line by animateColorAsState(if (on) c.accent else Color.Transparent, tween(200), label = "tabLine")
                Box(Modifier.weight(1f).height(46.dp).clickable { onTab(i) }, contentAlignment = Alignment.Center) {
                    // A long label ("Locations", "Ubicaciones") shrinks only as far as it must to stay on one line.
                    BasicText(
                        stringResource(t.label), maxLines = 1, style = DexType.tab.copy(color = fg),
                        autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = 13.sp), modifier = Modifier.padding(horizontal = 2.dp),
                    )
                    Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(2.dp).background(line))
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.divider))
    }
}

/** Streams a cry; one at a time. Falls back to [fallback] when the first URL can't be played. */
class CryPlayer {
    private var player: MediaPlayer? = null

    fun play(url: String, fallback: String? = null) {
        release()
        player = MediaPlayer().apply {
            setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            setOnPreparedListener { it.start() }
            setOnErrorListener { _, _, _ ->
                if (fallback != null && fallback != url) play(fallback)
                true
            }
            runCatching { setDataSource(url); prepareAsync() }
        }
    }

    fun release() {
        player?.runCatching { reset(); release() }
        player = null
    }
}

@Composable
internal fun GameNote(text: String) {
    Text(text, color = LocalDexColors.current.textSubtle, style = DexType.body.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium),
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
}
