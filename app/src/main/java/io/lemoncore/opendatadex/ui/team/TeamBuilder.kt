package io.lemoncore.opendatadex.ui.team

import androidx.compose.ui.res.stringResource
import io.lemoncore.opendatadex.R
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import io.lemoncore.opendatadex.data.MoveInfo
import io.lemoncore.opendatadex.data.PokemonSummary
import io.lemoncore.opendatadex.data.seed.SeedType
import io.lemoncore.opendatadex.data.user.Backup
import io.lemoncore.opendatadex.data.user.TeamEntity
import io.lemoncore.opendatadex.data.user.TeamMemberEntity
import io.lemoncore.opendatadex.ui.components.CategoryBadge
import io.lemoncore.opendatadex.ui.components.EmptyNote
import io.lemoncore.opendatadex.ui.components.HeaderChip
import io.lemoncore.opendatadex.ui.components.Loading
import io.lemoncore.opendatadex.ui.components.LocalWidthClass
import io.lemoncore.opendatadex.ui.components.PokemonArtwork
import io.lemoncore.opendatadex.ui.components.PokemonCard
import io.lemoncore.opendatadex.ui.components.Section
import io.lemoncore.opendatadex.ui.components.TypePill
import io.lemoncore.opendatadex.ui.components.cardRows
import io.lemoncore.opendatadex.ui.dexes.DexScaffold
import io.lemoncore.opendatadex.ui.components.gameName
import io.lemoncore.opendatadex.ui.components.gameChipLabel
import io.lemoncore.opendatadex.ui.components.gameColor
import androidx.compose.ui.platform.LocalResources
import io.lemoncore.opendatadex.ui.pokedex.GamePickerSheet
import io.lemoncore.opendatadex.ui.theme.LocalDexColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ---- team list -------------------------------------------------------------------------------------------------------------

@Composable
fun TeamListScreen(
    container: AppContainer, dex: Dex, currentGame: Game, artwork: Map<Int, String>,
    onOpenDrawer: () -> Unit, onOpenTeam: (Long) -> Unit,
) {
    val teams by container.teams.collectAsState(initial = null)
    val members by container.teamMembers.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val newTeamName = stringResource(R.string.team_default_name, (teams?.size ?: 0) + 1)
    DexScaffold(stringResource(R.string.nav_team_builder), onOpenDrawer, isRoot = true, filterable = false) { _, padding ->
        val list = teams ?: return@DexScaffold Loading()
        val perRow = LocalWidthClass.current.cardsPerRow
        LazyColumn(
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = padding.calculateTopPadding() + 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Row(
                    Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.primary).clickable {
                        scope.launch { onOpenTeam(container.createTeam(newTeamName, currentGame)) }
                    },
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.Filled.Add, null, tint = MaterialTheme.colorScheme.onPrimary)
                    Text("  " + stringResource(R.string.new_team_for, gameName(currentGame)), color = MaterialTheme.colorScheme.onPrimary, fontSize = 17.sp)
                }
            }
            if (list.isEmpty()) item {
                EmptyNote(stringResource(R.string.teams_intro))
            }
            cardRows(list, perRow, { it.id }) { t, modifier ->
                TeamCard(dex, t, members.filter { it.teamId == t.id }, artwork, modifier) { onOpenTeam(t.id) }
            }
        }
    }
}

@Composable
internal fun TeamCard(
    dex: Dex, team: TeamEntity, members: List<TeamMemberEntity>, artwork: Map<Int, String>, modifier: Modifier = Modifier.fillMaxWidth(), onClick: () -> Unit,
) {
    val game = Game.fromKey(team.game)
    Surface(shape = RoundedCornerShape(14.dp), color = LocalDexColors.current.sectionCard, shadowElevation = 1.dp,
        modifier = modifier.clickable(onClick = onClick)) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(team.name, color = MaterialTheme.colorScheme.primary, fontSize = 19.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Box(Modifier.clip(RoundedCornerShape(50)).background(gameColor(game)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                    Text(gameChipLabel(game), color = if (game.isNational) LocalDexColors.current.chipText else Color.White, fontSize = 13.sp)
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                for (slot in 0 until Backup.TEAM_SIZE) {
                    val m = members.firstOrNull { it.slot == slot }
                    Box(Modifier.size(48.dp).clip(CircleShape).background(LocalDexColors.current.chip), contentAlignment = Alignment.Center) {
                        if (m != null) PokemonArtwork(m.formId, Images.artwork(m.formId, artwork), Modifier.size(44.dp), dex.pokemon(m.formId)?.name)
                    }
                }
            }
        }
    }
}

// ---- team editor ---------------------------------------------------------------------------------------------------------------

private sealed interface TeamSheet {
    data object None : TeamSheet
    data object Game : TeamSheet
    data class AddMember(val slot: Int) : TeamSheet
    data class Member(val slot: Int) : TeamSheet
    data class Move(val slot: Int, val index: Int) : TeamSheet
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamEditorScreen(
    container: AppContainer, dex: Dex, teamId: Long, artwork: Map<Int, String>,
    onBack: () -> Unit, onOpenPokemon: (Int, Int) -> Unit,
) {
    val team by container.team(teamId).collectAsState(initial = null)
    val members by container.members(teamId).collectAsState(initial = emptyList())
    var sheet by remember { mutableStateOf<TeamSheet>(TeamSheet.None) }
    var renaming by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val t = team ?: return Loading()
    val game = Game.fromKey(t.game)

    Scaffold(topBar = {
        Column(Modifier.background(MaterialTheme.colorScheme.surface)) {
            TopAppBar(
                title = { Text(t.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } },
                actions = {
                    IconButton(onClick = { renaming = true }) { Icon(Icons.Filled.Edit, stringResource(R.string.rename_team)) }
                    IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, stringResource(R.string.delete_team)) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
                io.lemoncore.opendatadex.ui.components.GameChip(game, gameChipLabel(game), { sheet = TeamSheet.Game }, Modifier.weight(1f))
            }
            HorizontalDivider(color = LocalDexColors.current.divider)
        }
    }) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = padding.calculateTopPadding() + 10.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items((0 until Backup.TEAM_SIZE).toList(), key = { "slot-$it" }) { slot ->
                val m = members.firstOrNull { it.slot == slot }
                val p = m?.let { dex.summaryOfForm(it.formId, game) }
                if (m == null || p == null) {
                    EmptySlot { sheet = TeamSheet.AddMember(slot) }
                } else {
                    val moves = m.moveIds.mapNotNull { dex.move(it, game)?.name }
                    PokemonCard(p, p.types.map { dex.type(it)?.name ?: "" }, Images.artwork(p.formId, artwork), false, false,
                        onFavorite = null, onCaught = null, onClick = { sheet = TeamSheet.Member(slot) },
                        subtitle = if (moves.isEmpty()) null else moves.joinToString(" · "))
                }
            }
            if (members.isNotEmpty()) {
                item(key = "analysis") { TeamAnalysis(dex, game, members) }
            }
        }
    }

    when (val s = sheet) {
        TeamSheet.None -> Unit
        TeamSheet.Game -> GamePickerSheet(game, onPick = { container.updateTeam(t.copy(game = it.key)); sheet = TeamSheet.None }, onDismiss = { sheet = TeamSheet.None })
        is TeamSheet.AddMember -> PokemonPickerSheet(dex, game, artwork, onDismiss = { sheet = TeamSheet.None }) { p ->
            container.setMember(TeamMemberEntity(teamId, s.slot, p.id, p.formId, ""))
            sheet = TeamSheet.Member(s.slot)
        }
        is TeamSheet.Member -> members.firstOrNull { it.slot == s.slot }?.let { m ->
            MemberSheet(
                dex, game, m,
                onDismiss = { sheet = TeamSheet.None },
                onForm = { container.setMember(m.copy(formId = it)) },
                onMove = { index -> sheet = TeamSheet.Move(s.slot, index) },
                onClearMove = { index -> container.setMember(m.copy(moves = m.moveIds.filterIndexed { i, _ -> i != index }.joinToString(","))) },
                onOpenEntry = { sheet = TeamSheet.None; onOpenPokemon(m.speciesId, m.formId) },
                onRemove = { container.removeMember(teamId, s.slot); sheet = TeamSheet.None },
            )
        } // A member just added may not have reached the list yet; the sheet opens once it does.
        is TeamSheet.Move -> members.firstOrNull { it.slot == s.slot }?.let { m ->
            MovePickerSheet(dex, game, m, onDismiss = { sheet = TeamSheet.Member(s.slot) }) { move ->
                val ids = m.moveIds.toMutableList()
                if (s.index < ids.size) ids[s.index] = move.id else if (move.id !in ids) ids += move.id
                container.setMember(m.copy(moves = ids.distinct().take(4).joinToString(",")))
                sheet = TeamSheet.Member(s.slot)
            }
        }
    }

    if (renaming) {
        var name by rememberSaveable { mutableStateOf(t.name) }
        AlertDialog(
            onDismissRequest = { renaming = false },
            title = { Text(stringResource(R.string.rename_team)) },
            text = { OutlinedTextField(value = name, onValueChange = { name = it.take(40) }, singleLine = true) },
            confirmButton = {
                TextButton(onClick = { if (name.isNotBlank()) container.updateTeam(t.copy(name = name.trim())); renaming = false }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = { TextButton(onClick = { renaming = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_named, t.name)) },
            text = { Text(stringResource(R.string.delete_team_body)) },
            confirmButton = { TextButton(onClick = { confirmDelete = false; container.deleteTeam(teamId); onBack() }) { Text(stringResource(R.string.delete)) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun EmptySlot(onClick: () -> Unit) {
    val dex = LocalDexColors.current
    Row(
        Modifier.fillMaxWidth().height(86.dp).clip(RoundedCornerShape(14.dp)).border(1.5.dp, dex.divider, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Filled.Add, null, tint = dex.subtleText)
        Text("  " + stringResource(R.string.add_pokemon), color = dex.subtleText, fontSize = 17.sp)
    }
}

// ---- analysis ---------------------------------------------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TeamAnalysis(dex: Dex, game: Game, members: List<TeamMemberEntity>) {
    val sub = LocalDexColors.current.subtleText
    val forms = members.mapNotNull { dex.pokemon(it.formId) }
    val memberTypes = forms.map { dex.typesOf(it, game) }
    val res = LocalResources.current
    val types = dex.typesIn(game)
    Column {
        Section(stringResource(R.string.defensive_coverage), MaterialTheme.colorScheme.primary) {
            Text(stringResource(R.string.defensive_coverage_note), color = sub,
                style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
            Row(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                Text(stringResource(R.string.col_attack), Modifier.weight(1.4f), color = sub, fontSize = 13.sp, textAlign = TextAlign.Center)
                Text(stringResource(R.string.col_weak), Modifier.weight(1f), color = sub, fontSize = 13.sp, textAlign = TextAlign.Center)
                Text(stringResource(R.string.col_resist), Modifier.weight(1f), color = sub, fontSize = 13.sp, textAlign = TextAlign.Center)
                Text(stringResource(R.string.col_immune), Modifier.weight(1f), color = sub, fontSize = 13.sp, textAlign = TextAlign.Center)
            }
            types.forEach { t ->
                val factors = memberTypes.map { dex.factorAgainst(t.id, it, game) }
                val weak = factors.count { it > 100 }
                val resist = factors.count { it in 1..99 }
                val immune = factors.count { it == 0 }
                val danger = weak >= 2 && weak > resist + immune
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    TypePill(t.id, t.name, Modifier.weight(1.4f), height = 28.dp)
                    CountCell(weak, if (danger) Color(0xFFE53935) else MaterialTheme.colorScheme.onSurface, Modifier.weight(1f))
                    CountCell(resist, Color(0xFF43A047), Modifier.weight(1f))
                    CountCell(immune, Color(0xFF1E88E5), Modifier.weight(1f))
                }
            }
        }

        // Offence: each member's chosen damaging moves, or its own types (STAB) while it has none chosen.
        val chosenByMember = members.map { m -> m.moveIds.mapNotNull { dex.move(it, game) }.filter { it.category != 1 } }
        val chosen = chosenByMember.flatten()
        val attackTypes = chosenByMember.flatMapIndexed { i, moves ->
            if (moves.isNotEmpty()) moves.map { it.type } else memberTypes.getOrElse(i) { emptyList() }
        }.distinct()
        val covered = types.filter { def -> attackTypes.any { atk -> dex.factor(atk, def.id, game.dataGen) > 100 } }
        val uncovered = types - covered.toSet()
        Section(stringResource(R.string.offensive_coverage), MaterialTheme.colorScheme.primary) {
            Text(
                stringResource(if (chosen.isNotEmpty()) R.string.offensive_coverage_moves else R.string.offensive_coverage_types),
                color = sub, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            )
            TypeGroup(stringResource(R.string.super_effective_against), covered)
            TypeGroup(stringResource(R.string.nothing_super_effective), uncovered)
        }

        val warnings = buildList {
            types.forEach { t ->
                val weak = memberTypes.count { dex.factorAgainst(t.id, it, game) > 100 }
                val answers = memberTypes.count { dex.factorAgainst(t.id, it, game) < 100 }
                if (weak >= 3) add(res.getString(if (answers == 0) R.string.warn_weak_none_resist else R.string.warn_weak, weak, t.name))
            }
            if (uncovered.size > types.size / 3) add(res.getString(R.string.warn_coverage, uncovered.size))
            if (members.size < Backup.TEAM_SIZE) (Backup.TEAM_SIZE - members.size).let { add(res.getQuantityString(R.plurals.warn_empty_slots, it, it)) }
        }
        Section(stringResource(R.string.summary), MaterialTheme.colorScheme.primary) {
            val totals = forms.map { p -> dex.baseStats(p, game).sumOf { it.base } }
            Text(stringResource(R.string.average_bst, if (totals.isEmpty()) 0 else totals.average().toInt()), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            if (warnings.isEmpty()) EmptyNote(stringResource(R.string.no_weaknesses))
            warnings.forEach { Text("• $it", modifier = Modifier.padding(top = 6.dp)) }
        }
    }
}

@Composable
private fun CountCell(n: Int, color: Color, modifier: Modifier) {
    Text(if (n == 0) "–" else n.toString(), modifier, color = if (n == 0) LocalDexColors.current.subtleText else color,
        fontSize = 16.sp, textAlign = TextAlign.Center)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TypeGroup(title: String, list: List<SeedType>) {
    Text(title, color = LocalDexColors.current.subtleText, fontSize = 15.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 4.dp))
    if (list.isEmpty()) Text("—", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    else FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp), maxItemsInEachRow = 3) {
        list.forEach { TypePill(it.id, it.name, Modifier.weight(1f)) }
    }
}

// ---- sheets -------------------------------------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PokemonPickerSheet(dex: Dex, game: Game, artwork: Map<Int, String>, onDismiss: () -> Unit, onPick: (PokemonSummary) -> Unit) {
    var query by remember { mutableStateOf("") }
    val all = remember(game) { dex.pokemonFor(game) }
    val shown = all.filter { query.isBlank() || it.name.contains(query.trim(), true) || it.dexNumber.toString() == query.trim() }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxHeight(0.92f).padding(horizontal = 12.dp)) {
            OutlinedTextField(value = query, onValueChange = { query = it }, singleLine = true, placeholder = { Text(stringResource(R.string.name_or_number_in, gameName(game))) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                items(shown, key = { it.id }) { p ->
                    PokemonCard(p, p.types.map { dex.type(it)?.name ?: "" }, Images.artwork(p.formId, artwork), false, false,
                        onFavorite = null, onCaught = null, onClick = { onPick(p) }, height = 72.dp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MemberSheet(
    dex: Dex, game: Game, m: TeamMemberEntity, onDismiss: () -> Unit, onForm: (Int) -> Unit, onMove: (Int) -> Unit,
    onClearMove: (Int) -> Unit, onOpenEntry: () -> Unit, onRemove: () -> Unit,
) {
    val species = dex.speciesOf(m.speciesId) ?: return
    val forms = dex.formsIn(species, game)
    val moves = m.moveIds.mapNotNull { dex.move(it, game) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 32.dp)) {
            Text(dex.pokemon(m.formId)?.name ?: species.name, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp))
            if (forms.size > 1) {
                Row(Modifier.horizontalScroll(rememberScrollState()).padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    forms.forEach { f ->
                        val label = if (f.isDefault) f.form.ifEmpty { species.name } else f.form.ifEmpty { f.name }
                        HeaderChip(label, f.id == m.formId, { onForm(f.id) }, caret = false)
                    }
                }
            }
            Text(stringResource(R.string.section_moves), color = LocalDexColors.current.subtleText, fontSize = 15.sp, modifier = Modifier.padding(bottom = 6.dp))
            if (game.isNational) EmptyNote(stringResource(R.string.team_pick_game))
            else for (i in 0 until 4) {
                val mv = moves.getOrNull(i)
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(12.dp))
                        .border(1.dp, LocalDexColors.current.divider, RoundedCornerShape(12.dp)).clickable { onMove(if (mv == null) moves.size else i) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (mv == null) Text("+ " + stringResource(R.string.add_move), color = LocalDexColors.current.subtleText, modifier = Modifier.weight(1f))
                    else {
                        Text(mv.name, fontSize = 17.sp, modifier = Modifier.weight(1f))
                        TypePill(mv.type, dex.type(mv.type)?.name ?: "", Modifier.width(92.dp), height = 26.dp)
                        IconButton(onClick = { onClearMove(i) }, modifier = Modifier.size(36.dp)) { Icon(Icons.Filled.Delete, stringResource(R.string.remove_move), tint = LocalDexColors.current.subtleText) }
                    }
                }
                if (mv == null) break
            }
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TextButton(onClick = onOpenEntry, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.open_entry)) }
                TextButton(onClick = onRemove, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.remove_from_team), color = Color(0xFFE53935)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MovePickerSheet(dex: Dex, game: Game, m: TeamMemberEntity, onDismiss: () -> Unit, onPick: (MoveInfo) -> Unit) {
    var query by remember { mutableStateOf("") }
    val moves by produceState<List<MoveInfo>?>(null, game, m.formId) {
        value = withContext(Dispatchers.Default) { dex.pokemon(m.formId)?.let { dex.learnableMoves(it, game, dex.learnsets(game)) } ?: emptyList() }
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxHeight(0.92f).padding(horizontal = 12.dp)) {
            OutlinedTextField(value = query, onValueChange = { query = it }, singleLine = true, placeholder = { Text(stringResource(R.string.filter_moves)) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
            val list = moves ?: return@Column Loading()
            if (list.isEmpty()) EmptyNote(stringResource(R.string.team_moves_no_data, gameName(game)))
            LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                items(list.filter { query.isBlank() || it.name.contains(query.trim(), true) }, key = { it.id }) { mv ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onPick(mv) }.padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(mv.name, fontSize = 17.sp)
                            Text(stringResource(R.string.power_accuracy, if (mv.power > 1) mv.power.toString() else "—", if (mv.accuracy > 0) mv.accuracy.toString() else "—"),
                                color = LocalDexColors.current.subtleText, fontSize = 13.sp)
                        }
                        TypePill(mv.type, dex.type(mv.type)?.name ?: "", Modifier.width(92.dp), height = 26.dp)
                        CategoryBadge(mv.category, Modifier.width(84.dp))
                    }
                    HorizontalDivider(color = LocalDexColors.current.divider)
                }
            }
        }
    }
}
