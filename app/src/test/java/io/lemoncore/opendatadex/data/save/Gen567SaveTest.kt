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
import java.time.LocalDate

class Gen567SaveTest {
    private val tid = 12345
    private val sid = 54321
    private val fullId = tid.toLong() or (sid.toLong() shl 16)

    @Test
    fun readsBlack() {
        val save = GameSave.read(fakeBlack())!!
        assertEquals(Game.BLACK, save.game)
        assertEquals("Lynx", save.trainerName)
        assertEquals(tid, save.trainerId)
        assertEquals(99 * 60 + 5, save.playMinutes)
        assertEquals(5, save.badges.countOneBits())
        assertTrue(save.nationalDex)
        assertEquals(setOf(1, 649), save.owned)
        assertEquals(setOf(1, 2, 649), save.seen)
        assertEquals("Oak", save.boxNames.first())
        val mon = save.party.single()
        assertEquals(479, mon.speciesId)
        assertTrue(mon.ownCatch)
        assertEquals(2, mon.form)
        assertEquals(2, mon.gender)
        assertEquals(10, mon.nature)
        assertEquals(HIDDEN_ABILITY_SLOT, mon.abilitySlot)
        assertEquals(Game.BLACK, saveOrigin(mon.originGame))
    }

    @Test
    fun anEmulatorsBlackFileHasAFooterAfterTheSave() {
        assertEquals(479, GameSave.read(fakeBlack() + ByteArray(122))!!.party.single().speciesId)
    }

    @Test
    fun aDsSaveWithABadChecksumTableIsNoSave() {
        val bytes = fakeBlack()
        bytes[0x23F00] = 1
        assertNull(GameSave.read(bytes))
    }

    @Test
    fun readsX() {
        val save = GameSave.read(fake3ds(0x65600, 0x14000, 0x48, 0x14200, 24))!!
        assertEquals(Game.X, save.game)
        assertEquals(tid, save.trainerId)
        assertEquals(sid, save.secretId)
        val mon = save.party.single()
        assertEquals(26, mon.speciesId)
        assertEquals("Nido♂", mon.nickname)
        assertTrue(mon.ownCatch)
        assertEquals(1, mon.form)
        assertEquals(1, mon.gender)
        assertEquals(20, mon.nature)
        assertEquals(1, mon.abilitySlot)
        assertEquals(20170921, mon.metDate)
    }

    @Test
    fun aShinyInGenerationSixHasTwiceTheRoom() {
        assertTrue(GameSave.read(fake3ds(0x65600, 0x14000, 0x48, 0x14200, 24, shinyXor = 15))!!.party.single().shiny)
        assertFalse(GameSave.read(fake3ds(0x65600, 0x14000, 0x48, 0x14200, 24, shinyXor = 16))!!.party.single().shiny)
    }

    @Test
    fun theDayXAndYStartedComesFromTheirClockBlock() {
        val bytes = fake3ds(0x65600, 0x14000, 0x48, 0x14200, 24)
        putU32(bytes, 0x1200 + 0x18, 559_351_764)
        assertEquals(LocalDate.of(2017, 9, 21), consoleDate(GameSave.read(bytes)!!.startedAt!!))
    }

    @Test
    fun sunShowsSixDigitsOfTheWholeId() {
        val full = 1_234_567_890L
        val save = GameSave.read(fake3ds(0x6BE00, 0x1200, 0x38, 0x1400, 30, id = full))!!
        assertEquals(Game.SUN, save.game)
        assertEquals(567_890, save.trainerId)
        assertEquals(1234, save.secretId)
        assertFalse(save.nationalDex)
        assertTrue(save.party.single().ownCatch)
    }

    @Test
    fun ultraMoonHasItsOwnPlaces() {
        assertEquals(Game.ULTRA_MOON, GameSave.read(fake3ds(0x6CC00, 0x1400, 0x38, 0x1600, 33))!!.game)
    }

    @Test
    fun aFileTheSizeOfAThreeDsSaveWithNoBlockTableIsNoSave() {
        assertNull(GameSave.read(ByteArray(0x65600)))
    }

    @Test
    fun alolaHasNoBadgesToCount() {
        assertEquals(0, Game.SUN.badgeTotal)
        assertEquals(8, Game.X.badgeTotal)
        assertEquals(16, Game.CRYSTAL.badgeTotal)
    }

    @Test
    fun itemsAndPlacesFromLaterGenerationsMatchOdexData() {
        val dex = Dex(Json { ignoreUnknownKeys = true }.decodeFromString(Seed.serializer(), File("src/main/assets/seed/dex.json").readText()))
        assertEquals("master-ball", dex.saveItemKey(5, 1))
        assertEquals("blue-orb", dex.saveItemKey(6, 535))
        assertEquals("bright-powder", dex.saveItemKey(2, 3))
        assertNull(dex.saveItemKey(5, 0))
        assertEquals("Nuvema Town", dex.savePlaceName(5, 4))
        assertEquals("Aquacorde Town", dex.savePlaceName(6, 10))
        assertEquals("Route 1", dex.savePlaceName(7, 6))
        assertEquals("Twinleaf Town", dex.savePlaceName(4, 1))
        assertEquals("beast-ball", SAVE_BALLS[25])
        assertNull(dex.savePlaceName(5, 2_000))
    }

    /** A Black save with one Rotom in the party, five badges and a checksum table that passes. */
    private fun fakeBlack(): ByteArray {
        val b = ByteArray(0x80000)
        utf16(b, 4, "Oak", 0xFFFF)
        utf16(b, 0x19400 + 4, "Lynx", 0xFFFF)
        putU16(b, 0x19400 + 0x14, tid)
        putU16(b, 0x19400 + 0x16, sid)
        b[0x19400 + 0x1F] = 21
        putU16(b, 0x19400 + 0x24, 99)
        b[0x19400 + 0x26] = 5
        b[0x21200 + 4] = 0b0001_1111
        b[0x21600 + 4] = 1
        flag(b, 0x21600 + 8, 1); flag(b, 0x21600 + 8, 649)
        flag(b, 0x21600 + 0x5C, 1); flag(b, 0x21600 + 0x5C + 0x54, 2); flag(b, 0x21600 + 0x5C, 649)
        b[0x18E00 + 4] = 1
        pk5(b, 0x18E00 + 8)
        putU16(b, 0x23F9A, crc16(b, 0x23F00, 0x8C))
        return b
    }

    /** Writes one encrypted Rotom. A PID of 0 keeps its four parts in A, B, C, D order. */
    private fun pk5(b: ByteArray, at: Int) {
        val data = ByteArray(128)
        putU16(data, 0, 479)
        putU32(data, 4, fullId)
        data[32 + 0x18] = ((2 shl 3) or (2 shl 1)).toByte()
        data[32 + 0x19] = 10
        data[32 + 0x1A] = 1
        data[64 + 0x17] = 21
        utf16(data, 96, "Lynx", 0xFFFF)
        val sum = (0 until 64).sumOf { u16(data, it * 2) } and 0xFFFF
        putU16(b, at + 6, sum)
        encrypt(data, b, at + 8, sum)
    }

    /** A 3DS save with one Raichu in the party. An encryption constant of 0 keeps its four parts in A, B, C, D order. */
    private fun fake3ds(size: Int, status: Int, name: Int, party: Int, game: Int, id: Long = fullId, shinyXor: Int = 100): ByteArray {
        val b = ByteArray(size)
        putU32(b, size - 0x1F0, 0x42454546)
        putU32(b, status, id)
        b[status + 4] = game.toByte()
        utf16(b, status + name, "Dennis", 0)
        b[party + 6 * 260] = 1
        val data = ByteArray(224)
        fun put(offset: Int, value: Int) { data[offset - 8] = value.toByte() }
        putU16(data, 0x08 - 8, 26)
        putU32(data, 0x0C - 8, id)
        val low = (id and 0xFFFF).toInt() xor (id ushr 16).toInt() xor shinyXor
        putU32(data, 0x18 - 8, low.toLong())
        put(0x15, 2)
        put(0x1C, 20)
        put(0x1D, (1 shl 3) or (1 shl 1))
        utf16(data, 0x40 - 8, "Nido", 0)
        utf16(data, 0xB0 - 8, "Dennis", 0)
        put(0xD4, 17); put(0xD5, 9); put(0xD6, 21)
        put(0xDF, game)
        val sum = (0 until 112).sumOf { u16(data, it * 2) } and 0xFFFF
        putU16(b, party + 6, sum)
        encrypt(data, b, party + 8, 0)
        return b
    }

    private fun encrypt(data: ByteArray, b: ByteArray, at: Int, seed: Int) {
        var state = seed
        for (w in 0 until data.size / 2) {
            state = state * 0x41C64E6D + 0x6073
            putU16(b, at + w * 2, u16(data, w * 2) xor (state ushr 16))
        }
    }

    private fun flag(b: ByteArray, at: Int, species: Int) {
        val i = at + (species - 1) / 8
        b[i] = (b[i].toInt() or (1 shl ((species - 1) % 8))).toByte()
    }

    private fun utf16(b: ByteArray, at: Int, s: String, end: Int) {
        s.forEachIndexed { i, c -> putU16(b, at + i * 2, c.code) }
        putU16(b, at + s.length * 2, end)
    }

    private fun u16(b: ByteArray, at: Int) = (b[at].toInt() and 0xFF) or ((b[at + 1].toInt() and 0xFF) shl 8)
    private fun putU16(b: ByteArray, at: Int, v: Int) { b[at] = v.toByte(); b[at + 1] = (v shr 8).toByte() }
    private fun putU32(b: ByteArray, at: Int, v: Long) { putU16(b, at, (v and 0xFFFF).toInt()); putU16(b, at + 2, ((v ushr 16) and 0xFFFF).toInt()) }
}
