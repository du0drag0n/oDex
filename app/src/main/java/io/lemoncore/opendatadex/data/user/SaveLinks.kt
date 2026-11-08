package io.lemoncore.opendatadex.data.user

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.StringRes
import androidx.core.net.toUri
import io.lemoncore.opendatadex.R
import io.lemoncore.opendatadex.data.save.GameSave
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.zip.CRC32

/** Bigger than any save oDex reads; anything larger isn't read into memory at all. */
const val MAX_SAVE_BYTES = 1 shl 20

/** The file at [uri], stopping a byte past [MAX_SAVE_BYTES]; null when it can't be opened. */
suspend fun readSaveFile(context: Context, uri: Uri): ByteArray? = withContext(Dispatchers.IO) {
    runCatching {
        context.contentResolver.openInputStream(uri)?.use { input ->
            // InputStream.readNBytes needs a newer Android.
            val out = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(16 * 1024)
            while (out.size() <= MAX_SAVE_BYTES) {
                val n = input.read(buffer, 0, minOf(buffer.size, MAX_SAVE_BYTES + 1 - out.size()))
                if (n < 0) break
                out.write(buffer, 0, n)
            }
            out.toByteArray()
        }
    }.getOrNull()
}

/** Why a linked save didn't update its copy. Every one leaves the copy's last import in place. */
enum class SaveLinkProblem(@StringRes val message: Int) {
    /** The file has gone, or Android no longer lets oDex read it. */
    MISSING(R.string.save_link_missing),
    /** The file isn't a whole save, as when an emulator is still writing it. */
    UNREADABLE(R.string.save_link_unreadable),
    /** The file now holds another game or trainer, as after starting a new game. */
    OTHER_GAME(R.string.save_link_other_game);

    companion object {
        fun fromName(name: String?) = entries.firstOrNull { it.name == name }
    }
}

/** What a look at a linked save found. */
sealed interface SaveLinkResult {
    data object Unchanged : SaveLinkResult
    data class Updated(val fileName: String, val newCatches: Int) : SaveLinkResult
    data class Failed(val problem: SaveLinkProblem) : SaveLinkResult
}

/**
 * Keeps game copies up to date with the save files linked to them. Android lets oDex go on reading a file the trainer
 * picked, so whenever the app opens it reads each one again and imports it if it changed. oDex only sees a file while
 * it's open, so nothing watches them in the background.
 *
 * Android keeps a file picked from device storage linked by its path, so it survives being replaced. One picked from
 * Downloads is linked to that exact file: writing into it works, but replacing it breaks the link.
 */
class SaveLinks(
    private val context: Context, private val saves: SaveDao, private val scope: CoroutineScope,
    private val copies: suspend () -> List<GameCopyEntity>,
    private val import: suspend (GameSave, String, GameCopyEntity) -> Int,
) {
    private val lock = Mutex()
    private val updates = Channel<SaveLinkResult.Updated>(Channel.BUFFERED)

    val links = saves.links()

    /** Saves updated by themselves, for the trainer to be told about. */
    val updated = updates.receiveAsFlow()

    /** Looks at every linked save, unless that's already happening. */
    fun refreshAll() {
        scope.launch {
            if (!lock.tryLock()) return@launch
            try {
                saves.linksOnce().forEach { link -> (look(link) as? SaveLinkResult.Updated)?.let { updates.trySend(it) } }
            } finally {
                lock.unlock()
            }
        }
    }

    /** Looks at the save linked to [copyId] now. */
    suspend fun refresh(copyId: Long): SaveLinkResult? = lock.withLock {
        saves.linksOnce().firstOrNull { it.copyId == copyId }?.let { look(it) }
    }

    /** Links the file [bytes] came from to [copyId], replacing any link it had. False when the file's app won't allow lasting access. */
    suspend fun link(copyId: Long, uri: Uri, fileName: String, bytes: ByteArray): Boolean = lock.withLock {
        val held = runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }.isSuccess
        if (held) {
            val old = saves.linksOnce().firstOrNull { it.copyId == copyId }
            saves.upsertLink(SaveLinkEntity(copyId, uri.toString(), fileName, crcOf(bytes)))
            old?.takeIf { it.uri != uri.toString() }?.let { release(it.uri) }
        }
        held
    }

    /** Stops updating [copyId] from its file. The file and the copy's last import stay. */
    suspend fun unlink(copyId: Long) = lock.withLock {
        val link = saves.linksOnce().firstOrNull { it.copyId == copyId } ?: return@withLock
        saves.deleteLink(copyId)
        release(link.uri)
    }

    private suspend fun look(link: SaveLinkEntity): SaveLinkResult {
        val bytes = readSaveFile(context, link.uri.toUri()) ?: return failed(link, SaveLinkProblem.MISSING)
        val crc = crcOf(bytes)
        if (crc == link.contentCrc) {
            if (link.problem != null) saves.upsertLink(link.copy(problem = null))
            return SaveLinkResult.Unchanged
        }
        val copy = copies().firstOrNull { it.id == link.copyId } ?: return SaveLinkResult.Unchanged
        val read = withContext(Dispatchers.Default) { runCatching { GameSave.read(bytes, link.fileName) }.getOrNull() }
            ?: return failed(link, SaveLinkProblem.UNREADABLE)
        // Never swap one trainer's boxes for another's: a new game in the same file waits for the trainer to decide.
        val save = read.forCopy(copy) ?: return failed(link, SaveLinkProblem.OTHER_GAME)
        val added = import(save, link.fileName, copy)
        saves.upsertLink(link.copy(contentCrc = crc, problem = null))
        return SaveLinkResult.Updated(link.fileName, added)
    }

    private suspend fun failed(link: SaveLinkEntity, problem: SaveLinkProblem): SaveLinkResult {
        if (link.problem != problem.name) saves.upsertLink(link.copy(problem = problem.name))
        return SaveLinkResult.Failed(problem)
    }

    /** Gives up reading [uri], unless another copy is still linked to it. */
    private suspend fun release(uri: String) {
        if (saves.linksOnce().any { it.uri == uri }) return
        runCatching { context.contentResolver.releasePersistableUriPermission(uri.toUri(), Intent.FLAG_GRANT_READ_URI_PERMISSION) }
    }

    private fun crcOf(bytes: ByteArray) = CRC32().apply { update(bytes) }.value
}
