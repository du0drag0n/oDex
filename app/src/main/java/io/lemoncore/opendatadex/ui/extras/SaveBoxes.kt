package io.lemoncore.opendatadex.ui.extras

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import io.lemoncore.opendatadex.R
import io.lemoncore.opendatadex.data.Dex
import io.lemoncore.opendatadex.data.Game
import io.lemoncore.opendatadex.data.Images
import io.lemoncore.opendatadex.data.seed.SeedPokemon
import io.lemoncore.opendatadex.data.seed.SeedSpecies
import io.lemoncore.opendatadex.data.PokemonSummary
import io.lemoncore.opendatadex.data.save.MET_EGG
import io.lemoncore.opendatadex.data.save.MET_FATEFUL
import io.lemoncore.opendatadex.data.save.MET_TRADE
import io.lemoncore.opendatadex.data.save.HIDDEN_ABILITY_SLOT
import io.lemoncore.opendatadex.data.save.ORIGIN_COLOSSEUM
import io.lemoncore.opendatadex.data.save.ORIGIN_GO
import io.lemoncore.opendatadex.data.save.SAVE_BALLS
import io.lemoncore.opendatadex.data.save.consoleDate
import io.lemoncore.opendatadex.data.save.genderOf
import io.lemoncore.opendatadex.data.save.levelAt
import io.lemoncore.opendatadex.data.save.metLocalDate
import io.lemoncore.opendatadex.data.save.saveOrigin
import io.lemoncore.opendatadex.data.user.SaveImportEntity
import io.lemoncore.opendatadex.data.user.SavePokemonEntity
import io.lemoncore.opendatadex.ui.components.EmptyNote
import io.lemoncore.opendatadex.ui.components.LocalWidthClass
import io.lemoncore.opendatadex.ui.components.PokemonArtwork
import io.lemoncore.opendatadex.ui.components.TypeMark
import io.lemoncore.opendatadex.ui.components.dexNumber
import io.lemoncore.opendatadex.ui.components.gameName
import io.lemoncore.opendatadex.ui.theme.LocalDexColors
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Date
import kotlin.math.floor

/**
 * A game copy's imported save: its party and PC boxes laid out as the game shows them, swiped or picked box by box,
 * then the copy's Pokédex entries that aren't in any of them. Tapping a Pokémon shows what the save knows about it.
 * [all] is the copy's Pokédex and [caught] its checklist.
 */
@Composable
fun SaveBoxesView(
    dex: Dex, game: Game, import: SaveImportEntity, pokemon: List<SavePokemonEntity>, all: List<PokemonSummary>, caught: Set<Int>,
    onOpenPokemon: (Int) -> Unit,
) {
    val c = LocalDexColors.current
    // Japanese Gold, Silver and Crystal boxes hold 30 like every later game's.
    val boxSize = if (game.dataGen <= 2 && pokemon.none { it.language == 1 }) 20 else 30
    val byBox = remember(pokemon) { pokemon.groupBy { it.box } }
    val names = listOf(stringResource(R.string.party)) + import.boxNameList
    val missingPage = names.size
    val missing = remember(all, pokemon) {
        val held = pokemon.filter { !it.egg }.mapTo(HashSet()) { it.speciesId }
        all.filter { it.id !in held }
    }
    val pages = names + stringResource(R.string.missing_from_boxes)
    val pager = rememberPagerState { pages.size }
    val chips = rememberLazyListState()
    val scope = rememberCoroutineScope()
    // A tablet shows two pages at once, so the last it can start from is the second last.
    val perView = LocalWidthClass.current.cardsPerRow
    val lastStart = pages.size - perView
    val previous = @Composable {
        IconButton(onClick = { scope.launch { pager.animateScrollToPage(if (pager.currentPage <= 0) lastStart else pager.currentPage - 1) } }) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.back))
        }
    }
    val next = @Composable {
        IconButton(onClick = { scope.launch { pager.animateScrollToPage(if (pager.currentPage >= lastStart) 0 else pager.currentPage + 1) } }) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
        }
    }
    val pageTitle = @Composable { page: Int, modifier: Modifier ->
        Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(pages[page].ifBlank { stringResource(R.string.box_title, page) }, fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                if (page == missingPage) stringResource(R.string.missing_count, missing.size)
                else stringResource(R.string.box_count, byBox[page].orEmpty().size, if (page == 0) 6 else boxSize),
                color = c.subtleText, fontSize = 13.sp,
            )
        }
    }
    var open by remember { mutableStateOf<SavePokemonEntity?>(null) }
    val locale = LocalConfiguration.current.locales[0]
    val date = remember(import.importedAt, locale) { DateFormat.getDateInstance(DateFormat.MEDIUM, locale).format(Date(import.importedAt)) }
    LaunchedEffect(pager.currentPage) { chips.animateScrollToItem((pager.currentPage - 1).coerceAtLeast(0)) }

    val headerScroll = rememberScrollState()

    BoxWithConstraints(Modifier.fillMaxSize()) {
        // Too short for the header and a box together, as on a phone on its side, so the header scrolls away.
        val short = maxHeight < ShortView
        val viewHeight = maxHeight
        Column(Modifier.fillMaxSize().then(if (short) Modifier.verticalScroll(headerScroll) else Modifier).padding(vertical = 12.dp)) {
            Text(
                stringResource(R.string.save_summary, pokemon.size, pokemon.filter { !it.egg }.map { it.speciesId }.distinct().size, date),
                color = c.subtleText, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 16.dp),
            )
            TrainerStats(game, import, Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp))
            Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                if (perView > 1) previous()
                LazyRow(
                    state = chips, contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    itemsIndexed(pages) { i, name ->
                        val on = i in pager.currentPage until pager.currentPage + perView
                        Text(
                            name.ifBlank { stringResource(R.string.box_title, i) }, fontSize = 14.sp, maxLines = 1,
                            color = if (on) c.segmentOnText else c.chipText,
                            modifier = Modifier.clip(RoundedCornerShape(50)).background(if (on) c.segmentOn else c.chip)
                                .clickable { scope.launch { pager.animateScrollToPage(i) } }.padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
                if (perView > 1) next()
            }
            if (perView == 1) Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                previous()
                pageTitle(pager.currentPage, Modifier.weight(1f))
                next()
            }
            HorizontalPager(
                pager, pageSize = if (perView > 1) TwoPages else PageSize.Fill, pageSpacing = 12.dp, contentPadding = PaddingValues(horizontal = 12.dp),
                verticalAlignment = Alignment.Top,
                modifier = Modifier.padding(top = if (perView > 1) 8.dp else 0.dp).then(if (short) Modifier.height(viewHeight) else Modifier.weight(1f)),
            ) { page ->
                val here = byBox[page].orEmpty().associateBy { it.slot }
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (perView > 1) pageTitle(page, Modifier.fillMaxWidth())
                    BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
                        val room = DpSize(maxWidth, maxHeight)
                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                            if (page == missingPage) MissingPage(game, missing, caught, onOpenPokemon)
                            else if (page == 0) PartyGrid(dex, game, here) { open = it }
                            else BoxGrid(dex, game, here, boxSize, room) { open = it }
                        }
                    }
                }
            }
        }
    }
    open?.let { SavedPokemonSheet(dex, game, it, onDismiss = { open = null }) { id -> open = null; onOpenPokemon(id) } }
}

/** The trainer card's numbers, as far as the save records them. */
@Composable
private fun TrainerStats(game: Game, import: SaveImportEntity, modifier: Modifier) {
    val c = LocalDexColors.current
    val started = import.startedAt?.let { saveDateText(consoleDate(it)) }
    val stats = listOfNotNull(
        import.badges?.takeIf { game.badgeTotal > 0 }?.let { stringResource(R.string.badges) to "${it.countOneBits()} / ${game.badgeTotal}" },
        stringResource(R.string.time_played) to stringResource(R.string.played_time, import.playMinutes / 60, import.playMinutes % 60),
        started?.let { stringResource(R.string.adventure_started) to it },
    )
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        stats.forEach { (label, value) ->
            Column {
                Text(label, color = c.subtleText, fontSize = 12.sp, maxLines = 1)
                Text(value, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
        }
    }
}

/** A date from a save, which comes from the console's clock and so has no time zone to convert. */
@Composable
private fun saveDateText(date: LocalDate): String {
    val locale = LocalConfiguration.current.locales[0]
    return remember(date, locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale).format(date) }
}

/** Two pages side by side, for a tablet. */
private object TwoPages : PageSize {
    override fun Density.calculateMainAxisPageSize(availableSpace: Int, pageSpacing: Int) = (availableSpace - pageSpacing) / 2
}

/** Below this the view can't hold the header and a box at once. */
private val ShortView = 420.dp

/** The smallest a box's slot gets to fit the screen, so its Pokémon stay easy to tap. */
private val SmallestSlot = 44.dp

/** A PC box laid out as the game does, its slots as large as fit [room] so that all of its rows show at once. */
@Composable
private fun BoxGrid(dex: Dex, game: Game, here: Map<Int, SavePokemonEntity>, boxSize: Int, room: DpSize, onOpen: (SavePokemonEntity) -> Unit) {
    val c = LocalDexColors.current
    val columns = if (boxSize == 30) 6 else 5
    val gap = 4.dp
    val inset = 8.dp
    val across = (room.width - inset * 2 - gap * (columns - 1)) / columns
    val down = (room.height - inset * 2 - gap * (boxSize / columns - 1)) / (boxSize / columns)
    val slot = floor(max(min(across, down), min(across, SmallestSlot)).value).dp
    Column(
        Modifier.width(slot * columns + gap * (columns - 1) + inset * 2).clip(RoundedCornerShape(16.dp)).background(c.sectionCard)
            .border(1.dp, c.divider, RoundedCornerShape(16.dp)).padding(inset),
        verticalArrangement = Arrangement.spacedBy(gap),
    ) {
        (0 until boxSize).chunked(columns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                row.forEach { i -> Slot(dex, game, here[i], Modifier.weight(1f), onOpen = onOpen) }
            }
        }
    }
}

/**
 * The copy's Pokédex entries in neither the party nor a box: ones the Pokédex has caught first (traded or released
 * since, or evolved), then ones never caught. Tapping one opens it in the Pokédex.
 */
@Composable
private fun MissingPage(game: Game, missing: List<PokemonSummary>, caught: Set<Int>, onOpenPokemon: (Int) -> Unit) {
    val c = LocalDexColors.current
    if (missing.isEmpty()) {
        EmptyNote(stringResource(R.string.missing_none), Modifier.padding(24.dp))
        return
    }
    val (had, never) = missing.partition { it.id in caught }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        listOf(R.string.missing_caught to had, R.string.missing_never to never).filter { it.second.isNotEmpty() }.forEach { (title, list) ->
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.sectionCard).border(1.dp, c.divider, RoundedCornerShape(16.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(stringResource(title, list.size), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(4.dp))
                list.chunked(6).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        row.forEach { p ->
                            Column(
                                Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(c.chip).clickable { onOpenPokemon(p.id) }.padding(bottom = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                PokemonArtwork(p.id, Images.gameSprite(p.id, game) ?: Images.homeSprite(p.id), Modifier.fillMaxWidth().aspectRatio(1f).padding(2.dp), p.name)
                                Text(dexNumber(p.dexNumber), color = c.subtleText, fontSize = 11.sp, maxLines = 1)
                            }
                        }
                        // Keep a short last row's tiles the same size as the rest.
                        repeat(6 - row.size) { Box(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

/** The party: six larger slots with each Pokémon's name and level. */
@Composable
private fun PartyGrid(dex: Dex, game: Game, party: Map<Int, SavePokemonEntity>, onOpen: (SavePokemonEntity) -> Unit) {
    val c = LocalDexColors.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        (0 until 6).chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { slot ->
                    val mon = party[slot]
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(c.sectionCard).border(1.dp, c.divider, RoundedCornerShape(14.dp))
                            .then(if (mon != null) Modifier.clickable { onOpen(mon) } else Modifier).padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Slot(dex, game, mon, Modifier.widthIn(max = 112.dp).fillMaxWidth(0.8f), framed = false) { onOpen(it) }
                        Text(mon?.let { displayName(dex, it) } ?: " ", fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(mon?.takeIf { !it.egg }?.let { stringResource(R.string.level, levelOf(dex, it)) } ?: " ", color = c.subtleText, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun Slot(dex: Dex, game: Game, mon: SavePokemonEntity?, modifier: Modifier, framed: Boolean = true, onOpen: (SavePokemonEntity) -> Unit) {
    val c = LocalDexColors.current
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier.aspectRatio(1f).then(
            if (framed) Modifier.clip(shape).background(if (mon != null) c.chip else c.sectionCard).border(1.dp, c.divider, shape) else Modifier,
        ).then(if (mon != null) Modifier.clickable { onOpen(mon) } else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (mon == null) return@Box
        if (mon.egg) EggMark(Modifier.fillMaxSize(0.5f))
        else PokemonArtwork(mon.speciesId, spriteOf(dex, mon, game), Modifier.fillMaxSize().padding(2.dp), displayName(dex, mon))
        if (mon.shiny && !mon.egg) Icon(Icons.Filled.AutoAwesome, stringResource(R.string.sprite_shiny), tint = c.gold,
            modifier = Modifier.align(Alignment.TopEnd).padding(2.dp).size(12.dp))
    }
}

/** A plain egg, as the boxes show one. */
@Composable
private fun EggMark(modifier: Modifier) {
    Box(modifier.aspectRatio(0.8f).clip(RoundedCornerShape(50)).background(Color(0xFFF3EBD3)).border(1.dp, Color(0xFFB9AE8C), RoundedCornerShape(50))) {
        Box(Modifier.align(Alignment.Center).padding(bottom = 8.dp).size(6.dp).clip(CircleShape).background(Color(0xFF7CB342)))
    }
}

/** Let's Go's version group, then Brilliant Diamond and Shining Pearl's. */
private val OWN_PLACE_NUMBERS = setOf(19, 23)

private const val PIKACHU = 25
private const val ARCEUS = 493
private const val KYUREM = 646
private const val GRENINJA = 658
private const val VIVILLON = 666
private const val FLOETTE = 670
private const val ZYGARDE = 718
private const val MINIOR = 774

/** Vivillon's form numbers run through the patterns in order, with the Meadow pattern oDex shows by default among them. */
private const val VIVILLON_MEADOW = 6

/** Generations IV and V number Arceus's types with ???, at 9, among them; oDex lists it last. */
private const val ARCEUS_UNKNOWN = 9

/** Original to Alola Cap and Partner Cap Pikachu, from Sun and Moon on. */
private val PIKACHU_CAPS = listOf(10094, 10095, 10096, 10097, 10098, 10099, 10148)

/** The first of Omega Ruby and Alpha Sapphire's six Cosplay Pikachu, which have no HOME render. */
private const val PIKACHU_COSPLAY = 10080

/** Species with form numbers that don't line up with oDex's forms; [spriteOf] handles Pikachu, and the rest look like their default. */
private val UNORDERED_FORMS = setOf(PIKACHU, GRENINJA, MINIOR)

/** The key of the [SeedSpecies.cosmetic] look a form number means, or null for the default look or one that isn't cosmetic. */
private fun cosmeticKey(species: SeedSpecies, form: Int, generation: Int): String? {
    val index = when {
        species.id == VIVILLON -> if (form == VIVILLON_MEADOW) return null else if (form < VIVILLON_MEADOW) form else form - 1
        form == 0 -> return null
        species.id == ARCEUS && generation <= 5 && form == ARCEUS_UNKNOWN -> species.cosmetic.lastIndex
        species.id == ARCEUS && generation <= 5 && form > ARCEUS_UNKNOWN -> form - 2
        else -> form - 1
    }
    return species.cosmetic.getOrNull(index)?.key
}

/** The alternate form a saved Pokémon is in, or null for its default one or a form that only changes its looks. */
private fun formOf(dex: Dex, mon: SavePokemonEntity, generation: Int): SeedPokemon? {
    if (mon.form == 0 || mon.speciesId in UNORDERED_FORMS) return null
    val species = dex.speciesOf(mon.speciesId) ?: return null
    if (cosmeticKey(species, mon.form, generation) != null) return null
    // Mega Evolutions and Gigantamax forms never sit in a save, and oDex lists them among the forms that do.
    val forms = species.forms.mapNotNull { dex.pokemon(it) }.filter { !it.mega && !it.gmax }
    return when (mon.speciesId) {
        // The games number White Kyurem before Black; oDex lists Black first.
        KYUREM -> forms.getOrNull(3 - mon.form)
        // 10% Forme, with or without Power Construct. The rest are 50% Forme until they change in battle.
        ZYGARDE -> forms.firstOrNull { it.key == "zygarde-10" }?.takeIf { mon.form <= 2 }
        // The Eternal Flower comes after the four other colours.
        FLOETTE -> forms.getOrNull(1)
        else -> forms.getOrNull(mon.form)
    }
}

/** The first generation whose game sprites are animated and run to megabytes each, far too much for a box of 30. */
private const val ANIMATED_SPRITES_GEN = 7

/** The game's own sprite; HOME's render for anything else, since nearly every look has one. */
private fun spriteOf(dex: Dex, mon: SavePokemonEntity, game: Game): String {
    val species = dex.speciesOf(mon.speciesId)
    val female = mon.gender == 1 && species?.genderDiff == true
    return when {
        mon.speciesId == PIKACHU && mon.form > 0 && game.dataGen == 6 -> Images.pixelSprite(PIKACHU_COSPLAY + mon.form - 1)
        mon.speciesId == PIKACHU && mon.form > 0 -> PIKACHU_CAPS.getOrNull(mon.form - 1)?.let { Images.homeSprite(it, mon.shiny) }
        else -> species?.let { cosmeticKey(it, mon.form, game.dataGen) }?.let { Images.cosmeticForm(mon.speciesId, it, mon.shiny) }
            ?: formOf(dex, mon, game.dataGen)?.let { Images.homeSprite(it.id, mon.shiny) }
            ?: Images.homeSprite(mon.speciesId, mon.shiny, female = true).takeIf { female }
    } ?: Images.gameSprite(mon.speciesId, game, mon.shiny)?.takeIf { game.dataGen < ANIMATED_SPRITES_GEN } ?: Images.homeSprite(mon.speciesId, mon.shiny)
}

@Composable
private fun displayName(dex: Dex, mon: SavePokemonEntity) =
    if (mon.egg) stringResource(R.string.method_egg) else mon.nickname.ifBlank { dex.speciesOf(mon.speciesId)?.name.orEmpty() }

private fun levelOf(dex: Dex, mon: SavePokemonEntity): Int =
    levelAt(mon.exp, dex.speciesOf(mon.speciesId)?.let { dex.growthRate(it)?.exp100 } ?: 1_000_000)

/** PokéAPI stat ids in the order the games build natures from: Attack, Defense, Speed, Sp. Atk, Sp. Def. */
private val NATURE_STATS = listOf(2, 3, 6, 4, 5)

/** The times of day Crystal notes a catch at, from 1. */
private val MET_TIMES = listOf(R.string.time_morning, R.string.time_day, R.string.time_night)

/** Everything the save knows about one Pokémon. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SavedPokemonSheet(dex: Dex, game: Game, mon: SavePokemonEntity, onDismiss: () -> Unit, onOpenPokemon: (Int) -> Unit) {
    val c = LocalDexColors.current
    val species = dex.speciesOf(mon.speciesId)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = c.bg) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(96.dp).clip(RoundedCornerShape(20.dp)).background(c.chip), contentAlignment = Alignment.Center) {
                    if (mon.egg) EggMark(Modifier.size(44.dp))
                    else PokemonArtwork(mon.speciesId, spriteOf(dex, mon, game), Modifier.size(88.dp), species?.name)
                }
                Column(Modifier.padding(start = 16.dp).weight(1f)) {
                    Text(displayName(dex, mon), fontSize = 22.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (species != null && !mon.egg) Text(dexNumber(species.id) + "  " + species.name, color = c.subtleText, fontSize = 14.sp)
                    if (!mon.egg) Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                        Text(stringResource(R.string.level, levelOf(dex, mon)), fontSize = 16.sp)
                        when (if (mon.gender >= 0) mon.gender.takeIf { it < 2 }?.let { it == 1 } else species?.takeIf { game.dataGen >= 2 }?.let { genderOf(mon.pid, it.genderRate) }) {
                            true -> Text("  ♀", color = Color(0xFFE8508A), fontSize = 16.sp)
                            false -> Text("  ♂", color = Color(0xFF3D8BE8), fontSize = 16.sp)
                            null -> {}
                        }
                        if (mon.shiny) {
                            Icon(Icons.Filled.AutoAwesome, null, tint = c.gold, modifier = Modifier.padding(start = 10.dp).size(16.dp))
                            Text(" " + stringResource(R.string.sprite_shiny), color = c.goldText, fontSize = 14.sp)
                        }
                    }
                }
            }
            if (!mon.egg) {
                val generation = game.dataGen
                // Natures, abilities, balls and the game of origin all start with Generation III.
                val modern = generation >= 3
                val n = mon.natureIndex
                val nature = dex.natures.firstOrNull { it.up == NATURE_STATS[n / 5] && it.down == NATURE_STATS[n % 5] }.takeIf { modern }
                val form = species?.let { formOf(dex, mon, generation) ?: dex.defaultForm(it) }
                val ability = form?.takeIf { modern }?.let { f ->
                    val (hidden, normal) = dex.abilitiesOf(f, game).partition { it.hidden }
                    if (mon.abilitySlot == HIDDEN_ABILITY_SLOT) hidden.firstOrNull() else normal.getOrNull(mon.abilitySlot) ?: normal.firstOrNull()
                }
                val ballKey = SAVE_BALLS.getOrNull(mon.ball - 1)
                val heldKey = dex.saveItemKey(generation, mon.heldItem)
                val originGame = saveOrigin(mon.originGame)
                val origin = originGame?.let { gameName(it) } ?: when (mon.originGame) {
                    ORIGIN_COLOSSEUM -> stringResource(R.string.game_colosseum_xd)
                    ORIGIN_GO -> "Pokémon GO"
                    else -> null
                }
                // From Generation V a Pokémon keeps the place index of the game it's from, wherever it is now.
                val placeGeneration = originGame?.dataGen?.takeIf { it in 5..generation } ?: generation
                // Let's Go, Brilliant Diamond and Shining Pearl number places their own way, and oDex doesn't have their lists.
                val place = dex.savePlaceName(placeGeneration, mon.metLocation).takeIf { (originGame ?: game).versionGroup !in OWN_PLACE_NUMBERS }
                val met = when {
                    // Before Generation III only Crystal keeps this, and only for Pokémon caught there.
                    !modern -> place?.takeIf { mon.metLevel > 0 }?.let { stringResource(R.string.met_line, mon.metLevel, it) }
                    generation == 3 && mon.metLocation == MET_TRADE -> stringResource(R.string.met_trade)
                    generation == 3 && mon.metLocation == MET_FATEFUL -> stringResource(R.string.met_fateful)
                    generation == 3 && mon.metLocation == MET_EGG -> stringResource(R.string.met_gift_egg)
                    mon.metLevel == 0 -> (place ?: origin)?.let { stringResource(R.string.hatched_line, it) }
                    else -> (place ?: origin)?.let { stringResource(R.string.met_line, mon.metLevel, it) }
                }
                Section {
                    nature?.let { InfoRow(stringResource(R.string.nature), it.name) }
                    ability?.let { InfoRow(stringResource(R.string.filter_ability), it.ability.name) }
                    if (heldKey != null) InfoRow(stringResource(R.string.held_item), dex.itemByKey(heldKey)?.name ?: itemKeyName(heldKey)) {
                        PokemonArtwork(0, Images.item(heldKey), Modifier.size(24.dp))
                    }
                    if (ballKey != null) InfoRow(stringResource(R.string.ball_caught), dex.itemByKey(ballKey)?.name ?: ballKey) {
                        PokemonArtwork(0, Images.item(ballKey), Modifier.size(24.dp))
                    }
                    // Sun and Moon started showing six digits of the whole ID.
                    val otId = if ((originGame?.dataGen ?: 0) >= 7) "%06d".format(mon.otId % 1_000_000) else "%05d".format(mon.otVisibleId)
                    InfoRow(stringResource(R.string.original_trainer), "${mon.otName} · ID $otId")
                    if (modern) origin?.let { InfoRow(stringResource(R.string.origin_game), it) }
                    metLocalDate(mon.metDate)?.let { InfoRow(stringResource(R.string.met_on), saveDateText(it)) }
                    MET_TIMES.getOrNull(mon.metTime - 1)?.let { InfoRow(stringResource(R.string.time_of_day), stringResource(it)) }
                    met?.let { Text(it, color = c.subtleText, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp)) }
                }
                if (mon.moveIds.isNotEmpty()) Section(stringResource(R.string.section_moves)) {
                    mon.moveIds.forEach { id ->
                        val move = dex.move(id, game)
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (move != null) TypeMark(move.type, size = 22.dp)
                            Text(move?.name ?: "#$id", fontSize = 16.sp, modifier = Modifier.padding(start = 10.dp))
                        }
                    }
                }
                Section { StatTable(mon, modern) }
            }
            TextButton(onClick = { onOpenPokemon(mon.speciesId) }, modifier = Modifier.padding(top = 8.dp)) {
                Text(stringResource(R.string.open_in_pokedex))
            }
        }
    }
}

@Composable
private fun Section(title: String? = null, content: @Composable () -> Unit) {
    val c = LocalDexColors.current
    Column(Modifier.padding(top = 14.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.sectionCard).padding(12.dp)) {
        if (title != null) Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 4.dp))
        content()
    }
}

@Composable
private fun InfoRow(label: String, value: String, leading: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().height(32.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = LocalDexColors.current.subtleText, fontSize = 14.sp, modifier = Modifier.weight(1f))
        leading?.invoke()
        Text(value, fontSize = 15.sp, modifier = Modifier.padding(start = 6.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** An item oDex has no entry for (mail, mostly), named from its key. */
private fun itemKeyName(key: String) = key.split('-').joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }

/**
 * IVs and EVs side by side under each stat's short name. In the first two generations ([modern] false) these are DVs
 * and stat experience, which runs to five digits.
 */
@Composable
private fun StatTable(mon: SavePokemonEntity, modern: Boolean) {
    val c = LocalDexColors.current
    val labels = listOf(R.string.stat_short_hp, R.string.stat_short_attack, R.string.stat_short_defense,
        R.string.stat_short_sp_attack, R.string.stat_short_sp_defense, R.string.stat_short_speed)
    @Composable
    fun line(title: String, values: List<String>, color: Color, weight: FontWeight = FontWeight.Normal) {
        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = c.subtleText, fontSize = 13.sp, modifier = Modifier.weight(1.2f))
            values.forEach {
                Text(it, color = color, fontSize = if (modern) 14.sp else 11.sp, fontWeight = weight, textAlign = TextAlign.Center, maxLines = 1,
                    modifier = Modifier.weight(1f))
            }
        }
    }
    line("", labels.map { stringResource(it) }, c.subtleText)
    line(stringResource(if (modern) R.string.ivs else R.string.dvs), mon.ivList.map { it.toString() }, c.text, FontWeight.SemiBold)
    line(stringResource(if (modern) R.string.evs else R.string.stat_exp), mon.evList.map { it.toString() }, c.text)
}
