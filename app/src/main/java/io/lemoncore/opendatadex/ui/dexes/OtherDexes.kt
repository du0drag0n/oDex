package io.lemoncore.opendatadex.ui.dexes

import androidx.compose.ui.res.stringResource
import io.lemoncore.opendatadex.R
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import io.lemoncore.opendatadex.data.Dex
import io.lemoncore.opendatadex.data.Game
import io.lemoncore.opendatadex.data.Images
import io.lemoncore.opendatadex.ui.components.TypeMarkTile
import io.lemoncore.opendatadex.ui.components.generationLabel
import io.lemoncore.opendatadex.ui.components.gameName
import io.lemoncore.opendatadex.ui.components.gameChipLabel
import io.lemoncore.opendatadex.ui.components.learnWayText
import io.lemoncore.opendatadex.ui.components.EmptyNote
import io.lemoncore.opendatadex.ui.components.HeaderChip
import io.lemoncore.opendatadex.ui.components.PokemonCard
import io.lemoncore.opendatadex.ui.components.Section
import io.lemoncore.opendatadex.ui.components.levelRange
import io.lemoncore.opendatadex.ui.components.TypePill
import io.lemoncore.opendatadex.ui.pokedex.GamePickerSheet
import io.lemoncore.opendatadex.ui.theme.LocalDexColors
import io.lemoncore.opendatadex.ui.theme.TypeColors

@Composable
private fun ListCard(onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp), color = LocalDexColors.current.sectionCard, shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) { Column(Modifier.padding(14.dp).animateContentSize()) { content() } }
}

@Composable
internal fun GameChip(game: Game, onGame: (Game) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    io.lemoncore.opendatadex.ui.components.GameChip(game, gameChipLabel(game), { open = true }, modifier)
    if (open) GamePickerSheet(game, onPick = { onGame(it); open = false }, onDismiss = { open = false })
}

private fun pad(p: PaddingValues) = PaddingValues(start = 12.dp, end = 12.dp, top = p.calculateTopPadding() + 8.dp, bottom = 24.dp)

// ---- Ability Dex ---------------------------------------------------------------------------------------------------------

@Composable
fun AbilityDexScreen(dex: Dex, game: Game, onOpenDrawer: () -> Unit, onOpenAbility: (Int) -> Unit) {
    DexScaffold(stringResource(R.string.nav_ability_dex), onOpenDrawer, isRoot = true) { query, padding ->
        val shown = dex.abilities.filter { (game.isNational || it.gen <= game.dataGen) && (query.isBlank() || it.name.contains(query.trim(), true)) }
        LazyColumn(contentPadding = pad(padding), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Text(
                    when {
                        game.dataGen < 3 -> stringResource(R.string.abilities_gen3)
                        game.isNational -> stringResource(R.string.abilities_all)
                        else -> stringResource(R.string.abilities_in, gameName(game))
                    },
                    color = LocalDexColors.current.subtleText, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                )
            }
            items(shown, key = { it.id }) { a ->
                ListCard({ onOpenAbility(a.id) }) {
                    Text(a.name, color = MaterialTheme.colorScheme.primary, fontSize = 18.sp)
                    Text(a.short, style = MaterialTheme.typography.bodyMedium, color = LocalDexColors.current.subtleText)
                }
            }
        }
    }
}

@Composable
fun AbilityDetailScreen(dex: Dex, abilityId: Int, game: Game, artwork: Map<Int, String>, onBack: () -> Unit, onOpenPokemon: (Int, Int) -> Unit) {
    val a = dex.ability(abilityId) ?: return
    DexScaffold(a.name, onBack, isRoot = false, filterable = false) { _, padding ->
        val holders = remember(abilityId) { dex.pokemonWithAbility(abilityId) }
        LazyColumn(contentPadding = pad(padding), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Section(a.name, MaterialTheme.colorScheme.primary) {
                    val flavor = dex.abilityDescription(a, if (game.dataGen < 3) Game.NATIONAL else game)
                    if (flavor.isNotBlank()) Text(flavor, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(10.dp))
                    Text(a.effect.ifBlank { a.short }, style = MaterialTheme.typography.bodyMedium)
                    Text(stringResource(R.string.introduced_in, generationLabel(a.gen)), color = LocalDexColors.current.subtleText, style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
                }
            }
            item { Text(stringResource(R.string.ability_holders, a.name), textAlign = TextAlign.Center, color = LocalDexColors.current.subtleText, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) }
            items(holders, key = { it.first.formId }) { (p, hidden) ->
                PokemonCard(p, p.types.map { dex.type(it)?.name ?: "" }, Images.artwork(p.formId, artwork), false, false,
                    onFavorite = null, onCaught = null, onClick = { onOpenPokemon(p.id, p.formId) }, subtitle = if (hidden) stringResource(R.string.hidden_ability_long) else null)
            }
        }
    }
}

// ---- Item Dex ------------------------------------------------------------------------------------------------------------------

@Composable
fun ItemDexScreen(dex: Dex, game: Game, onGame: (Game) -> Unit, onOpenDrawer: () -> Unit) {
    var pocket by rememberSaveable { mutableStateOf<String?>(null) }
    val items = remember(game) { dex.items(game) }
    val pockets = remember(items) { items.map { it.pocket }.filter { it.isNotBlank() }.distinct().sorted() }
    var pocketSheet by remember { mutableStateOf(false) }
    DexScaffold(stringResource(R.string.nav_item_dex), onOpenDrawer, isRoot = true, chips = {
        GameChip(game, onGame, Modifier.weight(1.3f))
        HeaderChip(pocket ?: stringResource(R.string.all_pockets), pocket != null, { pocketSheet = true }, Modifier.weight(1f))
    }) { query, padding ->
        var expanded by rememberSaveable { mutableStateOf<Int?>(null) }
        val shown = items.filter { (pocket == null || it.pocket == pocket) && (query.isBlank() || it.name.contains(query.trim(), true)) }
        LazyColumn(contentPadding = pad(padding), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (shown.isEmpty()) item { EmptyNote(stringResource(R.string.no_items_match)) }
            items(shown, key = { it.id }) { item ->
                ListCard({ expanded = if (expanded == item.id) null else item.id }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(Images.item(item.key), null, Modifier.size(36.dp))
                        Column(Modifier.padding(start = 12.dp).weight(1f)) {
                            Text(item.name, color = MaterialTheme.colorScheme.primary, fontSize = 17.sp)
                            Text(item.category.replaceFirstChar(Char::titlecase), style = MaterialTheme.typography.bodySmall, color = LocalDexColors.current.subtleText)
                        }
                        if (item.cost > 0) Text("₽${item.cost}", color = LocalDexColors.current.subtleText)
                    }
                    if (expanded == item.id) {
                        Text(dex.itemDescription(item, game).ifBlank { stringResource(R.string.no_description) }, style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
        }
    }
    if (pocketSheet) io.lemoncore.opendatadex.ui.pokedex.OptionSheet(stringResource(R.string.select_pocket), listOf<Pair<String?, String>>(null to stringResource(R.string.all_pockets)) + pockets.map { it to it },
        { pocket = it; pocketSheet = false }, { pocketSheet = false })
}

// ---- Location Dex -----------------------------------------------------------------------------------------------------------------

@Composable
fun LocationDexScreen(dex: Dex, game: Game, onGame: (Game) -> Unit, onOpenDrawer: () -> Unit, onOpenLocation: (Int) -> Unit) {
    DexScaffold(stringResource(R.string.nav_location_dex), onOpenDrawer, isRoot = true, chips = { GameChip(game, onGame, Modifier.weight(1f)) }) { query, padding ->
        val areas = remember(game) { dex.areasIn(game) }
        val shown = areas.filter { query.isBlank() || it.second.contains(query.trim(), true) }
        LazyColumn(contentPadding = pad(padding), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (shown.isEmpty()) item {
                EmptyNote(if (dex.hasEncounterData(game)) stringResource(R.string.no_locations_match) else stringResource(R.string.locations_no_data, gameName(game)))
            }
            items(shown, key = { it.first.id }) { (area, name) ->
                ListCard({ onOpenLocation(area.id) }) {
                    Text(name, color = MaterialTheme.colorScheme.primary, fontSize = 17.sp)
                    dex.regionName(area.region)?.let { Text(it, color = LocalDexColors.current.subtleText, fontSize = 13.sp) }
                }
            }
        }
    }
}

// ---- Type Dex --------------------------------------------------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TypeDexScreen(dex: Dex, game: Game, onGame: (Game) -> Unit, onOpenDrawer: () -> Unit, onOpenType: (Int) -> Unit) {
    DexScaffold(stringResource(R.string.nav_type_dex), onOpenDrawer, isRoot = true, filterable = false, chips = { GameChip(game, onGame, Modifier.weight(1f)) }) { _, padding ->
        val types = dex.typesIn(game)
        LazyColumn(contentPadding = pad(padding), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp), maxItemsInEachRow = 3) {
                    types.forEach { t -> TypeMarkTile(t.id, t.name, Modifier.weight(1f)) { onOpenType(t.id) } }
                    // Keeps a short last row's tiles the same width as the others.
                    repeat((3 - types.size % 3) % 3) { androidx.compose.foundation.layout.Spacer(Modifier.weight(1f)) }
                }
            }
            item { TypeChart(dex, game) }
        }
    }
}

/** Attack (rows) × defence (columns) chart, like the in-game type chart. */
@Composable
private fun TypeChart(dex: Dex, game: Game) {
    val types = dex.typesIn(game)
    Section(if (game.isNational) stringResource(R.string.type_chart_today) else stringResource(R.string.type_chart_in, gameName(game)), MaterialTheme.colorScheme.primary) {
        Text(stringResource(R.string.type_chart_note), color = LocalDexColors.current.subtleText, style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp))
        androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth()) {
        val cell = maxWidth / (types.size + 1)
        Column {
        Row {
            Spacer(Modifier.width(cell))
            types.forEach { t -> ChartCell(t.name.take(2), TypeColors.tones(t.id, LocalDexColors.current.isDark).bg, TypeColors.tones(t.id, LocalDexColors.current.isDark).text, cell) }
        }
        types.forEach { atk ->
            Row {
                ChartCell(atk.name.take(2), TypeColors.tones(atk.id, LocalDexColors.current.isDark).bg, TypeColors.tones(atk.id, LocalDexColors.current.isDark).text, cell)
                types.forEach { def ->
                    when (dex.factor(atk.id, def.id, game.dataGen)) {
                        0 -> ChartCell("0", Color(0xFF424242), Color.White, cell)
                        50 -> ChartCell("½", Color(0xFFE57373), Color.White, cell)
                        200 -> ChartCell("2", Color(0xFF66BB6A), Color.White, cell)
                        else -> ChartCell("", Color.Transparent, Color.Unspecified, cell)
                    }
                }
            }
        }
        }
        }
    }
}

@Composable
private fun ChartCell(text: String, bg: Color, fg: Color, size: androidx.compose.ui.unit.Dp) {
    androidx.compose.foundation.layout.Box(
        Modifier.size(size).padding(0.5.dp).background(bg, RoundedCornerShape(2.dp)), contentAlignment = Alignment.Center,
    ) { Text(text, color = fg, fontSize = 9.sp, maxLines = 1) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TypeDetailScreen(dex: Dex, typeId: Int, game: Game, artwork: Map<Int, String>, onBack: () -> Unit, onOpenPokemon: (Int) -> Unit, onOpenType: (Int) -> Unit) {
    val t = dex.type(typeId) ?: return
    DexScaffold(stringResource(R.string.type_title, t.name), onBack, isRoot = false, filterable = false) { _, padding ->
        val types = dex.typesIn(game)
        fun row(title: Int, list: List<Pair<Int, String>>) = title to list
        val offense = types.map { it to dex.factor(typeId, it.id, game.dataGen) }
        val defense = types.map { it to dex.factor(it.id, typeId, game.dataGen) }
        val sections = listOf(
            row(R.string.super_effective_against, offense.filter { it.second > 100 }.map { it.first.id to it.first.name }),
            row(R.string.not_very_effective_against, offense.filter { it.second in 1..99 }.map { it.first.id to it.first.name }),
            row(R.string.no_effect_on, offense.filter { it.second == 0 }.map { it.first.id to it.first.name }),
            row(R.string.weak_to, defense.filter { it.second > 100 }.map { it.first.id to it.first.name }),
            row(R.string.resists, defense.filter { it.second in 1..99 }.map { it.first.id to it.first.name }),
            row(R.string.immune_to_type, defense.filter { it.second == 0 }.map { it.first.id to it.first.name }),
        )
        val pokemon = remember(typeId, game) { dex.pokemonWithType(typeId, game) }
        LazyColumn(contentPadding = pad(padding), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Section(t.name) {
                    sections.forEach { (title, list) ->
                        Text(stringResource(title), color = LocalDexColors.current.subtleText, fontSize = 16.sp, textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp))
                        if (list.isEmpty()) Text("—", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        else FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp), maxItemsInEachRow = 3) {
                            list.forEach { (id, name) -> TypePill(id, name, Modifier.weight(1f)) { onOpenType(id) } }
                        }
                    }
                }
            }
            item { Text(stringResource(R.string.type_pokemon_in, t.name, gameName(game)), textAlign = TextAlign.Center, color = LocalDexColors.current.subtleText, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) }
            items(pokemon, key = { it.id }) { p ->
                PokemonCard(p, p.types.map { dex.type(it)?.name ?: "" }, Images.artwork(p.formId, artwork), false, false,
                    onFavorite = null, onCaught = null, onClick = { onOpenPokemon(p.id) })
            }
        }
    }
}

// ---- Nature Dex -----------------------------------------------------------------------------------------------------------------

@Composable
fun NatureDexScreen(dex: Dex, onOpenDrawer: () -> Unit) {
    DexScaffold(stringResource(R.string.nav_nature_dex), onOpenDrawer, isRoot = true) { query, padding ->
        val shown = dex.natures.filter { query.isBlank() || it.name.contains(query.trim(), true) }
        LazyColumn(contentPadding = pad(padding), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Text(stringResource(R.string.natures_gen3), color = LocalDexColors.current.subtleText,
                    textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.fillMaxWidth().padding(8.dp))
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp)) {
                    Text(stringResource(R.string.nature), Modifier.weight(1f), color = LocalDexColors.current.subtleText)
                    Text(stringResource(R.string.raises), Modifier.width(96.dp), color = Color(0xFF43A047), textAlign = TextAlign.Center)
                    Text(stringResource(R.string.lowers), Modifier.width(96.dp), color = Color(0xFFE53935), textAlign = TextAlign.Center)
                }
            }
            items(shown, key = { it.id }) { n ->
                ListCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(n.name, color = MaterialTheme.colorScheme.primary, fontSize = 17.sp)
                            if (n.likes.isNotBlank()) Text(stringResource(R.string.nature_flavors, flavorName(n.likes), flavorName(n.hates)), fontSize = 12.sp, color = LocalDexColors.current.subtleText)
                        }
                        val neutral = n.up == n.down
                        Text(if (neutral) "—" else stringResource(Dex.statLabel(n.up)), Modifier.width(96.dp), textAlign = TextAlign.Center)
                        Text(if (neutral) "—" else stringResource(Dex.statLabel(n.down)), Modifier.width(96.dp), textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

/** A berry flavor ("Spicy") in the app language. */
@Composable
private fun flavorName(flavor: String) = when (flavor) {
    "Spicy" -> stringResource(R.string.flavor_spicy)
    "Dry" -> stringResource(R.string.flavor_dry)
    "Sweet" -> stringResource(R.string.flavor_sweet)
    "Bitter" -> stringResource(R.string.flavor_bitter)
    "Sour" -> stringResource(R.string.flavor_sour)
    else -> flavor.lowercase()
}
