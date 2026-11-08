package io.lemoncore.opendatadex.data.save

import io.lemoncore.opendatadex.data.Game
import io.lemoncore.opendatadex.data.user.GameCopyEntity
import java.time.LocalDate

/** A save file from a main-series game, read but never changed: the trainer, their Pokédex and every Pokémon they keep. */
data class GameSave(
    val game: Game,
    /** The games this could be when the file doesn't say (Red or Blue, say), [game] first; empty when it's known. */
    val gameOptions: List<Game> = emptyList(),
    val trainerName: String,
    /** The ID on the trainer card. */
    val trainerId: Int,
    /** The hidden half of the trainer's ID, which decides shininess; 0 before Generation III. */
    val secretId: Int,
    val playMinutes: Int,
    /** National Pokédex numbers the game has registered as caught. */
    val owned: Set<Int>,
    val seen: Set<Int>,
    val nationalDex: Boolean,
    val party: List<SavedPokemon>,
    /** Everything in the PC, box by box ([SavedPokemon.box] from 1). */
    val boxes: List<SavedPokemon>,
    /** One name per box; blank where the game doesn't name its boxes. */
    val boxNames: List<String>,
    /** The badges earned, one bit each in the game's own order; games with Kanto's badges as well keep them from bit 8. [UNKNOWN_BADGES] when the save doesn't say. */
    val badges: Int,
    /** When the adventure began, in [consoleDate]'s seconds; recorded from Generation IV onwards. */
    val startedAt: Long? = null,
) {
    val all: List<SavedPokemon> get() = party + boxes

    /** This save as [target]'s game when it's the same game and trainer, so that it can update the copy by itself; else null. */
    fun forCopy(target: GameCopyEntity): GameSave? {
        val targetGame = Game.fromKey(target.game)
        if (target.trainerId.toIntOrNull() != trainerId || (targetGame != game && targetGame !in gameOptions)) return null
        return copy(game = targetGame)
    }

    companion object {
        const val UNKNOWN_BADGES = -1

        /** Reads [bytes] as a save from any generation oDex knows; null when it isn't one. [fileName] helps tell paired games apart. */
        fun read(bytes: ByteArray, fileName: String? = null): GameSave? =
            Gen12Save.read(bytes, fileName) ?: Gen3Save.read(bytes, fileName) ?: Gen4Save.read(bytes, fileName)
                ?: Gen5Save.read(bytes, fileName) ?: Gen67Save.read(bytes, fileName) ?: LetsGoSave.read(bytes, fileName) ?: BdspSave.read(bytes, fileName)
    }
}

/**
 * A Pokémon read from a save. [box] is 0 for the party and from 1 for the PC boxes. What a save doesn't record is 0 or
 * empty: Generation I has no held items, and only Crystal among the first two generations notes where a Pokémon was met.
 */
data class SavedPokemon(
    val box: Int,
    val slot: Int,
    /** National Pokédex number. */
    val speciesId: Int,
    val nickname: String,
    val otName: String,
    /** The original trainer's full ID: the visible ID in the low 16 bits, the secret ID above. */
    val otId: Long,
    /** Caught by this save's trainer rather than traded in. */
    val ownCatch: Boolean,
    /** The personality value. Generation II has none, so its low byte holds the Attack DV × 16, which sets gender the same way. */
    val pid: Long,
    val exp: Int,
    /** The save's own item index (not a PokéAPI id); see [io.lemoncore.opendatadex.data.Dex.saveItemKey]. */
    val heldItem: Int,
    val friendship: Int,
    val moves: List<Int>,
    /** HP, Attack, Defense, Sp. Atk, Sp. Def, Speed. Generations I and II hold DVs here, with Special in both Sp. columns. */
    val ivs: List<Int>,
    /** As [ivs]; Generations I and II hold stat experience. */
    val evs: List<Int>,
    val metLevel: Int,
    /** The save's own place index; see [io.lemoncore.opendatadex.data.Dex.savePlaceName]. */
    val metLocation: Int,
    /** Game of origin; see [saveOrigin]. */
    val originGame: Int,
    /** Ball index; see [SAVE_BALLS]. */
    val ball: Int,
    val egg: Boolean,
    /** Which of the species' two abilities (0 or 1), or [HIDDEN_ABILITY_SLOT]. */
    val abilitySlot: Int,
    val shiny: Boolean,
    /** 1 Japanese, 2 English, 3 French, 4 Italian, 5 German, 7 Spanish, 8 Korean; 0 before Generation III, which doesn't say. */
    val language: Int,
    /** The day it was met as yyyymmdd, by the console's clock; 0 before Generation IV, which doesn't record it. */
    val metDate: Int = 0,
    /** The time of day it was caught in Crystal: 1 morning, 2 day, 3 night; 0 in every other game. */
    val metTime: Int = 0,
    /** The games' form number for its species; before Generation IV only Unown has one, from [gen2UnownForm] or [gen3UnownForm]. */
    val form: Int = 0,
    /** 0 male, 1 female, 2 genderless, as the save says from Generation IV; -1 before that, when the PID decides. */
    val gender: Int = -1,
    /** The nature's index, which the save holds from Generation V; -1 before that, when the PID decides. */
    val nature: Int = -1,
)

/** The [SavedPokemon.abilitySlot] of a hidden ability. */
const val HIDDEN_ABILITY_SLOT = 2

internal const val UNOWN = 201

/** Unown's letter in Generation II, 0 for A to 25 for Z: the middle two bits of each DV, read as one number, over 10. */
internal fun gen2UnownForm(attack: Int, defense: Int, speed: Int, special: Int): Int =
    (((attack shr 1 and 3) shl 6) or ((defense shr 1 and 3) shl 4) or ((speed shr 1 and 3) shl 2) or (special shr 1 and 3)) / 10

/** Unown's letter in Generation III, 0 for A to 27 for ?: the low two bits of each of the PID's bytes, read as one number. */
internal fun gen3UnownForm(pid: Long): Int =
    (((pid shr 24 and 3) shl 6) or ((pid shr 16 and 3) shl 4) or ((pid shr 8 and 3) shl 2) or (pid and 3)).toInt() % 28

/** The date for [seconds] from the start of 2000 by the console's clock, which is how Generation IV counts time. */
fun consoleDate(seconds: Long): LocalDate = LocalDate.of(2000, 1, 1).plusDays(seconds / 86_400)

/** The date for a [SavedPokemon.metDate], or null for none. */
fun metLocalDate(yyyymmdd: Int): LocalDate? =
    if (yyyymmdd == 0) null else runCatching { LocalDate.of(yyyymmdd / 10_000, yyyymmdd / 100 % 100, yyyymmdd % 100) }.getOrNull()

/** Which game of a pair this is: the one the trainer caught the most in, then the file's name; null when neither says. */
internal fun pairedGame(games: List<Game>, all: List<SavedPokemon>, fileName: String?, marks: Map<Game, List<String>>): Game? {
    val own = all.filter { it.ownCatch && !it.egg }
    val counts = games.map { game -> own.count { saveOrigin(it.originGame) == game } }
    if (counts[0] != counts[1]) return games[if (counts[0] > counts[1]) 0 else 1]
    return gameInFileName(fileName, games.map { it to marks.getValue(it) })
}

/** Which game of a pair, from a file name such as POKEMON_FIRE_BPRE-0.sav; null when the name doesn't say. */
internal fun gameInFileName(fileName: String?, marks: List<Pair<Game, List<String>>>): Game? {
    val name = fileName?.uppercase() ?: return null
    return marks.firstOrNull { (_, words) -> words.any { it in name } }?.first
}

internal fun ByteArray.u8(at: Int) = this[at].toInt() and 0xFF

internal fun ByteArray.u16(at: Int) = u8(at) or (u8(at + 1) shl 8)

internal fun ByteArray.u32(at: Int): Long = u16(at).toLong() or (u16(at + 2).toLong() shl 16)

/** The Pokédex's caught or seen bits for [count] species: bit n is National Pokédex number n + 1. */
internal fun dexFlags(b: ByteArray, at: Int, count: Int): Set<Int> =
    (0 until count).filter { n -> (b[at + n / 8].toInt() shr (n % 8)) and 1 == 1 }.mapTo(LinkedHashSet()) { it + 1 }

/**
 * The level a Pokémon with [exp] experience has reached on the growth curve that ends at [exp100] experience
 * (each curve has its own level-100 total). Saves store only experience for boxed Pokémon.
 */
fun levelAt(exp: Int, exp100: Int): Int {
    fun need(n: Int): Long {
        val cube = n.toLong() * n * n
        return when (exp100) {
            800_000 -> cube * 4 / 5
            1_059_860 -> cube * 6 / 5 - 15L * n * n + 100L * n - 140
            1_250_000 -> cube * 5 / 4
            600_000 -> when {
                n <= 50 -> cube * (100 - n) / 50
                n <= 68 -> cube * (150 - n) / 100
                n <= 98 -> cube * ((1911 - 10 * n) / 3) / 500
                else -> cube * (160 - n) / 100
            }
            1_640_000 -> when {
                n <= 15 -> cube * ((n + 1) / 3 + 24) / 50
                n <= 36 -> cube * (n + 14) / 50
                else -> cube * (n / 2 + 32) / 50
            }
            else -> cube
        }
    }
    return (2..100).lastOrNull { need(it) <= exp } ?: 1
}

/**
 * A Pokémon's gender from its PID: true female, false male, null genderless. [femaleEighths] is the species' share
 * of females in eighths (PokéAPI's gender rate), -1 for genderless species.
 */
fun genderOf(pid: Long, femaleEighths: Int): Boolean? = when (femaleEighths) {
    -1 -> null
    0 -> false
    8 -> true
    else -> (pid and 0xFF) < (femaleEighths * 32 - 1).coerceAtMost(254)
}

/** Ball indexes (1 Master Ball onwards; Generation III stops at Premier Ball) as PokéAPI item keys. */
val SAVE_BALLS = listOf(
    "master-ball", "ultra-ball", "great-ball", "poke-ball", "safari-ball", "net-ball", "dive-ball", "nest-ball", "repeat-ball",
    "timer-ball", "luxury-ball", "premier-ball", "dusk-ball", "heal-ball", "quick-ball", "cherish-ball", "fast-ball", "level-ball",
    "lure-ball", "heavy-ball", "love-ball", "friend-ball", "moon-ball", "sport-ball", "dream-ball", "beast-ball",
)

/** The game of origin code for Colosseum and XD, which [saveOrigin] has no game for. */
const val ORIGIN_COLOSSEUM = 15

/** The game of origin code for Pokémon GO, which [saveOrigin] has no game for. */
const val ORIGIN_GO = 34

/** Game of origin codes as games; null for Colosseum and XD, Pokémon GO or an unknown code. */
fun saveOrigin(code: Int): Game? = when (code) {
    1 -> Game.SAPPHIRE
    2 -> Game.RUBY
    3 -> Game.EMERALD
    4 -> Game.FIRERED
    5 -> Game.LEAFGREEN
    7 -> Game.HEARTGOLD
    8 -> Game.SOULSILVER
    10 -> Game.DIAMOND
    11 -> Game.PEARL
    12 -> Game.PLATINUM
    20 -> Game.WHITE
    21 -> Game.BLACK
    22 -> Game.WHITE_2
    23 -> Game.BLACK_2
    24 -> Game.X
    25 -> Game.Y
    26 -> Game.ALPHA_SAPPHIRE
    27 -> Game.OMEGA_RUBY
    30 -> Game.SUN
    31 -> Game.MOON
    32 -> Game.ULTRA_SUN
    33 -> Game.ULTRA_MOON
    // Virtual Console games sent up through Pokémon Bank. 36 is Japan's Green, which oDex lists with Blue.
    35 -> Game.RED
    36, 37 -> Game.BLUE
    38 -> Game.YELLOW
    39 -> Game.GOLD
    40 -> Game.SILVER
    41 -> Game.CRYSTAL
    42 -> Game.LETS_GO_PIKACHU
    43 -> Game.LETS_GO_EEVEE
    44 -> Game.SWORD
    45 -> Game.SHIELD
    47 -> Game.LEGENDS_ARCEUS
    48 -> Game.BRILLIANT_DIAMOND
    49 -> Game.SHINING_PEARL
    50 -> Game.SCARLET
    51 -> Game.VIOLET
    52 -> Game.LEGENDS_ZA
    else -> null
}
