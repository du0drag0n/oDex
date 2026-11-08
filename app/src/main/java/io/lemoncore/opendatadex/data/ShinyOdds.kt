package io.lemoncore.opendatadex.data

import androidx.annotation.StringRes
import io.lemoncore.opendatadex.R
import kotlin.math.pow

/** How a shiny hunt counts: wild encounters, soft resets (starters, legendaries) or hatched eggs by the Masuda method. */
enum class HuntMethod(@StringRes val label: Int, @StringRes val unit: Int) {
    ENCOUNTERS(R.string.hunt_encounters, R.string.hunt_unit_encounters),
    SOFT_RESETS(R.string.hunt_soft_resets, R.string.hunt_unit_resets),
    MASUDA(R.string.hunt_masuda, R.string.hunt_unit_eggs);

    companion object {
        fun fromName(name: String?) = entries.firstOrNull { it.name == name } ?: ENCOUNTERS
    }
}

/** Shiny Pokémon exist from Gold/Silver on (Red, Blue and Yellow had none). */
val Game.hasShinies get() = isNational || dataGen >= 2

/** The Masuda method (breeding with a Pokémon from another language's game) boosts eggs from Diamond/Pearl on. */
val Game.hasMasuda get() = isNational || dataGen >= 4

/** The Shiny Charm arrived in Black 2/White 2. */
val Game.hasShinyCharm get() = isNational || order >= Game.BLACK_2.order

/**
 * A game's shiny odds: each encounter makes [rolls] tries at 1 in [outOf]. Until Generation V the base is 1 in 8,192, then
 * 1 in 4,096; the Masuda method adds 4 tries in Generation IV and 5 after, and the Shiny Charm 2 more.
 */
data class ShinyOdds(val rolls: Int, val outOf: Int) {
    /** Chance of a shiny on one encounter. */
    val chance: Double get() = 1 - (1 - 1.0 / outOf).pow(rolls)

    /** The odds as "1 in N", rounded. */
    val oneIn: Int get() = Math.round(1 / chance).toInt()

    /** Chance of having seen at least one shiny after [count] encounters. */
    fun foundBy(count: Int): Double = 1 - (1 - chance).pow(count)
}

fun shinyOdds(game: Game, method: HuntMethod, charm: Boolean): ShinyOdds {
    val modern = game.isNational || game.dataGen >= 6
    var rolls = 1
    if (method == HuntMethod.MASUDA && game.hasMasuda) rolls += if (!game.isNational && game.dataGen == 4) 4 else 5
    if (charm && game.hasShinyCharm) rolls += 2
    return ShinyOdds(rolls, if (modern) 4096 else 8192)
}
