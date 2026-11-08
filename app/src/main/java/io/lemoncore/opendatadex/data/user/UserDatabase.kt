package io.lemoncore.opendatadex.data.user

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

/** The trainer's own data. Game data never lives here; it comes from the bundled seed and openDex. */
@Entity(tableName = "favorite", primaryKeys = ["speciesId"])
data class FavoriteEntity(val speciesId: Int, val addedAt: Long)

/** Caught is tracked per game ("national" is its own checklist), like dataDex. */
@Entity(tableName = "caught", primaryKeys = ["game", "speciesId"])
data class CaughtEntity(val game: String, val speciesId: Int, val caughtAt: Long)

/** Shiny Dex: shinies caught, per game. */
@Entity(tableName = "shiny", primaryKeys = ["game", "speciesId"])
data class ShinyEntity(val game: String, val speciesId: Int, val caughtAt: Long)

/** Image URLs served by openDex for a species; missing rows fall back to openDex's game media URLs. */
@Entity(tableName = "species_asset", primaryKeys = ["speciesId"])
data class SpeciesAssetEntity(val speciesId: Int, val artworkUrl: String?, val spriteUrl: String?, val fetchedAt: Long)

/** A Team Builder team: up to six members, planned for one game. */
@Entity(tableName = "team")
data class TeamEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String, val game: String, val updatedAt: Long)

/**
 * A catch list: a named plan for one game ("Red living dex"). Its catches are stored in the caught table under
 * [key] ("list:<id>"), so the Pokédex can use a list exactly like a game's own checklist.
 */
@Entity(tableName = "catch_list")
data class CatchListEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val game: String,
    /** "living" (keep one of each) or "complete" (register each). */
    val goal: String,
    val createdAt: Long,
    /** The list covers the game's National Pokédex rather than its regional one. */
    @ColumnInfo(defaultValue = "0") val nationalDex: Boolean = false,
) {
    val key: String get() = keyOf(id)

    companion object {
        const val PREFIX = "list:"
        fun keyOf(id: Long) = "$PREFIX$id"
    }
}

/**
 * A copy of a game the trainer owns or plays (a cartridge, a download, an emulator save). Each copy keeps its own
 * caught checklist in the caught table under [key] ("copy:<id>"), so two Emeralds are tracked apart.
 */
@Entity(tableName = "game_copy")
data class GameCopyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val game: String,
    /** The trainer's own name for the copy ("Japanese cart"); may be empty. */
    val label: String,
    val playerName: String,
    /** The trainer ID shown on the trainer card, which Pokémon caught in this copy carry as their OT ID. */
    val trainerId: String,
    /** Language of the copy, as a [io.lemoncore.opendatadex.data.seed.DataLanguage] code, or null if not given. */
    val language: String?,
    /** What it is played on: a [GamePlatform] name. */
    val platform: String,
    /** The National Pokédex has been unlocked in this copy. */
    val nationalDex: Boolean,
    val createdAt: Long,
) {
    val key: String get() = keyOf(id)

    companion object {
        const val PREFIX = "copy:"
        fun keyOf(id: Long) = "$PREFIX$id"
    }
}

/** The save file last imported into a game copy: when, and what its PC boxes are called. */
@Serializable
@Entity(tableName = "save_import")
data class SaveImportEntity(
    @PrimaryKey val copyId: Long,
    val importedAt: Long,
    val fileName: String,
    val playMinutes: Int,
    /** The box names, one per line, in box order. */
    val boxNames: String,
    /** The save's [io.lemoncore.opendatadex.data.save.GameSave.badges]; null for an import from before oDex read them. */
    val badges: Int? = null,
    /** The save's [io.lemoncore.opendatadex.data.save.GameSave.startedAt]. */
    val startedAt: Long? = null,
) {
    val boxNameList: List<String> get() = boxNames.split('\n')
}

/**
 * A save file linked to a game copy, read again each time the app opens. [contentCrc] is the file as last imported, so
 * an unchanged file is left alone; [problem] is the [SaveLinkProblem] from the last look at it, if any.
 */
@Entity(tableName = "save_link")
data class SaveLinkEntity(
    @PrimaryKey val copyId: Long,
    val uri: String,
    val fileName: String,
    val contentCrc: Long,
    val problem: String? = null,
)

/**
 * A Pokémon from a copy's imported save, in its party ([box] 0) or PC box slot. Values are the save's own: [heldItem],
 * [ball], [originGame] and [metLocation] are indexes from the copy's generation, and [ivs] and [evs] list HP to Speed,
 * comma-separated (DVs and stat experience before Generation III).
 */
@Serializable
@Entity(tableName = "save_pokemon", primaryKeys = ["copyId", "box", "slot"])
data class SavePokemonEntity(
    val copyId: Long,
    val box: Int,
    val slot: Int,
    val speciesId: Int,
    val nickname: String,
    val otName: String,
    val otId: Long,
    val ownCatch: Boolean,
    val pid: Long,
    val exp: Int,
    val heldItem: Int,
    val friendship: Int,
    val moves: String,
    val ivs: String,
    val evs: String,
    val metLevel: Int,
    val metLocation: Int,
    val originGame: Int,
    val ball: Int,
    val egg: Boolean,
    val abilitySlot: Int,
    val shiny: Boolean,
    val language: Int,
    @ColumnInfo(defaultValue = "0") val metDate: Int = 0,
    @ColumnInfo(defaultValue = "0") val metTime: Int = 0,
    @ColumnInfo(defaultValue = "0") val form: Int = 0,
    /** 0 male, 1 female, 2 genderless; -1 when the PID decides, before Generation IV or for an older import. */
    @ColumnInfo(defaultValue = "-1") val gender: Int = -1,
    /** The nature's index from Generation V on; -1 when the PID decides. */
    @ColumnInfo(defaultValue = "-1") val nature: Int = -1,
) {
    val moveIds: List<Int> get() = moves.split(',').mapNotNull { it.trim().toIntOrNull() }
    val ivList: List<Int> get() = ivs.split(',').mapNotNull { it.toIntOrNull() }
    val evList: List<Int> get() = evs.split(',').mapNotNull { it.toIntOrNull() }
    /** The nature's index in the games' order (Hardy, Lonely, Brave…). */
    val natureIndex: Int get() = if (nature >= 0) nature else (pid % 25).toInt()
    /** The visible half of the original trainer's ID. */
    val otVisibleId: Int get() = (otId and 0xFFFF).toInt()
}

/** One of a team's six slots. [moves] holds up to four move ids, comma-separated. */
@Entity(tableName = "team_member", primaryKeys = ["teamId", "slot"])
data class TeamMemberEntity(val teamId: Long, val slot: Int, val speciesId: Int, val formId: Int, val moves: String) {
    val moveIds: List<Int> get() = moves.split(',').mapNotNull { it.trim().toIntOrNull() }
}

@Dao
interface TrainerDao {
    @Query("SELECT speciesId FROM favorite")
    fun favorites(): Flow<List<Int>>

    @Query("SELECT * FROM favorite")
    suspend fun allFavorites(): List<FavoriteEntity>

    // Adding something already there keeps the original row (and its date); each returns -1 for those, so callers can count what was new.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addFavorite(f: FavoriteEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addFavorites(f: List<FavoriteEntity>): List<Long>

    @Query("DELETE FROM favorite WHERE speciesId = :speciesId")
    suspend fun removeFavorite(speciesId: Int): Int

    /** Adds or removes a favorite in one step, so fast taps can't both read the old state. */
    @Transaction
    suspend fun toggleFavorite(speciesId: Int, now: Long) {
        if (removeFavorite(speciesId) == 0) addFavorite(FavoriteEntity(speciesId, now))
    }

    @Query("SELECT speciesId FROM caught WHERE game = :game")
    fun caught(game: String): Flow<List<Int>>

    /** One checklist's catches with the date each was marked. */
    @Query("SELECT * FROM caught WHERE game = :game")
    fun caughtRows(game: String): Flow<List<CaughtEntity>>

    @Query("SELECT * FROM caught")
    fun allCaughtFlow(): Flow<List<CaughtEntity>>

    @Query("SELECT * FROM caught")
    suspend fun allCaught(): List<CaughtEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addCaught(c: List<CaughtEntity>): List<Long>

    @Query("SELECT * FROM caught WHERE game = :game AND speciesId = :speciesId")
    suspend fun caughtRow(game: String, speciesId: Int): CaughtEntity?

    @Query("DELETE FROM caught WHERE game = :game AND speciesId = :speciesId")
    suspend fun removeCaught(game: String, speciesId: Int)

    @Query("DELETE FROM caught WHERE game = :game")
    suspend fun clearCaught(game: String)

    @Query("SELECT game, COUNT(*) AS count FROM caught GROUP BY game")
    fun caughtCounts(): Flow<List<GameCount>>

    @Query("SELECT speciesId FROM shiny WHERE game = :game")
    fun shiny(game: String): Flow<List<Int>>

    @Query("SELECT * FROM shiny")
    fun allShinyFlow(): Flow<List<ShinyEntity>>

    @Query("SELECT * FROM shiny")
    suspend fun allShiny(): List<ShinyEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addShiny(s: List<ShinyEntity>): List<Long>

    @Query("DELETE FROM shiny WHERE game = :game AND speciesId = :speciesId")
    suspend fun removeShiny(game: String, speciesId: Int)
}

data class GameCount(val game: String, val count: Int)

/**
 * A shiny hunt for [speciesId] in [game] (a game key, like the Shiny Dex): [count] encounters, soft resets or eggs so
 * far by [method] (a [io.lemoncore.opendatadex.data.HuntMethod] name), until [foundAt].
 */
@Entity(tableName = "shiny_hunt")
data class ShinyHuntEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val game: String,
    val speciesId: Int,
    val formId: Int,
    val method: String,
    val shinyCharm: Boolean,
    val count: Int,
    val startedAt: Long,
    val foundAt: Long? = null,
)

@Dao
interface ShinyHuntDao {
    /** The newest hunt for a Pokémon in a game, finished or not. */
    @Query("SELECT * FROM shiny_hunt WHERE game = :game AND speciesId = :speciesId ORDER BY startedAt DESC LIMIT 1")
    fun latest(game: String, speciesId: Int): Flow<ShinyHuntEntity?>

    @Query("SELECT * FROM shiny_hunt WHERE foundAt IS NULL ORDER BY startedAt DESC")
    fun active(): Flow<List<ShinyHuntEntity>>

    @Query("SELECT * FROM shiny_hunt WHERE id = :id")
    fun byId(id: Long): Flow<ShinyHuntEntity?>

    @Query("SELECT * FROM shiny_hunt ORDER BY startedAt")
    suspend fun allOnce(): List<ShinyHuntEntity>

    @Insert
    suspend fun insert(hunt: ShinyHuntEntity): Long

    @Update
    suspend fun update(hunt: ShinyHuntEntity)

    /** Adds [delta] to a hunt's count in one step, so fast taps can't overwrite each other; it never goes below 0. */
    @Query("UPDATE shiny_hunt SET count = MAX(0, count + :delta) WHERE id = :id")
    suspend fun add(id: Long, delta: Int)

    @Query("DELETE FROM shiny_hunt WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface TeamDao {
    @Query("SELECT * FROM team ORDER BY updatedAt DESC")
    fun teams(): Flow<List<TeamEntity>>

    @Query("SELECT * FROM team_member ORDER BY teamId, slot")
    fun allMembers(): Flow<List<TeamMemberEntity>>

    @Query("SELECT * FROM team ORDER BY updatedAt")
    suspend fun allTeams(): List<TeamEntity>

    @Query("SELECT * FROM team_member ORDER BY teamId, slot")
    suspend fun allMembersOnce(): List<TeamMemberEntity>

    @Query("SELECT * FROM team WHERE id = :id")
    fun team(id: Long): Flow<TeamEntity?>

    @Query("SELECT * FROM team_member WHERE teamId = :id ORDER BY slot")
    fun members(id: Long): Flow<List<TeamMemberEntity>>

    @Insert
    suspend fun insert(team: TeamEntity): Long

    @Update
    suspend fun update(team: TeamEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMember(member: TeamMemberEntity)

    @Query("DELETE FROM team_member WHERE teamId = :teamId AND slot = :slot")
    suspend fun removeMember(teamId: Long, slot: Int)

    @Query("UPDATE team SET updatedAt = :now WHERE id = :teamId")
    suspend fun touch(teamId: Long, now: Long)

    @Query("SELECT slot FROM team_member WHERE teamId = :teamId")
    suspend fun usedSlots(teamId: Long): List<Int>

    /** Puts a Pokémon in the team's first free slot in one step, so two quick adds can't pick the same slot. False when full. */
    @Transaction
    suspend fun addToFirstFreeSlot(teamId: Long, speciesId: Int, formId: Int, now: Long): Boolean {
        val used = usedSlots(teamId).toSet()
        val slot = (0 until Backup.TEAM_SIZE).firstOrNull { it !in used } ?: return false
        upsertMember(TeamMemberEntity(teamId, slot, speciesId, formId, ""))
        touch(teamId, now)
        return true
    }

    @Query("DELETE FROM team_member WHERE teamId = :teamId")
    suspend fun deleteMembers(teamId: Long)

    @Query("DELETE FROM team WHERE id = :teamId")
    suspend fun deleteTeamRow(teamId: Long)

    @Transaction
    suspend fun delete(teamId: Long) {
        deleteMembers(teamId)
        deleteTeamRow(teamId)
    }
}

@Dao
interface CatchListDao {
    @Query("SELECT * FROM catch_list ORDER BY createdAt DESC")
    fun all(): Flow<List<CatchListEntity>>

    @Query("SELECT * FROM catch_list ORDER BY createdAt")
    suspend fun allOnce(): List<CatchListEntity>

    @Query("SELECT * FROM catch_list WHERE id = :id")
    fun list(id: Long): Flow<CatchListEntity?>

    @Insert
    suspend fun insert(list: CatchListEntity): Long

    @Update
    suspend fun update(list: CatchListEntity)

    @Query("DELETE FROM catch_list WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface GameCopyDao {
    @Query("SELECT * FROM game_copy ORDER BY createdAt")
    fun all(): Flow<List<GameCopyEntity>>

    @Query("SELECT * FROM game_copy ORDER BY createdAt")
    suspend fun allOnce(): List<GameCopyEntity>

    @Insert
    suspend fun insert(copy: GameCopyEntity): Long

    @Update
    suspend fun update(copy: GameCopyEntity)

    @Query("DELETE FROM game_copy WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface SaveDao {
    @Query("SELECT * FROM save_import")
    fun imports(): Flow<List<SaveImportEntity>>

    @Query("SELECT * FROM save_import WHERE copyId = :copyId")
    fun importOf(copyId: Long): Flow<SaveImportEntity?>

    @Query("SELECT * FROM save_import WHERE copyId = :copyId")
    suspend fun importOnce(copyId: Long): SaveImportEntity?

    @Query("SELECT * FROM save_pokemon WHERE copyId = :copyId ORDER BY box, slot")
    fun pokemon(copyId: Long): Flow<List<SavePokemonEntity>>

    @Query("SELECT * FROM save_pokemon WHERE copyId = :copyId ORDER BY box, slot")
    suspend fun pokemonOnce(copyId: Long): List<SavePokemonEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertImport(row: SaveImportEntity)

    @Insert
    suspend fun insertPokemon(rows: List<SavePokemonEntity>)

    @Query("DELETE FROM save_pokemon WHERE copyId = :copyId")
    suspend fun deletePokemon(copyId: Long)

    @Query("DELETE FROM save_import WHERE copyId = :copyId")
    suspend fun deleteImport(copyId: Long)

    @Query("SELECT * FROM save_link")
    fun links(): Flow<List<SaveLinkEntity>>

    @Query("SELECT * FROM save_link")
    suspend fun linksOnce(): List<SaveLinkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLink(row: SaveLinkEntity)

    @Query("DELETE FROM save_link WHERE copyId = :copyId")
    suspend fun deleteLink(copyId: Long)

    /** Swaps a copy's imported Pokémon for a newer save's in one step. */
    @Transaction
    suspend fun replace(row: SaveImportEntity, pokemon: List<SavePokemonEntity>) {
        deletePokemon(row.copyId)
        upsertImport(row)
        insertPokemon(pokemon)
    }

    @Transaction
    suspend fun delete(copyId: Long) {
        deletePokemon(copyId)
        deleteImport(copyId)
    }
}

@Dao
interface AssetDao {
    @Query("SELECT * FROM species_asset")
    fun all(): Flow<List<SpeciesAssetEntity>>

    @Query("SELECT MAX(fetchedAt) FROM species_asset")
    suspend fun lastFetch(): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rows: List<SpeciesAssetEntity>)
}

@Database(
    entities = [
        FavoriteEntity::class, CaughtEntity::class, SpeciesAssetEntity::class,
        ShinyEntity::class, TeamEntity::class, TeamMemberEntity::class, CatchListEntity::class, GameCopyEntity::class,
        ShinyHuntEntity::class, SaveImportEntity::class, SavePokemonEntity::class, SaveLinkEntity::class,
    ],
    version = 8,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3), AutoMigration(from = 3, to = 4), AutoMigration(from = 4, to = 5),
        AutoMigration(from = 5, to = 6), AutoMigration(from = 6, to = 7), AutoMigration(from = 7, to = 8),
    ],
)
abstract class UserDatabase : RoomDatabase() {
    abstract fun trainer(): TrainerDao
    abstract fun saves(): SaveDao
    abstract fun shinyHunts(): ShinyHuntDao
    abstract fun teams(): TeamDao
    abstract fun catchLists(): CatchListDao
    abstract fun gameCopies(): GameCopyDao
    abstract fun assets(): AssetDao

    companion object {
        fun open(context: Context) = Room.databaseBuilder(context, UserDatabase::class.java, "opendatadex.db").build()
    }
}
