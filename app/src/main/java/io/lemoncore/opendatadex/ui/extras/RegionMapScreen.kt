package io.lemoncore.opendatadex.ui.extras

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.PathParser
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.lemoncore.opendatadex.AppContainer
import io.lemoncore.opendatadex.R
import io.lemoncore.opendatadex.data.Dex
import io.lemoncore.opendatadex.data.Encounter
import io.lemoncore.opendatadex.data.Game
import io.lemoncore.opendatadex.data.Images
import io.lemoncore.opendatadex.data.MapNode
import io.lemoncore.opendatadex.data.RegionMap
import io.lemoncore.opendatadex.data.RegionMaps
import io.lemoncore.opendatadex.data.encountersOn
import io.lemoncore.opendatadex.ui.components.Bar
import io.lemoncore.opendatadex.ui.components.CaughtToggle
import io.lemoncore.opendatadex.ui.components.EmptyNote
import io.lemoncore.opendatadex.ui.components.LocalSettings
import io.lemoncore.opendatadex.ui.components.LocalWidthClass
import io.lemoncore.opendatadex.ui.components.WidthClass
import io.lemoncore.opendatadex.ui.components.PokemonArtwork
import io.lemoncore.opendatadex.ui.components.SegmentedButtons
import io.lemoncore.opendatadex.ui.components.gameName
import io.lemoncore.opendatadex.ui.components.levelRange
import io.lemoncore.opendatadex.ui.settings.SettingsScaffold
import io.lemoncore.opendatadex.ui.theme.DexColors
import io.lemoncore.opendatadex.ui.theme.DexType
import io.lemoncore.opendatadex.ui.theme.LocalDexColors
import io.lemoncore.opendatadex.ui.theme.Manrope
import io.lemoncore.opendatadex.ui.theme.TypeColors
import io.lemoncore.opendatadex.ui.theme.oklch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

/** The map's own colours: dark ones from the design, and light ones made to read the same way on a light page. */
private class MapColors(c: DexColors, hue: Float) {
    val dark = c.isDark
    val sea = if (dark) oklch(0.15f, 0.028f, 235f) else oklch(0.93f, 0.03f, 235f)
    val land = if (dark) oklch(0.205f, 0.02f, hue) else oklch(0.995f, 0.008f, hue)
    val road = if (dark) Color(0xFF4A4C54) else Color(0xFFC4C6CC)
    val seaRoad = if (dark) oklch(0.55f, 0.07f, 235f) else oklch(0.62f, 0.08f, 235f)
    val track = if (dark) Color(0xFF3D3F46) else Color(0xFF9FA1A9)
    val ground = c.bg
    val town = if (dark) Color(0xFFC9CAD1) else Color(0xFF71717A)
    val city = if (dark) Color(0xFFEEEEF0) else Color(0xFF27272A)
    val selectRing = if (dark) Color(0xFFEEEEF0) else Color(0xFF18181B)
    val cityLabel = if (dark) Color(0xFFC9CAD1) else Color(0xFF3F3F46)
    val townLabel = if (dark) Color(0xFF8D8E96) else Color(0xFF71717A)
    val strongLabel = if (dark) Color.White else Color(0xFF09090B)
    val dimRoad = if (dark) Color(0xFF2C2E34) else Color(0xFFE4E4E7)
    val dimTown = if (dark) Color(0xFF55575F) else Color(0xFFA1A1AA)
    val dimLabel = if (dark) Color(0xFF6D6F77) else Color(0xFFA1A1AA)
    val labelShadow = if (dark) Color.Black else Color.White
    val accent = c.accent
}

/** How the map draws one spot: a completion ring, a highlight (Find), dimmed, or plain. */
private sealed interface Look {
    /** [progress] is null where there's no encounter data, so the spot shows no ring rather than 0%. */
    data class Ring(val progress: Float?) : Look
    data object Plain : Look
    data object Lit : Look
    data object Dim : Look
}

/**
 * The selected game's region, with a ring on every route and landmark that fills as you catch what lives there. Tap a
 * spot to list its Pokémon, missing ones first, with the Poké Ball toggle. The national view picks the region by chip.
 */
@Composable
fun RegionMapScreen(container: AppContainer, dex: Dex, game: Game, onBack: () -> Unit, openForm: (Int, Int) -> Unit) {
    val c = LocalDexColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pages = RegionMaps.pagesOf(game)
    var region by rememberSaveable(game) { mutableStateOf(pages.firstOrNull() ?: RegionMaps.regions.first()) }
    val map = remember(region, game) { RegionMaps.load(context, region).forGame(game) }
    val hasData = dex.hasEncounterData(game)
    val spots by produceState<Map<String, List<Encounter>>?>(null, map, game) {
        value = withContext(Dispatchers.Default) { dex.encountersOn(map, game) }
    }
    val caughtFlow = remember(game) { container.caught(game) }
    val caught by caughtFlow.collectAsStateWithLifecycle(initialValue = emptySet())
    val keyFlow = remember(game) { container.checklistKey(game) }
    val key by keyFlow.collectAsStateWithLifecycle(initialValue = null)
    // Species per spot (each once, whatever the method), in National Dex order.
    val species = remember(spots) { spots?.mapValues { (_, list) -> list.distinctBy { it.speciesId }.sortedBy { it.speciesId } }.orEmpty() }
    val ringed = map.nodes.filter { it.ringed && species[it.id].orEmpty().isNotEmpty() }
    val done = ringed.count { n -> species.getValue(n.id).all { it.speciesId in caught } }
    // Until a spot is tapped, the first route with Pokémon left to catch is shown (or the first one with any).
    var tapped by rememberSaveable(region) { mutableStateOf<String?>(null) }
    val selected = tapped ?: (ringed.firstOrNull { n -> species.getValue(n.id).any { it.speciesId !in caught } } ?: ringed.firstOrNull())?.id

    SettingsScaffold(map.name, onBack, subtitle = if (game.isNational) stringResource(R.string.all_games) else gameName(game), trailing = {
        if (hasData && ringed.isNotEmpty()) Text(
            stringResource(R.string.map_areas_done, done, ringed.size).uppercase(), color = c.accentText,
            style = DexType.monoMeta, modifier = Modifier.padding(end = 16.dp),
        )
    }) {
        SideBySide(
            map = {
                RegionPicker(pages, region) { region = it }
                RegionMapView(
                    map, Modifier.padding(top = 4.dp),
                    look = { n ->
                        val list = species[n.id].orEmpty()
                        when {
                            !n.ringed -> Look.Plain
                            !hasData || list.isEmpty() -> Look.Ring(null)
                            else -> Look.Ring(list.count { it.speciesId in caught }.toFloat() / list.size)
                        }
                    },
                    selected = selected, lit = null,
                    showLabel = { n, near -> n.id == selected || (!n.ringed && !near(selected, n)) },
                    onTap = { tapped = it },
                )
                Legend()
            },
            details = {
                val node = map.nodes.firstOrNull { it.id == selected }
                if (node != null) SpotCard(node, species[node.id].orEmpty(), caught, hasData, game, dex, openForm) { id, on ->
                    val k = key ?: return@SpotCard
                    scope.launch { container.setCaughtIn(k, id, on) }
                }
                dex.encountersBorrowedFrom(game)?.let {
                    EmptyNote(stringResource(R.string.locations_borrowed, gameName(game), gameName(it)), Modifier.padding(start = 24.dp, end = 24.dp, bottom = 16.dp))
                }
            },
        )
    }
}

/** The map with what goes under it on a phone; a tablet in landscape puts that beside the map instead. */
@Composable
private fun SideBySide(map: @Composable ColumnScope.() -> Unit, details: @Composable ColumnScope.() -> Unit) {
    if (LocalWidthClass.current != WidthClass.EXPANDED) {
        Column(content = map)
        Column(content = details)
        return
    }
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f), content = map)
        Column(Modifier.width(380.dp), content = details)
    }
}

/**
 * Where [speciesId] lives on the selected game's map: every spot glows in its type colour and the rest dims, with a list
 * of places underneath (method, levels and rate). Places the map doesn't draw are listed after them.
 */
@Composable
fun FindOnMapScreen(dex: Dex, game: Game, speciesId: Int, onBack: () -> Unit, openLocation: (Int, Int) -> Unit) {
    val c = LocalDexColors.current
    val context = LocalContext.current
    val s = dex.speciesOf(speciesId) ?: return
    val found = remember(s, game) { dex.encountersOf(s, game) }
    // Opens on the map where it's found most (Kanto or the Sevii Islands in FireRed and LeafGreen, any region in the
    // national view).
    val pages = RegionMaps.pagesOf(game)
    var region by rememberSaveable(game, speciesId) {
        mutableStateOf(pages.ifEmpty { RegionMaps.regions }.maxBy { r ->
            val ids = RegionMaps.load(context, r).forGame(game).nodes.flatMap { it.locations }.toSet()
            found.count { dex.area(it.areaId)?.location in ids }
        })
    }
    val map = remember(region, game) { RegionMaps.load(context, region).forGame(game) }
    val summary = remember(s, game) { dex.summary(s, game) }
    val (typeHue, k) = TypeColors.hueOf(summary.types.firstOrNull() ?: 1)
    val lit = if (c.isDark) oklch(0.74f, 0.15f * k.coerceAtLeast(0.35f), typeHue) else oklch(0.6f, 0.16f * k.coerceAtLeast(0.35f), typeHue)
    val byNode = remember(map, found) {
        map.nodes.associate { n -> n.id to found.filter { dex.area(it.areaId)?.location in n.locations } }.filterValues { it.isNotEmpty() }
    }
    val elsewhere = remember(map, found) {
        val onMap = map.nodes.flatMap { it.locations }.toSet()
        found.filter { dex.area(it.areaId)?.location !in onMap }
    }

    SettingsScaffold(stringResource(R.string.map_where_to_find), onBack, trailing = {
        Text(if (game.isNational) stringResource(R.string.all_games) else gameName(game), color = c.accentText, style = DexType.monoMeta,
            modifier = Modifier.padding(end = 16.dp))
    }) {
        SideBySide(
            map = {
                RegionPicker(pages, region) { region = it }
                RegionMapView(
                    map, Modifier.padding(top = 4.dp),
                    look = { n -> if (n.id in byNode) Look.Lit else Look.Dim },
                    selected = null, lit = lit,
                    showLabel = { n, near -> n.id in byNode || (n.kind == MapNode.CITY && byNode.keys.none { near(it, n) }) },
                    onTap = null,
                )
            },
            details = { FoundPlaces(game, speciesId, summary.name, map, byNode, elsewhere, lit, openLocation) },
        )
    }
}

/** The places [byNode] (on the map) and [elsewhere] (off it) where the Pokémon is found, each with how and how often. */
@Composable
private fun FoundPlaces(
    game: Game, speciesId: Int, name: String, map: RegionMap, byNode: Map<String, List<Encounter>>, elsewhere: List<Encounter>,
    lit: Color, openLocation: (Int, Int) -> Unit,
) {
    val c = LocalDexColors.current
    Column {
        Row(Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 6.dp), verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(name, color = c.text, style = DexType.itemTitle.copy(fontSize = 16.sp))
            val where = if (game.isNational) map.name else gameName(game)
            Text(
                if (byNode.size == 1) stringResource(R.string.map_one_place, where) else stringResource(R.string.map_places, byNode.size, where),
                color = c.textSubtle, style = DexType.caption.copy(fontWeight = FontWeight.Medium),
            )
        }
        if (byNode.isEmpty() && elsewhere.isEmpty()) {
            EmptyNote(stringResource(R.string.map_none_here, name), Modifier.padding(horizontal = 24.dp))
            return@Column
        }
        val onMap = byNode.flatMap { (id, list) -> placeRows(map.nodes.first { it.id == id }.name, list, lit) }
        val offMap = elsewhere.groupBy { it.areaId }.values.flatMap { list -> placeRows(list.first().place, list, c.textFaint) }
        val shape = RoundedCornerShape(18.dp)
        Column(Modifier.padding(horizontal = 12.dp).fillMaxWidth().clip(shape).background(c.chip).border(1.dp, c.line, shape)) {
            (onMap + offMap).forEachIndexed { i, r ->
                if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(c.line.copy(alpha = 0.6f)))
                if (i == onMap.size) {
                    Text(stringResource(R.string.map_not_on_map).uppercase(), color = c.textSubtle, style = DexType.overline,
                        modifier = Modifier.padding(start = 16.dp, top = 12.dp))
                }
                Row(
                    Modifier.fillMaxWidth().clickable { openLocation(r.areaId, speciesId) }.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(r.dot))
                    Column(Modifier.weight(1f)) {
                        Text(r.name, color = c.text, style = DexType.itemTitle.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold))
                        Text(r.detail, color = c.textSubtle, style = DexType.caption.copy(fontWeight = FontWeight.Medium))
                    }
                    Text(r.rate, color = if (r.dot == lit) lit else c.textMuted, style = DexType.monoValue.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

private data class PlaceRow(val name: String, val detail: String, val rate: String, val dot: Color, val areaId: Int)

/** One row per way of finding it at a place: "Old Rod · Lv. 5–10 · 50%"; gifts and one-off encounters have no rate. */
@Composable
private fun placeRows(name: String, list: List<Encounter>, dot: Color): List<PlaceRow> =
    list.groupBy { it.method }.map { (method, es) ->
        val rate = es.maxOf { it.rarity }
        val oneOff = es.first().methodId in 18..20 || es.first().methodId == 36
        PlaceRow(name, "$method · ${levelRange(es.minOf { it.minLevel }, es.maxOf { it.maxLevel })}", if (oneOff || rate <= 0) "—" else "$rate%", dot, es.first().areaId)
    }

/** Region chips in the national view, a Kanto / Sevii Islands switch in FireRed and LeafGreen, and nothing otherwise. */
@Composable
private fun RegionPicker(pages: List<String>, region: String, onPick: (String) -> Unit) {
    val context = LocalContext.current
    when {
        pages.isEmpty() -> RegionChips(region, onPick)
        pages.size > 1 -> SegmentedButtons(
            pages.map { RegionMaps.load(context, it).name }, pages.indexOf(region), { onPick(pages[it]) },
            Modifier.padding(start = 12.dp, end = 12.dp, bottom = 8.dp),
        )
    }
}

@Composable
private fun RegionChips(region: String, onPick: (String) -> Unit) {
    val c = LocalDexColors.current
    val context = LocalContext.current
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(start = 12.dp, end = 12.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        RegionMaps.regions.forEach { r ->
            val on = r == region
            val shape = RoundedCornerShape(18.dp)
            Box(
                Modifier.height(36.dp).clip(shape).background(if (on) c.accentTint else c.chip).border(1.dp, if (on) c.accent.copy(alpha = 0.5f) else c.line, shape)
                    .clickable { onPick(r) }.padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center,
            ) { Text(RegionMaps.load(context, r).name, color = if (on) c.accentText else c.textSecondary, style = DexType.chip) }
        }
    }
}

@Composable
private fun Legend() {
    val c = LocalDexColors.current
    val m = MapColors(c, 0f)
    val style = DexType.caption.copy(fontSize = 11.sp)
    @Composable
    fun Item(label: String, swatch: Modifier) = Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(swatch)
        Text(label, color = c.textMuted, style = style)
    }
    Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Item(stringResource(R.string.map_legend_done), Modifier.size(11.dp).clip(CircleShape).background(c.accent))
        Item(stringResource(R.string.map_legend_some_left), Modifier.size(11.dp).drawBehind {
            drawCircle(m.track)
            drawArc(c.accent, -90f, 144f, useCenter = true)
        })
        Item(stringResource(R.string.map_legend_town), Modifier.size(9.dp).clip(RoundedCornerShape(2.dp)).background(m.town))
        Item(stringResource(R.string.map_legend_sea), Modifier.width(12.dp).height(2.dp).drawBehind {
            drawLine(m.seaRoad, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), size.height,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 2.dp.toPx())))
        })
    }
}

/** The card under the map for the tapped spot: what kind it is, how many you've caught and its Pokémon. */
@Composable
private fun SpotCard(
    node: MapNode, list: List<Encounter>, caught: Set<Int>, hasData: Boolean, game: Game, dex: Dex,
    openForm: (Int, Int) -> Unit, setCaught: (Int, Boolean) -> Unit,
) {
    val c = LocalDexColors.current
    val shape = RoundedCornerShape(20.dp)
    val count = list.count { it.speciesId in caught }
    Column(
        Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 24.dp).fillMaxWidth().clip(shape).background(c.chip).border(1.dp, c.line, shape).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                val kind = when {
                    node.kind == MapNode.ROUTE && node.sea -> R.string.map_legend_sea
                    node.kind == MapNode.ROUTE -> R.string.map_kind_route
                    node.kind == MapNode.TOWN -> R.string.map_legend_town
                    node.kind == MapNode.CITY -> R.string.map_kind_city
                    else -> R.string.map_kind_landmark
                }
                Text(stringResource(kind).uppercase(), color = c.accentText, style = DexType.overline)
                Text(node.name, color = c.text, style = DexType.sheetTitle)
            }
            if (list.isNotEmpty()) Text("$count / ${list.size}", color = c.textSecondary, style = DexType.monoValue.copy(fontWeight = FontWeight.Bold))
        }
        when {
            !hasData -> Text(stringResource(R.string.locations_no_data, gameName(game)), color = c.textMuted, style = DexType.body.copy(fontSize = 13.sp))
            list.isEmpty() -> Text(stringResource(R.string.map_town_empty), color = c.textMuted, style = DexType.body.copy(fontSize = 13.sp))
            else -> {
                Bar(count.toFloat() / list.size, c.accent, Modifier.fillMaxWidth())
                // Missing first, so what's left to catch is at the top.
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    list.sortedBy { it.speciesId in caught }.forEach { e ->
                        val on = e.speciesId in caught
                        val name = dex.summaryOfForm(e.pokemonId, game)?.name ?: dex.speciesOf(e.speciesId)?.name.orEmpty()
                        Row(
                            Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(12.dp)).clickable { openForm(e.speciesId, e.pokemonId) },
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(Modifier.size(44.dp).clip(CircleShape).background(if (on) c.accentTint else if (c.isDark) Color(0xFF16171B) else c.surface3),
                                contentAlignment = Alignment.Center) {
                                // Pixel sprites have a wide empty border, so they're drawn a little larger than the disc.
                                PokemonArtwork(e.pokemonId, Images.pixelSprite(e.pokemonId),
                                    Modifier.size(52.dp).graphicsLayer { scaleX = 1.3f; scaleY = 1.3f }.alpha(if (on) 1f else 0.55f), name)
                            }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(name, color = c.text, style = DexType.itemTitle.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold), maxLines = 1)
                                Text(stringResource(if (on) R.string.filter_caught else R.string.filter_missing), color = if (on) c.accentText else c.textMuted,
                                    style = DexType.caption.copy(fontWeight = FontWeight.Medium))
                            }
                            CaughtToggle(on, c.textMuted, { setCaught(e.speciesId, !on) }, size = 24.dp)
                        }
                    }
                }
            }
        }
    }
}

/**
 * The map itself: land, routes and spots drawn on one canvas, with pinch to zoom (one finger still scrolls the page
 * until you've zoomed in). Spots and labels keep their size when zoomed, so zooming spreads crowded areas apart.
 * [showLabel] gets a helper saying whether two spots are close enough for one label to cover the other.
 */
@Composable
private fun RegionMapView(
    map: RegionMap, modifier: Modifier, look: (MapNode) -> Look, selected: String?, lit: Color?,
    showLabel: (MapNode, near: (String?, MapNode) -> Boolean) -> Boolean, onTap: ((String) -> Unit)?,
) {
    val c = LocalDexColors.current
    val settings = LocalSettings.current
    val hue = if (settings.tintFromGame && !settings.game.isNational) settings.game.hue else 25f
    val m = MapColors(c, hue)
    val land = remember(map) { PathParser.createPathFromPathData(map.land).asComposePath() }
    val byId = remember(map) { map.nodes.associateBy { it.id } }
    val measurer = rememberTextMeasurer()
    var scale by remember(map) { mutableFloatStateOf(1f) }
    var offset by remember(map) { mutableStateOf(Offset.Zero) }
    val shape = RoundedCornerShape(20.dp)

    Canvas(
        modifier.padding(horizontal = 12.dp).fillMaxWidth().aspectRatio(map.width / map.height).clip(shape).border(1.dp, c.line, shape)
            .pointerInput(map) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val fingers = event.changes.count { it.pressed }
                        // The event that lifts the last finger has no centre to zoom around (it came out as NaN and
                        // moved the map off the canvas, leaving it blank), so only events with a finger down count.
                        val centroid = event.calculateCentroid(useCurrent = false)
                        val zoom = event.calculateZoom()
                        if (fingers > 0 && (fingers >= 2 || scale > 1f) && centroid.isSpecified && zoom.isFinite() && zoom > 0f) {
                            val next = (scale * zoom).coerceIn(1f, 3f)
                            val moved = centroid - (centroid - offset) * (next / scale) + event.calculatePan()
                            if (moved.isValid()) {
                                offset = Offset(moved.x.coerceIn(size.width * (1 - next), 0f), moved.y.coerceIn(size.height * (1 - next), 0f))
                                scale = next
                            }
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
            .then(if (onTap == null) Modifier else Modifier.pointerInput(map, scale, offset) {
                detectTapGestures { tap ->
                    val u = size.width / map.width * scale
                    val hit = map.nodes.minByOrNull { (offset + Offset(it.x * u, it.y * u) - tap).getDistance() } ?: return@detectTapGestures
                    if ((offset + Offset(hit.x * u, hit.y * u) - tap).getDistance() <= 22.dp.toPx()) onTap(hit.id)
                }
            }),
    ) {
        val base = size.width / map.width
        val u = base * scale
        fun at(n: MapNode) = offset + Offset(n.x * u, n.y * u)
        drawRect(m.sea)
        withTransform({ translate(offset.x, offset.y); scale(u, u, pivot = Offset.Zero) }) {
            drawPath(land, m.land)
            drawPath(land, m.land, style = Stroke(3f, join = StrokeJoin.Round))
        }
        map.edges.forEach { e ->
            val a = byId[e.from] ?: return@forEach
            val b = byId[e.to] ?: return@forEach
            // Lines are straight or bend once at a right angle, never diagonal.
            val from = at(a)
            val to = at(b)
            val line = Path().apply {
                moveTo(from.x, from.y)
                if (abs(b.x - a.x) >= 1.5f && abs(b.y - a.y) >= 1.5f) {
                    if (e.acrossFirst(b.x - a.x, b.y - a.y)) lineTo(to.x, from.y) else lineTo(from.x, to.y)
                }
                lineTo(to.x, to.y)
            }
            val road = if (lit != null) m.dimRoad else m.road
            if (e.isSea) drawPath(line, m.seaRoad, style = Stroke(0.5f * base * scale.coerceAtMost(1.6f),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(1.2f * base, 1.2f * base))))
            else drawPath(line, road, style = Stroke(0.6f * base * scale.coerceAtMost(1.6f), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        // Lit spots go on top, and the selected one above everything.
        map.nodes.sortedBy { (if (it.id == selected) 2 else 0) + (if (look(it) == Look.Lit) 1 else 0) }.forEach { n ->
            drawNode(n, at(n), look(n), n.id == selected, m, lit)
        }
        // Labels: two spots are "near" when one's label would sit on the other (distances in map units, so zooming in
        // makes room for more labels).
        val selLen = selected?.let { byId[it] }?.let { if (it.kind == MapNode.ROUTE) 3 else it.name.length } ?: 0
        val near: (String?, MapNode) -> Boolean = { id, n ->
            val o = id?.let { byId[it] }
            o != null && o.id != n.id && if (lit != null) abs(o.x - n.x) * scale < 13 && abs(o.y - n.y) * scale < 7
            else abs(o.x - n.x) * scale < (selLen * 1.6f + n.name.length * 1.3f) / 2 + 5 && abs(o.y - n.y) * scale < 8
        }
        val placed = mutableListOf<Rect>()
        // Strong labels (selected or lit) are placed first so a town's name never pushes them aside.
        map.nodes.filter { showLabel(it, near) }.sortedByDescending { it.id == selected || (lit != null && look(it) == Look.Lit) }.forEach { n ->
            val strong = n.id == selected || (lit != null && look(n) == Look.Lit)
            val name = if (strong && n.kind == MapNode.ROUTE) n.name.removePrefix("Route ") else n.name
            val (sizeSp, weight, color) = when {
                strong -> Triple(10.sp, FontWeight.ExtraBold, m.strongLabel)
                lit != null -> Triple(8.5.sp, FontWeight.SemiBold, m.dimLabel)
                n.kind == MapNode.CITY -> Triple(9.sp, FontWeight.Bold, m.cityLabel)
                else -> Triple(8.5.sp, FontWeight.SemiBold, m.townLabel)
            }
            drawLabel(measurer, name, n, at(n), sizeSp, weight, color, m.labelShadow, placed, strong)
        }
    }
}

private fun DrawScope.drawNode(n: MapNode, center: Offset, look: Look, selected: Boolean, m: MapColors, lit: Color?) {
    val side = when {
        look == Look.Lit -> 14.dp
        n.kind == MapNode.CITY -> 12.dp
        n.kind == MapNode.TOWN -> 9.dp
        n.kind == MapNode.LANDMARK -> 10.dp
        else -> 11.dp
    }.toPx()
    if (look == Look.Lit && lit != null) {
        drawCircle(Brush.radialGradient(listOf(lit.copy(alpha = 0.7f), lit.copy(alpha = 0f)), center, side / 2 + 16.dp.toPx()), side / 2 + 16.dp.toPx(), center)
        drawCircle(lit.copy(alpha = 0.28f), side / 2 + 4.dp.toPx(), center)
        drawCircle(lit, side / 2, center)
        return
    }
    if (selected) {
        shape(n, center, side, 5.dp.toPx(), m.selectRing)
        shape(n, center, side, 3.dp.toPx(), m.ground)
    }
    when (look) {
        Look.Dim -> shape(n, center, side, 0f, if (n.ringed) m.dimRoad else m.dimTown)
        Look.Plain -> shape(n, center, side, 0f, if (n.kind == MapNode.CITY) m.city else m.town)
        is Look.Ring -> {
            val p = look.progress
            when {
                p == null -> shape(n, center, side * 0.7f, 0f, m.track)
                p >= 1f -> shape(n, center, side, 0f, m.accent)
                else -> {
                    // The ring fills clockwise from 12 o'clock, with a hole in the middle.
                    shape(n, center, side, 0f, m.track, clipArc = p, arcColor = m.accent)
                    drawCircle(m.ground, 2.4.dp.toPx(), center)
                }
            }
        }
        Look.Lit -> Unit
    }
}

/** A spot's shape grown by [grow]: a circle for routes, a rounded square for towns and cities, a diamond for landmarks. */
private fun DrawScope.shape(n: MapNode, center: Offset, side: Float, grow: Float, color: Color, clipArc: Float? = null, arcColor: Color = color) {
    val s = side + grow * 2
    val topLeft = center - Offset(s / 2, s / 2)
    fun fill() {
        if (clipArc == null) {
            drawRect(color, topLeft, Size(s, s))
        } else {
            drawRect(color, topLeft, Size(s, s))
            drawArc(arcColor, -90f, 360f * clipArc, useCenter = true, topLeft = center - Offset(s, s), size = Size(s * 2, s * 2))
        }
    }
    when (n.kind) {
        MapNode.ROUTE -> {
            drawCircle(color, s / 2, center)
            if (clipArc != null) drawArc(arcColor, -90f, 360f * clipArc, useCenter = true, topLeft = topLeft, size = Size(s, s))
        }
        MapNode.LANDMARK -> {
            val path = Path().apply {
                addRoundRect(androidx.compose.ui.geometry.RoundRect(topLeft.x, topLeft.y, topLeft.x + s, topLeft.y + s, CornerRadius(2.dp.toPx() + grow)))
            }
            rotate(45f, center) { clipPath(path) { rotate(-45f, center) { fill() } } }
        }
        else -> {
            val r = (if (n.kind == MapNode.CITY) 3.dp else 2.dp).toPx() + grow
            drawRoundRect(color, topLeft, Size(s, s), CornerRadius(r))
        }
    }
}

/**
 * A spot's name: under it, or beside it near the bottom edge and where the map asks, nudged in from the sides. When
 * that would cover a label already in [placed], it tries the other side, then above; a town's name that still
 * doesn't fit is left out, while a selected or lit one is drawn anyway.
 */
private fun DrawScope.drawLabel(
    measurer: androidx.compose.ui.text.TextMeasurer, text: String, n: MapNode, center: Offset, size: TextUnit, weight: FontWeight,
    color: Color, shadow: Color, placed: MutableList<Rect>, strong: Boolean,
) {
    val layout = measurer.measure(text, TextStyle(fontFamily = Manrope, fontSize = size, fontWeight = weight, color = color,
        shadow = Shadow(shadow, Offset(0f, 1.dp.toPx()), 3.dp.toPx())), softWrap = false)
    val w = layout.size.width.toFloat()
    val h = layout.size.height.toFloat()
    val gap = 10.dp.toPx()
    val left = Offset(center.x - gap - w, center.y - h / 2)
    val right = Offset(center.x + gap, center.y - h / 2)
    val below = Offset(center.x - w * (if (n.x < 10) 0.18f else if (n.x > 88) 0.82f else 0.5f), center.y + 7.dp.toPx())
    val above = Offset(below.x, center.y - 7.dp.toPx() - h)
    val beside = n.y > 72 || n.labelSide == "right"
    val tries = when {
        beside && n.x > 88 -> listOf(left, above, below)
        beside -> listOf(right, above, below)
        n.x > 88 -> listOf(below, left, above)
        else -> listOf(below, right, above, left)
    }
    val pad = 2.dp.toPx()
    fun box(o: Offset) = Rect(o.x - pad, o.y - pad, o.x + w + pad, o.y + h + pad)
    val topLeft = tries.firstOrNull { o -> placed.none { it.overlaps(box(o)) } } ?: if (strong) tries.first() else return
    placed += box(topLeft)
    translate(topLeft.x, topLeft.y) { drawText(layout) }
}
