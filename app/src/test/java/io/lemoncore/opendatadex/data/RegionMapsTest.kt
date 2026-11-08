package io.lemoncore.opendatadex.data

import io.lemoncore.opendatadex.data.seed.Seed
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class RegionMapsTest {
    private val json = Json { ignoreUnknownKeys = true }
    private val maps = RegionMaps.regions.associateWith { json.decodeFromString(RegionMap.serializer(), File("src/main/assets/maps/$it.json").readText()) }

    @Test fun `every edge joins two nodes and ids are unique`() {
        maps.forEach { (region, map) ->
            val ids = map.nodes.map { it.id }
            assertEquals("$region has duplicate ids", ids.size, ids.toSet().size)
            map.edges.forEach {
                assertTrue("$region edge ${it.from}-${it.to}", it.from in ids && it.to in ids)
                assertTrue("$region edge ${it.from}-${it.to} elbow ${it.elbow}", it.elbow in setOf(null, "h", "v"))
            }
            map.nodes.forEach { assertTrue("$region ${it.id} kind ${it.kind}", it.kind in setOf(MapNode.ROUTE, MapNode.TOWN, MapNode.CITY, MapNode.LANDMARK)) }
        }
    }

    @Test fun `per-game spots and names name games that use the map`() {
        maps.forEach { (region, map) ->
            val users = Game.playable.filter { region in RegionMaps.pagesOf(it) }.map { it.key }.toSet()
            map.nodes.forEach { n ->
                (n.games.orEmpty() + n.names.keys).forEach { assertTrue("$region ${n.id} names game $it, which doesn't use this map", it in users) }
            }
        }
    }

    private val dex by lazy { Dex(json.decodeFromString(Seed.serializer(), File("src/main/assets/seed/dex.json").readText())) }

    @Test fun `every place with encounters is on one of the game's maps`() {
        // Not places: roaming Pokémon, trades in "random caves" on the Isle of Armor, event gifts at Poké Centers and Marts, berry trees, Poké Pelago, Ultra Space, flying
        // with Mega Latios/Latias and PokéAPI's "unknown area" rows. Left off until it's clear where they go: Kanto's
        // Underground Path (a trade), and Ultra Sun and Moon's Dividing Peak Tunnel and Sandy Cave.
        val offMap = setOf(799, 800, 801, 802, 1056, 1117, 1118, 1119, 1116, 1089, 1091, 1092, 1093, 728, 1049, 1121, 1052, 797, 1050, 1051, 707,
            148, 149, 150, 1113, 1046, 1048)
        Game.playable.filter { dex.hasEncounterData(it) }.forEach { g ->
            val onMap = RegionMaps.pagesOf(g).flatMap { p -> maps.getValue(p).forGame(g).nodes.flatMap { it.locations } }.toSet()
            val missing = dex.encountersByLocation(g).keys - onMap - offMap
            assertTrue("$g has encounters at locations $missing that no map shows", missing.isEmpty())
        }
    }

    @Test fun `routes and landmarks in games with encounter data have Pokemon`() {
        // Mt. Chimney's summit has no wild Pokémon (Jagged Pass and the crater are separate places), Route 2's lakeside
        // isn't its own PokéAPI location, and Trainer Tower has no wild Pokémon.
        val none = setOf("hoenn:chimney", "galar:lakeside", "sevii:tower")
        maps.forEach { (region, map) ->
            val games = Game.playable.filter { region in RegionMaps.pagesOf(it) && dex.hasEncounterData(it) }
            if (games.isEmpty()) return@forEach
            val found = games.map { dex.encountersOn(map, it) }
            map.nodes.filter { it.ringed && "$region:${it.id}" !in none }.forEach { n ->
                assertTrue("$region ${n.id} (${n.locations}) has no encounters in any of its games", found.any { it[n.id].orEmpty().isNotEmpty() })
            }
        }
    }
}
