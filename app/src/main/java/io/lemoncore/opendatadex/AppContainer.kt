package io.lemoncore.opendatadex

import android.content.Context
import io.lemoncore.opendatadex.data.ActiveChecklist
import io.lemoncore.opendatadex.data.Dex
import io.lemoncore.opendatadex.data.user.GameCopyEntity
import io.lemoncore.opendatadex.data.Game
import io.lemoncore.opendatadex.data.Images
import io.lemoncore.opendatadex.data.Settings
import io.lemoncore.opendatadex.data.SettingsStore
import io.lemoncore.opendatadex.data.opendex.AssetSync
import io.lemoncore.opendatadex.data.opendex.OpenDexClient
import io.lemoncore.opendatadex.data.seed.DataLanguage
import io.lemoncore.opendatadex.data.seed.Seed
import io.lemoncore.opendatadex.data.seed.SeedLanguage
import io.lemoncore.opendatadex.data.seed.translated
import io.lemoncore.opendatadex.data.user.Backup
import io.lemoncore.opendatadex.data.user.AutoBackup
import io.lemoncore.opendatadex.data.user.CatchListEntity
import io.lemoncore.opendatadex.data.user.CaughtEntity
import io.lemoncore.opendatadex.data.GamePlatform
import io.lemoncore.opendatadex.data.save.GameSave
import io.lemoncore.opendatadex.data.user.SaveImportEntity
import io.lemoncore.opendatadex.data.user.SaveLinks
import io.lemoncore.opendatadex.data.user.SavePokemonEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import io.lemoncore.opendatadex.data.user.ShinyEntity
import io.lemoncore.opendatadex.data.user.ShinyHuntEntity
import io.lemoncore.opendatadex.data.HuntMethod
import io.lemoncore.opendatadex.data.user.TeamEntity
import io.lemoncore.opendatadex.data.user.TeamMemberEntity
import io.lemoncore.opendatadex.data.user.UserDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import io.lemoncore.opendatadex.ui.components.FeelSounds
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/** Hand-wired dependencies; small enough not to need a DI framework. */
@OptIn(ExperimentalSerializationApi::class)
class AppContainer(context: Context) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val http: OkHttpClient = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build()
    val settings = SettingsStore(context)
    private val db = UserDatabase.open(context)
    private val trainerDao = db.trainer()
    private val teamDao = db.teams()
    private val catchListDao = db.catchLists()
    private val gameCopyDao = db.gameCopies()
    private val huntDao = db.shinyHunts()

    private val _dex = MutableStateFlow<Dex?>(null)
    /** Null until the bundled seed has been parsed (a few hundred milliseconds on first use). */
    val dex: StateFlow<Dex?> = _dex.asStateFlow()

    private val _settings = MutableStateFlow<Settings?>(null)
    /**
     * The trainer's settings, published only once the Dex has their form preferences, so no screen lists forms with
     * the old ones. Null until they have loaded.
     */
    val currentSettings: StateFlow<Settings?> = _settings.asStateFlow()
    /** Pairs a Dex with the settings' form preferences, whichever of the two arrives last. */
    private val formLock = Any()

    /** Set when the bundled seed is unreadable (a build problem, not something the trainer can fix). */
    val seedError = MutableStateFlow<String?>(null)

    /** openDex artwork URLs by species id. */
    val openDexArtwork: StateFlow<Map<Int, String>> = db.assets().all()
        .map { rows -> rows.mapNotNull { r -> r.artworkUrl?.let { r.speciesId to it } }.toMap() }
        .stateIn(scope, SharingStarted.Eagerly, emptyMap())

    val favorites: StateFlow<Set<Int>> = trainerDao.favorites().map { it.toSet() }
        .stateIn(scope, SharingStarted.Eagerly, emptySet())

    val caughtCounts = trainerDao.caughtCounts()

    /** The list the trainer last browsed, so a Pokémon screen can step to the next or previous entry. */
    val browseOrder = MutableStateFlow<List<Int>>(emptyList())

    /** The species the Pokémon screen was last showing (after any swiping), so the list can scroll to it on return. */
    val lastViewed = MutableStateFlow(0)

    private val assetSync = AssetSync(OpenDexClient(http), db.assets(), AssetSync.defaultMaxSpecies)

    init {
        val app = context.applicationContext
        scope.launch(Dispatchers.IO) {
            val json = Json { ignoreUnknownKeys = true }
            val seed = runCatching {
                app.assets.open("seed/dex.json").buffered().use { json.decodeFromStream(Seed.serializer(), it) }
            }.getOrElse {
                android.util.Log.e("oDex", "Bundled seed could not be read", it)
                seedError.value = it.message ?: it.toString()
                return@launch
            }
            val packs: (Int) -> String? = { vg -> runCatching { app.assets.open("seed/learnsets/$vg.json").bufferedReader().use { it.readText() } }.getOrNull() }
            // English data stays loaded; another data language is laid over it, and the Dex is rebuilt when it changes.
            settings.settings.map { it.dataLanguage }.distinctUntilChanged().collect { language ->
                val data = if (language == DataLanguage.ENGLISH) seed else runCatching {
                    val pack = app.assets.open("seed/lang/${language.code}.json").buffered().use { json.decodeFromStream(SeedLanguage.serializer(), it) }
                    seed.translated(pack)
                }.getOrElse {
                    android.util.Log.e("oDex", "Language pack ${language.code} could not be read", it)
                    seed
                }
                val built = Dex(data, packs)
                synchronized(formLock) {
                    _settings.value?.let { built.formOptions = it.formOptions }
                    _dex.value = built
                }
            }
        }
        scope.launch {
            settings.settings.collect { s ->
                synchronized(formLock) {
                    _dex.value?.formOptions = s.formOptions
                    Images.style = s.imageStyle
                    _settings.value = s
                }
            }
        }
        // Parse the selected game's move pools ahead of time so the Moves tab opens instantly.
        scope.launch(Dispatchers.IO) {
            combine(dex.filterNotNull(), settings.settings.map { it.game }.distinctUntilChanged()) { d, g -> d to g }
                .collect { (d, g) -> if (!g.isNational) d.learnsets(g) }
        }
        scope.launch(Dispatchers.IO) { assetSync.syncIfStale() }
    }

    /** Where [game]'s catches are recorded: its active catch list or game copy, or the game's own checklist. */
    fun checklistKey(game: Game): Flow<String> = settings.activeList(game).map { it ?: game.key }.distinctUntilChanged()

    /** The catch list or game copy [game]'s Pokédex records into, and whether it shows the National Pokédex. */
    fun activeChecklist(game: Game): Flow<ActiveChecklist> =
        combine(settings.activeList(game), catchLists, gameCopies, settings.nationalDex(game)) { key, lists, copies, gameNational ->
            val list = lists.firstOrNull { it.key == key }
            val copy = copies.firstOrNull { it.key == key }
            ActiveChecklist(list, copy, game.hasNationalDex && (copy?.nationalDex ?: list?.nationalDex ?: gameNational))
        }.distinctUntilChanged()

    /** Turns the National Pokédex on or off for whatever [game]'s Pokédex records into. */
    fun setNationalDex(game: Game, on: Boolean) = scope.launch {
        val active = activeChecklist(game).first()
        when {
            active.copy != null -> gameCopyDao.update(active.copy.copy(nationalDex = on))
            active.list != null -> catchListDao.update(active.list.copy(nationalDex = on))
            else -> settings.setNationalDex(game, on)
        }
    }

    /** Picks [game] in the Pokédex as a plain game: a game copy it was tracking is put aside (catch lists stay). */
    fun selectGame(game: Game) = scope.launch {
        settings.setGame(game)
        if (settings.activeList(game).first()?.startsWith(GameCopyEntity.PREFIX) == true) settings.setActiveList(game, null)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun caught(game: Game): Flow<Set<Int>> = checklistKey(game).flatMapLatest { caughtIn(it) }

    fun caughtIn(key: String): Flow<Set<Int>> = trainerDao.caught(key).map { it.toSet() }

    /** Species id -> when it was marked caught, for one checklist. */
    fun caughtDates(key: String): Flow<Map<Int, Long>> = trainerDao.caughtRows(key).map { rows -> rows.associate { it.speciesId to it.caughtAt } }

    fun toggleFavorite(speciesId: Int) = scope.launch { trainerDao.toggleFavorite(speciesId, System.currentTimeMillis()) }

    fun setCaught(game: Game, speciesId: Int, caught: Boolean) = scope.launch { setCaughtIn(checklistKey(game).first(), speciesId, caught) }

    suspend fun setCaughtIn(key: String, speciesId: Int, caught: Boolean) {
        if (caught) trainerDao.addCaught(listOf(CaughtEntity(key, speciesId, System.currentTimeMillis())))
        else trainerDao.caughtRow(key, speciesId)?.let { row ->
            trainerDao.removeCaught(key, speciesId)
            unmarked.tryEmit(row)
        }
    }

    /** Catches just unmarked one at a time, so the app can offer Undo. */
    val unmarked = MutableSharedFlow<CaughtEntity>(extraBufferCapacity = 8)

    /** Puts an unmarked catch back with its original date. */
    fun restoreCaught(row: CaughtEntity) = scope.launch { trainerDao.addCaught(listOf(row)) }

    /** The app's own short sounds (Settings → Feel). */
    val sounds = FeelSounds(context.applicationContext)

    fun markAllCaught(game: Game, ids: Collection<Int>) = scope.launch {
        val now = System.currentTimeMillis()
        val key = checklistKey(game).first()
        trainerDao.addCaught(ids.map { CaughtEntity(key, it, now) })
    }

    fun clearCaught(game: Game) = scope.launch { trainerDao.clearCaught(checklistKey(game).first()) }

    // ---- catch lists ----------------------------------------------------------------------------------------------------

    val catchLists: Flow<List<CatchListEntity>> = catchListDao.all()

    fun catchList(id: Long) = catchListDao.list(id)

    suspend fun createCatchList(name: String, game: Game, goal: String): Long =
        catchListDao.insert(CatchListEntity(name = name, game = game.key, goal = goal, createdAt = System.currentTimeMillis()))

    fun updateCatchList(list: CatchListEntity) = scope.launch { catchListDao.update(list) }

    /** Makes the Pokédex record [game]'s catches in [list] (null: the game's own checklist). */
    fun useCatchList(game: Game, list: CatchListEntity?) = scope.launch { settings.setActiveList(game, list?.key) }

    fun deleteCatchList(list: CatchListEntity) = scope.launch {
        val game = Game.fromKey(list.game)
        if (settings.activeList(game).first() == list.key) settings.setActiveList(game, null)
        trainerDao.clearCaught(list.key)
        catchListDao.delete(list.id)
    }

    // ---- game copies -----------------------------------------------------------------------------------------------------

    val gameCopies: Flow<List<GameCopyEntity>> = gameCopyDao.all()

    suspend fun createGameCopy(copy: GameCopyEntity): Long = gameCopyDao.insert(copy.copy(id = 0, createdAt = System.currentTimeMillis()))

    fun updateGameCopy(copy: GameCopyEntity) = scope.launch { gameCopyDao.update(copy) }

    /** Makes the Pokédex show [copy]'s game and record catches in [copy]. */
    fun useGameCopy(copy: GameCopyEntity) = scope.launch {
        val game = Game.fromKey(copy.game)
        settings.setGame(game)
        settings.setActiveList(game, copy.key)
    }

    fun deleteGameCopy(copy: GameCopyEntity) = scope.launch {
        val game = Game.fromKey(copy.game)
        if (settings.activeList(game).first() == copy.key) settings.setActiveList(game, null)
        trainerDao.clearCaught(copy.key)
        saveLinks.unlink(copy.id)
        saveDao.delete(copy.id)
        gameCopyDao.delete(copy.id)
    }

    // ---- imported saves ---------------------------------------------------------------------------------------------------

    private val saveDao = db.saves()

    val saveImports: Flow<List<SaveImportEntity>> = saveDao.imports()

    fun saveImport(copyId: Long) = saveDao.importOf(copyId)

    fun savedPokemon(copyId: Long) = saveDao.pokemon(copyId)

    /** Save files linked to game copies, read again whenever the app opens. */
    val saveLinks = SaveLinks(context.applicationContext, saveDao, scope, { gameCopyDao.allOnce() }) { save, fileName, copy ->
        importSave(save, fileName, copy, GamePlatform.fromName(copy.platform)).second
    }

    /** The copies an import of [save] could update: same game and trainer ID, then copies of the game with no ID yet. */
    fun copiesMatching(save: GameSave, copies: List<GameCopyEntity>) =
        copies.filter { it.game == save.game.key && (it.trainerId.isBlank() || it.trainerId.toIntOrNull() == save.trainerId) }
            .sortedBy { it.trainerId.isBlank() }

    /**
     * Imports [save] into [into], or a new copy when null: the trainer's details, the game's caught marks (added to the
     * copy's, never removing any), shinies the trainer caught (to the Shiny Dex), and the party and boxes, which replace
     * any earlier import. [platform] is what the trainer said the save came from. Returns the copy's id and how many
     * catches were new.
     */
    suspend fun importSave(save: GameSave, fileName: String, into: GameCopyEntity?, platform: GamePlatform): Pair<Long, Int> {
        val now = System.currentTimeMillis()
        val trainerId = "%05d".format(save.trainerId)
        val own = save.all.filter { it.ownCatch && !it.egg }
        val language = own.groupingBy { it.language }.eachCount().maxByOrNull { it.value }?.key?.let { SAVE_LANGUAGES[it] }
        val copyId = if (into != null) {
            gameCopyDao.update(into.copy(playerName = save.trainerName, trainerId = trainerId, nationalDex = into.nationalDex || save.nationalDex,
                language = into.language ?: language, platform = platform.name))
            into.id
        } else gameCopyDao.insert(GameCopyEntity(
            game = save.game.key, label = "", playerName = save.trainerName, trainerId = trainerId, language = language,
            platform = platform.name, nationalDex = save.nationalDex && save.game.hasNationalDex, createdAt = now,
        ))
        val added = trainerDao.addCaught(save.owned.map { CaughtEntity(GameCopyEntity.keyOf(copyId), it, now) }).count { it != -1L }
        trainerDao.addShiny(own.filter { it.shiny }.map { ShinyEntity(save.game.key, it.speciesId, now) })
        saveDao.replace(
            SaveImportEntity(copyId, now, fileName, save.playMinutes, save.boxNames.joinToString("\n"),
                save.badges.takeIf { it != GameSave.UNKNOWN_BADGES }, save.startedAt),
            save.all.map { p ->
                SavePokemonEntity(
                    copyId = copyId, box = p.box, slot = p.slot, speciesId = p.speciesId, nickname = p.nickname, otName = p.otName,
                    otId = p.otId, ownCatch = p.ownCatch, pid = p.pid, exp = p.exp, heldItem = p.heldItem, friendship = p.friendship,
                    moves = p.moves.joinToString(","), ivs = p.ivs.joinToString(","), evs = p.evs.joinToString(","),
                    metLevel = p.metLevel, metLocation = p.metLocation, originGame = p.originGame, ball = p.ball, egg = p.egg,
                    abilitySlot = p.abilitySlot, shiny = p.shiny, language = p.language, metDate = p.metDate, metTime = p.metTime,
                    form = p.form, gender = p.gender, nature = p.nature,
                )
            },
        )
        return copyId to added
    }

    /** Every game's caught checklist, for My Progress. */
    val allCaught = trainerDao.allCaughtFlow()

    // ---- Shiny Dex ------------------------------------------------------------------------------------------------------

    fun shiny(game: Game) = trainerDao.shiny(game.key).map { it.toSet() }

    val allShiny = trainerDao.allShinyFlow()

    // ---- shiny hunts --------------------------------------------------------------------------------------------------

    /** The newest hunt for [speciesId] in [game], finished or not. */
    fun hunt(game: Game, speciesId: Int) = huntDao.latest(game.key, speciesId)

    val activeHunts = huntDao.active()

    fun huntById(id: Long) = huntDao.byId(id)

    /** [game]'s unfinished hunt for [speciesId], or a new one; returns its id (for the hunt screen). */
    suspend fun openHunt(game: Game, speciesId: Int, formId: Int): Long =
        huntDao.latest(game.key, speciesId).first()?.takeIf { it.foundAt == null }?.id
            ?: huntDao.insert(ShinyHuntEntity(game = game.key, speciesId = speciesId, formId = formId, method = HuntMethod.ENCOUNTERS.name,
                shinyCharm = false, count = 0, startedAt = System.currentTimeMillis()))

    fun startHunt(game: Game, speciesId: Int, formId: Int, method: HuntMethod) = scope.launch {
        huntDao.insert(ShinyHuntEntity(game = game.key, speciesId = speciesId, formId = formId, method = method.name,
            shinyCharm = false, count = 0, startedAt = System.currentTimeMillis()))
    }

    fun countHunt(id: Long, delta: Int) = scope.launch { huntDao.add(id, delta) }

    fun updateHunt(hunt: ShinyHuntEntity) = scope.launch { huntDao.update(hunt) }

    fun deleteHunt(id: Long) = scope.launch { huntDao.delete(id) }

    /** Ends [hunt] as found and marks the Pokémon shiny in the Shiny Dex. */
    fun foundShiny(hunt: ShinyHuntEntity) = scope.launch {
        val now = System.currentTimeMillis()
        huntDao.update(hunt.copy(foundAt = now))
        trainerDao.addShiny(listOf(ShinyEntity(hunt.game, hunt.speciesId, now)))
    }

    fun setShiny(game: Game, speciesId: Int, caught: Boolean) = scope.launch {
        if (caught) trainerDao.addShiny(listOf(ShinyEntity(game.key, speciesId, System.currentTimeMillis())))
        else trainerDao.removeShiny(game.key, speciesId)
    }

    // ---- Team Builder -----------------------------------------------------------------------------------------------------

    val teams = teamDao.teams()
    val teamMembers = teamDao.allMembers()
    fun team(id: Long) = teamDao.team(id)
    fun members(teamId: Long) = teamDao.members(teamId)

    suspend fun createTeam(name: String, game: Game): Long =
        teamDao.insert(TeamEntity(name = name, game = game.key, updatedAt = System.currentTimeMillis()))

    fun updateTeam(team: TeamEntity) = scope.launch { teamDao.update(team.copy(updatedAt = System.currentTimeMillis())) }

    fun setMember(member: TeamMemberEntity) = scope.launch {
        teamDao.upsertMember(member)
        teamDao.touch(member.teamId, System.currentTimeMillis())
    }

    fun removeMember(teamId: Long, slot: Int) = scope.launch {
        teamDao.removeMember(teamId, slot)
        teamDao.touch(teamId, System.currentTimeMillis())
    }

    fun deleteTeam(teamId: Long) = scope.launch { teamDao.delete(teamId) }

    /** Puts a Pokémon (form [formId]) in [teamId]'s first free slot; does nothing when the team is full. */
    fun addToTeam(teamId: Long, speciesId: Int, formId: Int) = scope.launch {
        teamDao.addToFirstFreeSlot(teamId, speciesId, formId, System.currentTimeMillis())
    }

    /** Starts a new team for [game] with this Pokémon in its first slot. */
    fun createTeamWith(name: String, game: Game, speciesId: Int, formId: Int) = scope.launch {
        val id = createTeam(name, game)
        teamDao.upsertMember(TeamMemberEntity(id, 0, speciesId, formId, ""))
    }

    // ---- backup ----------------------------------------------------------------------------------------------------------

    val backup = Backup(db)

    /** Keeps the trainer's chosen backup file up to date as their data changes. */
    val autoBackup = AutoBackup(context.applicationContext, backup, db, settings, scope).also { it.start() }

    private companion object {
        /** Generation III language codes as data language codes. */
        val SAVE_LANGUAGES = mapOf(1 to "ja", 2 to "en", 3 to "fr", 4 to "it", 5 to "de", 7 to "es")
    }
}
