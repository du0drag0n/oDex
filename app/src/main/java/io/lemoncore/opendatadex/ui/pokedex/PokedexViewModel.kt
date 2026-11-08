package io.lemoncore.opendatadex.ui.pokedex

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.lemoncore.opendatadex.AppContainer
import io.lemoncore.opendatadex.data.ActiveChecklist
import io.lemoncore.opendatadex.data.Dex
import io.lemoncore.opendatadex.data.eventOnly
import io.lemoncore.opendatadex.data.user.GameCopyEntity
import kotlinx.coroutines.flow.distinctUntilChanged
import io.lemoncore.opendatadex.data.Game
import io.lemoncore.opendatadex.data.PokemonSummary
import io.lemoncore.opendatadex.data.Settings
import io.lemoncore.opendatadex.data.SortOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Which of the filtered Pokémon the list shows. */
enum class ShowFilter { ALL, FAVORITES, CAUGHT, MISSING }

data class ListFilters(
    val gen: Int? = null,
    val type: Int? = null,
    val show: ShowFilter = ShowFilter.ALL,
    val name: String? = null,
    /** dataDex 4's extra filters: an egg group, an ability (in the selected game) and a move it learns there. */
    val eggGroup: Int? = null,
    val ability: Int? = null,
    val move: Int? = null,
) {
    val extraCount get() = listOfNotNull(eggGroup, ability, move).size

    /** Drops what the game can't filter by: generations past its Pokémon, abilities before Ruby/Sapphire, moves in All game versions. */
    fun forGame(game: Game) = copy(
        gen = gen?.takeIf { it <= game.speciesGen },
        ability = ability?.takeIf { game.hasAbilities || game.isNational },
        move = move?.takeIf { !game.isNational },
    )
}

data class PokedexUiState(
    val dex: Dex,
    val settings: Settings,
    val filters: ListFilters,
    val shown: List<PokemonSummary>,
    /** All Pokémon in the selected game's dex, before filters (the checklist denominator). */
    val total: Int,
    val caught: Set<Int>,
    val favorites: Set<Int>,
    /** The catch list or game copy the checklist records into, and whether the National Pokédex is shown. */
    val active: ActiveChecklist = ActiveChecklist(),
    val copies: List<GameCopyEntity> = emptyList(),
    /** Pokémon that only an event gives in the selected game (labelled on their cards). */
    val eventOnly: Set<Int> = emptySet(),
    /** How many Pokémon each [ShowFilter] would show, given the other filters (in [ShowFilter] order). */
    val showCounts: List<Int> = listOf(0, 0, 0, 0),
)

@OptIn(ExperimentalCoroutinesApi::class)
class PokedexViewModel(private val container: AppContainer) : ViewModel() {
    val filters = MutableStateFlow(ListFilters())

    private val settings = container.currentSettings

    private val caught: StateFlow<Set<Int>> = settings.filterNotNull().map { it.game }
        .flatMapLatest { container.caught(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    private val active = settings.filterNotNull().map { it.game }.distinctUntilChanged().flatMapLatest { container.activeChecklist(it) }

    /**
     * The selected game's event-only Pokémon. Working out a game's catch plan takes a few seconds the first time, so the
     * list shows straight away and the Event tags follow.
     */
    private val eventOnly = combine(container.dex.filterNotNull(), settings.filterNotNull().map { it.game }.distinctUntilChanged(), active) { d, g, a ->
        Triple(d, g, a.nationalDex)
    }.distinctUntilChanged().flatMapLatest { (d, g, national) ->
        flow {
            emit(emptySet())
            if (!g.isNational) emit(d.eventOnly(g, national))
        }
    }.flowOn(Dispatchers.Default)

    private val trainer = combine(container.favorites, active, container.gameCopies, eventOnly) { favs, a, copies, events ->
        Trainer(favs, a, copies, events)
    }

    private data class Trainer(val favorites: Set<Int>, val active: ActiveChecklist, val copies: List<GameCopyEntity>, val events: Set<Int>)

    val state: StateFlow<PokedexUiState?> = combine(
        container.dex.filterNotNull(), settings.filterNotNull(), filters, caught, trainer,
    ) { dex, s, f, caughtIds, (favs, act, copies, events) ->
        val all = dex.pokemonFor(s.game, act.nationalDex)
        var list = all.asSequence()
        f.gen?.let { g -> list = list.filter { dex.speciesOf(it.id)?.gen == g } }
        f.type?.let { t -> list = list.filter { t in it.types } }
        f.eggGroup?.let { g -> list = list.filter { dex.inEggGroup(it, g) } }
        f.ability?.let { a -> list = list.filter { dex.hasAbility(it, a, s.game) } }
        f.move?.takeIf { !s.game.isNational }?.let { m ->
            val ls = dex.learnsets(s.game)
            list = list.filter { dex.learnsMove(it, m, ls) }
        }
        f.name?.takeIf { it.isNotBlank() }?.let { q -> list = list.filter { it.name.contains(q.trim(), ignoreCase = true) } }
        val filtered = list.toList()
        val caughtCount = filtered.count { it.id in caughtIds }
        val counts = listOf(filtered.size, filtered.count { it.id in favs }, caughtCount, filtered.size - caughtCount)
        val shown = when (f.show) {
            ShowFilter.ALL -> filtered
            ShowFilter.FAVORITES -> filtered.filter { it.id in favs }
            ShowFilter.CAUGHT -> filtered.filter { it.id in caughtIds }
            ShowFilter.MISSING -> filtered.filter { it.id !in caughtIds }
        }
        val sorted = sort(dex, s.game, shown, s.sort, s.sortDescending)
        PokedexUiState(dex, s, f, sorted, all.size, caughtIds, favs, act, copies, events, counts)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Caught and total for each of the trainer's game copies (the game picker shows their completion). */
    val copyProgress: StateFlow<Map<Long, Pair<Int, Int>>> = combine(container.dex.filterNotNull(), container.allCaught, container.gameCopies) { dex, caught, copies ->
        val byKey = caught.groupBy({ it.game }, { it.speciesId })
        copies.associate { copy ->
            val ids = dex.pokemonFor(Game.fromKey(copy.game), copy.nationalDex).mapTo(HashSet()) { it.id }
            copy.id to (byKey[copy.key].orEmpty().count { it in ids } to ids.size)
        }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    init {
        // "Hide caught Pokémon" now means starting on Missing.
        viewModelScope.launch { if (settings.filterNotNull().first().hideCaught) filters.update { it.copy(show = ShowFilter.MISSING) } }
        // Keep the browse order in step with what is on screen, for next/previous on the Pokémon screen.
        viewModelScope.launch { state.collect { st -> if (st != null) container.browseOrder.value = st.shown.map { it.id } } }
    }

    private fun sort(dex: Dex, game: Game, list: List<PokemonSummary>, order: SortOrder, descending: Boolean): List<PokemonSummary> {
        val stats = list.associate { p -> p.formId to (dex.pokemon(p.formId)?.let { dex.baseStats(it, game) } ?: emptyList()) }
        fun stat(p: PokemonSummary, statId: Int) = stats[p.formId]?.firstOrNull { it.statId == statId }?.base ?: 0
        val sorted = when (order) {
            SortOrder.NUMBER -> list.sortedWith(compareBy({ it.dexIndex }, { it.dexNumber }))
            SortOrder.NAME -> list.sortedBy { it.name }
            SortOrder.HP -> list.sortedBy { stat(it, 1) }
            SortOrder.ATTACK -> list.sortedBy { stat(it, 2) }
            SortOrder.DEFENSE -> list.sortedBy { stat(it, 3) }
            SortOrder.SPEED -> list.sortedBy { stat(it, 6) }
            SortOrder.TOTAL -> list.sortedBy { p -> stats[p.formId]?.sumOf { it.base } ?: 0 }
        }
        return if (descending) sorted.reversed() else sorted
    }

    fun setGame(game: Game) {
        container.selectGame(game)
        // Filters the new game can't use (a later generation, abilities before Ruby/Sapphire) make no sense; drop them.
        filters.update { it.forGame(game) }
    }

    fun useCopy(copy: GameCopyEntity) {
        container.useGameCopy(copy)
        filters.update { it.forGame(Game.fromKey(copy.game)) }
    }

    fun setEggGroup(id: Int?) = filters.update { it.copy(eggGroup = id) }
    fun setAbility(id: Int?) = filters.update { it.copy(ability = id) }
    fun setMove(id: Int?) = filters.update { it.copy(move = id) }

    fun setNationalDex(on: Boolean) {
        val game = settings.value?.game ?: return
        container.setNationalDex(game, on)
    }

    fun setGen(gen: Int?) = filters.update { it.copy(gen = gen) }
    fun setType(type: Int?) = filters.update { it.copy(type = type) }
    fun setShow(show: ShowFilter) = filters.update { it.copy(show = show) }
    fun setNameFilter(name: String?) = filters.update { it.copy(name = name) }
    fun setSort(order: SortOrder, descending: Boolean) = viewModelScope.launch { container.settings.setSort(order, descending) }

    /**
     * The Pokémon opened from the list, shown over it (null: none). The list stays composed underneath, so going back
     * is instant instead of rebuilding it.
     */
    val opened = MutableStateFlow<Int?>(null)
    fun openPokemon(id: Int) { opened.value = id }
    fun closePokemon() { opened.value = null }

    /** The species the Pokémon screen was last showing, once (0 when there is none). */
    fun takeLastViewed(): Int = container.lastViewed.value.also { container.lastViewed.value = 0 }

    fun toggleFavorite(id: Int) = container.toggleFavorite(id)
    fun toggleCaught(id: Int) {
        val game = settings.value?.game ?: return
        container.setCaught(game, id, id !in caught.value)
    }

    fun markShownCaught() {
        val st = state.value ?: return
        container.markAllCaught(st.settings.game, st.shown.map { it.id })
    }

    fun clearChecklist() {
        val game = settings.value?.game ?: return
        container.clearCaught(game)
    }
}
