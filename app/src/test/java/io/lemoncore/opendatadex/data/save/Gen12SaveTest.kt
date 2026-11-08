package io.lemoncore.opendatadex.data.save

import io.lemoncore.opendatadex.data.Dex
import io.lemoncore.opendatadex.data.Game
import io.lemoncore.opendatadex.data.seed.Seed
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class Gen12SaveTest {
    private val trainer = "RED"
    private val id = 0x1234

    @Test
    fun readsARedOrBlueSave() {
        val b = ByteArray(0x8000)
        text(b, 0x2598, trainer, 11)
        be16(b, 0x2605, id)
        b[0x25A3] = 0b0000_0101 // Bulbasaur and Venusaur
        b[0x2CED] = 12; b[0x2CEF] = 30
        b[0x2602] = 0b0001_1111
        b[0x284C] = 0x81.toByte() // Box 2 in use, boxes set up
        gen1List(b, 0x2F2C, 6, 44, listOf(0x99 to "BULBA"))
        gen1List(b, 0x4000, 20, 33, listOf(0xB0 to "CHAR"))
        // Box 2's stored copy is out of date: the one in main memory counts.
        gen1List(b, 0x4000 + 0x462, 20, 33, listOf(0xB1 to "OLD"))
        gen1List(b, 0x30C0, 20, 33, listOf(0x54 to "SPARKY"))
        gen1Sum(b)

        val save = GameSave.read(b, "POKEMON BLUE_APEE-0.sav")!!
        assertEquals(Game.BLUE, save.game)
        assertTrue(save.gameOptions.isEmpty())
        assertEquals(trainer, save.trainerName)
        assertEquals(id, save.trainerId)
        assertEquals(12 * 60 + 30, save.playMinutes)
        assertEquals(0b0001_1111, save.badges)
        assertEquals(setOf(1, 3), save.owned)
        assertEquals(listOf(1), save.party.map { it.speciesId })
        assertEquals(listOf(4 to 1, 25 to 2), save.boxes.map { it.speciesId to it.box })
        assertEquals("SPARKY", save.boxes[1].nickname)

        val mon = save.party.single()
        assertTrue(mon.ownCatch)
        assertEquals(1000, mon.exp)
        assertEquals(listOf(33, 45), mon.moves)
        // Attack 10, Defense 11, Speed 12, Special 13: the HP DV is the low bits 0, 1, 0, 1.
        assertEquals(listOf(5, 10, 11, 13, 13, 12), mon.ivs)
        assertEquals(listOf(100, 200, 300, 500, 500, 400), mon.evs)
        assertFalse(mon.shiny)
    }

    @Test
    fun readsInGameTradeTrainersAndPkMnInNicknames() {
        val b = ByteArray(0x8000)
        text(b, 0x2598, trainer, 11)
        gen1List(b, 0x2F2C, 6, 44, listOf(0x99 to "DUX"))
        val otAt = 0x2F2C + 2 + 6 + 6 * 44
        b[otAt] = 0x5D; b[otAt + 1] = 0x50
        val nickAt = otAt + 6 * 11
        b[nickAt + 3] = 0xE1.toByte(); b[nickAt + 4] = 0xE2.toByte(); b[nickAt + 5] = 0x50
        b[0x30C0] = 0; b[0x30C1] = 0xFF.toByte()
        gen1Sum(b)

        val mon = GameSave.read(b, "RED.sav")!!.party.single()
        assertEquals("TRAINER", mon.otName)
        assertEquals("DUXPKMN", mon.nickname)
        assertFalse(mon.ownCatch)
    }

    @Test
    fun asksWhichGameWhenTheFileNameDoesNotSay() {
        val b = ByteArray(0x8000)
        text(b, 0x2598, trainer, 11)
        b[0x2F2C] = 0; b[0x2F2D] = 0xFF.toByte()
        b[0x30C0] = 0; b[0x30C1] = 0xFF.toByte()
        gen1Sum(b)
        assertEquals(listOf(Game.RED, Game.BLUE, Game.YELLOW), GameSave.read(b, "save.sav")!!.gameOptions)

        b[0x29C3] = 0x54 // Yellow's starter
        gen1Sum(b)
        assertEquals(Game.YELLOW, GameSave.read(b, "save.sav")!!.game)
    }

    @Test
    fun ignoresBoxesTheGameHasNotSetUp() {
        val b = ByteArray(0x8000)
        text(b, 0x2598, trainer, 11)
        b[0x2F2C] = 0; b[0x2F2D] = 0xFF.toByte()
        b[0x30C0] = 0; b[0x30C1] = 0xFF.toByte()
        gen1List(b, 0x4000 + 0x462, 20, 33, listOf(0x99 to "GHOST"))
        gen1Sum(b)
        assertTrue(GameSave.read(b, "RED.sav")!!.boxes.isEmpty())
    }

    @Test
    fun blankCartridgeMemoryIsNotASave() {
        assertNull(GameSave.read(ByteArray(0x8000), "POKEMON RED.sav"))
        assertNull(GameSave.read(ByteArray(0x8000) { 0xFF.toByte() }, "POKEMON RED.sav"))
        assertTrue(Gen12Save.isCartridgeSized(ByteArray(32816)))
        assertFalse(Gen12Save.isCartridgeSized(ByteArray(131072)))
    }

    @Test
    fun readsACrystalSave() {
        val b = ByteArray(0x8000)
        be16(b, 0x2009, id)
        text(b, 0x200B, "KRIS", 11)
        be16(b, 0x2052, 7); b[0x2054] = 15
        b[0x23E5] = 0xFF.toByte(); b[0x23E6] = 0b0000_0011 // All of Johto's badges and two of Kanto's
        b[0x2A27] = 0x80.toByte() // Number 8, Wartortle
        for (box in 0 until 14) text(b, 0x2703 + box * 9, "BOX${box + 1}", 9)
        // 158 Totodile, shiny; an Egg, listed as 0xFD; a traded Mareep in box 8.
        gen2List(b, 0x2865, 6, 48, listOf(Mon2(158, 158, "TOTO", "KRIS", id, 0xAAAA), Mon2(0xFD, 25, "EGG", "KRIS", id, 0x1234)))
        b[0x2700] = 3
        gen2List(b, 0x2D10, 20, 32, listOf(Mon2(179, 179, "MAREEP", "GOLD", 999, 0x1234)))
        gen2List(b, 0x6000, 20, 32, listOf(Mon2(152, 152, "LEAFY", "KRIS", id, 0x1234)))
        gen2Sum(b, 0x2B82, 0x2D0D)

        val save = GameSave.read(b, "Crystal.sav")!!
        assertEquals(Game.CRYSTAL, save.game)
        assertEquals("KRIS", save.trainerName)
        assertEquals(7 * 60 + 15, save.playMinutes)
        assertEquals(10, save.badges.countOneBits())
        assertEquals(setOf(8), save.owned)
        assertEquals("BOX1", save.boxNames[0])

        val (toto, egg) = save.party
        assertEquals(158, toto.speciesId)
        assertTrue(toto.shiny)
        assertEquals(0xA0L, toto.pid)
        assertEquals(5, toto.heldItem)
        assertEquals(9, toto.metLevel)
        assertEquals(46, toto.metLocation)
        assertEquals(2, toto.metTime) // Caught in the day
        assertTrue(toto.ownCatch)
        assertTrue(egg.egg)
        assertEquals(25, egg.speciesId)
        assertFalse(egg.shiny)

        assertEquals(listOf(179 to 4, 152 to 8), save.boxes.map { it.speciesId to it.box })
        assertFalse(save.boxes[0].ownCatch)
    }

    @Test
    fun goldAndSilverKeepTheirOwnLayoutAndAskWhichGame() {
        val b = ByteArray(0x8000)
        be16(b, 0x2009, id)
        text(b, 0x200B, "GOLD", 11)
        be16(b, 0x2053, 2); b[0x2055] = 5
        b[0x23E4] = 0b0000_0111; b[0x23E5] = 0b1000_0000.toByte() // Three of Johto's badges and Kanto's last
        gen2List(b, 0x288A, 6, 48, listOf(Mon2(155, 155, "CYNDA", "GOLD", id, 0x1234)))
        b[0x2D6C] = 0; b[0x2D6D] = 0xFF.toByte()
        gen2Sum(b, 0x2D68, 0x2D69)

        val unknown = GameSave.read(b, "save.sav")!!
        assertEquals(listOf(Game.GOLD, Game.SILVER), unknown.gameOptions)
        assertEquals(125, unknown.playMinutes)
        assertEquals(0b1000_0000_0000_0111, unknown.badges)
        val named = GameSave.read(b, "POKEMON_SLV_AAXE-0.sav")!!
        assertEquals(Game.SILVER, named.game)
        assertTrue(named.gameOptions.isEmpty())
        assertEquals(0L, named.party.single().metLevel.toLong())
        assertEquals(0, named.party.single().metTime)
    }

    @Test
    fun everyGeneration1IndexMapsToADifferentPokemon() {
        val national = (1..255).mapNotNull { Gen1Species.national(it) }
        assertEquals((1..151).toList(), national.sorted())
        assertEquals(112, Gen1Species.national(1))
        assertEquals(25, Gen1Species.national(0x54))
        assertEquals(71, Gen1Species.national(0xBE))
        assertNull(Gen1Species.national(0x1F))
        assertNull(Gen1Species.national(0))
    }

    @Test
    fun readsJapaneseCrystal() {
        val b = ByteArray(0x10000)
        b[0x2009] = 0x22; b[0x200A] = 0x0B
        // クリス, with リ written as the games write it, on り's tile.
        val name = intArrayOf(0x87, 0xD8, 0x8C, 0x50)
        name.forEachIndexed { i, c -> b[0x200B + i] = c.toByte() }
        b[0x2035] = 1; b[0x2036] = 2
        b[0x23C7] = 0b0000_0111
        // Totodile in the party, and a Rattata in the seventh box, the first in the second memory bank.
        b[0x281A] = 1; b[0x281B] = 158.toByte(); b[0x281C] = 0xFF.toByte()
        b[0x2822] = 158.toByte(); b[0x2828] = 0x22; b[0x2829] = 0x0B
        name.forEachIndexed { i, c -> b[0x2822 + 48 * 6 + i] = c.toByte() }
        b[0x2D10] = 0; b[0x2D11] = 0xFF.toByte()
        for (box in 0 until 9) { val at = if (box < 6) 0x4000 + box * 0x54A else 0x6000 + (box - 6) * 0x54A; b[at + 1] = 0xFF.toByte() }
        b[0x6000] = 1; b[0x6001] = 19; b[0x6002] = 0xFF.toByte()
        b[0x6020] = 19
        b[0x26E2] = 0
        var sum = 0
        for (i in 0x2009..0x2AE1) sum += b[i].toInt() and 0xFF
        b[0x2D0D] = sum.toByte(); b[0x2D0E] = (sum shr 8).toByte()

        val save = GameSave.read(b)!!
        assertEquals(Game.CRYSTAL, save.game)
        assertEquals("クリス", save.trainerName)
        assertEquals(62, save.playMinutes)
        assertEquals(3, save.badges.countOneBits())
        assertEquals(158, save.party.single().speciesId)
        assertTrue(save.party.single().ownCatch)
        assertEquals(1, save.party.single().language)
        assertEquals(7, save.boxes.single().box)
        assertEquals(19, save.boxes.single().speciesId)
        assertEquals(9, save.boxNames.size)
    }

    @Test
    fun itemAndPlaceIndexesComeFromTheSeed() {
        val dex = Dex(Json { ignoreUnknownKeys = true }.decodeFromString(Seed.serializer(), File("src/main/assets/seed/dex.json").readText()))
        assertEquals("leftovers", dex.saveItemKey(2, 0x92))
        assertEquals("leftovers", dex.saveItemKey(4, 234))
        assertEquals("poke-ball", dex.saveItemKey(4, 4))
        assertEquals("New Bark Town", dex.savePlaceName(2, 1))
        assertEquals("New Bark Town", dex.savePlaceName(4, 126))
        assertEquals("cherish-ball", SAVE_BALLS[15])
        assertEquals(Game.HEARTGOLD, saveOrigin(7))
    }

    private fun gen1List(b: ByteArray, at: Int, capacity: Int, size: Int, mons: List<Pair<Int, String>>) {
        b[at] = mons.size.toByte()
        mons.forEachIndexed { i, (species, _) -> b[at + 1 + i] = species.toByte() }
        b[at + 1 + mons.size] = 0xFF.toByte()
        val monsAt = at + 2 + capacity
        mons.forEachIndexed { i, (species, nick) ->
            val m = monsAt + i * size
            b[m] = species.toByte()
            b[m + 8] = 33; b[m + 9] = 45
            be16(b, m + 12, id)
            b[m + 15] = 0x03; b[m + 16] = 0xE8.toByte() // 1000 experience
            for (s in 0 until 5) be16(b, m + 17 + s * 2, (s + 1) * 100)
            be16(b, m + 27, 0xABCD)
            text(b, monsAt + capacity * size + i * 11, trainer, 11)
            text(b, monsAt + capacity * size + capacity * 11 + i * 11, nick, 11)
        }
    }

    private class Mon2(val listed: Int, val species: Int, val nick: String, val ot: String, val otId: Int, val dvs: Int)

    private fun gen2List(b: ByteArray, at: Int, capacity: Int, size: Int, mons: List<Mon2>) {
        b[at] = mons.size.toByte()
        mons.forEachIndexed { i, mon -> b[at + 1 + i] = mon.listed.toByte() }
        b[at + 1 + mons.size] = 0xFF.toByte()
        val monsAt = at + 2 + capacity
        mons.forEachIndexed { i, mon ->
            val m = monsAt + i * size
            b[m] = mon.species.toByte()
            b[m + 1] = 5
            b[m + 2] = 10
            be16(b, m + 6, mon.otId)
            b[m + 10] = 100
            be16(b, m + 21, mon.dvs)
            b[m + 27] = 70
            b[m + 29] = (9 or (2 shl 6)).toByte(); b[m + 30] = 46
            text(b, monsAt + capacity * size + i * 11, mon.ot, 11)
            text(b, monsAt + capacity * size + capacity * 11 + i * 11, mon.nick, 11)
        }
    }

    private fun gen1Sum(b: ByteArray) {
        b[0x3523] = (0x2598 until 0x3523).sumOf { b[it].toInt() and 0xFF }.inv().toByte()
    }

    private fun gen2Sum(b: ByteArray, end: Int, at: Int) {
        val sum = (0x2009..end).sumOf { b[it].toInt() and 0xFF } and 0xFFFF
        b[at] = sum.toByte(); b[at + 1] = (sum shr 8).toByte()
    }

    private fun be16(b: ByteArray, at: Int, v: Int) { b[at] = (v shr 8).toByte(); b[at + 1] = v.toByte() }

    private fun text(b: ByteArray, at: Int, s: String, length: Int) {
        for (i in 0 until length) b[at + i] = 0x50
        s.forEachIndexed { i, c ->
            b[at + i] = when (c) {
                in 'A'..'Z' -> 0x80 + (c - 'A')
                in '0'..'9' -> 0xF6 + (c - '0')
                else -> 0x7F
            }.toByte()
        }
    }
}
