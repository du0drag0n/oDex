package io.lemoncore.opendatadex.data

import androidx.annotation.StringRes
import io.lemoncore.opendatadex.R

/**
 * Game versions the app can show data for. NATIONAL is "all game versions": modern data and the national dex.
 *
 * [dataGen] is the generation whose rules apply (types, stats, type chart); NATIONAL uses today's data.
 * [order] is the release order of the game's version group (PokéAPI's ids are not chronological).
 * [pokedexIds] are the regional dexes the game tracks, in order (Kalos, Galar and Paldea have more than one).
 * [extraVersions] are DLC versions whose encounters belong to this game.
 */
enum class Game(
    val key: String,
    @StringRes val label: Int,
    val dataGen: Int,
    val versionId: Int,
    val versionGroup: Int,
    val order: Int,
    val pokedexIds: List<Int>,
    val region: Int,
    val color: Long,
    val extraVersions: List<Int> = emptyList(),
    /** Versions whose Pokédex text this game reuses when it has none of its own. */
    val flavorFallback: List<Int> = emptyList(),
    /** Version whose wild encounters this game reuses while the source data has none of its own (a faithful remake). */
    val encounterFallback: Int? = null,
) {
    NATIONAL("national", R.string.game_national, LATEST_GEN, 0, 0, Int.MAX_VALUE, listOf(1), 0, 0xFFCBD3DD),
    RED("red", R.string.game_red, 1, 1, 1, 3, listOf(2), 1, 0xFFF44336),
    BLUE("blue", R.string.game_blue, 1, 2, 1, 3, listOf(2), 1, 0xFF1E88E5),
    YELLOW("yellow", R.string.game_yellow, 1, 3, 2, 4, listOf(2), 1, 0xFFFFC107),
    GOLD("gold", R.string.game_gold, 2, 4, 3, 5, listOf(3), 2, 0xFFD4AF37),
    SILVER("silver", R.string.game_silver, 2, 5, 3, 5, listOf(3), 2, 0xFFAEBCC4),
    CRYSTAL("crystal", R.string.game_crystal, 2, 6, 4, 6, listOf(3), 2, 0xFF4DD0E1),
    RUBY("ruby", R.string.game_ruby, 3, 7, 5, 7, listOf(4), 3, 0xFFE53935),
    SAPPHIRE("sapphire", R.string.game_sapphire, 3, 8, 5, 7, listOf(4), 3, 0xFF1E5BC6),
    EMERALD("emerald", R.string.game_emerald, 3, 9, 6, 8, listOf(4), 3, 0xFF2E9E5B),
    FIRERED("firered", R.string.game_firered, 3, 10, 7, 11, listOf(2), 1, 0xFFF4511E),
    LEAFGREEN("leafgreen", R.string.game_leafgreen, 3, 11, 7, 11, listOf(2), 1, 0xFF7CB342),
    DIAMOND("diamond", R.string.game_diamond, 4, 12, 8, 12, listOf(5), 4, 0xFF5C84C4),
    PEARL("pearl", R.string.game_pearl, 4, 13, 8, 12, listOf(5), 4, 0xFFCF7FA6),
    PLATINUM("platinum", R.string.game_platinum, 4, 14, 9, 13, listOf(6), 4, 0xFF7E8B98),
    HEARTGOLD("heartgold", R.string.game_heartgold, 4, 15, 10, 14, listOf(7), 2, 0xFFC9A227),
    SOULSILVER("soulsilver", R.string.game_soulsilver, 4, 16, 10, 14, listOf(7), 2, 0xFF8FA0B0),
    BLACK("black", R.string.game_black, 5, 17, 11, 15, listOf(8), 5, 0xFF424242),
    WHITE("white", R.string.game_white, 5, 18, 11, 15, listOf(8), 5, 0xFF90A4AE),
    BLACK_2("black-2", R.string.game_black_2, 5, 21, 14, 16, listOf(9), 5, 0xFF263238),
    WHITE_2("white-2", R.string.game_white_2, 5, 22, 14, 16, listOf(9), 5, 0xFF78909C),
    X("x", R.string.game_x, 6, 23, 15, 17, listOf(12, 13, 14), 6, 0xFF1565C0),
    Y("y", R.string.game_y, 6, 24, 15, 17, listOf(12, 13, 14), 6, 0xFFC62828),
    OMEGA_RUBY("omega-ruby", R.string.game_omega_ruby, 6, 25, 16, 18, listOf(15), 3, 0xFFB71C1C),
    ALPHA_SAPPHIRE("alpha-sapphire", R.string.game_alpha_sapphire, 6, 26, 16, 18, listOf(15), 3, 0xFF0D47A1),
    SUN("sun", R.string.game_sun, 7, 27, 17, 19, listOf(16), 7, 0xFFEF8F12),
    MOON("moon", R.string.game_moon, 7, 28, 17, 19, listOf(16), 7, 0xFF6A4FB3),
    ULTRA_SUN("ultra-sun", R.string.game_ultra_sun, 7, 29, 18, 20, listOf(21), 7, 0xFFE0662A),
    ULTRA_MOON("ultra-moon", R.string.game_ultra_moon, 7, 30, 18, 20, listOf(21), 7, 0xFF4F3AA6),
    LETS_GO_PIKACHU("lets-go-pikachu", R.string.game_lets_go_pikachu, 7, 31, 19, 21, listOf(26), 1, 0xFFE0B000),
    LETS_GO_EEVEE("lets-go-eevee", R.string.game_lets_go_eevee, 7, 32, 19, 21, listOf(26), 1, 0xFFB07A44),
    SWORD("sword", R.string.game_sword, 8, 33, 20, 22, listOf(27, 28, 29), 8, 0xFF2C8FD0, extraVersions = listOf(35, 36)),
    SHIELD("shield", R.string.game_shield, 8, 34, 20, 22, listOf(27, 28, 29), 8, 0xFFC2185B, extraVersions = listOf(50, 51)),
    BRILLIANT_DIAMOND("brilliant-diamond", R.string.game_brilliant_diamond, 8, 37, 23, 25, listOf(5), 4, 0xFF3F86D6, flavorFallback = listOf(12), encounterFallback = 12),
    SHINING_PEARL("shining-pearl", R.string.game_shining_pearl, 8, 38, 23, 25, listOf(5), 4, 0xFFD06C9C, flavorFallback = listOf(13), encounterFallback = 13),
    LEGENDS_ARCEUS("legends-arceus", R.string.game_legends_arceus, 8, 39, 24, 26, listOf(30), 9, 0xFF5C8A72),
    SCARLET("scarlet", R.string.game_scarlet, 9, 40, 25, 27, listOf(31, 32, 33), 10, 0xFFD84315, extraVersions = listOf(42, 43)),
    VIOLET("violet", R.string.game_violet, 9, 41, 25, 27, listOf(31, 32, 33), 10, 0xFF7B1FA2, extraVersions = listOf(52, 53)),
    LEGENDS_ZA("legends-za", R.string.game_legends_za, 9, 47, 30, 30, listOf(34, 35), 6, 0xFF2F8F64, extraVersions = listOf(48));

    val isNational get() = this == NATIONAL

    /** Highest generation of species that appear in this game. */
    val speciesGen get() = if (isNational) MAX_SUPPORTED_GEN else dataGen

    val versionIds get() = listOf(versionId) + extraVersions

    /** Mega Evolution exists in Generation VI–VII games (Let's Go included) and Legends: Z-A. */
    val hasMegas get() = isNational || dataGen in 6..7 || this == LEGENDS_ZA

    val hasGigantamax get() = isNational || versionGroup == 20

    /** Let's Go and Legends: Arceus left out abilities. */
    val hasAbilities get() = dataGen >= 3 && versionGroup != 19 && versionGroup != 24

    /** Let's Go and Legends: Arceus have no Day Care or eggs. */
    val hasBreeding get() = dataGen >= 2 && versionGroup != 19 && versionGroup != 24

    /**
     * Size of the National Pokédex the game upgrades to after its regional dex (Ruby to Omega Ruby, and Brilliant
     * Diamond/Shining Pearl), or 0 when it has none. Red to Crystal already list every Pokémon of their time.
     */
    val nationalDexSize get() = when {
        versionGroup == 23 -> 493
        dataGen == 3 -> 386
        dataGen == 4 -> 493
        dataGen == 5 -> 649
        dataGen == 6 -> 721
        else -> 0
    }

    val hasNationalDex get() = nationalDexSize > 0

    /** How many badges the game has: Johto's games add Kanto's eight, and Alola's island challenge has none. */
    val badgeTotal get() = when (this) {
        GOLD, SILVER, CRYSTAL, HEARTGOLD, SOULSILVER -> 16
        SUN, MOON, ULTRA_SUN, ULTRA_MOON -> 0
        else -> 8
    }

    /** OKLCH hue (0–360) the app's accent takes while this game is selected; the National Pokédex keeps red. */
    val hue: Float get() = when (this) {
        NATIONAL, RED, FIRERED, RUBY, OMEGA_RUBY, SCARLET -> 25f
        BLUE, SAPPHIRE -> 255f
        ALPHA_SAPPHIRE -> 250f
        EMERALD -> 155f
        YELLOW, LETS_GO_PIKACHU -> 95f
        LETS_GO_EEVEE -> 60f
        GOLD, HEARTGOLD -> 85f
        SILVER, SOULSILVER, SWORD -> 230f
        CRYSTAL, WHITE, WHITE_2 -> 200f
        LEAFGREEN -> 140f
        DIAMOND, BRILLIANT_DIAMOND, X -> 240f
        PEARL, SHINING_PEARL -> 340f
        PLATINUM -> 280f
        BLACK, BLACK_2 -> 270f
        Y -> 20f
        SUN, ULTRA_SUN -> 55f
        MOON, ULTRA_MOON -> 285f
        SHIELD -> 10f
        VIOLET -> 305f
        LEGENDS_ARCEUS -> 70f
        LEGENDS_ZA -> 150f
    }

    companion object {
        fun fromKey(key: String?) = entries.firstOrNull { it.key == key } ?: NATIONAL
        val playable get() = entries.filter { !it.isNational }
        fun byGeneration() = playable.groupBy { it.dataGen }
        /** The game that owns encounter or text rows for [versionId] (DLC versions map to their base game). */
        fun ofVersion(versionId: Int) = playable.firstOrNull { versionId in it.versionIds }
    }
}

/** Marker generation for "today's data" in the national view. */
const val LATEST_GEN = 99

/** The app ships data for every generation through IX. */
const val MAX_SUPPORTED_GEN = 9

fun romanGen(gen: Int) = when (gen) {
    1 -> "I"; 2 -> "II"; 3 -> "III"; 4 -> "IV"; 5 -> "V"; 6 -> "VI"; 7 -> "VII"; 8 -> "VIII"; 9 -> "IX"; else -> gen.toString()
}
