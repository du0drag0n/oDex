package io.lemoncore.opendatadex.data

import androidx.annotation.StringRes
import io.lemoncore.opendatadex.R
import io.lemoncore.opendatadex.data.user.CatchListEntity
import io.lemoncore.opendatadex.data.user.GameCopyEntity

/** What a game copy is played on. */
enum class GamePlatform(@StringRes val label: Int) {
    CARTRIDGE(R.string.platform_cartridge),
    DIGITAL(R.string.platform_digital),
    EMULATOR(R.string.platform_emulator),
    OTHER(R.string.platform_other);

    companion object {
        fun fromName(name: String?) = entries.firstOrNull { it.name == name } ?: CARTRIDGE
    }
}

/** What a game's Pokédex records catches into: a catch list, a game copy, or (both null) the game's own checklist. */
data class ActiveChecklist(
    val list: CatchListEntity? = null,
    val copy: GameCopyEntity? = null,
    /** The Pokédex shows the game's National Pokédex. */
    val nationalDex: Boolean = false,
)
