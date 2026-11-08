package io.lemoncore.opendatadex.ui.dexes

import androidx.compose.ui.res.stringResource
import io.lemoncore.opendatadex.R
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.lemoncore.opendatadex.data.Dex
import io.lemoncore.opendatadex.data.Game
import io.lemoncore.opendatadex.data.Images
import io.lemoncore.opendatadex.data.MoveInfo
import io.lemoncore.opendatadex.ui.components.generationLabel
import io.lemoncore.opendatadex.ui.components.gameName
import io.lemoncore.opendatadex.ui.components.gameChipLabel
import io.lemoncore.opendatadex.ui.components.learnWayText
import io.lemoncore.opendatadex.ui.components.CategoryBadge
import io.lemoncore.opendatadex.ui.components.EmptyNote
import io.lemoncore.opendatadex.ui.components.HeaderChip
import io.lemoncore.opendatadex.ui.components.PokemonCard
import io.lemoncore.opendatadex.ui.components.Section
import io.lemoncore.opendatadex.ui.components.TypeBar
import io.lemoncore.opendatadex.ui.components.ValueBox
import io.lemoncore.opendatadex.ui.components.formatMoveValue
import io.lemoncore.opendatadex.ui.pokedex.GamePickerSheet
import io.lemoncore.opendatadex.ui.pokedex.OptionSheet
import io.lemoncore.opendatadex.ui.theme.CategoryColors
import io.lemoncore.opendatadex.ui.theme.LocalDexColors
import io.lemoncore.opendatadex.ui.theme.TypeColors

private enum class MoveSheet { NONE, GAME, GEN, TYPE, CAT }

@Composable
fun MoveDexScreen(dex: Dex, game: Game, onGame: (Game) -> Unit, onOpenDrawer: () -> Unit, onOpenMove: (Int) -> Unit) {
    var gen by rememberSaveable { mutableStateOf<Int?>(null) }
    var type by rememberSaveable { mutableStateOf<Int?>(null) }
    var cat by rememberSaveable { mutableStateOf<Int?>(null) }
    var sheet by remember { mutableStateOf(MoveSheet.NONE) }
    val all = remember(game) { dex.allMoves(game) }
    DexScaffold(stringResource(R.string.nav_move_dex), onOpenDrawer, isRoot = true, chips = {
        io.lemoncore.opendatadex.ui.components.GameChip(game, gameChipLabel(game), { sheet = MoveSheet.GAME }, Modifier.weight(1.1f))
        HeaderChip(gen?.let { stringResource(R.string.gen_short, it) } ?: stringResource(R.string.all_gens), gen != null, { sheet = MoveSheet.GEN }, Modifier.weight(1f))
        HeaderChip(type?.let { dex.type(it)?.name } ?: stringResource(R.string.all_types), type != null, { sheet = MoveSheet.TYPE }, Modifier.weight(1f), activeColors = type?.let { TypeColors.tones(it, LocalDexColors.current.isDark) }?.let { Triple(it.bg, it.dot, it.text) })
        HeaderChip(cat?.let { stringResource(CategoryColors.label(it)) } ?: stringResource(R.string.all_categories_short), cat != null, { sheet = MoveSheet.CAT }, Modifier.weight(1f))
    }) { query, padding ->
        val shown = all.filter { m ->
            (gen == null || m.gen == gen) && (type == null || m.type == type) && (cat == null || m.category == cat) &&
                (query.isBlank() || m.name.contains(query.trim(), true))
        }
        LazyColumn(contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = padding.calculateTopPadding() + 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                    Text(stringResource(R.string.col_move), Modifier.weight(1f), textAlign = TextAlign.Center, color = LocalDexColors.current.subtleText)
                    Text(stringResource(R.string.col_power), Modifier.width(56.dp), textAlign = TextAlign.Center, color = LocalDexColors.current.subtleText)
                    Text(stringResource(R.string.col_accuracy), Modifier.width(50.dp), textAlign = TextAlign.Center, color = LocalDexColors.current.subtleText)
                    Text(stringResource(R.string.col_pp), Modifier.width(40.dp), textAlign = TextAlign.Center, color = LocalDexColors.current.subtleText)
                }
            }
            if (shown.isEmpty()) item { EmptyNote(stringResource(R.string.no_moves_match)) }
            items(shown, key = { it.id }) { m -> MoveCard(dex, m) { onOpenMove(m.id) } }
        }
    }
    when (sheet) {
        MoveSheet.GAME -> GamePickerSheet(game, onPick = { onGame(it); sheet = MoveSheet.NONE }, onDismiss = { sheet = MoveSheet.NONE })
        MoveSheet.GEN -> OptionSheet(stringResource(R.string.select_generation), listOf<Pair<Int?, String>>(null to stringResource(R.string.all_gens)) + (1..game.speciesGen).map { it to generationLabel(it) },
            { gen = it; sheet = MoveSheet.NONE }, { sheet = MoveSheet.NONE })
        MoveSheet.TYPE -> OptionSheet(stringResource(R.string.select_type), listOf<Pair<Int?, String>>(null to stringResource(R.string.all_types)) + dex.typesIn(game).map { it.id to it.name },
            { type = it; sheet = MoveSheet.NONE }, { sheet = MoveSheet.NONE })
        MoveSheet.CAT -> OptionSheet(stringResource(R.string.select_category), listOf<Pair<Int?, String>>(null to stringResource(R.string.all_categories)) + listOf(2, 3, 1).map { it to stringResource(CategoryColors.label(it)) },
            { cat = it; sheet = MoveSheet.NONE }, { sheet = MoveSheet.NONE })
        MoveSheet.NONE -> Unit
    }
}

@Composable
fun MoveCard(dex: Dex, m: MoveInfo, onClick: () -> Unit) {
    Surface(shape = RoundedCornerShape(14.dp), color = LocalDexColors.current.sectionCard, shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(m.name, Modifier.weight(1f), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.primary, fontSize = 18.sp)
                Text(formatMoveValue(m.power), Modifier.width(56.dp), textAlign = TextAlign.Center, color = LocalDexColors.current.subtleText, fontSize = 17.sp)
                Text(formatMoveValue(m.accuracy), Modifier.width(50.dp), textAlign = TextAlign.Center, color = LocalDexColors.current.subtleText, fontSize = 17.sp)
                Text(formatMoveValue(m.pp), Modifier.width(40.dp), textAlign = TextAlign.Center, color = LocalDexColors.current.subtleText, fontSize = 17.sp)
            }
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                TypeBar(m.type, dex.type(m.type)?.name ?: "", Modifier.weight(1.7f))
                CategoryBadge(m.category, Modifier.weight(1f))
                Icon(Icons.Outlined.Info, stringResource(R.string.details), tint = LocalDexColors.current.subtleText)
            }
        }
    }
}

@Composable
fun MoveDetailScreen(
    dex: Dex, moveId: Int, game: Game, artwork: Map<Int, String>, favorites: Set<Int>,
    onBack: () -> Unit, onOpenPokemon: (Int) -> Unit, onToggleFavorite: (Int) -> Unit,
) {
    val m = dex.move(moveId, game) ?: return
    DexScaffold(m.name, onBack, isRoot = false, filterable = false) { _, padding ->
        val learners by produceState(emptyList<Pair<io.lemoncore.opendatadex.data.PokemonSummary, List<io.lemoncore.opendatadex.data.LearnWay>>>(), moveId, game) {
            if (!game.isNational) value = withContext(Dispatchers.Default) { dex.learners(moveId, game, dex.learnsets(game)) }
        }
        LazyColumn(contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = padding.calculateTopPadding(), bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Section(m.name, MaterialTheme.colorScheme.primary) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TypeBar(m.type, dex.type(m.type)?.name ?: "", Modifier.weight(1.6f))
                        CategoryBadge(m.category, Modifier.weight(1f))
                    }
                    Text(m.description, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ValueBox(formatMoveValue(m.power), stringResource(R.string.col_power), Modifier.weight(1f))
                        ValueBox(formatMoveValue(m.accuracy), stringResource(R.string.accuracy), Modifier.weight(1f))
                        ValueBox(formatMoveValue(m.pp), stringResource(R.string.col_pp), Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ValueBox(if (m.priority > 0) "+${m.priority}" else m.priority.toString(), stringResource(R.string.priority), Modifier.weight(1f))
                        ValueBox(generationLabel(m.gen), stringResource(R.string.introduced), Modifier.weight(1.4f))
                    }
                    ValueBox(m.effect.ifBlank { m.shortEffect }, stringResource(R.string.effect_current))
                    if (game.dataGen <= 3 && m.category != 1) {
                        Text(stringResource(R.string.category_by_type_note), color = LocalDexColors.current.subtleText,
                            style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            item {
                Text(if (game.isNational) stringResource(R.string.learners_pick_game, m.name) else stringResource(R.string.learners_in, m.name, gameName(game)),
                    style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, color = LocalDexColors.current.subtleText,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp))
            }
            items(learners, key = { it.first.formId }) { (p, how) ->
                PokemonCard(p, p.types.map { dex.type(it)?.name ?: "" }, Images.artwork(p.formId, artwork), p.id in favorites, false,
                    onFavorite = { onToggleFavorite(p.id) }, onCaught = null, onClick = { onOpenPokemon(p.id) }, subtitle = how.map { learnWayText(it) }.joinToString(", "))
            }
        }
    }
}
