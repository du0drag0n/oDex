package io.lemoncore.opendatadex.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.booleanResource
import io.lemoncore.opendatadex.R
import io.lemoncore.opendatadex.data.Game
import io.lemoncore.opendatadex.data.GamePlatform
import io.lemoncore.opendatadex.data.LearnMethod
import io.lemoncore.opendatadex.data.seed.DataLanguage
import io.lemoncore.opendatadex.data.user.GameCopyEntity
import io.lemoncore.opendatadex.data.LearnWay
import io.lemoncore.opendatadex.data.Obtain
import io.lemoncore.opendatadex.data.ObtainKind
import io.lemoncore.opendatadex.data.romanGen

/** Interface wording shared by several screens, in the app language. */

@Composable
fun gameName(game: Game) = stringResource(game.label)

/** The game's name, or "All games" for the national view (chips and pickers). */
@Composable
fun gameChipLabel(game: Game) = if (game.isNational) stringResource(R.string.all_games) else gameName(game)

/** "Emerald – Japanese cart": the game, then the trainer's own name for the copy (just the game when there is none). */
@Composable
fun copyTitle(copy: GameCopyEntity): String {
    val game = gameName(Game.fromKey(copy.game))
    return if (copy.label.isBlank()) game else stringResource(R.string.copy_title, game, copy.label)
}

/** "OT Jayden · ID 12345 · 日本語 · Emulator": whatever the trainer filled in about a copy. */
@Composable
fun copyDetails(copy: GameCopyEntity): String = listOfNotNull(
    copy.playerName.takeIf { it.isNotBlank() }?.let { stringResource(R.string.copy_ot, it) },
    copy.trainerId.takeIf { it.isNotBlank() }?.let { stringResource(R.string.copy_id, it) },
    copy.language?.let { code -> DataLanguage.entries.firstOrNull { it.code == code }?.label },
    stringResource(GamePlatform.fromName(copy.platform).label),
).joinToString(" · ")

/** "Generation IV", or "第4世代" in languages that number generations with plain digits (R.bool.generation_digits). */
@Composable
fun generationLabel(gen: Int) =
    stringResource(R.string.generation, if (booleanResource(R.bool.generation_digits)) gen.toString() else romanGen(gen))

/** "Lv. 20" or "Lv. 3–5". */
@Composable
fun levelRange(min: Int, max: Int) =
    if (min == max) stringResource(R.string.level, min) else stringResource(R.string.level_range, min, max)

@Composable
fun learnWayText(way: LearnWay) = when (way.method) {
    LearnMethod.LEVEL_UP -> stringResource(R.string.level, way.level)
    LearnMethod.MACHINE -> way.machine ?: stringResource(R.string.method_tm)
    else -> stringResource(way.method.label)
}

/** What a catch plan says about one Pokémon, for a [living] dex or a complete one. */
@Composable
fun obtainText(o: Obtain, game: Game, living: Boolean): String = when (o.kind) {
    ObtainKind.WILD, ObtainKind.GIFT -> {
        val where = o.places.joinToString(", ") + if (o.morePlaces > 0) " " + stringResource(R.string.plan_more_places, o.morePlaces) else ""
        val what = if (o.kind == ObtainKind.WILD) levelRange(o.minLevel, o.maxLevel) else o.method.orEmpty()
        "$what · $where"
    }
    ObtainKind.EVOLVE -> {
        val evolve = stringResource(if (living) R.string.plan_evolve_second else R.string.plan_evolve, o.from.orEmpty())
        if (o.how != null) "$evolve (${o.how})" else evolve
    }
    ObtainKind.BREED -> stringResource(R.string.plan_breed, o.from.orEmpty())
    ObtainKind.OTHER_VERSION -> stringResource(R.string.plan_other_version, gameName(o.otherGame ?: game))
    ObtainKind.OTHER_GAME -> stringResource(R.string.plan_other_game, gameName(o.otherGame ?: game))
    ObtainKind.EVENT -> if (o.place != null) stringResource(R.string.plan_event_at, o.place) else stringResource(R.string.plan_event)
    ObtainKind.UNKNOWN -> stringResource(if (o.hasData) R.string.plan_not_found else R.string.plan_no_data, gameName(game))
}

/** A game's short code for its swatch in pickers and My Progress ("FR", "LGE", "ZA"). */
fun gameCode(game: Game): String = when (game) {
    Game.NATIONAL -> "ALL"; Game.RED -> "R"; Game.BLUE -> "B"; Game.YELLOW -> "YE"; Game.GOLD -> "G"; Game.SILVER -> "S"
    Game.CRYSTAL -> "C"; Game.RUBY -> "RU"; Game.SAPPHIRE -> "SA"; Game.EMERALD -> "E"; Game.FIRERED -> "FR"; Game.LEAFGREEN -> "LG"
    Game.DIAMOND -> "D"; Game.PEARL -> "P"; Game.PLATINUM -> "PT"; Game.HEARTGOLD -> "HG"; Game.SOULSILVER -> "SS"
    Game.BLACK -> "BK"; Game.WHITE -> "W"; Game.BLACK_2 -> "B2"; Game.WHITE_2 -> "W2"; Game.X -> "X"; Game.Y -> "Y"
    Game.OMEGA_RUBY -> "OR"; Game.ALPHA_SAPPHIRE -> "AS"; Game.SUN -> "SU"; Game.MOON -> "MO"; Game.ULTRA_SUN -> "US"
    Game.ULTRA_MOON -> "UM"; Game.LETS_GO_PIKACHU -> "LGP"; Game.LETS_GO_EEVEE -> "LGE"; Game.SWORD -> "SW"; Game.SHIELD -> "SH"
    Game.BRILLIANT_DIAMOND -> "BD"; Game.SHINING_PEARL -> "SP"; Game.LEGENDS_ARCEUS -> "LA"; Game.SCARLET -> "SC"
    Game.VIOLET -> "VI"; Game.LEGENDS_ZA -> "ZA"
}
