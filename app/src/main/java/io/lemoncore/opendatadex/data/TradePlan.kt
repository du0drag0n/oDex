package io.lemoncore.opendatadex.data

import androidx.annotation.StringRes
import io.lemoncore.opendatadex.R
import io.lemoncore.opendatadex.data.user.GameCopyEntity

/** How a Pokémon moves from one game to another. Trades go both ways; the transfer services only go forward. */
enum class LinkKind(@StringRes val label: Int, val twoWay: Boolean) {
    TRADE(R.string.link_trade, true),
    PAL_PARK(R.string.link_pal_park, false),
    POKE_TRANSFER(R.string.link_poke_transfer, false),
    POKE_BANK(R.string.link_poke_bank, false),
    POKEMON_HOME(R.string.link_home, true),
}

private val GEN_1_2 = setOf(1, 2, 3, 4)
private val GEN_3 = setOf(5, 6, 7)
private val GEN_4 = setOf(8, 9, 10)
private val GEN_5 = setOf(11, 14)
private val GEN_6 = setOf(15, 16)
private val GEN_7 = setOf(17, 18)
private const val LETS_GO = 19
/** Sword/Shield, Brilliant Diamond/Shining Pearl, Legends: Arceus, Scarlet/Violet and Legends: Z-A: all on HOME. */
private val SWITCH_HOME = setOf(20, 23, 24, 25, 30)

/**
 * How a Pokémon gets from [from]'s game into [to]'s directly, or null when it can't: games of one era trade over a
 * link, and Pal Park, Poké Transfer, Poké Bank (with Poké Transporter) and HOME carry them forward a generation or
 * more. Red to Crystal can't link Japanese and other-language carts, and an emulator save only links with another.
 */
fun linkBetween(from: GameCopyEntity, to: GameCopyEntity): LinkKind? {
    if (from.id == to.id) return null
    val a = Game.fromKey(from.game).versionGroup
    val b = Game.fromKey(to.game).versionGroup
    val fromEmulator = GamePlatform.fromName(from.platform) == GamePlatform.EMULATOR
    val toEmulator = GamePlatform.fromName(to.platform) == GamePlatform.EMULATOR
    if (fromEmulator != toEmulator) return null
    // The 3DS Virtual Console releases of Red to Crystal send to Sun/Moon through Poké Transporter.
    val virtualConsole = a in GEN_1_2 && GamePlatform.fromName(from.platform) == GamePlatform.DIGITAL
    return when {
        a in GEN_1_2 && b in GEN_1_2 -> {
            val japanese = (from.language == "ja") to (to.language == "ja")
            if (from.language != null && to.language != null && japanese.first != japanese.second) null else LinkKind.TRADE
        }
        virtualConsole && b in GEN_7 -> LinkKind.POKE_BANK
        virtualConsole && b in SWITCH_HOME -> LinkKind.POKEMON_HOME
        a in GEN_3 && b in GEN_3 -> LinkKind.TRADE
        a in GEN_3 && b in GEN_4 -> LinkKind.PAL_PARK
        a in GEN_4 && b in GEN_4 -> LinkKind.TRADE
        a in GEN_4 && b in GEN_5 -> LinkKind.POKE_TRANSFER
        a in GEN_5 && b in GEN_5 -> LinkKind.TRADE
        a in GEN_5 && (b in GEN_6 || b in GEN_7) -> LinkKind.POKE_BANK
        a in GEN_6 && b in GEN_6 -> LinkKind.TRADE
        a in GEN_7 && b in GEN_7 -> LinkKind.TRADE
        a in GEN_6 && b in GEN_7 -> LinkKind.POKE_BANK
        a == LETS_GO && b == LETS_GO -> LinkKind.TRADE
        (a in GEN_5 || a in GEN_6 || a in GEN_7 || a == LETS_GO) && b in SWITCH_HOME -> LinkKind.POKEMON_HOME
        a in SWITCH_HOME && b in SWITCH_HOME -> if (a == b) LinkKind.TRADE else LinkKind.POKEMON_HOME
        else -> null
    }
}

/** Where one missing Pokémon can come from: [copy] has it already ([caught]) or can get it there ([obtain]). */
data class TradeSource(val copy: GameCopyEntity, val link: LinkKind, val caught: Boolean, val obtain: Obtain?)

/** The Pokémon [copy] can provide for the copy being planned, caught ones first. */
data class SourceGroup(val copy: GameCopyEntity, val link: LinkKind, val pokemon: List<Pair<PokemonSummary, TradeSource>>)

/** A missing Pokémon that evolves by trading, from [from]; [partners] are your games that can trade it there and back. */
data class TradeEvolution(val pokemon: PokemonSummary, val from: String, val partners: List<GameCopyEntity>)

data class TradePlan(
    val groups: List<SourceGroup>,
    val tradeEvolutions: List<TradeEvolution>,
    /** Missing Pokémon none of your other games can provide. */
    val unsourced: List<PokemonSummary>,
    val missing: Int,
)

/**
 * What [target] is missing and which of the trainer's other [copies] can provide it: Pokémon a copy has caught
 * (ready to send now), then ones it can catch, evolve or breed itself. Trade evolutions are listed apart, with the
 * games that can trade them there and back. [caught] holds each copy's caught species by its checklist key.
 */
fun Dex.tradePlan(target: GameCopyEntity, copies: List<GameCopyEntity>, caught: Map<String, Set<Int>>): TradePlan {
    val game = Game.fromKey(target.game)
    val mine = caught[target.key].orEmpty()
    val missing = pokemonFor(game, target.nationalDex).filter { it.id !in mine }
    val usable = setOf(ObtainKind.WILD, ObtainKind.GIFT, ObtainKind.EVOLVE, ObtainKind.BREED)

    val groups = copies.mapNotNull { other ->
        val link = linkBetween(other, target) ?: return@mapNotNull null
        val otherGame = Game.fromKey(other.game)
        val theirs = caught[other.key].orEmpty()
        val plan = catchPlan(otherGame, other.nationalDex)
        val found = missing.mapNotNull { p ->
            val obtain = plan[p.id]?.takeIf { it.kind in usable }
            when {
                p.id in theirs -> p to TradeSource(other, link, caught = true, obtain = obtain)
                obtain != null -> p to TradeSource(other, link, caught = false, obtain = obtain)
                else -> null
            }
        }.sortedBy { !it.second.caught }
        if (found.isEmpty()) null else SourceGroup(other, link, found)
    }.sortedWith(compareBy({ !it.link.twoWay }, { -it.pokemon.count { p -> p.second.caught } }, { -it.pokemon.size }))

    val partners = copies.filter { linkBetween(it, target) == LinkKind.TRADE && linkBetween(target, it) == LinkKind.TRADE }
    val tradeEvolutions = missing.mapNotNull { p ->
        val s = speciesOf(p.id) ?: return@mapNotNull null
        if (s.evolvesFrom == 0 || !evolvesByTrade(s, game)) return@mapNotNull null
        TradeEvolution(p, speciesOf(s.evolvesFrom)?.name.orEmpty(), partners)
    }
    val sourced = groups.flatMap { g -> g.pokemon.map { it.first.id } }.toSet() + tradeEvolutions.map { it.pokemon.id }
    return TradePlan(
        groups = groups,
        tradeEvolutions = tradeEvolutions,
        unsourced = missing.filter { it.id !in sourced },
        missing = missing.size,
    )
}

/** PC box size: 20 Pokémon a box in Red to Crystal, 30 from Ruby/Sapphire on. */
val Game.boxSize get() = if (dataGen <= 2) 20 else 30
