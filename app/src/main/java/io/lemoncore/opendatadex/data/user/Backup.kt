package io.lemoncore.opendatadex.data.user

import androidx.room.withTransaction
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** The trainer's data as a portable JSON file: favorites, caught and shiny checklists per game, teams and imported saves. */
@Serializable
data class BackupFile(
    val app: String = APP,
    val version: Int = 2,
    val exportedAt: Long,
    val favorites: List<Int> = emptyList(),
    /** Game key -> species ids. */
    val caught: Map<String, List<Int>> = emptyMap(),
    val shiny: Map<String, List<Int>> = emptyMap(),
    val teams: List<BackupTeam> = emptyList(),
    val catchLists: List<BackupCatchList> = emptyList(),
    val gameCopies: List<BackupGameCopy> = emptyList(),
    val shinyHunts: List<BackupShinyHunt> = emptyList(),
) {
    companion object {
        const val APP = "oDex"
        /** Backups made before the app was renamed from openDataDex still restore. */
        val APPS = setOf(APP, "openDataDex")
    }
}

@Serializable
data class BackupTeam(val name: String, val game: String, val members: List<BackupMember>)

@Serializable
data class BackupMember(val slot: Int, val species: Int, val form: Int, val moves: List<Int> = emptyList())

@Serializable
data class BackupCatchList(
    val name: String, val game: String, val goal: String, val caught: List<Int> = emptyList(), val nationalDex: Boolean = false,
)

@Serializable
data class BackupGameCopy(
    val game: String,
    val label: String = "",
    val playerName: String = "",
    val trainerId: String = "",
    val language: String? = null,
    val platform: String = "",
    val nationalDex: Boolean = false,
    val caught: List<Int> = emptyList(),
    /** The copy's last import, with its boxes. A link to the save file itself only works on the device that made it. */
    val saveImport: SaveImportEntity? = null,
    val savedPokemon: List<SavePokemonEntity> = emptyList(),
)

@Serializable
data class BackupShinyHunt(
    val game: String, val species: Int, val form: Int, val method: String, val shinyCharm: Boolean = false,
    val count: Int, val startedAt: Long, val foundAt: Long? = null,
)

data class RestoreSummary(val favorites: Int, val caught: Int, val shiny: Int, val teams: Int, val catchLists: Int = 0, val gameCopies: Int = 0, val saves: Int = 0)

/**
 * Writes and reads [BackupFile]s. Restoring merges: it adds what the file has and never removes anything,
 * so restoring an old backup cannot wipe newer progress. Teams in the file are added as new teams.
 */
class Backup(private val db: UserDatabase) {
    private val trainer = db.trainer()
    private val teams = db.teams()
    private val catchLists = db.catchLists()
    private val gameCopies = db.gameCopies()
    private val hunts = db.shinyHunts()
    private val saves = db.saves()
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    suspend fun export(now: Long = System.currentTimeMillis()): String {
        val members = teams.allMembersOnce().groupBy { it.teamId }
        val caught = trainer.allCaught().groupBy({ it.game }, { it.speciesId }).mapValues { it.value.sorted() }
        val file = BackupFile(
            exportedAt = now,
            favorites = trainer.allFavorites().map { it.speciesId }.sorted(),
            // Catch lists' and game copies' catches travel with them (their ids differ between devices).
            caught = caught.filterKeys { !it.startsWith(CatchListEntity.PREFIX) && !it.startsWith(GameCopyEntity.PREFIX) },
            catchLists = catchLists.allOnce().map { BackupCatchList(it.name, it.game, it.goal, caught[it.key].orEmpty(), it.nationalDex) },
            gameCopies = gameCopies.allOnce().map {
                BackupGameCopy(it.game, it.label, it.playerName, it.trainerId, it.language, it.platform, it.nationalDex, caught[it.key].orEmpty(),
                    saves.importOnce(it.id), saves.pokemonOnce(it.id))
            },
            shiny = trainer.allShiny().groupBy({ it.game }, { it.speciesId }).mapValues { it.value.sorted() },
            teams = teams.allTeams().map { t ->
                BackupTeam(t.name, t.game, members[t.id].orEmpty().map { BackupMember(it.slot, it.speciesId, it.formId, it.moveIds) })
            },
            shinyHunts = hunts.allOnce().map { BackupShinyHunt(it.game, it.speciesId, it.formId, it.method, it.shinyCharm, it.count, it.startedAt, it.foundAt) },
        )
        return json.encodeToString(BackupFile.serializer(), file)
    }

    /** True when there is nothing to back up (a fresh install), so an automatic backup doesn't replace a good file. */
    suspend fun isEmpty(): Boolean = trainer.allFavorites().isEmpty() && trainer.allCaught().isEmpty() && trainer.allShiny().isEmpty() &&
        teams.allTeams().isEmpty() && catchLists.allOnce().isEmpty() && gameCopies.allOnce().isEmpty() && hunts.allOnce().isEmpty()

    /** Throws [IllegalArgumentException] when [text] is not an oDex backup. */
    suspend fun restore(text: String, now: Long = System.currentTimeMillis()): RestoreSummary {
        val file = runCatching { json.decodeFromString(BackupFile.serializer(), text) }
            .getOrElse { throw IllegalArgumentException("This file is not an oDex backup.", it) }
        require(file.app in BackupFile.APPS) { "This file is not an oDex backup." }
        // All or nothing: a restore that fails partway leaves the trainer's data as it was (and auto backup never saves half a merge).
        return db.withTransaction { merge(file, now) }
    }

    private suspend fun merge(file: BackupFile, now: Long): RestoreSummary {
        // Rows already here keep their dates; the counts are of what this restore added.
        fun List<Long>.added() = count { it != -1L }
        val newFavorites = trainer.addFavorites(file.favorites.map { FavoriteEntity(it, now) }).added()
        val caught = file.caught.filterKeys { !it.startsWith(CatchListEntity.PREFIX) && !it.startsWith(GameCopyEntity.PREFIX) }
            .flatMap { (game, ids) -> ids.map { CaughtEntity(game, it, now) } }
        var newCaught = trainer.addCaught(caught).added()
        // A list with the same name and game merges into the existing one; others are added.
        val existingLists = catchLists.allOnce()
        var newLists = 0
        file.catchLists.forEach { l ->
            val key = existingLists.firstOrNull { it.name == l.name && it.game == l.game }?.key
                ?: CatchListEntity.keyOf(catchLists.insert(
                    CatchListEntity(name = l.name, game = l.game, goal = l.goal, createdAt = now, nationalDex = l.nationalDex),
                )).also { newLists++ }
            newCaught += trainer.addCaught(l.caught.map { CaughtEntity(key, it, now) }).added()
        }
        // Likewise a copy of the same game with the same name, player and trainer ID.
        val existingCopies = gameCopies.allOnce()
        var newCopies = 0
        var newSaves = 0
        file.gameCopies.forEach { c ->
            val id = existingCopies.firstOrNull { it.game == c.game && it.label == c.label && it.playerName == c.playerName && it.trainerId == c.trainerId }?.id
                ?: gameCopies.insert(GameCopyEntity(
                    game = c.game, label = c.label, playerName = c.playerName, trainerId = c.trainerId, language = c.language,
                    platform = c.platform, nationalDex = c.nationalDex, createdAt = now,
                )).also { newCopies++ }
            newCaught += trainer.addCaught(c.caught.map { CaughtEntity(GameCopyEntity.keyOf(id), it, now) }).added()
            // An import newer than the backup's stays, so an old backup can't undo a later save.
            val imported = c.saveImport ?: return@forEach
            if ((saves.importOnce(id)?.importedAt ?: Long.MIN_VALUE) < imported.importedAt) {
                saves.replace(imported.copy(copyId = id), c.savedPokemon.map { it.copy(copyId = id) })
                newSaves++
            }
        }
        val newShiny = trainer.addShiny(file.shiny.flatMap { (game, ids) -> ids.map { ShinyEntity(game, it, now) } }).added()
        // A team identical to one already here (same name, game and members) is skipped, so restoring twice is harmless.
        val existingMembers = teams.allMembersOnce().groupBy { it.teamId }
        val existing = teams.allTeams().map { e ->
            Triple(e.name, e.game, existingMembers[e.id].orEmpty().map { BackupMember(it.slot, it.speciesId, it.formId, it.moveIds) })
        }.toSet()
        val newTeams = file.teams.filter { t -> Triple(t.name, t.game, t.members.sortedBy { it.slot }) !in existing }
        newTeams.forEach { t ->
            val id = teams.insert(TeamEntity(name = t.name, game = t.game, updatedAt = now))
            t.members.filter { it.slot in 0 until TEAM_SIZE }.forEach { m ->
                teams.upsertMember(TeamMemberEntity(id, m.slot, m.species, m.form, m.moves.take(4).joinToString(",")))
            }
        }
        // A hunt is the same one when its game, Pokémon and start time match; those are skipped.
        val existingHunts = hunts.allOnce().map { Triple(it.game, it.speciesId, it.startedAt) }.toSet()
        file.shinyHunts.filter { Triple(it.game, it.species, it.startedAt) !in existingHunts }.forEach { h ->
            hunts.insert(ShinyHuntEntity(game = h.game, speciesId = h.species, formId = h.form, method = h.method,
                shinyCharm = h.shinyCharm, count = h.count, startedAt = h.startedAt, foundAt = h.foundAt))
        }
        return RestoreSummary(newFavorites, newCaught, newShiny, newTeams.size, newLists, newCopies, newSaves)
    }

    companion object { const val TEAM_SIZE = 6 }
}
