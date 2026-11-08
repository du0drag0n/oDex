package io.lemoncore.opendatadex.data

import android.content.Context
import androidx.annotation.StringRes
import io.lemoncore.opendatadex.R
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.lemoncore.opendatadex.data.seed.DataLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("settings")

enum class ThemeMode(@StringRes val label: Int) { SYSTEM(R.string.theme_system), LIGHT(R.string.theme_light), DARK(R.string.theme_dark) }

enum class SortOrder(@StringRes val label: Int) {
    NUMBER(R.string.sort_number), NAME(R.string.sort_name), HP(R.string.stat_hp), ATTACK(R.string.stat_attack),
    DEFENSE(R.string.stat_defense), SPEED(R.string.stat_speed), TOTAL(R.string.sort_total)
}

/** Where Pokémon pictures come from: official artwork, HOME renders or the games' pixel sprites. */
enum class ImageStyle(@StringRes val label: Int) { ARTWORK(R.string.images_artwork), HOME(R.string.images_home), PIXEL(R.string.images_pixel) }

/** How the Pokédex lays out its Pokémon on a tablet: the phone's rows, in columns, or square cards. */
enum class TabletList(@StringRes val label: Int) { ROWS(R.string.tablet_list_rows), GRID(R.string.tablet_list_grid) }

data class Settings(
    val game: Game = Game.NATIONAL,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val autoPlayCry: Boolean = false,
    val hideCaught: Boolean = false,
    val sort: SortOrder = SortOrder.NUMBER,
    val sortDescending: Boolean = false,
    val seenWelcome: Boolean = false,
    /** Lists show a regional variant in its own region (Alolan Raichu in Sun/Moon). */
    val regionalForms: Boolean = true,
    /** Mega Evolutions and Gigantamax forms are offered as forms. */
    val battleForms: Boolean = true,
    /** Language for buttons, labels and messages; null follows the phone. */
    val appLanguage: DataLanguage? = null,
    /** Language for Pokémon, move, ability, item and place names and in-game text. */
    val dataLanguage: DataLanguage = DataLanguage.ENGLISH,
    /** The document automatic backups are written to, or null when they are off. */
    val autoBackupUri: String? = null,
    /** When the last automatic backup was written (0: never). */
    val lastAutoBackup: Long = 0,
    /** The accent colour follows the selected game. */
    val tintFromGame: Boolean = true,
    val imageStyle: ImageStyle = ImageStyle.ARTWORK,
    /** Swiping a card right toggles caught. */
    val swipeToMark: Boolean = true,
    /** Unmarking a catch offers Undo for a few seconds. */
    val undoUnmark: Boolean = true,
    val haptics: Boolean = true,
    val sounds: Boolean = false,
    /** Tapping the artwork on a Pokémon's page plays its cry. */
    val playCry: Boolean = true,
    /** Milestone cards already shown, as "<checklist key>:<percent>". */
    val milestonesShown: Set<String> = emptySet(),
    /** The side menu's entries in the trainer's order (empty: the default order), by menu key. */
    val menuOrder: List<String> = emptyList(),
    /** Side menu entries the trainer has hidden, by menu key. */
    val menuHidden: Set<String> = emptySet(),
    val tabletList: TabletList = TabletList.ROWS,
) {
    val formOptions get() = FormOptions(regionalForms, battleForms)
}

class SettingsStore(private val context: Context) {
    private object Keys {
        val game = stringPreferencesKey("game")
        val theme = stringPreferencesKey("theme")
        val autoPlayCry = booleanPreferencesKey("auto_play_cry")
        val hideCaught = booleanPreferencesKey("hide_caught")
        val sort = stringPreferencesKey("sort")
        val sortDescending = booleanPreferencesKey("sort_descending")
        val seenWelcome = booleanPreferencesKey("seen_welcome")
        val regionalForms = booleanPreferencesKey("regional_forms")
        val battleForms = booleanPreferencesKey("battle_forms")
        val dataLanguage = stringPreferencesKey("data_language")
        val appLanguage = stringPreferencesKey("app_language")
        val autoBackupUri = stringPreferencesKey("auto_backup_uri")
        val lastAutoBackup = longPreferencesKey("last_auto_backup")
        val tintFromGame = booleanPreferencesKey("tint_from_game")
        val imageStyle = stringPreferencesKey("image_style")
        val swipeToMark = booleanPreferencesKey("swipe_to_mark")
        val undoUnmark = booleanPreferencesKey("undo_unmark")
        val haptics = booleanPreferencesKey("haptics")
        val sounds = booleanPreferencesKey("sounds")
        val playCry = booleanPreferencesKey("play_cry")
        val milestonesShown = stringSetPreferencesKey("milestones_shown")
        val menuOrder = stringPreferencesKey("menu_order")
        val menuHidden = stringSetPreferencesKey("menu_hidden")
        val tabletList = stringPreferencesKey("tablet_list")
    }

    val settings: Flow<Settings> = context.dataStore.data.map { p ->
        Settings(
            game = Game.fromKey(p[Keys.game]),
            theme = p[Keys.theme]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
            autoPlayCry = p[Keys.autoPlayCry] ?: false,
            hideCaught = p[Keys.hideCaught] ?: false,
            sort = p[Keys.sort]?.let { runCatching { SortOrder.valueOf(it) }.getOrNull() } ?: SortOrder.NUMBER,
            sortDescending = p[Keys.sortDescending] ?: false,
            seenWelcome = p[Keys.seenWelcome] ?: false,
            regionalForms = p[Keys.regionalForms] ?: true,
            battleForms = p[Keys.battleForms] ?: true,
            dataLanguage = DataLanguage.fromCode(p[Keys.dataLanguage]),
            appLanguage = p[Keys.appLanguage]?.let { code -> DataLanguage.entries.firstOrNull { it.code == code } },
            autoBackupUri = p[Keys.autoBackupUri],
            lastAutoBackup = p[Keys.lastAutoBackup] ?: 0,
            tintFromGame = p[Keys.tintFromGame] ?: true,
            imageStyle = p[Keys.imageStyle]?.let { runCatching { ImageStyle.valueOf(it) }.getOrNull() } ?: ImageStyle.ARTWORK,
            swipeToMark = p[Keys.swipeToMark] ?: true,
            undoUnmark = p[Keys.undoUnmark] ?: true,
            haptics = p[Keys.haptics] ?: true,
            sounds = p[Keys.sounds] ?: false,
            playCry = p[Keys.playCry] ?: true,
            milestonesShown = p[Keys.milestonesShown] ?: emptySet(),
            menuOrder = p[Keys.menuOrder]?.split(',')?.filter { it.isNotBlank() }.orEmpty(),
            menuHidden = p[Keys.menuHidden] ?: emptySet(),
            tabletList = p[Keys.tabletList]?.let { runCatching { TabletList.valueOf(it) }.getOrNull() } ?: TabletList.ROWS,
        )
    }

    private suspend fun set(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }

    suspend fun setGame(game: Game) = set { it[Keys.game] = game.key }
    suspend fun setTheme(mode: ThemeMode) = set { it[Keys.theme] = mode.name }
    suspend fun setAutoPlayCry(on: Boolean) = set { it[Keys.autoPlayCry] = on }
    suspend fun setHideCaught(on: Boolean) = set { it[Keys.hideCaught] = on }
    suspend fun setSort(order: SortOrder, descending: Boolean) = set {
        it[Keys.sort] = order.name
        it[Keys.sortDescending] = descending
    }
    suspend fun setSeenWelcome() = set { it[Keys.seenWelcome] = true }
    suspend fun setRegionalForms(on: Boolean) = set { it[Keys.regionalForms] = on }
    suspend fun setBattleForms(on: Boolean) = set { it[Keys.battleForms] = on }
    suspend fun setDataLanguage(language: DataLanguage) = set { it[Keys.dataLanguage] = language.code }
    suspend fun setAutoBackupUri(uri: String?) = set { if (uri == null) it.remove(Keys.autoBackupUri) else it[Keys.autoBackupUri] = uri }
    suspend fun setLastAutoBackup(at: Long) = set { it[Keys.lastAutoBackup] = at }
    suspend fun setTintFromGame(on: Boolean) = set { it[Keys.tintFromGame] = on }
    suspend fun setImageStyle(style: ImageStyle) = set { it[Keys.imageStyle] = style.name }
    suspend fun setTabletList(list: TabletList) = set { it[Keys.tabletList] = list.name }
    suspend fun setMenu(order: List<String>, hidden: Set<String>) = set {
        it[Keys.menuOrder] = order.joinToString(",")
        it[Keys.menuHidden] = hidden
    }
    suspend fun resetMenu() = set {
        it.remove(Keys.menuOrder)
        it.remove(Keys.menuHidden)
    }
    suspend fun setSwipeToMark(on: Boolean) = set { it[Keys.swipeToMark] = on }
    suspend fun setUndoUnmark(on: Boolean) = set { it[Keys.undoUnmark] = on }
    suspend fun setHaptics(on: Boolean) = set { it[Keys.haptics] = on }
    suspend fun setSounds(on: Boolean) = set { it[Keys.sounds] = on }
    suspend fun setPlayCry(on: Boolean) = set { it[Keys.playCry] = on }
    suspend fun addMilestoneShown(key: String) = set { it[Keys.milestonesShown] = (it[Keys.milestonesShown] ?: emptySet()) + key }
    suspend fun setAppLanguage(language: DataLanguage?) = set { if (language == null) it.remove(Keys.appLanguage) else it[Keys.appLanguage] = language.code }

    /**
     * Checklist key the Pokédex records catches under for [game]: a catch list ("list:<id>"), a game copy
     * ("copy:<id>"), or null for the game's own.
     */
    fun activeList(game: Game): Flow<String?> = context.dataStore.data.map { it[activeListKey(game)] }

    suspend fun setActiveList(game: Game, key: String?) = set { if (key == null) it.remove(activeListKey(game)) else it[activeListKey(game)] = key }

    private fun activeListKey(game: Game) = stringPreferencesKey("active_list_${game.key}")

    /** The game's own checklist uses its National Pokédex (catch lists and game copies keep their own setting). */
    fun nationalDex(game: Game): Flow<Boolean> = context.dataStore.data.map { it[nationalDexKey(game)] ?: false }

    suspend fun setNationalDex(game: Game, on: Boolean) = set { it[nationalDexKey(game)] = on }

    private fun nationalDexKey(game: Game) = booleanPreferencesKey("national_dex_${game.key}")
}
