package io.lemoncore.opendatadex.ui.games

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Link
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import java.text.DateFormat
import java.util.Date
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.lemoncore.opendatadex.AppContainer
import io.lemoncore.opendatadex.R
import io.lemoncore.opendatadex.data.Dex
import io.lemoncore.opendatadex.data.Game
import io.lemoncore.opendatadex.data.GamePlatform
import io.lemoncore.opendatadex.data.seed.DataLanguage
import io.lemoncore.opendatadex.data.user.GameCopyEntity
import io.lemoncore.opendatadex.data.user.SaveImportEntity
import io.lemoncore.opendatadex.data.user.SaveLinkEntity
import io.lemoncore.opendatadex.data.user.SaveLinkProblem
import io.lemoncore.opendatadex.data.user.SaveLinkResult
import io.lemoncore.opendatadex.ui.components.EmptyNote
import io.lemoncore.opendatadex.ui.components.HeaderChip
import io.lemoncore.opendatadex.ui.components.Loading
import io.lemoncore.opendatadex.ui.components.LocalWidthClass
import io.lemoncore.opendatadex.ui.components.cardRows
import io.lemoncore.opendatadex.ui.components.copyDetails
import io.lemoncore.opendatadex.ui.components.copyTitle
import io.lemoncore.opendatadex.ui.components.gameName
import io.lemoncore.opendatadex.ui.dexes.DexScaffold
import io.lemoncore.opendatadex.ui.pokedex.GamePickerSheet
import io.lemoncore.opendatadex.ui.pokedex.OptionSheet
import io.lemoncore.opendatadex.ui.theme.LocalDexColors
import kotlinx.coroutines.launch

/**
 * My Games: the copies of games the trainer owns or plays (two Emeralds, a Japanese FireRed, an emulator save).
 * Each copy has its own caught checklist; tapping one tracks it in the Pokédex.
 */
@Composable
fun MyGamesScreen(
    container: AppContainer, dex: Dex, currentGame: Game, onOpenDrawer: () -> Unit, onOpenBoxes: (Long) -> Unit, onTrack: (GameCopyEntity) -> Unit,
) {
    val copies by container.gameCopies.collectAsState(initial = null)
    val caught by container.allCaught.collectAsState(initial = emptyList())
    val imports by container.saveImports.collectAsState(initial = emptyList())
    val links by container.saveLinks.links.collectAsState(initial = emptyList())
    var editing by remember { mutableStateOf<GameCopyEntity?>(null) }
    var adding by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val importSave = rememberSaveImport(container, copies.orEmpty(), onOpenBoxes)
    val locale = LocalConfiguration.current.locales[0]
    val dateFormat = remember(locale) { DateFormat.getDateInstance(DateFormat.MEDIUM, locale) }
    DexScaffold(stringResource(R.string.nav_my_games), onOpenDrawer, isRoot = true, filterable = false) { _, padding ->
        val all = copies ?: return@DexScaffold Loading()
        val byKey = caught.groupBy({ it.game }, { it.speciesId })
        val perRow = LocalWidthClass.current.cardsPerRow
        LazyColumn(
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = padding.calculateTopPadding() + 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                if (perRow > 1) Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AddCopyButton(Modifier.weight(1f).height(52.dp)) { adding = true }
                    ImportSaveButton(Modifier.weight(1f).height(52.dp)) { importSave(false) }
                } else Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AddCopyButton(Modifier.fillMaxWidth().height(52.dp)) { adding = true }
                    ImportSaveButton(Modifier.fillMaxWidth().height(48.dp)) { importSave(false) }
                }
            }
            if (all.isEmpty()) item { EmptyNote(stringResource(R.string.my_games_intro)) }
            cardRows(all, perRow, { it.id }) { copy, modifier ->
                val game = Game.fromKey(copy.game)
                val total = remember(game, copy.nationalDex) { dex.pokemonFor(game, copy.nationalDex).map { it.id }.toSet() }
                val active by container.settings.activeList(game).collectAsState(initial = null)
                val import = imports.firstOrNull { it.copyId == copy.id }
                CopyCard(copy, game, byKey[copy.key].orEmpty().count { it in total }, total.size,
                    tracked = game == currentGame && active == copy.key, import = import, imported = import?.let { dateFormat.format(Date(it.importedAt)) },
                    link = links.firstOrNull { it.copyId == copy.id }, modifier = modifier, onClick = { onTrack(copy) },
                    onEdit = { editing = copy }, onBoxes = { onOpenBoxes(copy.id) })
            }
        }
    }
    if (adding) GameCopyDialog(null, if (currentGame.isNational) Game.EMERALD else currentGame, onDismiss = { adding = false }) { copy ->
        adding = false
        scope.launch { container.createGameCopy(copy) }
    }
    editing?.let { copy ->
        GameCopyDialog(
            copy, Game.fromKey(copy.game), onDismiss = { editing = null }, onDelete = { editing = null; container.deleteGameCopy(copy) },
            link = links.firstOrNull { it.copyId == copy.id },
            onLinkSave = { editing = null; importSave(true) },
            onRefreshLink = {
                editing = null
                scope.launch {
                    val message = when (val result = container.saveLinks.refresh(copy.id)) {
                        is SaveLinkResult.Updated -> context.getString(R.string.save_link_updated, result.fileName, result.newCatches)
                        is SaveLinkResult.Failed -> context.getString(result.problem.message)
                        else -> context.getString(R.string.save_link_current)
                    }
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                }
            },
            onUnlink = { scope.launch { container.saveLinks.unlink(copy.id) } },
        ) {
            editing = null
            container.updateGameCopy(it)
        }
    }
}

@Composable
private fun AddCopyButton(modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier.clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.primary).clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Filled.Add, null, tint = MaterialTheme.colorScheme.onPrimary)
        Text("  " + stringResource(R.string.add_game_copy), color = MaterialTheme.colorScheme.onPrimary, fontSize = 17.sp)
    }
}

@Composable
private fun ImportSaveButton(modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier.clip(RoundedCornerShape(14.dp)).border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp)).clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Filled.FileOpen, null, tint = MaterialTheme.colorScheme.primary)
        Text("  " + stringResource(R.string.import_save), color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)
    }
}

@Composable
private fun CopyCard(
    copy: GameCopyEntity, game: Game, caught: Int, total: Int, tracked: Boolean, import: SaveImportEntity?, imported: String?, link: SaveLinkEntity?,
    modifier: Modifier, onClick: () -> Unit, onEdit: () -> Unit, onBoxes: () -> Unit,
) {
    val color = Color(game.color)
    val fraction = if (total == 0) 0f else caught.toFloat() / total
    Surface(shape = RoundedCornerShape(14.dp), color = LocalDexColors.current.sectionCard, shadowElevation = 1.dp,
        modifier = modifier.clickable(onClick = onClick)) {
        Row(Modifier.padding(start = 12.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(8.dp).fillMaxHeight().clip(RoundedCornerShape(50)).background(color))
            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                Text(copyTitle(copy), color = MaterialTheme.colorScheme.primary, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(copyDetails(copy), color = LocalDexColors.current.subtleText, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                    LinearProgressIndicator(
                        progress = { fraction }, color = color, trackColor = color.copy(alpha = 0.2f),
                        modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(50)),
                    )
                    Text("  $caught / $total", color = LocalDexColors.current.subtleText, fontSize = 14.sp)
                }
                val notes = listOfNotNull(
                    stringResource(R.string.national_pokedex).takeIf { copy.nationalDex && game.hasNationalDex },
                    stringResource(R.string.tracked_in_pokedex).takeIf { tracked },
                )
                if (notes.isNotEmpty()) Text(notes.joinToString(" · "), color = color, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                if (imported != null) Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.save_imported, imported), color = LocalDexColors.current.subtleText, fontSize = 13.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        import?.let { saved ->
                            val badges = saved.badges?.takeIf { game.badgeTotal > 0 }?.let { stringResource(R.string.badges_count, it.countOneBits(), game.badgeTotal) }
                            Text(listOfNotNull(badges, stringResource(R.string.hours_played, saved.playMinutes / 60)).joinToString(" · "),
                                color = LocalDexColors.current.subtleText, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    TextButton(onClick = onBoxes) { Text(stringResource(R.string.tab_boxes)) }
                }
                link?.let {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Link, null, tint = LocalDexColors.current.subtleText, modifier = Modifier.size(16.dp))
                        Text(stringResource(R.string.save_link_file, it.fileName), color = LocalDexColors.current.subtleText, fontSize = 13.sp,
                            modifier = Modifier.padding(start = 4.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    SaveLinkProblem.fromName(it.problem)?.let { problem ->
                        Text(stringResource(problem.message), color = MaterialTheme.colorScheme.error, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }
            IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, stringResource(R.string.edit_game_copy)) }
        }
    }
}

/** Adds a copy ([initial] null) or edits one, along with the save file linked to it. */
@Composable
internal fun GameCopyDialog(
    initial: GameCopyEntity?, initialGame: Game, onDismiss: () -> Unit, onDelete: (() -> Unit)? = null, link: SaveLinkEntity? = null,
    onLinkSave: (() -> Unit)? = null, onRefreshLink: () -> Unit = {}, onUnlink: () -> Unit = {}, onSave: (GameCopyEntity) -> Unit,
) {
    var game by remember { mutableStateOf(initialGame) }
    var label by rememberSaveable { mutableStateOf(initial?.label.orEmpty()) }
    var player by rememberSaveable { mutableStateOf(initial?.playerName.orEmpty()) }
    var trainerId by rememberSaveable { mutableStateOf(initial?.trainerId.orEmpty()) }
    var language by rememberSaveable { mutableStateOf(initial?.language) }
    var platform by remember { mutableStateOf(GamePlatform.fromName(initial?.platform)) }
    var national by rememberSaveable { mutableStateOf(initial?.nationalDex ?: false) }
    var picking by remember { mutableStateOf(false) }
    var choosingLanguage by remember { mutableStateOf(false) }
    var choosingPlatform by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (initial == null) R.string.add_game_copy else R.string.edit_game_copy)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                io.lemoncore.opendatadex.ui.components.GameChip(game, gameName(game), { picking = true }, Modifier.fillMaxWidth())
                OutlinedTextField(value = label, onValueChange = { label = it.take(40) }, singleLine = true,
                    label = { Text(stringResource(R.string.copy_label)) }, placeholder = { Text(stringResource(R.string.copy_label_hint)) })
                OutlinedTextField(value = player, onValueChange = { player = it.take(12) }, singleLine = true,
                    label = { Text(stringResource(R.string.player_name)) })
                OutlinedTextField(value = trainerId, onValueChange = { v -> trainerId = v.filter { it.isDigit() }.take(6) }, singleLine = true,
                    label = { Text(stringResource(R.string.trainer_id)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                Choice(stringResource(R.string.copy_language),
                    language?.let { code -> DataLanguage.entries.firstOrNull { it.code == code }?.label } ?: stringResource(R.string.language_not_set)) { choosingLanguage = true }
                Choice(stringResource(R.string.played_on), stringResource(platform.label)) { choosingPlatform = true }
                if (game.hasNationalDex) Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.national_unlocked), fontSize = 16.sp)
                        Text(stringResource(R.string.national_unlocked_body, game.nationalDexSize), color = LocalDexColors.current.subtleText, fontSize = 13.sp)
                    }
                    Switch(checked = national, onCheckedChange = { national = it })
                }
                if (onLinkSave != null) Column {
                    Text(stringResource(R.string.save_link_section), color = LocalDexColors.current.subtleText, fontSize = 13.sp, modifier = Modifier.padding(start = 4.dp))
                    Text(link?.fileName ?: stringResource(R.string.save_link_none), fontSize = 15.sp, modifier = Modifier.padding(start = 4.dp, top = 2.dp))
                    FlowRow {
                        TextButton(onClick = onLinkSave) { Text(stringResource(if (link == null) R.string.save_link_add else R.string.save_link_change)) }
                        if (link != null) {
                            TextButton(onClick = onRefreshLink) { Text(stringResource(R.string.save_link_update_now)) }
                            TextButton(onClick = onUnlink) { Text(stringResource(R.string.save_link_remove)) }
                        }
                    }
                }
                if (onDelete != null) TextButton(onClick = { confirmDelete = true }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(GameCopyEntity(
                    id = initial?.id ?: 0, game = game.key, label = label.trim(), playerName = player.trim(), trainerId = trainerId,
                    language = language, platform = platform.name, nationalDex = national && game.hasNationalDex,
                    createdAt = initial?.createdAt ?: 0,
                ))
            }) { Text(stringResource(if (initial == null) R.string.create else R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
    if (picking) GamePickerSheet(game, onPick = { game = it; picking = false }, onDismiss = { picking = false }, includeNational = false)
    if (choosingLanguage) OptionSheet(
        stringResource(R.string.copy_language),
        listOf<Pair<String?, String>>(null to stringResource(R.string.language_not_set)) + DataLanguage.entries.map { it.code to it.label },
        onPick = { language = it; choosingLanguage = false }, onDismiss = { choosingLanguage = false },
    )
    if (choosingPlatform) OptionSheet(
        stringResource(R.string.played_on), GamePlatform.entries.map { it to stringResource(it.label) },
        onPick = { platform = it; choosingPlatform = false }, onDismiss = { choosingPlatform = false },
    )
    if (confirmDelete && initial != null) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text(stringResource(R.string.delete_named, copyTitle(initial))) },
        text = { Text(stringResource(R.string.delete_copy_body, gameName(Game.fromKey(initial.game)))) },
        confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete?.invoke() }) { Text(stringResource(R.string.delete)) } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } },
    )
}

/** A labelled value that opens a chooser. */
@Composable
private fun Choice(title: String, value: String, onClick: () -> Unit) {
    Column {
        Text(title, color = LocalDexColors.current.subtleText, fontSize = 13.sp, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
        HeaderChip(value, false, onClick, Modifier.fillMaxWidth())
    }
}
