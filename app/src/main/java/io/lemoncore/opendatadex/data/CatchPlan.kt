package io.lemoncore.opendatadex.data

import androidx.annotation.StringRes
import io.lemoncore.opendatadex.R
import io.lemoncore.opendatadex.data.seed.SeedSpecies

/** How a Pokémon can be obtained in one game, for planning a catch list before playing. */
enum class ObtainKind(@StringRes val title: Int) {
    WILD(R.string.obtain_wild),
    GIFT(R.string.obtain_gift),
    EVOLVE(R.string.obtain_evolve),
    BREED(R.string.obtain_breed),
    OTHER_VERSION(R.string.obtain_other_version),
    OTHER_GAME(R.string.obtain_other_game),
    /** Only from an event: a distribution, an event ticket island (Birth Island, Navel Rock…) or a bonus disc. */
    EVENT(R.string.obtain_event),
    UNKNOWN(R.string.obtain_unknown),
}

/**
 * [place] is where to find it first (the area with the lowest levels, which is usually the earliest), with [level]
 * the lowest level there. Pokémon you evolve or breed inherit the level of what they come from, so a plan can be
 * read in rough game order. The other fields hold what the plan says about it; the screen words them.
 */
data class Obtain(
    val kind: ObtainKind,
    val place: String?,
    val level: Int,
    /** WILD: the level range at [place]. */
    val minLevel: Int = 0,
    val maxLevel: Int = 0,
    /** GIFT: the encounter method (gift, static encounter, in-game trade). */
    val method: String? = null,
    /** WILD and GIFT: up to three places, and how many more there are. */
    val places: List<String> = emptyList(),
    val morePlaces: Int = 0,
    /** EVOLVE: the Pokémon to evolve, and how it evolves (null if unknown). BREED: the Pokémon to breed. */
    val from: String? = null,
    val how: String? = null,
    /** OTHER_VERSION and OTHER_GAME: the game it can be found in. */
    val otherGame: Game? = null,
    /** UNKNOWN: whether the game has location data at all. */
    val hasData: Boolean = true,
)

/** Gift, gift egg, static encounter, in-game trade and the Colosseum/XD and event bonus methods. */
private val GIFT_METHODS = setOf(18, 19, 20, 36) + (45..53)

/** Bonus discs and other distributions: Colosseum's Jirachi and Celebi, Pokémon Channel, Ranger's Manaphy, the NYC egg. */
private val EVENT_METHODS = 45..50

/**
 * Places reached only with an event ticket or a distribution: Birth Island, Navel Rock, Faraway Island, Newmoon Island,
 * Flower Paradise, the Hall of Origin, Liberty Garden, and the Pokémon Center gift men of the bonus discs.
 */
private val EVENT_AREAS = setOf(806, 807, 808, 838, 839, 840, 1167, 1210, 1212)

/** Southern Island needed the Eon Ticket in Ruby, Sapphire and Emerald; in Omega Ruby/Alpha Sapphire it is part of the story. */
private const val SOUTHERN_ISLAND = 785

private fun isEventEncounter(e: Encounter, game: Game) =
    e.methodId in EVENT_METHODS || e.areaId in EVENT_AREAS || (e.areaId == SOUTHERN_ISLAND && game.dataGen == 3)

/** Encounters in [game] that a trainer can reach without an event. */
private fun Dex.playableEncounters(s: SeedSpecies, game: Game) = encountersOf(s, game).filterNot { isEventEncounter(it, game) }

/** How to get every Pokémon in [game]'s dex (see [computeCatchPlan]), worked out once per game. */
fun Dex.catchPlan(game: Game, national: Boolean = false): Map<Int, Obtain> =
    catchPlans.getOrPut(game to national) { computeCatchPlan(game, national) }

/** The Pokémon in [game]'s dex that can only come from an event there (Mew, Deoxys, Navel Rock's Lugia…). */
fun Dex.eventOnly(game: Game, national: Boolean = false): Set<Int> =
    catchPlan(game, national).filterValues { it.kind == ObtainKind.EVENT }.keys

/**
 * Works out how to get every Pokémon in [game]'s dex: wild or gift encounters first, then by evolving or breeding
 * from something obtainable, then as the paired version's exclusive. With [national], the plan covers the game's
 * National Pokédex, and Pokémon from other games are traded or transferred in. What is left is event-only when it is
 * found only through events in the game, or is a mythical Pokémon the game has no encounter for.
 */
private fun Dex.computeCatchPlan(game: Game, national: Boolean): Map<Int, Obtain> {
    if (game.isNational) return emptyMap()
    val dexIds = pokemonFor(game, national).map { it.id }
    val out = LinkedHashMap<Int, Obtain>()
    val eventPlaces = HashMap<Int, String>()

    for (id in dexIds) {
        val s = speciesOf(id) ?: continue
        val all = encountersOf(s, game)
        val found = all.filterNot { isEventEncounter(it, game) }
        if (found.isEmpty()) {
            all.firstOrNull()?.let { eventPlaces[id] = it.place }
            continue
        }
        val wild = found.filter { it.methodId !in GIFT_METHODS }
        val use = wild.ifEmpty { found }
        val places = use.groupBy { it.place }
            .map { (place, list) -> Triple(place, list.minOf { it.minLevel }, list.maxOf { it.maxLevel }) }
            .sortedBy { it.second }
        val first = places.first()
        val shown = places.take(3).map { it.first }
        out[id] = Obtain(
            kind = if (wild.isNotEmpty()) ObtainKind.WILD else ObtainKind.GIFT,
            place = first.first, level = first.second, minLevel = first.second, maxLevel = first.third,
            method = if (wild.isEmpty()) use.first().method else null,
            places = shown, morePlaces = places.size - shown.size,
        )
    }

    fun evolveAndBreed() {
        var changed = true
        while (changed) {
            changed = false
            for (id in dexIds) {
                if (id in out) continue
                val s = speciesOf(id) ?: continue
                val from = out[s.evolvesFrom]
                if (s.evolvesFrom != 0 && from != null) {
                    out[id] = Obtain(ObtainKind.EVOLVE, null, from.level, from = speciesOf(s.evolvesFrom)!!.name, how = evolutionMethod(s, game))
                    changed = true
                    continue
                }
                // Babies (Pichu, Azurill…) come from breeding their evolved form.
                val parent = if (game.hasBreeding) species.firstOrNull { it.evolvesFrom == id && it.id in out } else null
                if (parent != null) {
                    out[id] = Obtain(ObtainKind.BREED, null, out.getValue(parent.id).level, from = parent.name)
                    changed = true
                }
            }
        }
    }
    evolveAndBreed()

    // Version exclusives: found in the paired version (Blue for Red), so trade for them.
    val siblings = Game.playable.filter { it.versionGroup == game.versionGroup && it != game }
    for (id in dexIds) {
        if (id in out) continue
        val s = speciesOf(id) ?: continue
        val other = siblings.firstOrNull { playableEncounters(s, it).isNotEmpty() } ?: continue
        out[id] = Obtain(ObtainKind.OTHER_VERSION, null, Int.MAX_VALUE, otherGame = other)
    }
    evolveAndBreed()

    // A National Pokédex fills up from other games: same-generation games trade, and earlier games (Generation III
    // on) transfer forward. The newest game that has it is named.
    if (national) {
        val sources = Game.playable
            .filter { it.versionGroup != game.versionGroup && it.dataGen in 3..game.dataGen }
            .sortedWith(compareBy({ it.dataGen != game.dataGen }, { -it.order }))
        for (id in dexIds) {
            if (id in out) continue
            val s = speciesOf(id) ?: continue
            val other = sources.firstOrNull { playableEncounters(s, it).isNotEmpty() } ?: continue
            out[id] = Obtain(ObtainKind.OTHER_GAME, null, Int.MAX_VALUE, otherGame = other)
        }
        evolveAndBreed()
    }

    val hasData = hasEncounterData(game)
    for (id in dexIds) {
        if (id in out) continue
        val event = id in eventPlaces || (hasData && speciesOf(id)?.mythical == true)
        out[id] = if (event) Obtain(ObtainKind.EVENT, eventPlaces[id], Int.MAX_VALUE)
        else Obtain(ObtainKind.UNKNOWN, null, Int.MAX_VALUE, hasData = hasData)
    }
    return out
}
