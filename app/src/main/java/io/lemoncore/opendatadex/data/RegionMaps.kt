package io.lemoncore.opendatadex.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.abs

/**
 * A region drawn in oDex's own simple style (assets/maps/<region>.json): a land shape, and dots for routes, squares for
 * towns and diamonds for caves, forests and mountains, joined by lines. Coordinates are in a 100 × 82 space.
 */
@Serializable
data class RegionMap(
    val region: String,
    val name: String,
    val viewBox: List<Float> = listOf(100f, 82f),
    /** SVG path data (M, L, a and Z only). */
    val land: String,
    val nodes: List<MapNode>,
    val edges: List<MapEdge>,
) {
    val width get() = viewBox[0]
    val height get() = viewBox[1]

    /**
     * The map as [game] has it: spots the game doesn't have are left out with their lines, and spots take the game's
     * name for them (Tin Tower before HeartGold and SoulSilver). The national view keeps every spot.
     */
    fun forGame(game: Game): RegionMap {
        if (game.isNational) return this
        val kept = nodes.filter { it.games == null || game.key in it.games }.map { n -> n.names[game.key]?.let { n.copy(name = it) } ?: n }
        val ids = kept.mapTo(HashSet()) { it.id }
        return copy(nodes = kept, edges = edges.filter { it.from in ids && it.to in ids })
    }
}

@Serializable
data class MapNode(
    val id: String,
    val name: String,
    /** route, town, city or landmark. */
    val kind: String,
    val x: Float,
    val y: Float,
    /** PokéAPI location ids whose encounter areas belong to this spot. */
    val locations: List<Int> = emptyList(),
    val sea: Boolean = false,
    /** "right" puts the label beside the node rather than under it. */
    val labelSide: String? = null,
    /** Keys of the games this spot is in (HeartGold and SoulSilver's Route 47); null when every game using the map has it. */
    val games: List<String>? = null,
    /** The spot's name in particular games, by game key. */
    val names: Map<String, String> = emptyMap(),
) {
    /** Routes and landmarks get a completion ring; towns and cities are plain squares. */
    val ringed get() = kind == ROUTE || kind == LANDMARK

    companion object {
        const val ROUTE = "route"
        const val TOWN = "town"
        const val CITY = "city"
        const val LANDMARK = "landmark"
    }
}

/**
 * A line between two spots: straight when they're nearly in line, otherwise one right-angle elbow. The elbow goes along
 * the longer side first unless [elbow] says otherwise ("h" for across first, "v" for up or down first).
 */
@Serializable
data class MapEdge(val from: String, val to: String, val style: String = "land", val elbow: String? = null) {
    val isSea get() = style != "land"

    /** Whether the line goes across first (true) or up or down first (false), for spots [dx] and [dy] apart. */
    fun acrossFirst(dx: Float, dy: Float) = when (elbow) {
        "h" -> true
        "v" -> false
        else -> abs(dx) >= abs(dy)
    }
}

object RegionMaps {
    /** Map files in the order the regions came out; areas drawn on their own (the Sevii Islands) follow their region. */
    val regions = listOf("kanto", "sevii", "johto", "hoenn", "sinnoh", "battlezone", "unova", "kalos", "alola", "galar", "armor", "tundra", "paldea")

    /** The map for [game]'s region; Legends: Arceus uses Sinnoh's until Hisui has its own. Null for the national view. */
    fun regionOf(game: Game): String? = when (game.region) {
        1 -> "kanto"; 2 -> "johto"; 3 -> "hoenn"; 4, 9 -> "sinnoh"; 5 -> "unova"; 6 -> "kalos"; 7 -> "alola"; 8 -> "galar"; 10 -> "paldea"
        else -> null
    }

    /**
     * Every map [game] uses: FireRed and LeafGreen add the Sevii Islands after Kanto, the Johto games add Kanto, the Sinnoh
     * games the Battle Zone, and Sword and Shield their two DLC islands. Empty for the national view.
     */
    fun pagesOf(game: Game): List<String> = when (game) {
        Game.FIRERED, Game.LEAFGREEN -> listOf("kanto", "sevii")
        Game.GOLD, Game.SILVER, Game.CRYSTAL, Game.HEARTGOLD, Game.SOULSILVER -> listOf("johto", "kanto")
        Game.DIAMOND, Game.PEARL, Game.PLATINUM, Game.BRILLIANT_DIAMOND, Game.SHINING_PEARL -> listOf("sinnoh", "battlezone")
        Game.SWORD, Game.SHIELD -> listOf("galar", "armor", "tundra")
        else -> listOfNotNull(regionOf(game))
    }

    private val json = Json { ignoreUnknownKeys = true }
    private val cache = HashMap<String, RegionMap>()

    fun load(context: Context, region: String): RegionMap = synchronized(cache) {
        cache.getOrPut(region) {
            json.decodeFromString(RegionMap.serializer(), context.assets.open("maps/$region.json").bufferedReader().use { it.readText() })
        }
    }
}

/** The encounters at each spot on [map] in [game] (every game in the national view), keyed by node id. */
fun Dex.encountersOn(map: RegionMap, game: Game): Map<String, List<Encounter>> {
    val byLocation = encountersByLocation(game)
    return map.nodes.associate { n -> n.id to n.locations.flatMap { byLocation[it].orEmpty() } }
}
