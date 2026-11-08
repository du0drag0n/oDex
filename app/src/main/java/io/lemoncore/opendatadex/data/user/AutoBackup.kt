package io.lemoncore.opendatadex.data.user

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.net.toUri
import io.lemoncore.opendatadex.data.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Keeps a backup file the trainer chose up to date: a few seconds after their data changes, and when the app goes to
 * the background. The file can be anywhere Android can save a document, so choosing Google Drive in the picker keeps it
 * in the cloud. An empty profile is never written over a backup (a fresh install must not wipe the file).
 */
class AutoBackup(
    private val context: Context, private val backup: Backup, private val db: UserDatabase, private val settings: SettingsStore,
    private val scope: CoroutineScope,
) {
    private val lock = Mutex()
    private val changes = MutableStateFlow(0)
    @Volatile private var pending = false

    /** The last automatic backup's failure, cleared when one succeeds. */
    val failure = MutableStateFlow<String?>(null)

    @OptIn(FlowPreview::class)
    fun start() {
        scope.launch {
            db.invalidationTracker.createFlow(*TABLES, emitInitialState = false).collect {
                pending = true
                changes.value++
            }
        }
        scope.launch { changes.drop(1).debounce(DELAY_MS).collect { if (pending) backupQuietly() } }
    }

    /** The app is going to the background: write now if anything changed since the last backup. */
    fun flush() {
        if (pending) scope.launch { backupQuietly() }
    }

    private suspend fun backupQuietly() {
        runCatching { backupNow() }.onFailure { failure.value = it.message ?: it.javaClass.simpleName }
    }

    /** Writes the backup file now. Returns when it was written, or null when no file is chosen or there is nothing to save. */
    suspend fun backupNow(): Long? = lock.withLock {
        val uri = settings.settings.first().autoBackupUri?.toUri() ?: return null
        pending = false
        if (backup.isEmpty()) return null
        val text = backup.export()
        withContext(Dispatchers.IO) {
            context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(text.toByteArray()) } ?: error("Can't open the backup file")
        }
        val now = System.currentTimeMillis()
        settings.setLastAutoBackup(now)
        failure.value = null
        now
    }

    /** Merges the backup file into the trainer's data (nothing is removed). */
    suspend fun restore(): RestoreSummary? {
        val uri = settings.settings.first().autoBackupUri?.toUri() ?: return null
        val text = withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() } ?: error("Can't open the backup file")
        }
        return backup.restore(text)
    }

    /** Starts backing up to [uri], a document the trainer just created, keeping access to it across restarts. */
    suspend fun choose(uri: Uri) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
        settings.setAutoBackupUri(uri.toString())
        backupNow()
    }

    /** Stops automatic backups. The file stays where it is. */
    suspend fun turnOff() {
        settings.settings.first().autoBackupUri?.toUri()?.let { uri ->
            runCatching {
                context.contentResolver.releasePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
        }
        settings.setAutoBackupUri(null)
        failure.value = null
    }

    /** The chosen file's name, as its app shows it ("oDex-auto-backup.json"). */
    suspend fun fileName(uri: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.query(uri.toUri(), arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        }.getOrNull()
    }

    companion object {
        /** The tables a backup holds; a change to any of them schedules one. */
        private val TABLES = arrayOf("favorite", "caught", "shiny", "team", "team_member", "catch_list", "game_copy", "shiny_hunt", "save_import", "save_pokemon")
        private const val DELAY_MS = 5_000L
        const val FILE_NAME = "oDex-auto-backup.json"
    }
}
