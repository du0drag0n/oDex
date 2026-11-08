package io.lemoncore.opendatadex.ui.pokedex

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.lemoncore.opendatadex.R
import io.lemoncore.opendatadex.data.Dex
import io.lemoncore.opendatadex.data.Game
import io.lemoncore.opendatadex.ui.components.Loading
import io.lemoncore.opendatadex.ui.theme.LocalDexColors
import io.lemoncore.opendatadex.ui.theme.DexType
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private enum class Picking { MENU, EGG_GROUP, ABILITY, MOVE }

/** Abilities exist from Ruby/Sapphire on (the national view shows today's); moves need the game's own move pools. */
private val Game.filtersAbilities get() = hasAbilities || isNational
private val Game.filtersMoves get() = !isNational

/**
 * dataDex 4's extra Pokédex filters: Pokémon in an egg group, with an ability in the selected game, or that learn a
 * move there. Each choice opens a list (searchable for abilities and moves); any can be cleared.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MoreFiltersSheet(dex: Dex, game: Game, national: Boolean, filters: ListFilters, vm: PokedexViewModel, onDismiss: () -> Unit) {
    var picking by remember { mutableStateOf(Picking.MENU) }
    DexSheet(null, onDismiss, rememberModalBottomSheetState(skipPartiallyExpanded = picking != Picking.MENU)) {
        when (picking) {
            Picking.MENU -> Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).navigationBarsPadding().padding(bottom = 16.dp)) {
                Text(stringResource(R.string.more_filters), style = DexType.sheetTitle, color = LocalDexColors.current.text,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp))
                FilterRow(stringResource(R.string.filter_egg_group), filters.eggGroup?.let { dex.eggGroupName(it) }, { picking = Picking.EGG_GROUP }) { vm.setEggGroup(null) }
                if (game.filtersAbilities) {
                    FilterRow(stringResource(R.string.filter_ability), filters.ability?.let { dex.ability(it)?.name }, { picking = Picking.ABILITY }) { vm.setAbility(null) }
                }
                if (game.filtersMoves) {
                    FilterRow(stringResource(R.string.filter_move), filters.move?.let { dex.move(it, game)?.name }, { picking = Picking.MOVE }) { vm.setMove(null) }
                }
                if (filters.extraCount > 0) TextButton(
                    onClick = { vm.setEggGroup(null); vm.setAbility(null); vm.setMove(null) },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) { Text(stringResource(R.string.filter_clear_all)) }
            }
            Picking.EGG_GROUP -> ChoiceList(stringResource(R.string.filter_egg_group), null, dex.eggGroups) { vm.setEggGroup(it); onDismiss() }
            Picking.ABILITY -> {
                val abilities by produceState<List<Pair<Int, String>>?>(null, game, national) {
                    value = withContext(Dispatchers.Default) { dex.abilitiesAmong(dex.pokemonFor(game, national), game).map { it.id to it.name } }
                }
                ChoiceList(stringResource(R.string.filter_ability), stringResource(R.string.filter_find_ability), abilities) { vm.setAbility(it); onDismiss() }
            }
            Picking.MOVE -> {
                val moves by produceState<List<Pair<Int, String>>?>(null, game) {
                    value = withContext(Dispatchers.Default) { dex.allMoves(game).map { it.id to it.name } }
                }
                ChoiceList(stringResource(R.string.filter_move), stringResource(R.string.filter_find_move), moves) { vm.setMove(it); onDismiss() }
            }
        }
    }
}

@Composable
private fun FilterRow(label: String, value: String?, onPick: () -> Unit, onClear: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onPick).padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(vertical = 6.dp)) {
            Text(label, style = DexType.itemTitle, color = LocalDexColors.current.text)
            Text(value ?: stringResource(R.string.filter_any), style = DexType.caption,
                color = if (value != null) LocalDexColors.current.accentText else LocalDexColors.current.textSubtle)
        }
        if (value != null) IconButton(onClick = onClear) { Icon(Icons.Filled.Close, stringResource(R.string.clear)) }
    }
    HorizontalDivider(color = LocalDexColors.current.divider)
}

/** A choice list for one filter; with a [searchHint], a search box narrows it by name. */
@Composable
private fun ChoiceList(title: String, searchHint: String?, choices: List<Pair<Int, String>>?, onPick: (Int) -> Unit) {
    var query by remember { mutableStateOf("") }
    Column(Modifier.fillMaxHeight(0.92f).padding(horizontal = 16.dp)) {
        Text(title, style = DexType.sheetTitle, color = LocalDexColors.current.text, modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp))
        if (searchHint != null) OutlinedTextField(
            value = query, onValueChange = { query = it }, singleLine = true, placeholder = { Text(searchHint) },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        )
        val list = choices ?: return@Column Loading()
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
            items(list.filter { query.isBlank() || it.second.contains(query.trim(), ignoreCase = true) }, key = { it.first }) { (id, name) ->
                Text(name, style = DexType.itemTitle.copy(fontWeight = FontWeight.SemiBold), color = LocalDexColors.current.text, modifier = Modifier.fillMaxWidth().clickable { onPick(id) }.padding(vertical = 12.dp, horizontal = 4.dp))
                HorizontalDivider(color = LocalDexColors.current.divider)
            }
        }
    }
}

/** The extra filters in use, under the Pokédex's filter chips, each removable with its cross. */
@Composable
internal fun ExtraFilterChips(dex: Dex, game: Game, filters: ListFilters, vm: PokedexViewModel) {
    if (filters.extraCount == 0) return
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        filters.eggGroup?.let { id -> RemovableChip(stringResource(R.string.chip_egg_group, dex.eggGroupName(id).orEmpty())) { vm.setEggGroup(null) } }
        filters.ability?.let { id -> RemovableChip(stringResource(R.string.chip_ability, dex.ability(id)?.name.orEmpty())) { vm.setAbility(null) } }
        filters.move?.let { id -> RemovableChip(stringResource(R.string.chip_move, dex.move(id, game)?.name.orEmpty())) { vm.setMove(null) } }
    }
}

@Composable
private fun RemovableChip(label: String, onRemove: () -> Unit) {
    val c = LocalDexColors.current
    Row(
        Modifier.height(30.dp).clip(RoundedCornerShape(10.dp)).background(c.accentTint).border(1.dp, c.accent.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .clickable(onClick = onRemove).padding(start = 12.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = c.accentText, style = DexType.chip, maxLines = 1)
        Spacer(Modifier.width(4.dp))
        Icon(Icons.Filled.Close, stringResource(R.string.clear), tint = c.accentText, modifier = Modifier.size(14.dp))
    }
}
