package io.lemoncore.opendatadex.ui.settings

import io.lemoncore.opendatadex.ui.MENU_POKEDEX
import io.lemoncore.opendatadex.ui.menuEntries
import android.text.format.DateUtils
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ManageSearch
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.SingletonImageLoader
import io.lemoncore.opendatadex.AppContainer
import io.lemoncore.opendatadex.BuildConfig
import io.lemoncore.opendatadex.R
import io.lemoncore.opendatadex.data.ImageStyle
import io.lemoncore.opendatadex.data.Settings
import io.lemoncore.opendatadex.data.TabletList
import io.lemoncore.opendatadex.data.ThemeMode
import io.lemoncore.opendatadex.data.seed.DataLanguage
import io.lemoncore.opendatadex.data.user.AutoBackup
import io.lemoncore.opendatadex.data.user.RestoreSummary
import io.lemoncore.opendatadex.ui.components.GameSwatch
import io.lemoncore.opendatadex.ui.components.LocalWidthClass
import io.lemoncore.opendatadex.ui.components.gameName
import io.lemoncore.opendatadex.ui.theme.DexType
import io.lemoncore.opendatadex.ui.theme.JetBrainsMono
import io.lemoncore.opendatadex.ui.theme.LocalDexColors
import io.lemoncore.opendatadex.ui.theme.oklch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

private enum class LanguageChoice { APP, DATA }

/** The five Settings groups; only one is open at a time. */
private enum class SettingsGroup { LOOK, POKEDEX, MARKING, FEEL, DATA }

@Composable
fun SettingsScreen(
    container: AppContainer, settings: Settings, onBack: () -> Unit, onPickGame: () -> Unit, onMenu: () -> Unit, onSupport: () -> Unit, onAbout: () -> Unit,
) {
    val store = container.settings
    val scope = rememberCoroutineScope()
    val res = LocalResources.current
    var open by rememberSaveable { mutableStateOf<SettingsGroup?>(SettingsGroup.LOOK) }
    var choosingLanguage by remember { mutableStateOf<LanguageChoice?>(null) }
    var backingUp by remember { mutableStateOf(false) }
    var managingData by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val files = rememberBackupFiles(container) { message = it }
    fun toggle(g: SettingsGroup) { open = if (open == g) null else g }
    fun onOff(on: Boolean) = res.getString(if (on) R.string.state_on else R.string.state_off)

    SettingsScaffold(stringResource(R.string.nav_settings), onBack) {
        Column(Modifier.padding(horizontal = 12.dp).padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            GroupCard(
                Icons.Filled.Brush, oklch(0.70f, 0.13f, 300f), stringResource(R.string.settings_look),
                stringResource(R.string.settings_look_summary, stringResource(settings.theme.label), stringResource(settings.imageStyle.label)),
                open == SettingsGroup.LOOK, { toggle(SettingsGroup.LOOK) },
            ) {
                SegmentedRow(stringResource(R.string.theme), listOf(ThemeMode.SYSTEM, ThemeMode.DARK, ThemeMode.LIGHT).map { it to stringResource(it.label) }, settings.theme) {
                    scope.launch { store.setTheme(it) }
                }
                RowDivider()
                ToggleRow(stringResource(R.string.tint_from_game), stringResource(R.string.tint_from_game_summary, gameName(settings.game)), settings.tintFromGame) {
                    scope.launch { store.setTintFromGame(it) }
                }
                RowDivider()
                SegmentedRow(stringResource(R.string.pokemon_images), ImageStyle.entries.map { it to stringResource(it.label) }, settings.imageStyle) {
                    scope.launch { store.setImageStyle(it) }
                }
                RowDivider()
                // Only a tablet lays the Pokédex out differently.
                if (LocalWidthClass.current.large) {
                    SegmentedRow(stringResource(R.string.tablet_list), TabletList.entries.map { it to stringResource(it.label) }, settings.tabletList) {
                        scope.launch { store.setTabletList(it) }
                    }
                    RowDivider()
                }
                val shown = menuEntries.count { it.key == MENU_POKEDEX || it.key !in settings.menuHidden }
                NavRow(stringResource(R.string.menu_customise), value = stringResource(R.string.menu_customise_summary, shown, menuEntries.size), onClick = onMenu)
                RowDivider()
                NavRow(stringResource(R.string.app_language), value = settings.appLanguage?.label ?: stringResource(R.string.language_phone)) {
                    choosingLanguage = LanguageChoice.APP
                }
            }
            GroupCard(
                Icons.AutoMirrored.Filled.ManageSearch, oklch(0.72f, 0.14f, 25f), stringResource(R.string.nav_pokedex),
                stringResource(R.string.settings_pokedex_summary, gameName(settings.game), settings.dataLanguage.label),
                open == SettingsGroup.POKEDEX, { toggle(SettingsGroup.POKEDEX) },
            ) {
                NavRow(stringResource(R.string.current_game), subtitle = gameName(settings.game), leading = { GameSwatch(settings.game, 20.dp) }, onClick = onPickGame)
                RowDivider()
                NavRow(stringResource(R.string.pokemon_names), value = settings.dataLanguage.label) { choosingLanguage = LanguageChoice.DATA }
                RowDivider()
                ToggleRow(stringResource(R.string.regional_forms), stringResource(if (settings.regionalForms) R.string.regional_forms_on else R.string.regional_forms_off), settings.regionalForms) {
                    scope.launch { store.setRegionalForms(it) }
                }
                RowDivider()
                ToggleRow(stringResource(R.string.battle_forms), stringResource(if (settings.battleForms) R.string.battle_forms_on else R.string.battle_forms_off), settings.battleForms) {
                    scope.launch { store.setBattleForms(it) }
                }
                RowDivider()
                ToggleRow(stringResource(R.string.hide_caught), stringResource(if (settings.hideCaught) R.string.hide_caught_on else R.string.hide_caught_off), settings.hideCaught) {
                    scope.launch { store.setHideCaught(it) }
                }
            }
            GroupCard(
                Icons.Filled.CheckCircle, oklch(0.72f, 0.14f, 150f), stringResource(R.string.settings_marking),
                stringResource(if (settings.swipeToMark) R.string.swipe_to_mark else R.string.tap_the_number),
                open == SettingsGroup.MARKING, { toggle(SettingsGroup.MARKING) },
            ) {
                ToggleRow(stringResource(R.string.swipe_to_mark), stringResource(R.string.swipe_to_mark_summary), settings.swipeToMark) { scope.launch { store.setSwipeToMark(it) } }
                RowDivider()
                ToggleRow(stringResource(R.string.undo_unmark), stringResource(R.string.undo_unmark_summary), settings.undoUnmark) { scope.launch { store.setUndoUnmark(it) } }
            }
            GroupCard(
                Icons.Filled.Vibration, oklch(0.75f, 0.13f, 80f), stringResource(R.string.settings_feel),
                stringResource(R.string.settings_feel_summary, onOff(settings.haptics), onOff(settings.sounds)),
                open == SettingsGroup.FEEL, { toggle(SettingsGroup.FEEL) },
            ) {
                ToggleRow(stringResource(R.string.haptics), null, settings.haptics) { scope.launch { store.setHaptics(it) } }
                RowDivider()
                ToggleRow(stringResource(R.string.sounds), stringResource(R.string.sounds_summary), settings.sounds) { scope.launch { store.setSounds(it) } }
                RowDivider()
                ToggleRow(stringResource(R.string.play_cry), null, settings.playCry) { scope.launch { store.setPlayCry(it) } }
                RowDivider()
                ToggleRow(stringResource(R.string.auto_play_cry), stringResource(if (settings.autoPlayCry) R.string.auto_play_cry_on else R.string.auto_play_cry_off), settings.autoPlayCry) {
                    scope.launch { store.setAutoPlayCry(it) }
                }
            }
            val lastBackup = if (settings.autoBackupUri == null || settings.lastAutoBackup == 0L) null
                else DateUtils.getRelativeTimeSpanString(settings.lastAutoBackup, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString()
            GroupCard(
                Icons.Filled.CloudUpload, oklch(0.72f, 0.12f, 230f), stringResource(R.string.settings_data),
                lastBackup?.let { stringResource(R.string.backed_up, it) } ?: stringResource(R.string.never_backed_up),
                open == SettingsGroup.DATA, { toggle(SettingsGroup.DATA) },
            ) {
                NavRow(
                    stringResource(R.string.back_up_and_restore),
                    subtitle = stringResource(if (settings.autoBackupUri != null) R.string.auto_backup else R.string.auto_backup_off_short) +
                        (lastBackup?.let { " · $it" } ?: ""),
                ) { backingUp = true }
                RowDivider()
                NavRow(stringResource(R.string.backup_export), trailingMono = ".JSON", onClick = files.export)
                RowDivider()
                NavRow(stringResource(R.string.backup_restore), subtitle = stringResource(R.string.backup_restore_summary), onClick = files.restore)
                RowDivider()
                OfflineDataRow { managingData = true }
            }
            Spacer(Modifier.height(12.dp))
            Column(Modifier.fillMaxWidth().groupCard(false)) {
                PlainRow(Icons.Filled.Favorite, Color(0xFFFF8A80), stringResource(R.string.support_odex), onClick = onSupport)
                RowDivider()
                PlainRow(Icons.Filled.Info, LocalDexColors.current.textMuted, stringResource(R.string.nav_about), trailing = "v${BuildConfig.VERSION_NAME}", onClick = onAbout)
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    choosingLanguage?.let { choice -> LanguageDialog(choice, settings, { choosingLanguage = null }) { lang ->
        scope.launch { if (choice == LanguageChoice.APP) store.setAppLanguage(lang) else store.setDataLanguage(lang ?: DataLanguage.ENGLISH) }
    } }
    if (backingUp) BackupSheet(container.autoBackup, settings, files.restoredMessage, { backingUp = false }) { message = it }
    if (managingData) ClearImagesDialog({ managingData = false }) { message = it }
    message?.let { m ->
        AlertDialog(onDismissRequest = { message = null }, text = { Text(m) },
            confirmButton = { TextButton(onClick = { message = null }) { Text(stringResource(R.string.ok)) } })
    }
}

/** Export to and restore from a file the trainer picks, and the message a restore shows. */
private class BackupFiles(val export: () -> Unit, val restore: () -> Unit, val restoredMessage: (RestoreSummary) -> String)

@Composable
private fun rememberBackupFiles(container: AppContainer, onMessage: (String) -> Unit): BackupFiles {
    val context = LocalContext.current
    val res = LocalResources.current
    val scope = rememberCoroutineScope()
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            onMessage(runCatching {
                val text = container.backup.export()
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(text.toByteArray()) } ?: error(res.getString(R.string.file_open_failed))
                }
                res.getString(R.string.backup_saved)
            }.getOrElse { res.getString(R.string.backup_failed, it.message.orEmpty()) })
        }
    }
    val restoredMessage: (RestoreSummary) -> String = { r ->
        fun count(id: Int, n: Int) = res.getQuantityString(id, n, n)
        res.getString(
            R.string.restored,
            count(R.plurals.restored_favorites, r.favorites), count(R.plurals.restored_caught, r.caught),
            count(R.plurals.restored_shinies, r.shiny), count(R.plurals.restored_teams, r.teams),
            count(R.plurals.restored_catch_lists, r.catchLists), count(R.plurals.restored_game_copies, r.gameCopies),
        ) + if (r.saves > 0) " " + count(R.plurals.restored_saves, r.saves) else ""
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            onMessage(runCatching {
                val text = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() } ?: error(res.getString(R.string.file_open_failed))
                }
                restoredMessage(container.backup.restore(text))
            }.getOrElse { if (it is IllegalArgumentException) res.getString(R.string.restore_not_backup) else res.getString(R.string.restore_failed) })
        }
    }
    return remember(exportLauncher, restoreLauncher) {
        BackupFiles(
            export = { exportLauncher.launch("oDex-backup-${java.time.LocalDate.now()}.json") },
            restore = { restoreLauncher.launch(arrayOf("application/json", "application/octet-stream", "text/plain")) },
            restoredMessage = restoredMessage,
        )
    }
}

/** Automatic backup: the trainer creates the file once (Google Drive keeps it in the cloud) and oDex keeps it current. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BackupSheet(autoBackup: AutoBackup, settings: Settings, restoredMessage: (RestoreSummary) -> String, onDismiss: () -> Unit, onMessage: (String) -> Unit) {
    val c = LocalDexColors.current
    val res = LocalResources.current
    val scope = rememberCoroutineScope()
    val failure by autoBackup.failure.collectAsState()
    var fileName by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(settings.autoBackupUri) { fileName = settings.autoBackupUri?.let { autoBackup.fileName(it) } }
    val chooseFile = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            runCatching { autoBackup.choose(uri) }.onFailure { onMessage(res.getString(R.string.backup_failed, it.message.orEmpty())) }
        }
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = c.bgRaised) {
        Column(Modifier.navigationBarsPadding().padding(bottom = 16.dp)) {
            Text(stringResource(R.string.back_up_and_restore), color = c.text, style = DexType.sheetTitle, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
            val on = settings.autoBackupUri != null
            ToggleRow(
                stringResource(R.string.auto_backup),
                if (on) stringResource(R.string.auto_backup_on, fileName ?: AutoBackup.FILE_NAME) else stringResource(R.string.auto_backup_off), on,
            ) { turnOn -> if (turnOn) chooseFile.launch(AutoBackup.FILE_NAME) else scope.launch { autoBackup.turnOff() } }
            if (on) {
                val locale = LocalConfiguration.current.locales[0]
                Text(
                    if (settings.lastAutoBackup == 0L) stringResource(R.string.auto_backup_never)
                    else stringResource(R.string.auto_backup_latest, DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale).format(Date(settings.lastAutoBackup))),
                    color = c.accentText, style = DexType.caption, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
                failure?.let {
                    Text(stringResource(R.string.auto_backup_failed, it), color = Color(0xFFFF7E76), style = DexType.caption, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                }
                RowDivider()
                NavRow(stringResource(R.string.auto_backup_now), subtitle = stringResource(R.string.auto_backup_now_summary)) {
                    scope.launch {
                        onMessage(runCatching { res.getString(if (autoBackup.backupNow() != null) R.string.backup_saved else R.string.backup_nothing) }
                            .getOrElse { res.getString(R.string.backup_failed, it.message.orEmpty()) })
                    }
                }
                RowDivider()
                NavRow(stringResource(R.string.auto_backup_restore), subtitle = stringResource(R.string.auto_backup_restore_summary)) {
                    scope.launch {
                        onMessage(runCatching { autoBackup.restore()?.let { restoredMessage(it) } ?: res.getString(R.string.restore_failed) }
                            .getOrElse { if (it is IllegalArgumentException) res.getString(R.string.restore_not_backup) else res.getString(R.string.restore_failed) })
                    }
                }
                RowDivider()
                NavRow(stringResource(R.string.auto_backup_change), subtitle = stringResource(R.string.auto_backup_change_summary)) { chooseFile.launch(AutoBackup.FILE_NAME) }
            } else {
                Text(stringResource(R.string.auto_backup_tip), color = c.textSubtle, style = DexType.caption, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }
            Text(stringResource(R.string.backup_device_note), color = c.textSubtle, style = DexType.caption, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        }
    }
}

/** How much space saved pictures and the trainer's database take, with a way to clear the pictures. */
@Composable
private fun OfflineDataRow(onManage: () -> Unit) {
    val context = LocalContext.current
    var sizes by remember { mutableStateOf<Pair<Long, Long>?>(null) }
    LaunchedEffect(Unit) {
        sizes = withContext(Dispatchers.IO) {
            context.cacheDir.resolve("images").walkTopDown().filter { it.isFile }.sumOf { it.length() } to
                context.getDatabasePath("opendatadex.db").let { db -> listOf(db, java.io.File(db.path + "-wal")).sumOf { it.length() } }
        }
    }
    val c = LocalDexColors.current
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable(onClick = onManage).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.offline_data), color = c.text, style = rowTitle)
            sizes?.let { (images, data) ->
                Text(
                    stringResource(R.string.offline_data_summary, Formatter.formatShortFileSize(context, images), Formatter.formatShortFileSize(context, data)),
                    color = c.textSubtle, style = rowSubtitle,
                )
            }
        }
        Text(stringResource(R.string.manage), color = c.accentText, style = DexType.chip.copy(fontSize = 13.sp))
    }
}

@Composable
private fun ClearImagesDialog(onDismiss: () -> Unit, onDone: (String) -> Unit) {
    val context = LocalContext.current
    val res = LocalResources.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.clear_images)) },
        text = { Text(stringResource(R.string.clear_images_body)) },
        confirmButton = {
            TextButton(onClick = {
                SingletonImageLoader.get(context).let { it.diskCache?.clear(); it.memoryCache?.clear() }
                onDismiss()
                onDone(res.getString(R.string.images_cleared))
            }) { Text(stringResource(R.string.clear)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun LanguageDialog(choice: LanguageChoice, settings: Settings, onDismiss: () -> Unit, onPick: (DataLanguage?) -> Unit) {
    val options: List<Pair<DataLanguage?, String>> =
        (if (choice == LanguageChoice.APP) listOf(null to stringResource(R.string.language_phone)) else emptyList()) + DataLanguage.entries.map { it to it.label }
    val current = if (choice == LanguageChoice.APP) settings.appLanguage else settings.dataLanguage
    val pick = { lang: DataLanguage? -> onPick(lang); onDismiss() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (choice == LanguageChoice.APP) R.string.app_language else R.string.pokemon_names)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                options.forEach { (lang, label) ->
                    Row(Modifier.fillMaxWidth().clickable { pick(lang) }, verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = current == lang, onClick = { pick(lang) })
                        Text(label, fontSize = 17.sp)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
    )
}

// ---- shared pieces (Settings and About) ---------------------------------------------------------------------------------

internal val rowTitle get() = DexType.itemTitle.copy(fontSize = 15.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
internal val rowSubtitle get() = DexType.caption.copy(fontSize = 12.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)

/** A screen with the Settings top bar (back and title over the page, 95% opaque) and a scrolling body. */
@Composable
internal fun SettingsScaffold(
    title: String, onBack: () -> Unit, trailing: (@Composable () -> Unit)? = null, subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = LocalDexColors.current
    Column(Modifier.fillMaxSize().background(c.bg)) {
        Row(
            Modifier.fillMaxWidth().background(c.bg.copy(alpha = 0.95f)).statusBarsPadding().height(60.dp).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), tint = c.text) }
            Column(Modifier.weight(1f)) {
                Text(title, color = c.text, style = DexType.sheetTitle)
                if (subtitle != null) Text(subtitle, color = c.textMuted, style = DexType.caption.copy(fontSize = 11.sp), maxLines = 1)
            }
            trailing?.invoke()
        }
        Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding(), content = content)
    }
}

/** The card behind a group of rows: a little lighter while open. */
@Composable
internal fun Modifier.groupCard(open: Boolean): Modifier {
    val c = LocalDexColors.current
    val bg by animateColorAsState(
        if (c.isDark) (if (open) Color(0xFF0F1013) else Color(0xFF0C0C0F)) else (if (open) Color(0xFFFAFAF9) else Color.White), tween(200), label = "groupBg",
    )
    val border by animateColorAsState(
        if (c.isDark) (if (open) Color(0xFF26272D) else Color(0xFF1A1B1F)) else (if (open) Color(0xFFD4D4D8) else Color(0xFFE4E4E7)), tween(200), label = "groupLine",
    )
    val shape = RoundedCornerShape(18.dp)
    return clip(shape).background(bg).border(1.dp, border, shape)
}

@Composable
private fun GroupCard(
    icon: ImageVector, color: Color, title: String, summary: String, open: Boolean, onToggle: () -> Unit, content: @Composable ColumnScope.() -> Unit,
) {
    val c = LocalDexColors.current
    val turn by animateFloatAsState(if (open) 180f else 0f, tween(200), label = "caret")
    Column(Modifier.fillMaxWidth().groupCard(open)) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 72.dp).clickable(onClick = onToggle).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(color.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = color, modifier = Modifier.size(21.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, color = c.text, style = DexType.sectionTitle.copy(fontSize = 16.sp))
                Text(summary, color = c.textSubtle, style = rowSubtitle.copy(fontSize = 12.5.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Filled.ExpandMore, null, tint = c.textFaint, modifier = Modifier.size(18.dp).rotate(turn))
        }
        AnimatedVisibility(
            open,
            enter = expandVertically(tween(220, easing = FastOutSlowInEasing)) + fadeIn(tween(220, easing = FastOutSlowInEasing)),
            exit = shrinkVertically(tween(220, easing = FastOutSlowInEasing)) + fadeOut(tween(220, easing = FastOutSlowInEasing)),
        ) {
            Column(Modifier.fillMaxWidth()) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(if (c.isDark) Color(0xFF1D1E23) else c.line))
                content()
            }
        }
    }
}

@Composable
internal fun RowDivider(inset: androidx.compose.ui.unit.Dp = 16.dp) {
    val c = LocalDexColors.current
    Box(Modifier.padding(start = inset).fillMaxWidth().height(1.dp).background(if (c.isDark) Color(0xFF1A1B1F) else c.line))
}

/** A row that is all one switch. */
@Composable
internal fun ToggleRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    val c = LocalDexColors.current
    Row(
        Modifier.fillMaxWidth().heightIn(min = if (subtitle != null) 64.dp else 56.dp)
            .toggleable(checked, role = Role.Switch, onValueChange = onChange).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = c.text, style = rowTitle)
            if (subtitle != null) Text(subtitle, color = c.textSubtle, style = rowSubtitle)
        }
        DexSwitch(checked)
    }
}

/** The switch drawn by [ToggleRow]: a 48 by 28 track in the accent when on, and a 20 dp knob. */
@Composable
internal fun DexSwitch(on: Boolean) {
    val c = LocalDexColors.current
    val track by animateColorAsState(if (on) c.accent else if (c.isDark) Color(0xFF26272D) else Color(0xFFD4D4D8), tween(200), label = "track")
    val knob by animateColorAsState(if (on) c.onAccent else if (c.isDark) Color(0xFF8D8E96) else Color.White, tween(200), label = "knob")
    val x by animateDpAsState(if (on) 20.dp else 0.dp, tween(200), label = "knobX")
    Box(Modifier.width(48.dp).height(28.dp).clip(RoundedCornerShape(14.dp)).background(track).padding(4.dp)) {
        Box(Modifier.offset(x = x).size(20.dp).clip(CircleShape).background(knob))
    }
}

/** A title over a segmented control. */
@Composable
private fun <T> SegmentedRow(title: String, options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    val c = LocalDexColors.current
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, color = c.text, style = rowTitle)
        val shape = RoundedCornerShape(12.dp)
        Row(
            Modifier.fillMaxWidth().clip(shape).background(if (c.isDark) Color(0xFF050506) else Color(0xFFF0F0EE))
                .border(1.dp, if (c.isDark) Color(0xFF1D1E23) else c.line, shape).padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            options.forEach { (value, label) ->
                val on = value == selected
                val bg by animateColorAsState(if (on) c.segmentOn else Color.Transparent, tween(200), label = "seg")
                Box(
                    Modifier.weight(1f).height(36.dp).clip(RoundedCornerShape(9.dp)).background(bg).clickable { onSelect(value) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(label, color = if (on) c.segmentOnText else c.textMuted, style = DexType.chip.copy(fontSize = 13.sp), maxLines = 1)
                }
            }
        }
    }
}

/** A row that opens something: title (and subtitle), a value or mono tag, and a caret. */
@Composable
internal fun NavRow(
    title: String, subtitle: String? = null, value: String? = null, trailingMono: String? = null,
    leading: (@Composable () -> Unit)? = null, onClick: () -> Unit,
) {
    val c = LocalDexColors.current
    Row(
        Modifier.fillMaxWidth().heightIn(min = if (subtitle != null) 64.dp else 56.dp).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        leading?.invoke()
        Column(Modifier.weight(1f)) {
            Text(title, color = c.text, style = rowTitle)
            if (subtitle != null) Text(subtitle, color = c.textSubtle, style = rowSubtitle, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        if (value != null) Text(value, color = c.textSubtle, style = DexType.caption.copy(fontSize = 13.sp), maxLines = 1)
        if (trailingMono != null) Text(trailingMono, color = c.textFaint, style = DexType.monoMeta.copy(fontFamily = JetBrainsMono))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = c.textFaint, modifier = Modifier.size(18.dp))
    }
}

/** A plain row with a coloured icon, as in the Support / About card. */
@Composable
internal fun PlainRow(icon: ImageVector, iconColor: Color, title: String, trailing: String? = null, onClick: () -> Unit) {
    val c = LocalDexColors.current
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, null, tint = iconColor, modifier = Modifier.size(19.dp))
        Text(title, color = c.text, style = rowTitle, modifier = Modifier.weight(1f))
        if (trailing != null) Text(trailing, color = c.textFaint, style = DexType.monoMeta)
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = c.textFaint, modifier = Modifier.size(18.dp))
    }
}
