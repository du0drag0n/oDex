package io.lemoncore.opendatadex.ui.games

import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.lemoncore.opendatadex.AppContainer
import io.lemoncore.opendatadex.R
import io.lemoncore.opendatadex.data.Game
import io.lemoncore.opendatadex.data.GamePlatform
import io.lemoncore.opendatadex.data.save.GameSave
import io.lemoncore.opendatadex.data.save.Gen12Save
import io.lemoncore.opendatadex.data.user.GameCopyEntity
import io.lemoncore.opendatadex.data.user.MAX_SAVE_BYTES
import io.lemoncore.opendatadex.data.user.readSaveFile
import io.lemoncore.opendatadex.ui.components.copyTitle
import io.lemoncore.opendatadex.ui.components.gameName
import io.lemoncore.opendatadex.ui.theme.LocalDexColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** A file the trainer picked that oDex could read. */
private class PickedSave(val save: GameSave, val name: String, val uri: Uri, val bytes: ByteArray)

/**
 * The save-file import: picks a file, reads it, and asks which game copy it belongs to before importing. Returns the
 * function that starts it, whose argument starts the dialog's link switch on; [onImported] gets the copy the save went into.
 */
@Composable
fun rememberSaveImport(container: AppContainer, copies: List<GameCopyEntity>, onImported: (Long) -> Unit): (Boolean) -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var read by remember { mutableStateOf<PickedSave?>(null) }
    var linkFirst by rememberSaveable { mutableStateOf(false) }
    var problem by remember { mutableStateOf<Int?>(null) }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val name = withContext(Dispatchers.IO) { runCatching { displayName(context, uri) }.getOrNull() } ?: "save.sav"
            val bytes = readSaveFile(context, uri)
            val save = withContext(Dispatchers.Default) {
                runCatching { bytes?.takeIf { it.size <= MAX_SAVE_BYTES }?.let { GameSave.read(it, name) } }.getOrNull()
            }
            when {
                save != null && bytes != null -> read = PickedSave(save, name, uri, bytes)
                bytes != null && Gen12Save.isCartridgeSized(bytes) -> problem = R.string.import_save_blank
                else -> problem = R.string.import_save_unreadable
            }
        }
    }
    read?.let { picked ->
        ImportDialog(picked.save, { container.copiesMatching(it, copies) }, linkFirst, onDismiss = { read = null }) { chosen, into, platform, link ->
            read = null
            scope.launch {
                val (copyId, added) = container.importSave(chosen, picked.name, into, platform)
                Toast.makeText(context, context.getString(R.string.import_save_done, chosen.owned.size, added), Toast.LENGTH_LONG).show()
                if (link && !container.saveLinks.link(copyId, picked.uri, picked.name, picked.bytes)) {
                    Toast.makeText(context, context.getString(R.string.save_link_failed), Toast.LENGTH_LONG).show()
                }
                onImported(copyId)
            }
        }
    }
    problem?.let { message ->
        AlertDialog(
            onDismissRequest = { problem = null },
            title = { Text(stringResource(R.string.import_save_title)) },
            text = { Text(stringResource(message)) },
            confirmButton = { TextButton(onClick = { problem = null }) { Text(stringResource(R.string.ok)) } },
        )
    }
    return { link ->
        linkFirst = link
        pick.launch(arrayOf("*/*"))
    }
}

private fun displayName(context: android.content.Context, uri: Uri): String? =
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
        if (c.moveToFirst()) c.getString(0) else null
    }

/** Where a save file can have come from. A copy on anything else (a download) keeps its platform and isn't asked. */
private val SAVE_SOURCES = listOf(GamePlatform.CARTRIDGE, GamePlatform.EMULATOR)

/**
 * What the file holds, which game it is when the file doesn't say, whether it updates a copy already tracking this
 * trainer or becomes a new one, and whether it came from a cartridge or an emulator, which decides what it can trade with.
 */
@Composable
private fun ImportDialog(
    read: GameSave, copiesFor: (GameSave) -> List<GameCopyEntity>, linkFirst: Boolean, onDismiss: () -> Unit,
    onImport: (GameSave, GameCopyEntity?, GamePlatform, Boolean) -> Unit,
) {
    val c = LocalDexColors.current
    var game by remember { mutableStateOf(read.game) }
    val save = read.copy(game = game)
    val matching = remember(game) { copiesFor(save) }
    var into by remember(game) { mutableStateOf(matching.firstOrNull()) }
    var source by remember(into) { mutableStateOf(into?.let { GamePlatform.fromName(it.platform) }) }
    var link by remember { mutableStateOf(linkFirst) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.import_save_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.width(6.dp).height(40.dp).clip(RoundedCornerShape(3.dp)).background(Color(save.game.color)))
                    Column(Modifier.padding(start = 10.dp)) {
                        Text(gameName(save.game), fontSize = 18.sp)
                        Text(stringResource(R.string.import_save_trainer, save.trainerName, "%05d".format(save.trainerId), save.playMinutes / 60),
                            color = c.subtleText, fontSize = 14.sp)
                    }
                }
                Text(stringResource(R.string.import_save_counts, save.owned.size, save.all.size), fontSize = 15.sp)
                if (read.gameOptions.isNotEmpty()) Column(Modifier.padding(top = 4.dp)) {
                    Text(stringResource(R.string.import_save_which_game), color = c.subtleText, fontSize = 14.sp)
                    read.gameOptions.forEach { option: Game -> Choice(gameName(option), game == option) { game = option } }
                }
                Column(Modifier.padding(top = 4.dp)) {
                    matching.forEach { copy -> Choice(stringResource(R.string.import_save_into, copyTitle(copy)), into == copy) { into = copy } }
                    Choice(stringResource(R.string.import_save_new_copy), into == null) { into = null }
                }
                if (source == null || source in SAVE_SOURCES) Column(Modifier.padding(top = 4.dp)) {
                    Text(stringResource(R.string.import_save_source), color = c.subtleText, fontSize = 14.sp)
                    Choice(stringResource(R.string.import_save_cartridge), source == GamePlatform.CARTRIDGE) { source = GamePlatform.CARTRIDGE }
                    Choice(stringResource(R.string.platform_emulator), source == GamePlatform.EMULATOR) { source = GamePlatform.EMULATOR }
                }
                Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(stringResource(R.string.import_save_link), fontSize = 15.sp)
                        Text(stringResource(R.string.import_save_link_body), color = c.subtleText, fontSize = 13.sp)
                    }
                    Switch(checked = link, onCheckedChange = { link = it })
                }
                Text(stringResource(R.string.import_save_note), color = c.subtleText, fontSize = 13.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = { source?.let { onImport(save, into, it, link) } }, enabled = source != null) { Text(stringResource(R.string.import_action)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun Choice(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label, fontSize = 15.sp)
    }
}
