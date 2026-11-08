package io.lemoncore.opendatadex.data

import androidx.annotation.StringRes
import io.lemoncore.opendatadex.R
import io.lemoncore.opendatadex.data.save.gen3ItemKey
import io.lemoncore.opendatadex.data.save.gen3Place
import io.lemoncore.opendatadex.data.seed.Seed
import io.lemoncore.opendatadex.data.seed.SeedAbility
import io.lemoncore.opendatadex.data.seed.SeedArea
import io.lemoncore.opendatadex.data.seed.SeedEvolution
import io.lemoncore.opendatadex.data.seed.SeedItem
import io.lemoncore.opendatadex.data.seed.SeedLearnsets
import io.lemoncore.opendatadex.data.seed.SeedMove
import io.lemoncore.opendatadex.data.seed.SeedNature
import io.lemoncore.opendatadex.data.seed.SeedPokemon
import io.lemoncore.opendatadex.data.seed.SeedSpecies
import io.lemoncore.opendatadex.data.seed.SeedType
import io.lemoncore.opendatadex.data.seed.textAt
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

// ---- view models of the data -------------------------------------------------------------------------------------------

data class PokemonSummary(
    /** Species id: what favorites, the caught checklist and navigation key on. */
    val id: Int,
    /** Species name, or the form's name for a regional variant shown in its region ("Alolan Rattata"). */
    val name: String,
    val types: List<Int>,
    /** Number in the selected game's regional dex (national number in the national view). */
    val dexNumber: Int,
    val colorId: Int,
    /** Pokémon (form) id used for artwork and sprites. */
    val formId: Int = id,
    /** Which of the game's dexes lists it (0 for the main one; Isle of Armor is 1 in Sword/Shield, and so on). */
    val dexIndex: Int = 0,
)

data class StatLine(val statId: Int, @StringRes val label: Int, val base: Int, val min: Int, val max: Int)

/** A move as it was in a given game: type, power, PP, accuracy and category follow that game's rules. */
data class MoveInfo(
    val id: Int,
    val name: String,
    val gen: Int,
    val type: Int,
    val power: Int,
    val pp: Int,
    val accuracy: Int,
    val priority: Int,
    /** 1 status, 2 physical, 3 special. */
    val category: Int,
    val shortEffect: String,
    val effect: String,
    val description: String,
)

data class LearnedMove(val move: MoveInfo, val level: Int, val machine: String?, val machineNumber: Int)

/** One way a Pokémon learns a move: at [level] for LEVEL_UP, from [machine] ("TM24") for MACHINE. */
data class LearnWay(val method: LearnMethod, val level: Int, val machine: String?)

enum class LearnMethod(val id: Int, @StringRes val label: Int) {
    LEVEL_UP(1, R.string.method_level_up), MACHINE(4, R.string.method_tm_hm), EGG(2, R.string.method_egg), TUTOR(3, R.string.method_tutor);
}

data class EvolutionNode(
    val pokemon: PokemonSummary, val method: String?, val next: List<EvolutionNode>,
    /** How this Pokémon evolves from the one before it (empty for the first in the family). */
    val methodKinds: Set<EvolutionKind> = emptySet(),
) {
    /** Every evolution's kinds below this point of the family, for the Evolution Dex filter. */
    val kinds: Set<EvolutionKind> get() = next.flatMapTo(HashSet()) { it.methodKinds + it.kinds }
}

/** The Evolution Dex's ways to evolve, as in dataDex 4: by level (friendship and beauty count), trade, item, or otherwise. */
enum class EvolutionKind(@StringRes val label: Int) {
    LEVEL(R.string.evolve_by_level), TRADE(R.string.evolve_by_trade), ITEM(R.string.evolve_by_item), OTHER(R.string.evolve_by_other),
}

private val LEVEL_PREFIXES = listOf("Level", "High friendship", "High affection", "Max Beauty")
/** Methods that are neither levelling, trading nor using an item: Shedinja's spare slot, Alcremie's spin, Kubfu's scrolls… */
private val OTHER_PREFIXES = listOf("Use move", "Train", "Take recoil damage", "Shed", "Spin", "Land three critical hits", "Go somewhere",
    "Defeat ", "Collect ", "Feed ", "Scroll of ")

data class Encounter(
    val game: Game,
    val areaId: Int,
    val place: String,
    val method: String,
    /** PokéAPI encounter method id (18 gift, 20 static, 36 in-game trade…). */
    val methodId: Int,
    val minLevel: Int,
    val maxLevel: Int,
    val rarity: Int,
    val conditions: List<String>,
    val speciesId: Int,
    /** The form found there (an Alolan Rattata is a different Pokémon id from a Kantonian one). */
    val pokemonId: Int,
)

/** Which alternate forms the trainer wants to see; mirrors [Settings.regionalForms] and [Settings.battleForms]. */
data class FormOptions(val regionalForms: Boolean = true, val battleForms: Boolean = true)

data class HeldItem(val item: SeedItem, val rarity: Int, val game: Game)

data class AbilityRef(val ability: SeedAbility, val hidden: Boolean)

data class SearchHit(val kind: Kind, val id: Int, val title: String, val subtitle: String) {
    enum class Kind(@StringRes val label: Int) {
        POKEMON(R.string.search_pokemon), MOVE(R.string.search_moves), ABILITY(R.string.search_abilities),
        ITEM(R.string.search_items), NATURE(R.string.search_natures), LOCATION(R.string.search_locations),
    }
}

/** One game pair's move pools, parsed from its learnset pack. */
class Learnsets(val versionGroup: Int, private val machineByMove: Map<Int, Int>, private val byPokemon: Map<Int, IntArray>) {
    fun machineNumber(moveId: Int) = machineByMove[moveId]
    fun rows(pokemonId: Int): IntArray? = byPokemon[pokemonId]
    val pokemonIds: Set<Int> get() = byPokemon.keys
}

/**
 * In-memory index over the bundled seed. Every query takes the [Game] so that game-specific rules
 * (types and stats of the time, move changes, regional dex numbers and forms, evolution methods) are applied in one place.
 *
 * Move pools live in one pack per game pair and are parsed on first use by [learnsets]; [readPack] supplies the
 * pack text for a version group id (null when missing).
 */
class Dex(private val seed: Seed, private val readPack: (Int) -> String? = { null }) {
    val species: List<SeedSpecies> = seed.species.sortedBy { it.id }
    private val speciesById = species.associateBy { it.id }
    private val pokemonById = seed.pokemon.associateBy { it.id }
    val types: List<SeedType> = seed.types.sortedBy { it.id }
    private val typeById = types.associateBy { it.id }
    private val movesById = seed.moves.associateBy { it.id }
    private val abilitiesById = seed.abilities.associateBy { it.id }
    private val itemsById = seed.items.associateBy { it.id }
    private val itemsByKey = seed.items.associateBy { it.key }
    private val areasById = seed.areas.associateBy { it.id }
    private val locationNames = seed.areas.associate { it.location to it.name } + seed.placeNames.associate { it.id to it.name }
    private val saveItems by lazy { seed.saveItems.groupBy { it[0] }.mapValues { (_, rows) -> rows.associate { it[1] to it[2] } } }
    private val savePlaces by lazy { seed.savePlaces.groupBy { it[0] }.mapValues { (_, rows) -> rows.associate { it[1] to it[2] } } }
    private val evolutionsBySpecies = seed.evolutions.groupBy { it.species }
    private val growthById = seed.growthRates.associateBy { it.id }
    private val heldItemsByPokemon = seed.heldItems.groupBy { it[0] }
    private val pokedexById = seed.pokedexes.associateBy { it.id }
    private val efficacy = seed.efficacy.associate { (a, d, f) -> (a to d) to f }
    private val efficacyPast = seed.efficacyPast.groupBy { (a, d) -> a to d }
    val natures: List<SeedNature> = seed.natures.sortedBy { it.name }
    val statNames: Map<Int, String> = seed.stats.mapKeys { it.key.toInt() }

    private val ownEncounterGames = seed.encounters.mapNotNullTo(HashSet()) { Game.ofVersion(it[1]) }

    /** Games with no encounters of their own, by the game whose encounters they show instead (Brilliant Diamond shows Diamond's). */
    private val encounterSources: Map<Game, Game> = Game.playable.mapNotNull { g ->
        val from = g.encounterFallback?.let { Game.ofVersion(it) }
        if (from != null && g !in ownEncounterGames && from in ownEncounterGames) g to from else null
    }.toMap()

    private val encounters: List<Encounter> = seed.encounters.flatMap { row ->
        val game = Game.ofVersion(row[1]) ?: return@flatMap emptyList()
        val conditions = row.drop(7)
        val borrowers = encounterSources.filterValues { it == game }.keys
        // The remakes have no GBA slot and no Pokémon Ranger link: they get the rows that apply with no GBA game inserted
        // (a row lists every slot it applies with), minus the slot conditions.
        val lend = borrowers.isNotEmpty() && row[3] != RANGER_MANAPHY && (conditions.none { it in GBA_SLOT } || NO_GBA_GAME in conditions)
        listOfNotNull(encounter(row, game, conditions)) +
            if (lend) borrowers.mapNotNull { encounter(row, it, conditions.filterNot { c -> c in GBA_SLOT }) } else emptyList()
    }

    private fun encounter(row: List<Int>, game: Game, conditions: List<Int>): Encounter? {
        val area = areasById[row[2]] ?: return null
        val pokemon = pokemonById[row[0]] ?: return null
        return Encounter(
            game = game,
            areaId = area.id,
            place = placeName(area),
            method = seed.encounterMethods[row[3].toString()] ?: "Unknown",
            methodId = row[3],
            minLevel = row[4],
            maxLevel = row[5],
            rarity = row[6],
            conditions = conditions.mapNotNull { seed.encounterConditions[it.toString()] },
            speciesId = pokemon.species,
            pokemonId = pokemon.id,
        )
    }

    private val encountersBySpecies = encounters.groupBy { it.speciesId }
    private val gamesWithEncounters = encounters.mapTo(HashSet()) { it.game }

    /** The game whose wild encounters [game] shows because the source data has none of its own, or null. */
    fun encountersBorrowedFrom(game: Game): Game? = encounterSources[game]
    private val gamesWithHeldItems = seed.heldItems.mapNotNullTo(HashSet()) { Game.ofVersion(it[1]) }

    /** False for games the source data has no wild encounters for yet (an empty list there means "unknown", not "none"). */
    fun hasEncounterData(game: Game) = game.isNational || game in gamesWithEncounters

    fun hasHeldItemData(game: Game) = game.isNational || game in gamesWithHeldItems

    // ---- species and forms ----------------------------------------------------------------------------------------------

    fun speciesOf(id: Int): SeedSpecies? = speciesById[id]

    fun pokemon(id: Int): SeedPokemon? = pokemonById[id]

    fun defaultForm(s: SeedSpecies): SeedPokemon = pokemonById.getValue(s.forms.first())

    /** The trainer's form preferences (Settings); applied by every query that picks or lists forms. */
    @Volatile var formOptions = FormOptions()

    /** Forms of [s] that exist in [game]: the default form plus alternates introduced by then (Megas and Gigantamax only where they exist). */
    fun formsIn(s: SeedSpecies, game: Game): List<SeedPokemon> = s.forms.mapNotNull { pokemonById[it] }.filter { p ->
        p.isDefault || ((formOptions.battleForms || (!p.mega && !p.gmax)) &&
            (game.isNational || (p.introduced <= game.order && (!p.mega || game.hasMegas) && (!p.gmax || game.hasGigantamax))))
    }

    /** Looks-only forms of [s] that exist in [game]. */
    fun cosmeticFormsIn(s: SeedSpecies, game: Game) = s.cosmetic.filter { game.isNational || it.introduced <= game.order }

    /** The form a game's dex shows: its regional variant where one exists (Alolan Raichu in Sun), else the default form. */
    fun displayForm(s: SeedSpecies, game: Game): SeedPokemon {
        if (game.region >= 7 && formOptions.regionalForms) {
            formsIn(s, game).firstOrNull { it.region == game.region }?.let { return it }
        }
        return defaultForm(s)
    }

    /** [game]'s dex: its regional dexes, or with [national] (once unlocked in the game) its National Pokédex. */
    fun pokemonFor(game: Game, national: Boolean = false): List<PokemonSummary> {
        if (national && game.hasNationalDex) {
            return species.filter { it.id <= game.nationalDexSize }.sortedBy { it.id }
                .map { s -> summary(s, game).copy(dexNumber = s.id, dexIndex = 0) }
        }
        val dexes = game.pokedexIds.map { it.toString() }
        return species.mapNotNull { s ->
            if (s.gen > game.speciesGen) return@mapNotNull null
            val index = dexes.indexOfFirst { s.dex.containsKey(it) }
            if (index < 0) return@mapNotNull null
            summary(s, game, displayForm(s, game)).copy(dexNumber = s.dex.getValue(dexes[index]), dexIndex = index)
        }.sortedWith(compareBy({ it.dexIndex }, { it.dexNumber }))
    }

    fun summary(s: SeedSpecies, game: Game, form: SeedPokemon = displayForm(s, game)): PokemonSummary {
        val dexes = game.pokedexIds.map { it.toString() }
        val index = dexes.indexOfFirst { s.dex.containsKey(it) }
        return PokemonSummary(
            id = s.id,
            name = if (form.region != 0) form.name else s.name,
            types = typesOf(form, game),
            dexNumber = if (index >= 0) s.dex.getValue(dexes[index]) else s.id,
            colorId = s.color,
            formId = form.id,
            dexIndex = index.coerceAtLeast(0),
        )
    }

    fun summary(id: Int, game: Game): PokemonSummary? = speciesById[id]?.let { summary(it, game) }

    /** Summary of a specific form (used where a list names the form, such as encounters and ability holders). */
    fun summaryOfForm(pokemonId: Int, game: Game): PokemonSummary? {
        val p = pokemonById[pokemonId] ?: return null
        val s = speciesById[p.species] ?: return null
        return summary(s, game, p).copy(name = p.name)
    }

    fun pokedexName(id: Int) = pokedexById[id]?.name ?: ""

    fun regionName(id: Int) = seed.regions[id.toString()]

    fun typesOf(p: SeedPokemon, game: Game): List<Int> {
        val past = p.typesPast.filter { it[0] >= game.dataGen }
        if (past.isEmpty()) return p.types
        val gen = past.minOf { it[0] }
        return past.filter { it[0] == gen }.sortedBy { it[1] }.map { it[2] }
    }

    /** Pokédex text for [game] with the version it came from, falling back to a sibling game's or the latest text (null version). */
    fun flavorText(s: SeedSpecies, game: Game): Pair<String, String?>? {
        if (!game.isNational) {
            for (v in listOf(game.versionId) + game.flavorFallback) {
                s.flavor[v.toString()]?.let { text -> return text to seed.versions.firstOrNull { it.id == v }?.name }
            }
        }
        return s.flavor["latest"]?.let { it to null }
    }

    /** Base stats with level-100 minimum and maximum using that generation's stat formula. */
    fun baseStats(p: SeedPokemon, game: Game): List<StatLine> {
        val gen = game.dataGen
        fun pastValue(statId: Int): Int? = p.statsPast.filter { it[1] == statId && it[0] >= gen }.minByOrNull { it[0] }?.get(2)
        val ids = if (gen == 1) listOf(1, 2, 3, 9, 6) else listOf(1, 2, 3, 4, 5, 6)
        return ids.map { statId ->
            val base = pastValue(statId) ?: if (statId == 9) p.stats[3] else p.stats[statId - 1]
            val (min, max) = statRange(statId == 1, base, gen)
            StatLine(statId, statLabel(statId), base, min, max)
        }
    }

    fun evYield(p: SeedPokemon): List<Pair<Int, Int>> = p.ev.mapIndexedNotNull { i, v -> if (v > 0) statLabel(i + 1) to v else null }

    fun growthRate(s: SeedSpecies) = growthById[s.growth]

    fun eggGroupNames(s: SeedSpecies) = s.eggGroups.mapNotNull { seed.eggGroups[it.toString()] }

    /** Every egg group, as id and name, by name. */
    val eggGroups: List<Pair<Int, String>> get() = seed.eggGroups.map { (id, name) -> id.toInt() to name }.sortedBy { it.second }

    fun eggGroupName(id: Int) = seed.eggGroups[id.toString()]

    fun inEggGroup(p: PokemonSummary, groupId: Int) = speciesById[p.id]?.eggGroups?.contains(groupId) == true

    /** [p] (its form) has [abilityId] in [game], normal or hidden. */
    fun hasAbility(p: PokemonSummary, abilityId: Int, game: Game): Boolean =
        pokemonById[p.formId]?.let { form -> abilitiesOf(form, game).any { it.ability.id == abilityId } } == true

    /** Abilities that Pokémon in [list] have in [game], by name (the Pokédex's ability filter). */
    fun abilitiesAmong(list: List<PokemonSummary>, game: Game): List<SeedAbility> = list
        .flatMap { p -> pokemonById[p.formId]?.let { abilitiesOf(it, game).map { ref -> ref.ability } }.orEmpty() }
        .distinctBy { it.id }.sortedBy { it.name }

    /** [p] (its form) learns [moveId] in the game of [ls] by any method. */
    fun learnsMove(p: PokemonSummary, moveId: Int, ls: Learnsets): Boolean {
        val rows = pokemonById[p.formId]?.let { rowsFor(it, ls) } ?: return false
        for (i in rows.indices step 3) if (rows[i] == moveId) return true
        return false
    }

    /** Abilities as they were in [game]: earlier slot contents applied, and no hidden abilities before Generation V. */
    fun abilitiesOf(p: SeedPokemon, game: Game): List<AbilityRef> {
        val slots = p.abilities.associate { (slot, id, hidden) -> slot to (id to hidden) }.toMutableMap()
        p.abilitiesPast.filter { it[0] >= game.dataGen }.groupBy { it[1] }.forEach { (slot, rows) ->
            val row = rows.minBy { it[0] }
            if (row[2] == 0) slots.remove(slot) else slots[slot] = row[2] to row[3]
        }
        return slots.toSortedMap().values
            .filter { (_, hidden) -> hidden == 0 || game.dataGen >= 5 }
            .mapNotNull { (id, hidden) -> abilitiesById[id]?.let { AbilityRef(it, hidden == 1) } }
    }

    // ---- types ---------------------------------------------------------------------------------------------------------

    fun type(id: Int): SeedType? = typeById[id]

    fun typesIn(game: Game): List<SeedType> = types.filter { it.gen <= if (game.isNational) LATEST_GEN else game.dataGen }

    /** Damage factor in percent (0, 25, 50, 100, 200, 400) of an attacking type against one defending type. */
    fun factor(attack: Int, defend: Int, gen: Int): Int {
        efficacyPast[attack to defend]?.filter { it[3] >= gen }?.minByOrNull { it[3] }?.let { return it[2] }
        return efficacy[attack to defend] ?: 100
    }

    /** Combined factor of an [attack]ing type against a Pokémon of [defending] types in [game]. */
    fun factorAgainst(attack: Int, defending: List<Int>, game: Game): Int =
        defending.fold(100) { acc, def -> acc * factor(attack, def, game.dataGen) / 100 }

    /** Defending factor against every attacking type that exists in [game], grouped by factor (highest first). */
    fun damageTaken(defending: List<Int>, game: Game): List<Pair<Int, List<SeedType>>> = typesIn(game)
        .groupBy { atk -> defending.fold(100) { acc, def -> acc * factor(atk.id, def, game.dataGen) / 100 } }
        .toList()
        .sortedByDescending { it.first }

    // ---- moves ---------------------------------------------------------------------------------------------------------

    fun move(id: Int, game: Game): MoveInfo? = movesById[id]?.let { moveAsIn(it, game) }

    fun allMoves(game: Game): List<MoveInfo> = seed.moves
        .filter { it.gen <= game.speciesGen }
        .map { moveAsIn(it, game) }
        .sortedBy { it.name }

    private fun moveAsIn(m: SeedMove, game: Game): MoveInfo {
        val later = m.changes.filter { it[0] > game.order }.sortedBy { it[0] }
        fun field(index: Int, unset: Int, current: Int) = later.firstOrNull { it[index] != unset }?.get(index) ?: current
        val type = field(1, 0, m.type)
        // Until Generation IV, a damaging move's category came from its type.
        val category = if (m.dmgClass == 1 || game.dataGen > 3) m.dmgClass else typeById[type]?.dmgClass ?: m.dmgClass
        val description = m.flavor.textAt(game.order) ?: m.short
        return MoveInfo(
            id = m.id, name = m.name, gen = m.gen, type = type,
            power = field(2, -1, m.power), pp = field(3, -1, m.pp), accuracy = field(4, -1, m.acc),
            priority = m.priority, category = category, shortEffect = m.short, effect = m.effect, description = description,
        )
    }

    private val packJson = Json { ignoreUnknownKeys = true }
    private val learnsetCache = ConcurrentHashMap<Int, Learnsets>()

    /** Catch plans by game and National Pokédex mode (see [catchPlan]). */
    internal val catchPlans = ConcurrentHashMap<Pair<Game, Boolean>, Map<Int, Obtain>>()

    /** Move pools for [game]; parses the game's pack on first use, so call it off the main thread. */
    fun learnsets(game: Game): Learnsets = learnsetCache.getOrPut(game.versionGroup) {
        val text = readPack(game.versionGroup)
        if (text == null) Learnsets(game.versionGroup, emptyMap(), emptyMap())
        else {
            val pack = packJson.decodeFromString(SeedLearnsets.serializer(), text)
            Learnsets(
                pack.vg,
                pack.machines.associate { (number, move) -> move to number },
                pack.pokemon.entries.associate { (id, rows) -> id.toInt() to rows.toIntArray() },
            )
        }
    }

    /** Move pools for [game] if they have already been parsed. */
    fun learnsetsIfLoaded(game: Game): Learnsets? = learnsetCache[game.versionGroup]

    /** The move rows for [p], or the default form's when the form has no move pool of its own (Megas, for example). */
    private fun rowsFor(p: SeedPokemon, ls: Learnsets): IntArray? =
        ls.rows(p.id) ?: speciesById[p.species]?.let { ls.rows(it.forms.first()) }

    fun movesLearned(p: SeedPokemon, game: Game, method: LearnMethod, ls: Learnsets): List<LearnedMove> {
        if (game.isNational) return emptyList()
        val rows = rowsFor(p, ls) ?: return emptyList()
        val out = mutableListOf<LearnedMove>()
        for (i in rows.indices step 3) {
            if (rows[i + 1] != method.id) continue
            val moveId = rows[i]
            val info = movesById[moveId]?.let { moveAsIn(it, game) } ?: continue
            val number = if (method == LearnMethod.MACHINE) ls.machineNumber(moveId) else null
            out += LearnedMove(info, rows[i + 2], number?.let { machineLabel(it, game.versionGroup) }, number ?: Int.MAX_VALUE)
        }
        return when (method) {
            LearnMethod.MACHINE -> out.sortedBy { it.machineNumber }
            LearnMethod.LEVEL_UP -> out.sortedBy { it.level }
            else -> out.sortedBy { it.move.name }
        }
    }

    /** Every move [p] can learn in [game] by any method, by name (the Team Builder's move choices). */
    fun learnableMoves(p: SeedPokemon, game: Game, ls: Learnsets): List<MoveInfo> {
        val rows = rowsFor(p, ls) ?: return emptyList()
        return (rows.indices step 3).map { rows[it] }.distinct()
            .mapNotNull { id -> movesById[id]?.let { moveAsIn(it, game) } }
            .sortedBy { it.name }
    }

    @StringRes fun methodLabel(method: LearnMethod, game: Game) = when {
        method != LearnMethod.MACHINE -> method.label
        game.versionGroup == 20 -> R.string.method_tm_tr
        game.dataGen in 1..6 -> R.string.method_tm_hm
        else -> R.string.method_tm
    }

    /** Pokémon that learn [moveId] in [game] by any method, with how. */
    fun learners(moveId: Int, game: Game, ls: Learnsets): List<Pair<PokemonSummary, List<LearnWay>>> {
        if (game.isNational) return emptyList()
        return pokemonFor(game).mapNotNull { summary ->
            val p = pokemonById[summary.formId] ?: return@mapNotNull null
            val rows = rowsFor(p, ls) ?: return@mapNotNull null
            val how = mutableListOf<LearnWay>()
            for (i in rows.indices step 3) {
                if (rows[i] != moveId) continue
                how += when (val method = LearnMethod.entries.firstOrNull { it.id == rows[i + 1] } ?: LearnMethod.TUTOR) {
                    LearnMethod.LEVEL_UP -> LearnWay(method, rows[i + 2].coerceAtLeast(1), null)
                    LearnMethod.MACHINE -> LearnWay(method, 0, ls.machineNumber(moveId)?.let { machineLabel(it, game.versionGroup) })
                    else -> LearnWay(method, 0, null)
                }
            }
            if (how.isEmpty()) null else summary to how.distinct()
        }
    }

    // ---- evolutions -------------------------------------------------------------------------------------------------------

    fun evolutionTree(s: SeedSpecies, game: Game): EvolutionNode? {
        val family = species.filter { it.chain == s.chain && it.gen <= game.speciesGen }
        if (family.size <= 1) return null
        val ids = family.map { it.id }.toSet()
        fun node(sp: SeedSpecies): EvolutionNode = EvolutionNode(
            pokemon = summary(sp, game),
            method = if (sp.evolvesFrom in ids) evolutionMethod(sp, game) else null,
            next = family.filter { it.evolvesFrom == sp.id }.map(::node),
            methodKinds = if (sp.evolvesFrom in ids) evolutionKinds(sp, game) else emptySet(),
        )
        val root = family.firstOrNull { it.evolvesFrom !in ids } ?: return null
        return node(root)
    }

    /** Every evolution family with a member in [game]'s dex, in dex order (the Evolution Dex). */
    fun evolutionFamilies(game: Game): List<EvolutionNode> = pokemonFor(game)
        .distinctBy { speciesById[it.id]?.chain }
        .mapNotNull { p -> speciesById[p.id]?.let { evolutionTree(it, game) } }

    /**
     * How a Pokémon evolves into [target] in [game]: the newest method at or before the game for each resulting form.
     * Regional methods (Alolan Raichu) replace the usual one inside their region and are labelled elsewhere.
     */
    fun evolutionMethod(target: SeedSpecies, game: Game): String? {
        val texts = evolutionRows(target, game).map { it.text }.distinct()
        // Male and female evolving into their own forms the same way (Lechonk, Espurr…) reads as one method.
        val ungendered = texts.map { it.removeSuffix(" (male)").removeSuffix(" (female)") }.distinct()
        return (if (ungendered.size == 1 && texts.size > 1) ungendered else texts).joinToString(" / ").ifEmpty { null }
    }

    /** The evolution rows that apply to [target] in [game] (see [evolutionMethod]). */
    private fun evolutionRows(target: SeedSpecies, game: Game): List<SeedEvolution> {
        val rows = evolutionsBySpecies[target.id] ?: return emptyList()
        val inRegion = rows.filter { it.region != 0 && it.region == game.region }
        val pool = when {
            game.isNational -> rows
            inRegion.isNotEmpty() -> inRegion
            else -> rows.filter { it.region == 0 }
        }
        return pool.groupBy { it.region to it.toForm }.values.mapNotNull { group ->
            group.filter { it.vg <= game.order }.maxByOrNull(SeedEvolution::vg) ?: group.minByOrNull(SeedEvolution::vg)
        }
    }

    /** [target] evolves by trading in [game] (Alakazam, Gengar, Steelix…); the seed words that trigger "Trade". */
    fun evolvesByTrade(target: SeedSpecies, game: Game): Boolean = evolutionRows(target, game).any { it.text.startsWith("Trade") }

    /** How [target] evolves in [game], sorted into the Evolution Dex's kinds (from the seed's English method text). */
    fun evolutionKinds(target: SeedSpecies, game: Game): Set<EvolutionKind> = evolutionRows(target, game).mapTo(HashSet()) { row ->
        val t = row.text
        when {
            t.startsWith("Trade") -> EvolutionKind.TRADE
            LEVEL_PREFIXES.any { t.startsWith(it) } -> EvolutionKind.LEVEL
            OTHER_PREFIXES.any { t.startsWith(it) } -> EvolutionKind.OTHER
            // Anything else names the item used on it: Fire Stone, Tart Apple, Peat Block…
            else -> EvolutionKind.ITEM
        }
    }

    // ---- locations and held items ---------------------------------------------------------------------------------------------

    fun encountersOf(s: SeedSpecies, game: Game): List<Encounter> =
        encountersBySpecies[s.id].orEmpty().filter { game.isNational || it.game == game }

    fun areasIn(game: Game): List<Pair<SeedArea, String>> = encounters
        .filter { game.isNational || it.game == game }
        .map { it.areaId }.distinct()
        .mapNotNull { areasById[it] }
        .map { it to placeName(it) }
        .sortedBy { it.second }

    fun encountersAt(areaId: Int, game: Game): List<Encounter> =
        encounters.filter { it.areaId == areaId && (game.isNational || it.game == game) }

    fun area(id: Int) = areasById[id]

    /** A PokéAPI location's name in the data language, when oDex has encounters there. */
    fun locationName(locationId: Int): String? = locationNames[locationId]

    /** The item a save from [generation] means by [index], or null for none or one oDex doesn't know. */
    fun saveItemKey(generation: Int, index: Int): String? =
        if (generation == 3) gen3ItemKey(index) else saveItems[generation]?.get(index)?.let { itemsById[it]?.key }

    /** The place a save from [generation] means by [index], in the data language where oDex has it, or null. */
    fun savePlaceName(generation: Int, index: Int): String? =
        if (generation == 3) gen3Place(index)?.let { p -> p.locationId.takeIf { it != 0 }?.let { locationName(it) } ?: p.name }
        else savePlaces[generation]?.get(index)?.let { locationName(it) }

    /** [game]'s encounters grouped by PokéAPI location (a location can have several areas, such as a cave's floors). */
    fun encountersByLocation(game: Game): Map<Int, List<Encounter>> = encounters
        .filter { game.isNational || it.game == game }
        .groupBy { areasById.getValue(it.areaId).location }

    /**
     * The location's name plus whatever the area adds: "Route 2 (south, towards Viridian City)", "Great Marsh Area 1".
     * Area names alone are inconsistent (Gen I areas say "Road 1" for Route 1).
     */
    fun placeName(a: SeedArea): String {
        val detail = PLACE_DETAIL.find(a.area)?.groupValues?.get(1)
        return when {
            detail != null -> "${a.name} ($detail)"
            a.area.length > a.name.length && a.area.startsWith(a.name, ignoreCase = true) -> a.name + a.area.substring(a.name.length)
            else -> a.name
        }
    }

    /** Items wild [p] may hold in [game] (every game in the national view). */
    fun heldItems(p: SeedPokemon, game: Game): List<HeldItem> = heldItemsByPokemon[p.id].orEmpty().mapNotNull { (_, v, itemId, rarity) ->
        val g = Game.ofVersion(v) ?: return@mapNotNull null
        if (!game.isNational && g != game) return@mapNotNull null
        itemsById[itemId]?.let { HeldItem(it, rarity, g) }
    }

    // ---- abilities, items, natures -----------------------------------------------------------------------------------------

    val abilities: List<SeedAbility> get() = seed.abilities.sortedBy { it.name }

    fun ability(id: Int) = abilitiesById[id]

    fun abilityDescription(a: SeedAbility, game: Game): String = a.flavor.textAt(game.order) ?: a.short

    /** Every Pokémon and form that has [abilityId] today, with whether it is the hidden ability. */
    fun pokemonWithAbility(abilityId: Int): List<Pair<PokemonSummary, Boolean>> = seed.pokemon
        .mapNotNull { p ->
            val slot = p.abilities.firstOrNull { it[1] == abilityId } ?: return@mapNotNull null
            summaryOfForm(p.id, Game.NATIONAL)?.let { it to (slot[2] == 1) }
        }
        .sortedWith(compareBy({ it.first.id }, { it.first.formId }))

    fun items(game: Game): List<SeedItem> = seed.items
        .filter { it.gens.isNotEmpty() && (game.isNational || game.dataGen in it.gens) }
        .sortedBy { it.name }

    fun item(id: Int) = itemsById[id]

    /** An item by its PokéAPI key ("poke-ball"). */
    fun itemByKey(key: String) = itemsByKey[key]

    fun itemDescription(item: SeedItem, game: Game): String = item.flavor.textAt(game.order) ?: ""

    fun pokemonWithType(typeId: Int, game: Game) = pokemonFor(game).filter { typeId in it.types }

    // ---- search -------------------------------------------------------------------------------------------------------------

    fun search(query: String, game: Game): List<SearchHit> {
        val q = query.trim()
        if (q.length < 2) return emptyList()
        fun hit(name: String) = name.contains(q, ignoreCase = true)
        val out = mutableListOf<SearchHit>()
        pokemonFor(game).filter { hit(it.name) || it.dexNumber.toString() == q.trimStart('#', '0') }
            .forEach {
                val number = "#%03d".format(it.dexNumber)
                // In games with several dexes, say which one the number belongs to ("Blueberry #169").
                val subtitle = if (it.dexIndex > 0) "${pokedexName(game.pokedexIds[it.dexIndex])} $number" else number
                out += SearchHit(SearchHit.Kind.POKEMON, it.id, it.name, subtitle)
            }
        allMoves(game).filter { hit(it.name) }
            .forEach { out += SearchHit(SearchHit.Kind.MOVE, it.id, it.name, typeById[it.type]?.name ?: "") }
        abilities.filter { (game.isNational || it.gen <= game.dataGen) && hit(it.name) }
            .forEach { out += SearchHit(SearchHit.Kind.ABILITY, it.id, it.name, it.short) }
        items(game).filter { hit(it.name) }.forEach { out += SearchHit(SearchHit.Kind.ITEM, it.id, it.name, it.category) }
        natures.filter { hit(it.name) }.forEach { out += SearchHit(SearchHit.Kind.NATURE, it.id, it.name, "") }
        areasIn(game).filter { hit(it.second) }
            .forEach { (a, name) -> out += SearchHit(SearchHit.Kind.LOCATION, a.id, name, regionName(a.region) ?: "") }
        return out
    }

    companion object {
        private val PLACE_DETAIL = Regex("\\(([^)]*)\\)\\s*$")

        /** PokéAPI encounter conditions "No game in slot 2" to "LeafGreen in slot 2" (Generation IV's GBA slot). */
        private const val NO_GBA_GAME = 8
        private val GBA_SLOT = 8..13
        /** PokéAPI encounter method "Receiving a Manaphy from Pokemon Ranger". */
        private const val RANGER_MANAPHY = 48

        /**
         * In-game machine name: HMs are numbered from 101 until Generation VII, Sword/Shield numbers TRs from 100,
         * and Scarlet/Violet onwards use three digits.
         */
        fun machineLabel(number: Int, versionGroup: Int) = when {
            versionGroup == 20 -> if (number < 100) "TM%02d".format(number) else "TR%02d".format(number - 100)
            versionGroup == 25 || versionGroup == 30 -> "TM%03d".format(number)
            number > 100 && versionGroup < 17 -> "HM%02d".format(number - 100)
            else -> "TM%02d".format(number)
        }

        @StringRes fun statLabel(statId: Int) = when (statId) {
            1 -> R.string.stat_hp; 2 -> R.string.stat_attack; 3 -> R.string.stat_defense; 4 -> R.string.stat_sp_attack
            5 -> R.string.stat_sp_defense; 6 -> R.string.stat_speed; else -> R.string.stat_special
        }

        /** Level-100 stat range: neutral nature/no training up to perfect IVs/EVs (or max DVs and Stat Exp. in Gen I–II). */
        fun statRange(isHp: Boolean, base: Int, gen: Int): Pair<Int, Int> = when {
            gen <= 2 && isHp -> (2 * base + 110) to (2 * base + 203)
            gen <= 2 -> (2 * base + 5) to (2 * base + 98)
            isHp -> (2 * base + 110) to (2 * base + 204)
            else -> ((2 * base + 5) * 9 / 10) to ((2 * base + 99) * 11 / 10)
        }
    }
}
